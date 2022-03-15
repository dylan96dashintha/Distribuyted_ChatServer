import java.io.*;  
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner; 
import org.json.JSONObject;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ClientHandler.ClientHandler;
import Connection.ClientServerConnection;
import Connection.Server2ServerConnection;
import Heartbeat.ConsensusJob;
import Heartbeat.GossipJob;
import Messaging.Sender;
import Server.ServerState;
import model.Constants;


public class App 
{
	private static final Logger logger = LogManager.getLogger(App.class);
	
	private static Integer alive_interval = 3;
	private static Integer alive_error_factor = 5;
	private static Integer consensus_interval=10;
    private static Integer consensus_vote_duration=5;
	
    public static void main( String[] args )
    {
    	

    	 
    	//execute with jar    	
    	String serverName = args[0];
    	String confFilePath = args[1];
//    	
    	//execute with eclips   	
//    	String serverName = "s1";
//    	String confFilePath = "conf.txt"; 	    	
    	

    	
    	logger.info("Starting Server "+ serverName);
    	boolean iterate = true;
    	ArrayList<String> configuration = new ArrayList<String>();
    	while(iterate) {
    		logger.debug("Running Loop");
    		try {
    			File file = new File(confFilePath);
    			Scanner reader= new Scanner(file);
    			while (reader.hasNextLine()) {
    		        String[] data = reader.nextLine().split("\t");
    		        configuration.add(new JSONObject()
    		        			.put("server-name", data[0])
    		        			.put("address", data[1])
    		        			.put("client-port", data[2])
    		        			.put("server-port", data[3])
    		        			.toString());
    		     }
    			reader.close();
    			iterate = false;
    			logger.debug("File reading finished");
    		} catch (FileNotFoundException e1) {
    			iterate = false;
    			logger.info("No such file in " + confFilePath + " file path");
    			System.exit(0);
    		}    
    	}
    	
    	ServerState currentServer = ServerState.getServerState().initializeServer(serverName, configuration);	
	
    	//create server connection    	
    	Thread server2serverListingThread = new Thread() {
    		public void run() {
    			ServerSocket serverSocket = null;
    			Socket socket = null;
    			try {
    				serverSocket = new ServerSocket();
    				SocketAddress socketAddress = new InetSocketAddress(ServerState.getServerState().getServerAddress(),
    						ServerState.getServerState().getServerPort());
    				serverSocket.bind(socketAddress);
    				logger.debug("Server " + ServerState.getServerState().getServerName()
    						+ " Listening for other servers, Address: " + ServerState.getServerState().getServerAddress()
    						+ ", Port: " + ServerState.getServerState().getServerPort());

    			} catch (IOException e) {
    				logger.error(e.getMessage());
    			}
    			
    			while (true) {
    				try {
    					socket = serverSocket.accept();
    					Server2ServerConnection servr2ServerConnection = new Server2ServerConnection(socket);
    					servr2ServerConnection.start();
    				} catch (IOException e) {

    					logger.error(e.getMessage());
    					logger.error("Server Stop Listening");

    				}
    			}
    		}
    	};
    	
    	server2serverListingThread.start();
    	
    	//Create client connection
    	ServerSocket serverSocket = null;
    	Socket socket = null;
    	try {
    		serverSocket = new ServerSocket();
    		SocketAddress socketAddress = new InetSocketAddress(currentServer.getServerAddress(), currentServer.getClientPort());    		
    		serverSocket.bind(socketAddress);
    		logger.debug("Server "+ currentServer.getServerName() +" Listening for Clients, Address: "+ currentServer.getServerAddress()+ ", Port: "+ currentServer.getClientPort());
    	}catch (IOException e) {
    		logger.error(e.getMessage());		
    	}
    	
	    	
    	boolean isListening = true;
    	
    	if(isListening) {
    		logger.info("Failure Detection is running GOSSIP mode");
	    	startGossipJob();
	    	startConsensusJob();
    	}
    	
    	while (true) {
	       try {
               socket = serverSocket.accept();
               ClientServerConnection clientServerConnection = new ClientServerConnection(socket);
       			clientServerConnection.start();
//       			JSONObject json = new JSONObject();
//       			json.put("type", "newidentity");
//       			json.put("approved", "true");
//       			Sender.sendRespond(socket, json);
           } catch (IOException e) {
        	   isListening = false;
        	   logger.error(e.getMessage());
        	   logger.error("Server Stop Listening");
        	   
           }
    	}

    	}
//        try{  
//        	ServerSocket ss=new ServerSocket(4444);  
//        	Socket s=ss.accept();
//        	InputStream inputFromClient = s.getInputStream();
//            Scanner scanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
//            while (true) {
//                String line = scanner.nextLine();
//                System.out.println("Line == "+line);
//                ClientHandler clientHandler = new ClientHandler(getType(line));
//                clientHandler.getTypeFunctionality();
//                
//            }
//           // ss.close();  
//        	}catch(Exception e){System.out.println(e);}  
//      }
//    
//    protected static JSONObject getType(String line) {
//    	JSONObject jsnObj = new JSONObject(line);
//        //String type = jsnObj.getString("type");
//        return jsnObj;
//    }
    
    private static void startGossipJob() {
        try {

            JobDetail gossipJob = JobBuilder.newJob(GossipJob.class)
                    .withIdentity(Constants.GOSSIP_JOB, "group1").build();

            gossipJob.getJobDataMap().put("aliveErrorFactor", alive_error_factor);

            Trigger gossipTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(Constants.GOSSIP_JOB_TRIGGER, "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(alive_interval).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(gossipJob, gossipTrigger);

        } catch (SchedulerException e) {
            logger.error("Error in starting gossibJobing(Heartbeat)");
        }
    }
    
    private static void startConsensusJob() {
        try {

            JobDetail consensusJob = JobBuilder.newJob(ConsensusJob.class)
                    .withIdentity(Constants.CONSENSUS_JOB, "group1").build();

            consensusJob.getJobDataMap().put("consensusVoteDuration", consensus_vote_duration);

            Trigger consensusTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(Constants.CONSENSUS_JOB_TRIGGER, "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(consensus_interval).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(consensusJob, consensusTrigger);

        } catch (SchedulerException e) {
            logger.error("Error in starting consensusJob(Heartbeat)");
        }
    }
}
