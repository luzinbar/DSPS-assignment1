import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EC2 {

    private Ec2Client ec2;
    private String ami = "ami-00e95a9222311e8ed";

    EC2(){
            ec2 = Ec2Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    String checkManagerActivity() {
        try {
            Filter filter = Filter.builder()
                    .build();

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .filters(filter)
                    .build();
            DescribeInstancesResponse response = ec2.describeInstances(request);
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    for (Tag tag : instance.tags()) {
                        if (tag.value().equals("manager") & instance.state().name().toString().equals("running")) {
                            return instance.instanceId();
                        }
                    }
                }
            }
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }

    String createInstance(String NameI){
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(ami)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .userData(getUserData())
                .keyName("eden_inbar")
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();
        Tag tag = Tag.builder()
                .key("manager")
                .value("manager")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();
        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s\n",
                    instanceId, ami);
            return instanceId;
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
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

    static String getUserData(){
        String userData =
        "#!/bin/bash\n"+
        "echo \"user data is running\n\""+
        "mkdir /home/ec2-user/.aws\n"+
        "echo [default] > /home/ec2-user/.aws/config\n"+
        "echo region=" + LocalApp.REGION + ">> /home/ec2-user/.aws/config\n"+
        "echo [default] > /home/ec2-user/.aws/credentials\n"+
        "echo aws_access_key_id=" + LocalApp.AWS_ACCESS_KEY_ID + " >> /home/ec2-user/.aws/credentials\n"+
        "echo aws_secret_access_key=" + LocalApp.AWS_SECRET_ACCESS_KEY + " >> /home/ec2-user/.aws/credentials\n"+
        "echo aws_session_token=" + LocalApp.AWS_SESSION_TOKEN + " >> /home/ec2-user/.aws/credentials\n"+
        "aws configure set AWS_ACCESS_KEY_ID " + LocalApp.AWS_ACCESS_KEY_ID + "\n"+
        "aws configure set AWS_SECRET_ACCESS_KEY " + LocalApp.AWS_SECRET_ACCESS_KEY + "\n"+
        "aws configure set AWS_SESSION_TOKEN " + LocalApp.AWS_SESSION_TOKEN + "\n"+
        "export AWS_ACCESS_KEY_ID=" + LocalApp.AWS_ACCESS_KEY_ID + "\n"+
        "export AWS_SECRET_ACCESS_KEY=" + LocalApp.AWS_SECRET_ACCESS_KEY + "\n"+
        "export AWS_SESSION_TOKEN=" + LocalApp.AWS_SESSION_TOKEN + "\n"+
        "aws s3 cp s3://eden.inbar.jars/manager.jar /home/ec2-user/\n"+
        "java -jar /home/ec2-user/manager.jar\n";
        return Base64.getEncoder().encodeToString(userData.getBytes(StandardCharsets.UTF_8));
    }


}



