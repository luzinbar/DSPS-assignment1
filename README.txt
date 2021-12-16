DSPS hw01


----- Running Instructions -----

* To run a local application:
	>java -jar LocalApp.jar inputFileName outputFileName n
* To run a local application with a "terminate" message:
	>java -jar LocalApp.jar inputFileName outputFileName n terminate
Where:
- localJar.jar is the name of the jar file containing our code.
- inputFileName is the name of the input file.
- outputFileName is the name of the output file.
- n is the workers' files ratio (how many PDF files per worker).
- terminate indicates that the application should terminate the manager at the end.



----- Program Explanation -----

Terms:
1. App2managerQ – a general SQS to send messages from all the local apps to the manager.
2. Manager2appQ – a general SQS to send message from the manager to all the local apps.
3. appB – a personal bucket in S3 for every local app.
4. Manager2workersQ – a general SQS to send message from the manager to the workers.
5. Workers2managerQ – a general SQS to send a message from the workers to the manager. 


***** Local Application *****

- A quick brief:
Every local app is communicating with the manager with SQS queues. We have two SQSs for this part – one from the local app to the manager and one from the manager to the local app. 
Only the first local app that connects to the system and initializes the manager, creates the SQS queues, the others are looking for the existing queues by their name to use them. 
The local app uploads the file to S3, wait for a done message from the manager, indicating the status of all the tasks in its input file, downloads from S3 the output file, and convert it to HTML.

1. The first local application starts and checks the manager activity, which is tagged as manager. 
2. If the manager does not exist, the local app creates a new EC2 instance for the manager and provides the manager a user data containing the credentials for AWS services, JDK to download, 
the path to the jar, etc. Moreover, the local app creates two SQSs queues (app2managerQ, manager2appQ).
3. Else, the manager already exists, and the local application looks for the relevant queues to use.
4. The local application creates a personal bucket and uploads the input file to S3.
5. The local application lets the manager know that a new file was uploaded to S3, by sending a message through app2managerQ with the information: appB (to know where to find the file) and 
args[2] = n (number of missions per worker).
6. The local application is waiting for a "done" message that contains the relevant appB (to know that this message belongs to this local app) from the manager by checking the Manager2appQ.
7. The local application downloads the summary file from S3 and creates an HTML file with the data from the summary file.
8. If the local application didn’t receive a "terminate" flag in args[3], the local app will finish it's run.
9. Else, if the local application contains "terminate" flag in args[3], the local application will send a message to the manager through app2managerQ to tell him to terminate everything.
10. After the manager finished terminating the workers, queues and buckets, it sends a "done terminate" message back to the local application, and the local application now can terminate the 
manager (EC2 instance), terminate the last SQS (manager2appQ), and finish the program.


***** Manager ***** 

- A quick brief:
We divided the manager's work into two threads:	
One is the CreateMissionsManager who is responsible for creating app handlers for each local application (which is a thread from the thread pool, handling the entire work related to the specific 
application), and terminate the entire program when a terminate message arrives (app handlers, workers, buckets and queues).	
The second is ReceiveMissionsManager who is responsible for sending each app handler the relevant file after the workers finished converting it.

1. After the first local application initializes the manager, the main manager creates an SQS from the workers to the manager. Moreover, the main manager is responsible to create all the common 
fields for both CreateMissionsManager and ReceiveMissionsManager. 
2. The main manager creates a CreateMissionsManager and a new thread that will be responsible to run it.
3. The main manager creates a ReceiveMissionsManager that will use the main manager thread.
4. The main manager finished its work.

== CreateMissionsManager ==
1. The CreateMissionsManager is waiting for a message from a local app that a new file was uploaded or to terminate the program.
2. If the message is about a new file, the CreateMissionsManager creates a new app handler for this local app and a new thread from the threads pool. The CreateMissionsManager gives the new 
app handler the path of the input file for its own local application and deletes the message from the app2managerQ.
3. If the message is a terminate message, the CreateMissionsManager sends a terminate message to the workers. 
4. The CreateMissionsManager is responsible to kill all the app handlers, all the workers (after they send back a "worker terminate" message), all the SQSs (except for manager2appQ) and all the 
buckets in S3. After everything is terminated, the CreateMissionsManager sends a "manager terminated" message to the local app that sends the "terminate message".

