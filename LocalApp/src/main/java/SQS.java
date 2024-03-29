import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class SQS {
    private SqsClient sqs;

    SQS(){
        sqs = SqsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
    }

    String createQ (String QName){
        try {
            CreateQueueRequest req = CreateQueueRequest.builder()
                    .queueName(QName)
                    .build();
            String queueUrl = sqs.createQueue(req).queueUrl();
            return queueUrl;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> listQueues(String prefix) {
        try {
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(prefix).build();
            ListQueuesResponse listQueuesResponse = sqs.listQueues(listQueuesRequest);
            for (String url : listQueuesResponse.queueUrls()) {
                return listQueuesResponse.queueUrls();
            }
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }

     String findRelevantQueues(String QName) {
         List<String> queuesList = listQueues(QName);
         for (String queue : queuesList) { //TODO : is the name part of the url ? if not different identifie
             return queue;
         }
         return null;
     }

    void sendMessage(String queueUrl, String msg){
        try {
            /*GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(q)
                    .build();
            String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();*/
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(msg)
                    .build();

            sqs.sendMessage(sendMsgRequest);

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

     void deleteMessage(String queueUrl, Message message) {
        try {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqs.deleteMessage(deleteMessageRequest);
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    Boolean waitForMessage(String appQ, String done){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(appQ)
                .waitTimeSeconds(20)
                .build();

        List<Message> messages =sqs.receiveMessage(receiveRequest).messages();
        if (messages.size() == 0)
            return false;
        if(messages.get(0).body().equals(done)) {
            deleteMessage("manager2appQ", messages.get(0));
            return true;
        }
        return false;
    }

    void deleteQueue(String queueName) {
        try {
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();
            String queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();
            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();
            sqs.deleteQueue(deleteQueueRequest);
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
