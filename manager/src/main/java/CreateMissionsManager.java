import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateMissionsManager {
    private SQS sqs;
    private EC2 ec2 = new EC2();
    private S3 s3 = new S3();
    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private String manager2appQ;
    private String app2managerQ;
    private String manager2WorkersQ; // through this q, manager receives new app requests
    private String workers2managerQ;
    private ConcurrentHashMap appsHandlers;
    private ConcurrentLinkedQueue<String> workers;
    private AtomicBoolean terminate;
    private AtomicInteger n;
    private AtomicInteger notProcessedMissionsAmount;
    private AtomicInteger currNumOfWorkers;

    CreateMissionsManager(SQS sqs, ConcurrentHashMap appsHandlers, String workers2managerQ, AtomicBoolean terminate,
                          AtomicInteger n, AtomicInteger notProcessedMissionsAmount,AtomicInteger currNumOfWorkers, ConcurrentLinkedQueue<String> workers){
        this.sqs = sqs;
        this.appsHandlers = appsHandlers;
        this.workers2managerQ = workers2managerQ;
        this.manager2appQ = sqs.findQueue("manager2appQ");
        this.app2managerQ = sqs.findQueue("app2ManagerQ");
        this.manager2WorkersQ = sqs.createQ("manager2WorkersQ");
        this.terminate = terminate;
        this.n = n;
        this.notProcessedMissionsAmount = notProcessedMissionsAmount;
        this.currNumOfWorkers = currNumOfWorkers;
        this.workers = workers;
    }

    public void createMissions() throws InterruptedException {
        System.out.println("M CMM - CREATE MISSION MANAGER STARTS");
        while(!terminate.get()){
            // ---------------------------------------------------------- new app request -----------------------------------------------------------
            String appB = "";
            Thread.sleep(1000);
            Message msg = sqs.recieveMessage(app2managerQ);
            if (msg == null){
                continue;
            }
            System.out.println("M CMM - recieved a message");
            sqs.deleteMessage(app2managerQ, msg);
            if (msg.body().equals("terminate")){
                System.out.println("M CMM - recieved a terminate message");
                terminate.set(true);
                sqs.sendMessage(manager2WorkersQ,"terminate");
            }
            else {
                System.out.println("M CMM - recieved a new file message");
                String[] body = msg.body().split("\t");
                appB = body[0];
                n.set(Integer.parseInt(body[1]));
                // new app handler:
                AppHandler apphandler = new AppHandler(s3, sqs, manager2WorkersQ, workers2managerQ, manager2appQ, appB, notProcessedMissionsAmount, appsHandlers);
                appsHandlers.putIfAbsent(appB,apphandler);
                threadPool.execute(apphandler);
                System.out.println("M CMM - created a new app handler and thread for a new local app");
            }
        }
        System.out.println("M CMM - START TERMINATE");
        System.out.println("M CMM - waiting for app handlers to finish");
        while (!appsHandlers.isEmpty()){
            Thread.sleep(5000);
        }
        System.out.println("M CMM - start killing workers");
        while (currNumOfWorkers.get() > 0) {
            List<Message> messages = sqs.recieveMessages(workers2managerQ);
            for (Message message : messages) {
                if (message.body().equals("worker terminated")) {
                    currNumOfWorkers.decrementAndGet();
                    sqs.deleteMessage(workers2managerQ,message);
                }
            }
        }

        while (!workers.isEmpty()){
            ec2.terminateInstance(workers.poll());
        }
        System.out.println("M CMM - finished killing workers");
        // delete all queues
        System.out.println("M CMM - killing queues and buckets");
        sqs.deleteQueue("manager2WorkersQ");
        sqs.deleteQueue("workers2managerQ");
        sqs.deleteQueue("app2ManagerQ");
        s3.deleteAllBuckets();
        sqs.sendMessage(manager2appQ,"manager terminated");
        System.out.println("M CMM - send to local app a \"manager terminate\" message");
        System.out.println("M CMM - CREATE MISSION MANAGER FINISHED");
        System.exit(0);
    }
}