== App handler ==
1. The app handler downloads the relevant file from the relevant bucket.
2. After the app handler downloads the file, it divides the file into lines where each line is a new mission, adds indications of the specific application and mission, and finally sends the mission as a 
message through manager2workersQ. During the preparation of the mission, it counts the number of missions, to later create the right number of workers and to follow the results.
3. Now, when the app handler finished sending all the missions to the workers, it waits for all the missions to come back (to its mission's queue, by the ReceiveMissionsManager). The app handler 
checks if the mission was handled or failed with the first part of the message body ("h" or "f"). It appends all the mission to one summary file and organizes all the data.
4. After the app manager got all the missions back, it uploads the file to S3 with the name "summery-file" and sends a "done" message to the local app through manager2appQ.

== ReceiveMissionsManager ==
1. The ReceiveMissionsManager takes care of the creation of new workers if needed with the formula: numOfNotProcessedMission divide by n, rounding up, and then subtracting the number of 
active workers (already running). It also checks that there are no more than 15 workers in total.
2. If needed, the ReceiveMissionsManager creates new workers – new EC2 instances, provides them the user data containing the credentials for AWS services, JDK to download, the path to the jar, etc.
3. After the ReceiveMissionsManager is finished with the workers for that iteration, it looks for messages from workers in workers2managerQ. 
4. If new messages have arrived, it will read the messages and send them to the relevant app handler. After taking care of the message, it deletes it.


***** Workers *****

- A quick brief:
A worker is an EC2 instance that the ReceiveMissionsManager creates as needed. Each worker can communicate with the manager through SQSs manager2workersQ and worker2managerQ.

1. The create mission manager is waiting for a message that a new mission is up or to terminate itself.
2. If the message is about a new mission, after every step of the way the worker checks if it succeeded or not and updates the output message accordingly.
3. First, the worker downloads the pdf file and checks if it succeeded.
If succeeded, the worker opens the file by creating a new file from it.
If succeeded, the worker is doing the operation it asked to (converting the file).
If succeeded, the worker uploads the converted file to s3.
4. The worker sends a message to the manager that it finished this mission (either if it failed or succeeded).
5. The worker deletes the input message so no other worker will do it again.



----- Additional Data -----

* ami:  ami-00e95a9222311e8ed
* Instance type: T2_MICRO
* Time to finish the program: 15 minutes.
* Number of n: 100
* Security: the machine supplies the service with security credentials that passed with userData. Those credentials are for AWS connection and the manager and workers programs stored in S3.
* Scalability: as explained above, the ReceiveMissionsManager calculates the number of workers to create as many as needed workers (with the limited number of EC2 machines that AWS allows). 
By that, we take care of handling as much local application in parallel as possible.
* Persistence: all the messages will be deleted just after handling them, so we make sure that none of the messages will be lost and not be treated. For example, if a worker dies, another worker 
will take his job and do it instead. The app handler checks all the workers' work and takes care of non-duplicate or missing missions. Also, every local application checks once in a while if the 
manager is still running. If not, the local application will take care of initializing a new manager that will continue the old one's job (all the SQSs continue existing with all the data in them).
* Threads: as explained above, we divided the manager's work into two threads: one is the CreateMissionsManager that gets a new thread for its work, the second is the ReceiveMissionsManager 
that will use the main manager thread. Moreover, the CreateMissionsManager creates a new app handler for every local application and a new thread from the threads pool for each app handler. 
With all these threads, we make sure that the manager can handle a big number of applications simultaneously without mixing the data between them.
* Terminate: as explained above, the CreateMissionsManager is responsible to kill all the app handlers, all the workers, all the SQSs (except for manager2appQ), and all the buckets in S3. Then the 
local application that sent the "terminate" flag would kill the manager and the manager2appQ queue.
* System limitation: we create a new EC2 instance only if needed, so we make sure that the number of instances is as small as possible.
* Workers: each worker works hard and continuously. If a worker currently doesn’t work on a mission, it looks for the next mission by checking the manager2workersQ. 
* Defined tasks: as explained above, each part of the system has properly defined tasks. If we thought that a part of the system takes care of too much, we divided it to more classes in order to make 
the program as clean and as understandable as can be.
* Distributed: the system is distributed and all the program is chronologic, creating workers only if needed. All the workers take their missions from the same queue so that no mission will be untreated. 
Moreover, every local application has a personal app handler so the applications are not interdependent.
