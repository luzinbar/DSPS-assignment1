import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class SQS {
    static private SqsClient sqs;

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

    Message recieveMessage(String q){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(q)
                .maxNumberOfMessages(1)
                .build();
        List<Message> messages = sqs.receiveMessage(receiveRequest).messages();
        if (messages.size() == 0)
            return null;
        return messages.get(0);
    }

    List<Message> recieveMessages(String q){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(q)
                .build();
        return sqs.receiveMessage(receiveRequest).messages(); /// only one msg TODO!
    }

    void sendMessage(String queueUrl, String msg){
        try {
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

    void waitForProcessDone(String appQ, String done){
        while(true){
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(appQ)
                    .waitTimeSeconds(20)
                    .build();

            ReceiveMessageResponse response =sqs.receiveMessage(receiveRequest);
            if(response.messages().contains(done))
                return;
            /*List<Message> msgList = sqs.receiveMessage(appQ).getMessages(); // TODO : delete old
            for (Message msg : msgList){
                if(msg.getBody().contains(done))
                    return;
            }*/
        }
    }

    public static void deleteMessage(String queueUrl, Message message) {
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

    String findQueue(String QName) {
        List<String> queuesList = listQueues(QName);
        for (String queue : queuesList) {
            return queue;
        }
        return null;
    }

}
