import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import sun.misc.ObjectInputFilter;

import java.io.*;
import java.util.List;

public class LocalApp {
    static SQS sqs = new SQS();
    static S3 s3 = new S3();
    static EC2 ec2 = new EC2();
    static String manager2appQ = null;
    static String app2ManagerQ = null;
    static String managerId = null;
    static String appB = null;
    static boolean terminate = false;

    static final String REGION = "us-east-1";
    static final String AWS_ACCESS_KEY_ID = "ASIARPDHIHFCZII2TDLW";
    static final String AWS_SECRET_ACCESS_KEY = "UlTJNqLvBHXkjjWd9Scs4p/v3PGGPzxZbwDRAJaR";
    static final String AWS_SESSION_TOKEN = "FwoGZXIvYXdzEJ///////////wEaDHW5EgW3YrnoL3KVqiLGAfK8QJ5LACaM7wM/D9JzCdNEjM0vTwuraHrZMoinvTa451R6vq7Jmlv7CI6Bflsfq9JMrXb+68H8RdtQ20gW4phmaDm4vPiGhe/CH6VjXwMj7LtPrjnx2lqzGwdt3z84AZo3eIxPFW1a4kUr3u6YDvaWUsG1tNll0utW5eprpYXnFFo8ceqUjstTFmz9PggFEK/dkrGeUvB9gDUKr0TvHv76jBK1DnzuSvcgYEhAItc3TTpJUVcRoGMOPkf40KEIqxEA8Cn+YSia3eeNBjItEjQSLrdeUstZOw+gMluzn+vZ6Ei2EO5KIfoBVWwdbbyN0ar/YuPoBBZ40INf";
    public static void main(String[] args) throws IOException, InterruptedException {

        // Environment variables for Default Credential Provider Chain:
        System.setProperty("aws.accessKeyId",AWS_ACCESS_KEY_ID);
        System.setProperty("aws.secretKey",AWS_SECRET_ACCESS_KEY);
        System.setProperty("aws.sessionToken",AWS_SESSION_TOKEN);

        if (args.length == 4){
            terminate = true;
        }
        managerId = ec2.checkManagerActivity();
        if (managerId == null) // manager isn't active : start the manager node
            managerId = initManager();
        else {
            findRelevantQueues(); // manager is active: find it's id
        }
        initApp(args[0]);
        System.out.println(appB+"\t"+args[2]);
        sqs.sendMessage(app2ManagerQ, appB+"\t"+args[2]);
        waitForSummeryFile();
        BufferedReader summeryFile = s3.downloadFromS3(appB); // take the response from S3
        createHtml(summeryFile,args[1]);
        if (terminate){
            sqs.sendMessage(app2ManagerQ,"terminate");
            while (! sqs.waitForMessage(manager2appQ,"manager terminated"));
            sqs.deleteQueue("manager2appQ");
            ec2.terminateInstance(managerId);
        }
    }

    static String initManager(){
        managerId = ec2.createInstance("managerI");
        app2ManagerQ = sqs.createQ("app2ManagerQ"); // create the application's queue
        //sqs.listQueuesFilter(app2ManagerQ);
        manager2appQ = sqs.createQ("manager2appQ");
        //sqs.listQueuesFilter(manager2appQ);
        return managerId;
    }

    static void findRelevantQueues(){
        app2ManagerQ = sqs.findRelevantQueues("app2ManagerQ");
        manager2appQ = sqs.findRelevantQueues("manager2appQ");
    }

    static void initApp(String inputFilePath) throws IOException {
        appB = s3.createBucket();  // create the application's bucket - different bucket for each app
        File inputFile = new File(inputFilePath);
        s3.uploadToS3(inputFile,appB);  // upload the file to S3
    }

    static void createHtml(BufferedReader summeryFile, String output) throws IOException {
        BufferedWriter buffer = new BufferedWriter(new FileWriter(output + ".html"));
        String htmlString =
                "<!DOCTYPE html>\n"+
                "<html>\n"+
                "<style>\n" +
                "table {\n" +
                "  border:1px solid black;\n" +
                "}\n" +
                "table {\n" +
                "  width: 100%;\n" +
                "}\n" +
                "h1{text-align: center;}\n" +
                "th, td {\n" +
                "  text-align: left;\n" +
                "  padding: 8px;\n" +
                "}\n"+
                "td {\n" +
                "font-weight:normal"+
                "}\n"+
                "tr:nth-child(even) {\n" +
                "  background-color: #D6EEEE;\n" +
                "}\n" +
                "</style><body>" + "<h1>Output File</h1>\n"+
               "<table style=\"width:100%\"><tr><th>Operation</th><th>Input File</th>"+
                "<th>Ouput File</th><th>Description of Exception</th></tr>";
        String line = summeryFile.readLine();
        while (line != null) {
            htmlString = htmlString + line ;
            line = summeryFile.readLine();
        }
            htmlString += "</table></body></html>";
        buffer.write(htmlString);
        buffer.close();
    }

    static void waitForSummeryFile() throws InterruptedException {
        while (true){
            for (int i = 0 ; i < 10000 ; i++){
                if (sqs.waitForMessage(manager2appQ,"done\t"+appB))
                    return;
            }
            Thread.sleep(1000);
            if (ec2.checkManagerActivity() == null){
                managerId = initManager();
            }
        }

    }

}
