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

    String getQueue(String QName) {
        List<String> queuesList = listQueues(QName);
        for (String queue : queuesList) {
            return queue;
        }
        return null;
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

}
