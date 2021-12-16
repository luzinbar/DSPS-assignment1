import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class MainManager {
    static final String REGION = "us-east-1";
    static final String AWS_ACCESS_KEY_ID = "ASIARPDHIHFCZII2TDLW";
    static final String AWS_SECRET_ACCESS_KEY = "UlTJNqLvBHXkjjWd9Scs4p/v3PGGPzxZbwDRAJaR";
    static final String AWS_SESSION_TOKEN = "FwoGZXIvYXdzEJ///////////wEaDHW5EgW3YrnoL3KVqiLGAfK8QJ5LACaM7wM/D9JzCdNEjM0vTwuraHrZMoinvTa451R6vq7Jmlv7CI6Bflsfq9JMrXb+68H8RdtQ20gW4phmaDm4vPiGhe/CH6VjXwMj7LtPrjnx2lqzGwdt3z84AZo3eIxPFW1a4kUr3u6YDvaWUsG1tNll0utW5eprpYXnFFo8ceqUjstTFmz9PggFEK/dkrGeUvB9gDUKr0TvHv76jBK1DnzuSvcgYEhAItc3TTpJUVcRoGMOPkf40KEIqxEA8Cn+YSia3eeNBjItEjQSLrdeUstZOw+gMluzn+vZ6Ei2EO5KIfoBVWwdbbyN0ar/YuPoBBZ40INf";

    public static void main(String[] args) throws InterruptedException {
        // Environment variables for Default Credential Provider Chain:
        System.setProperty("aws.accessKeyId",AWS_ACCESS_KEY_ID);
        System.setProperty("aws.secretKey",AWS_SECRET_ACCESS_KEY);
        System.setProperty("aws.sessionToken",AWS_SESSION_TOKEN);

        SQS sqs = new SQS();
        String workers2managerQ = sqs.createQ("workers2managerQ");
        ConcurrentHashMap appsHandlers= new ConcurrentHashMap();
        AtomicBoolean terminate = new AtomicBoolean(false);
        AtomicInteger n = new AtomicInteger(0);
        AtomicInteger notProcessedMissionsAmount = new AtomicInteger(0);
        AtomicInteger currNumOfWorkers = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> workers = new ConcurrentLinkedQueue<>();


        ReceiveMissionsManager missionsManager = new ReceiveMissionsManager(appsHandlers,workers2managerQ,sqs,terminate,n,notProcessedMissionsAmount,currNumOfWorkers,workers);
        Thread ReceiveMissionsManagerThread = new Thread(missionsManager);
        ReceiveMissionsManagerThread.start();
        CreateMissionsManager manager = new CreateMissionsManager(sqs,appsHandlers,workers2managerQ,terminate,n,notProcessedMissionsAmount,currNumOfWorkers,workers);
        manager.createMissions();
    }
}
