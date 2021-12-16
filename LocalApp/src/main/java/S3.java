import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.*;


public class S3 {

    static private S3Client s3;

    S3(){
        Region region = Region.US_EAST_1;
        s3 = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String createBucket() {
        CreateBucketResponse resp = s3.createBucket(CreateBucketRequest
                .builder()
                .bucket("app-bucket-"+System.currentTimeMillis())
                .createBucketConfiguration(CreateBucketConfiguration.builder().build())
                .build());

        return resp.location().substring(1);
    }

    void uploadToS3(File file, String nameB) throws IOException {
        try {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(nameB)
                    .key("input-file")
                    .build(), RequestBody.fromFile(file));
        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    BufferedReader downloadFromS3(String appB) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key("summery-file")
                    .bucket(appB)
                    .build();

            ResponseInputStream<GetObjectResponse> res = s3.getObject(objectRequest);
            return new BufferedReader(new InputStreamReader(res));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
