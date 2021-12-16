import software.amazon.awssdk.services.sqs.model.Message;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class worker {
    public static void main(String[] args) throws IOException, ParserConfigurationException {
        System.out.println("W - A NEW WORKER STARTS RUNNING");
        SQS sqs = new SQS();
        S3 s3 = new S3();
        String inputQ = sqs.getQueue("manager2WorkersQ");
        String outputQ = sqs.getQueue("workers2managerQ");
        System.out.println("W - conected to the relevant workers");

        while (true){
            Message msg = sqs.recieveMessage(inputQ);// TODO : assume 1 msg in the list !!!!!!!!!!!
            if (msg == null) {
                continue;
            }
            System.out.println("W - recieved a message");
            String msgBody = msg.body();
            if (msgBody.equals("terminate")) {
                System.out.println("W - recieved a terminate message");
                sqs.sendMessage(outputQ,"worker terminated");
                System.exit(0);
            }
            else {
                System.out.println("M CMM - recieved a new mission message");
                String outputMsg;
                String[] parts = msgBody.split("\t");
                // [0]: operation, [1]: url of PDF. [2]: app bucket, [3]: TimeStamp
                String pdfile = downloadFromUrl(parts[1]);
                System.out.println("W - download pdf");
                if (!pdfile.substring(0,5).equals("/home")){
                    outputMsg = "f\t"+parts[0]+"\t"+parts[1]+"\t"+parts[2]+"\t"+parts[3]+"\t"+pdfile;
                    System.out.println("W - failed with downloading fron s3");
                }
                else {
                    System.out.println("W - succeeded with downloading fron s3");
                    File file = new File(pdfile);
                    if (file == null) {
                        outputMsg = "f\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3]+ "\t"+"file can't be opened";
                        System.out.println("W - failed with open the downloaded file");
                    }
                    else {
                        System.out.println("W - succeeded with open the downloaded file");
                        String res;
                        switch (parts[0]) {
                            case "ToImage":
                                res = PDFfunctions.ToImage(file);
                                break;
                            case "ToHTML":
                                res = PDFfunctions.ToHTML(file);
                                break;
                            default: //ToText
                                res = PDFfunctions.ToText(file);
                        }
                        if (!res.substring(0, 5).equals("/home")) {
                            outputMsg = "f\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3]+"\t"+res;
                            System.out.println("W - failed with converting the file");
                        }
                        else {
                            System.out.println("W - succeeded with converting the file");
                            // upload to S3
                            s3.uploadToS3(res, parts[2], parts[3]);
                            System.out.println("W - uploaded to s3");
                            // notify the manager on the operation:
                            outputMsg = "h\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3];
                        }
                    }
                }
                sqs.sendMessage(outputQ,outputMsg);
                System.out.println("W - send to the manager a finished mission message");
                // remove the processed msg from sqs:
                sqs.deleteMessage(inputQ,msg);
                System.out.println("W - delete the input message");
            }
        }
    }

    static private String downloadFromUrl(String filePath) throws IOException{
        try {
            String pdfPath = "/home/ec2-user/pdf/pdfile.pdf";
            URL url = new URL(filePath);
            InputStream inputStream = url.openStream();
            ReadableByteChannel readableByteChannel = Channels.newChannel( url.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream( new File(pdfPath));
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
            readableByteChannel.close();
            return pdfPath;
        }catch (Exception e){
            return e.toString();
        }
    }

}



/* left:

1. terminate
2.
 */
