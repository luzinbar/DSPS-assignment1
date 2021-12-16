import software.amazon.awssdk.services.sqs.model.Message;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiveMissionsManager implements Runnable {
    private SQS sqs;
    private EC2 ec2 = new EC2();
    private S3 s3 = new S3();
    private ConcurrentHashMap appsHandlers;
    private int workersLimit = 8;
    private AtomicBoolean terminate;
    private AtomicInteger n;
    private String workers2managerQ;
    private AtomicInteger notProcessedMissionsAmount;
    private AtomicInteger currNumOfWorkers;
    private ConcurrentLinkedQueue<String> workers;

    ReceiveMissionsManager(ConcurrentHashMap appHandlers, String workers2managerQ, SQS sqs, AtomicBoolean terminate,
                           AtomicInteger n, AtomicInteger notProcessedMissionsAmount, AtomicInteger currNumOfWorker, ConcurrentLinkedQueue<String> workers){
        this.appsHandlers = appHandlers;
        this.workers2managerQ = workers2managerQ;
        this.sqs = sqs;
        this.terminate = terminate;
        this.n = n;
        this.notProcessedMissionsAmount = notProcessedMissionsAmount;
        this.currNumOfWorkers = currNumOfWorker;
        this.workers = workers;
    }

     public void run() {
        System.out.println("M RMM - RECEIVE MISSION MANAGER STARTS");
         try {
             Thread.sleep(5000);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
         System.out.println("M RMM - after sleep");
         while (!terminate.get()) {
            // ------------------------------------------------------- update workers amount --------------------------------------------------------
           if (n.get() == 0) {
               continue;
           }
            if (currNumOfWorkers.get() < workersLimit) {
                int newWorkersAmount =(int) Math.ceil(notProcessedMissionsAmount.get() / n.get()) - currNumOfWorkers.get();
                if (newWorkersAmount + currNumOfWorkers.get() > workersLimit) {
                    newWorkersAmount = workersLimit - currNumOfWorkers.get();
                }
                if (newWorkersAmount > 0) {
                    System.out.println("M RMM - creating workers:"+newWorkersAmount);
                    List<String> workerIds = ec2.createInstances("worker", newWorkersAmount);
                    System.out.println("M RMM - a new worker created");
                    for (String workerId : workerIds) {
                        workers.add(workerId);
                        currNumOfWorkers.addAndGet(1);
                    }
                }
            }

            List<Message> messages = sqs.recieveMessages(workers2managerQ);
            for (Message message : messages) {
                System.out.println("M RMM - recieve took a msg from worker");
                String[] msgBody = message.body().split("\t");
                String appB = msgBody[3];
                AppHandler appHandler = (AppHandler) appsHandlers.get(appB);
                appHandler.addMission(message.body());
                System.out.println("M RMM - put a new msg for app handler");
                sqs.deleteMessage(workers2managerQ,message);
            }
        }
         System.out.println("M RMM - RECEIVE MISSION MANAGER FINISHED");
     }
}
