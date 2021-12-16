import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class EC2 {

    private Ec2Client ec2;
    private String ami = "ami-00e95a9222311e8ed";

    EC2() {
        Region region = Region.US_EAST_1;
        ec2 = Ec2Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(region)
                .build();
    }

    List<String> createInstances(String NameI, int count){
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(ami)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(count)
                .minCount(count)
                .userData(getUserData())
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        List<Instance> instances = response.instances();
        List<String> instanceIds = new ArrayList<String>();
        for (Instance instance: instances){
            instanceIds.add(instance.instanceId());
        }
        return instanceIds;
    }

    void terminateInstance(String instanceID) {
        try{
            TerminateInstancesRequest ti = TerminateInstancesRequest.builder()
                    .instanceIds(instanceID)
                    .build();

            TerminateInstancesResponse response = ec2.terminateInstances(ti);
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    String getUserData(){
        String userData =
                "#!/bin/bash\n"+
                "echo \"user data is running\n\""+
                "mkdir /home/ec2-user/.aws\n"+
                "echo [default] > /home/ec2-user/.aws/config\n"+
                "echo region=" + MainManager.REGION + ">> /home/ec2-user/.aws/config\n"+
                "echo [default] > /home/ec2-user/.aws/credentials\n"+
                "echo aws_access_key_id=" + MainManager.AWS_ACCESS_KEY_ID + " >> /home/ec2-user/.aws/credentials\n"+
                "echo aws_secret_access_key=" + MainManager.AWS_SECRET_ACCESS_KEY + " >> /home/ec2-user/.aws/credentials\n"+
                "echo aws_session_token=" + MainManager.AWS_SESSION_TOKEN + " >> /home/ec2-user/.aws/credentials\n"+
                "aws configure set AWS_ACCESS_KEY_ID " + MainManager.AWS_ACCESS_KEY_ID + "\n"+
                "aws configure set AWS_SECRET_ACCESS_KEY " + MainManager.AWS_SECRET_ACCESS_KEY + "\n"+
                "aws configure set AWS_SESSION_TOKEN " + MainManager.AWS_SESSION_TOKEN + "\n"+
                "export AWS_ACCESS_KEY_ID=" + MainManager.AWS_ACCESS_KEY_ID + "\n"+
                "export AWS_SECRET_ACCESS_KEY=" + MainManager.AWS_SECRET_ACCESS_KEY + "\n"+
                "export AWS_SESSION_TOKEN=" + MainManager.AWS_SESSION_TOKEN + "\n"+
                "aws s3 cp s3://eden.inbar.jars/worker.jar /home/ec2-user/\n"+
                "mkdir /home/ec2-user/pdf\n"+
                "mkdir /home/ec2-user/outputs\n"+
                "java -jar /home/ec2-user/worker.jar\n";
        return Base64.getEncoder().encodeToString(userData.getBytes(StandardCharsets.UTF_8));
    }


}
