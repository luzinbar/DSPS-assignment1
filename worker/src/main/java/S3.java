import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class S3 {

    static private S3Client s3;

    S3(){
        Region region = Region.US_EAST_1;
        s3 = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    void uploadToS3(String file, String nameB, String TimeStamp) throws IOException {
        try {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(nameB)
                    .key(TimeStamp)
                    .build(), RequestBody.fromFile(new File(file)));
        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static byte[] getObjectFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytesArray;
    }
}
