import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.fit.pdfdom.PDFDomTree;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;


public class PDFfunctions {
    //TODO - arrange comments

   public static String ToImage(File file) {
        /*We need to use PDFRenderer, in order to render PDF as a BufferedImage. Also, each page of the PDF file needs to be rendered separately.
Finally, we use ImageIOUtil, from Apache PDFBox Tools, to write an image, with the extension that we specify. Possible file formats are jpeg, jpg, gif, tiff or png.
Note that Apache PDFBox is an advanced tool – we can create our own PDF files from scratch, fill forms inside PDF file, sign and/or encrypt the PDF file.*/
       try {
           System.out.println("W ToImage - started converting");
           String output = "/home/ec2-user/outputs/newFormat.png";
           PDDocument doc = PDDocument.load(file);
           PDFRenderer pdfRenderer = new PDFRenderer(doc);
           BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300, ImageType.RGB);
           ImageIOUtil.writeImage(bim, output, 300);
           System.out.println("W ToImage - finished converting");
           return output;
       }
       catch (IOException e) {
           System.out.println("W ToImage - exception in converting");
           return e.getStackTrace().toString();
       }
    }

   public static String ToText(File file) {
        /*In order to read a PDF file, we use PDFParser, with an “r” (read) option. Moreover, we need to use the parser.parse() method that will cause the PDF to be parsed as a stream and populated into the COSDocument object.
Let's take a look at the extracting text part:
In the first line, we'll save COSDocument inside the cosDoc variable. It will be then used to construct PDocument, which is the in-memory representation of the PDF document. Finally, we will use PDFTextStripper to return the raw text of a document. After all of those operations, we'll need to use close() method to close all the used streams.
In the last part, we'll save text into the newly created file using the simple Java PrintWriter:
         */
       try {
           System.out.println("W ToText - started converting");
           PDFParser parser = new PDFParser(new RandomAccessFile(file,"r"));
           parser.parse();
           COSDocument cosDoc = parser.getDocument();
           PDFTextStripper pdfStripper = new PDFTextStripper();
           pdfStripper.setStartPage(1); // @TODO : added show to Inbar !!!!!!
           pdfStripper.setEndPage(1);
           PDDocument pdDoc = new PDDocument(cosDoc);
           String parsedText = pdfStripper.getText(pdDoc);
           String output = "/home/ec2-user/outputs/newFormat.txt";
           PrintWriter pw = new PrintWriter(output);
           pw.print(parsedText);
           pw.close();
           pdDoc.close();
           System.out.println("W ToText - finished converting");
           return output;
       } catch (IOException e){
           System.out.println("W ToText - excepting in converting");
           return e.getStackTrace().toString();
       }
    }

    public static String ToHTML(File file) {
       try {
           System.out.println("W ToHTML - started converting");
           PDDocument pdf = PDDocument.load(file);
           String output = "/home/ec2-user/outputs/newFormat.html";
           PDPage pdPage = pdf.getPage(0);
           PDDocument onePagePdf = new PDDocument();
           onePagePdf.addPage(pdPage);
           Writer writerOutput = new PrintWriter(output, "utf-8");
           new PDFDomTree().writeText(onePagePdf,writerOutput);
           System.out.println("W ToHTML - finished converting");
           return output;
       } catch (IOException | ParserConfigurationException e) {
           System.out.println("W ToHTML - exception in converting");
           return e.getStackTrace().toString();
       }
    }
        /*we load the PDF file, using the load API from PDFBox. With the PDF loaded, we use the parser to parse the file and write to output specified by java.io.Writer.
Note that converting PDF to HTML is never a 100%, pixel-to-pixel result. The results depend on the complexity and the structure of the particular PDF file.*/



}
