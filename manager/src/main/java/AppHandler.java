import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AppHandler implements Runnable{

    private S3 s3;
    private SQS sqs;
    private String manager2WorkersQ;
    private String workers2managerQ;
    private String appQ;
    private String appB;
    private AtomicInteger allMissionsAmount;
    private ConcurrentLinkedQueue<String> missions;
    private int missionsCounter= 0;
    private Map<String,Boolean> missionsStatus;
    private ConcurrentHashMap appsHandlers;

    AppHandler(S3 s3, SQS sqs, String manager2WorkersQ, String workers2managerQ, String appQ, String appB, AtomicInteger notProcessedMissionsAmount, ConcurrentHashMap appsHandlers){
        this.s3 = s3;
        this.sqs = sqs;
        this.manager2WorkersQ = manager2WorkersQ;
        this.workers2managerQ = workers2managerQ;
        this.appQ = appQ;
        this.appB = appB;
        this.allMissionsAmount = notProcessedMissionsAmount;
        this.missions = new ConcurrentLinkedQueue<>();
        this.missionsStatus = new HashMap<>();
        this.appsHandlers = appsHandlers;
    }

    public void run() {
        System.out.println("M AH - A NEW APP HANDLER STARTS RUNNING");

        // create its missions, and wait for the response
        BufferedReader inputFile = null;
        try {
            inputFile = s3.downloadFromS3(appB);
            System.out.println("M AH - downloaded from s3 the input file");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("M AH - exception in downloading from s3");
        }
        try{
            System.out.println("M AH - start prossesing the missions");
            String line = inputFile.readLine();
            while (line != null) {
                String missionId = String.valueOf(System.currentTimeMillis());
                String mission = line +"\t"+appB+"\t"+missionId;
                missionsCounter++;
                missionsStatus.put(missionId,false);
                sqs.sendMessage(manager2WorkersQ,mission);
                line = inputFile.readLine();
            }
            System.out.println("M AH - finished prossesing the missions");
        } catch (IOException e) {
            System.out.println("M AH - exception in prossesing the message");
            e.printStackTrace();
        }
        allMissionsAmount.addAndGet(missionsCounter);
        System.out.println("M AH - the current total missions amount "+allMissionsAmount.get());
        try {
            System.out.println("M AH - start writing a summery file");
            Writer summeryFile;
            summeryFile = new BufferedWriter(new FileWriter(appB+"-summery-file",false));
            System.out.println("M AH - mission counter"+ missionsCounter);
            while (missionsCounter > 0){
                if (missions.isEmpty()) {
                    continue;
                }
                String msg = missions.poll();
                System.out.println("M AH - app handler polled a message");
                String[] msgBody = msg.split("\t");
                if (missionsStatus.get(msgBody[4])){
                    continue;
                }
                missionsStatus.replace(msgBody[4],true);
                missionsCounter --;
                System.out.println("MissionsCounter: "+missionsCounter);
                if (msgBody[0].equals("f")){
                    summeryFile.append("<tr><th>"+msgBody[1]+"</th>\n<th>"+msgBody[2]+"</th>\n<th>-----</th>\n<th>"+msgBody[5]+"</th>\n</tr>");
                }
                else {
                String url = "https://"+appB+".s3.amazonaws.com/"+appB+"-"+msgBody[4];
                summeryFile.append("<tr>\n<th>"+msgBody[1]+"</th>\n<th>"+msgBody[2]+"</th>\n<th>"+url+"</th>\n<th>-----</th>\n</tr>"); // check if \n is needed
                }
            }
            System.out.println("M AH - all mission added to summery file");
            summeryFile.close();
            // TODO: upload the summery file to s3   : check if summery is the path
            s3.uploadToS3(appB+"-summery-file",appB);
            System.out.println("M AH - upload the summery file to s3");
            new File(appB+"-summery-file").delete();
            sqs.sendMessage(appQ,"done\t"+appB);
            System.out.println("M AH - sand a \"done\" message to local app");
            appsHandlers.remove(appB);
        } catch (IOException ioException) {
            System.out.println("M AH - exception in creating a summery file");
            ioException.printStackTrace();
        }
        System.out.println("M AH - APP HANDLER FINISHED RUNNING");
    }

    public void addMission(String msg) {
        missions.add(msg);
    }
}
