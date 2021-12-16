import software.amazon.awssdk.services.sqs.model.Message;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MissionsMaster implements Runnable {
    private ConcurrentHashMap<String,AppHandler> appHandlers;
    private String workersQ;
    private SQS sqs;
    private AtomicBoolean terminate;

    MissionsMaster(ConcurrentHashMap appHandlers, String workersQ, SQS sqs, AtomicBoolean terminate){
        this.appHandlers = appHandlers;
        this.workersQ = workersQ;
        this.sqs = sqs;
    }

    @Override
    public void run() {
        while (!terminate.get()) {
            List<Message> messages = sqs.recieveMessages(workersQ);
            for (Message message : messages) {
                String[] msgBody = message.body().split("\t");
                String appB = msgBody[3];
                appHandlers.get(appB).addMission(message);
                sqs.deleteMessage(workersQ,message);
            }
        }
    }
}
