import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.*;
import java.util.List;

public class S3 {

    static private S3Client s3;

    S3(){
        Region region = Region.US_EAST_1;
        s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    BufferedReader downloadFromS3(String appB) throws IOException {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(appB)
                    .key("input-file")
                    .build();
            ResponseInputStream<GetObjectResponse> res = s3.getObject(getObjectRequest);
            return new BufferedReader(new InputStreamReader(res));
    }

    String uploadToS3(String file, String nameB) throws IOException {
        try{
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(nameB)
                    .key("summery-file")
                    .build();

            PutObjectResponse response = s3.putObject(putOb,
                    RequestBody.fromBytes(getObjectFile(file)));

            return response.eTag();

        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
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

    public void deleteAllBuckets() {
        try {
            List<Bucket> listOfBuckets = s3.listBuckets().buckets();
            for (Bucket b : listOfBuckets) {
                // To delete a bucket, all the objects in the bucket must be deleted first
                ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(b.name()).build();
                ListObjectsV2Response listObjectsV2Response;

                do {
                    listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
                    for (S3Object s3Object : listObjectsV2Response.contents()) {
                        s3.deleteObject(DeleteObjectRequest.builder()
                                .bucket(b.name())
                                .key(s3Object.key())
                                .build());
                    }

                    listObjectsV2Request = ListObjectsV2Request.builder().bucket(b.name())
                            .continuationToken(listObjectsV2Response.nextContinuationToken())
                            .build();

                } while (listObjectsV2Response.isTruncated());
                // snippet-end:[s3.java2.s3_bucket_ops.delete_bucket]

                DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(b.name()).build();
                s3.deleteBucket(deleteBucketRequest);
            }
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
