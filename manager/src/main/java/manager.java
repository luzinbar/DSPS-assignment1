import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.lang.management.ManagementPermission;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class manager {

    static private SQS sqs = new SQS();
    static private EC2 ec2 = new EC2();
    static private S3 s3 = new S3();
    static ListQueuesResponse queuesList;
    static private String manager2appQ = null; // through this q, manager receives new app requests
    static private String app2managerQ = null;
    static private String manager2WorkersQ = null; // through this q, manager receives new app requests
    static private String workers2managerQ;

    // application handler thread pool:
    static private ExecutorService threadPool = Executors.newFixedThreadPool(8); //TODO : how many ??
    static private ConcurrentHashMap<String, AppHandler> appsHandlers = new ConcurrentHashMap<String, AppHandler>();
    static private AtomicInteger notProcessedMissionsAmount = new AtomicInteger(0);
    static private int n;
    static private AtomicInteger currNumOfWorkers = new AtomicInteger(0);
    static private int workersLimit = 19;
    static private ConcurrentLinkedQueue<String> workers = new ConcurrentLinkedQueue<>();

    static private AtomicBoolean terminate = new AtomicBoolean(false);

    public static void main(String[] args) {
        // ------------------------------------------------------------ handle new apps: ------------------------------------------------------------
        queuesList = sqs.listQueues();
        manager2WorkersQ = sqs.createQ("manager2WorkersQ"); // create the application's queue
        sqs.listQueuesFilter(manager2WorkersQ);
        workers2managerQ = sqs.createQ("workers2managerQ");
        sqs.listQueuesFilter(workers2managerQ);
        app2managerQ = sqs.getQueue(app2managerQ);
        manager2appQ = sqs.getQueue(manager2appQ);

        // ------------------------------------------------- thread to handle messages back from workers: -------------------------------------------

        MissionsMaster missionsManager = new MissionsMaster(appsHandlers,workers2managerQ,sqs,terminate);
        Thread missionsMasterThread = new Thread(missionsManager);
        missionsMasterThread.start();

        while(!terminate.get()){
            // ---------------------------------------------------------- new app request -----------------------------------------------------------
            String appB = "";
            Message msg = sqs.recieveMessage(app2managerQ);
            if (msg.body().equals("terminate")){
                terminate.set(true);
                sqs.sendMessage(manager2WorkersQ,"terminate");
            }
            else {
                String[] body = msg.body().split("\t");
                appB = body[0];
                n = Integer.parseInt(body[1]);
                // new app handler:
                AppHandler apphandler = new AppHandler(s3, sqs, manager2WorkersQ, workers2managerQ, manager2appQ, appB, notProcessedMissionsAmount);
                appsHandlers.putIfAbsent(appB, apphandler);
                Thread appHandlerThread = new Thread(apphandler); // new thread for curr appHandler
                appHandlerThread.start();
                // ------------------------------------------------------- update workers amount --------------------------------------------------------
                if (currNumOfWorkers.get() < workersLimit) {
                    int newWorkersAmount = (int) Math.ceil(notProcessedMissionsAmount.get() / n) - currNumOfWorkers.get();
                    if (newWorkersAmount + currNumOfWorkers.get() > workersLimit) {
                        newWorkersAmount = workersLimit - currNumOfWorkers.get();
                    }
                    if (newWorkersAmount > 0) {
                        List<String> workerIds = ec2.createInstances("worker", newWorkersAmount);
                        for (String workerId : workerIds) {
                            workers.add(workerId);
                            currNumOfWorkers.addAndGet(1);
                        }
                    }
                }
            }
        }
        while (!appsHandlers.isEmpty()){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (currNumOfWorkers.get() > 0) {
            List<Message> messages = sqs.recieveMessages(workers2managerQ);
            for (Message message : messages) {
                if (message.body().equals("worker terminated")) {
                    currNumOfWorkers.decrementAndGet();
                    sqs.deleteMessage(workers2managerQ,message);
                }
            }
        }
        while (! workers.isEmpty()){
            ec2.terminateInstance(workers.poll());
        }
        // delete all queues
        sqs.deleteQueue(manager2WorkersQ);
        sqs.deleteQueue(workers2managerQ);
        sqs.deleteQueue(app2managerQ);
        s3.deleteAllBuckets();
        sqs.sendMessage(manager2appQ,"manager terminated");
        System.exit(0);
    }
}
