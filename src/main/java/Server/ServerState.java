package Server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.json.JSONObject;
import ClientHandler.ClientHandler;
import ClientHandler.User;
import Connection.Server2ServerConnection;
import Messaging.Sender;

public class ServerState {
	
	/* Maintaining current running server state*/
	
	private static final Logger logger = LogManager.getLogger(ServerState.class);

	private String serverName, serverAddress;
	
	private int clientPort, serverPort;
	
//	private Server currentServer;
	
	private static ServerState serverState;
	

	
	private ConcurrentHashMap<String, Server> serversHashmap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ChatRoom> chatRoomHashmap = new ConcurrentHashMap<>();
	private  ConcurrentLinkedQueue<User> identityList = new ConcurrentLinkedQueue<>();
	
	private ConcurrentHashMap<String, String> otherServersChatRooms = new ConcurrentHashMap<>();
//	otherServersChatRooms<ChatroomName, server_name>
	private ConcurrentHashMap<String, String> otherServersUsers = new ConcurrentHashMap<>();
//	otherServersUsers<user_identity, server_name>
	
	private ServerState() {}
	
	//create single ServerState object
	public static ServerState getServerState() {
        if (serverState == null) {
            synchronized (ServerState.class) {
                if (serverState == null) {
                    serverState = new ServerState();
                }
            }
        }
        return serverState;
    }
	
	
	//initialize server
	

	public ServerState initializeServer(String serverName, String confFilePath) {
		//TODO
		//Have to initialize server using configure file
		//hard coded start
		serversHashmap.put("s1", new Server("s1", "localhost", 4444, 5555));
		serversHashmap.put("s2", new Server("s2", "localhost", 4445, 5556));
		serversHashmap.put("s3", new Server("s3", "localhost", 4446, 5557));
		
		for (ConcurrentHashMap.Entry<String,Server> e : serversHashmap.entrySet()) {
			if(e.getKey().equals(serverName)) {
				this.serverName = serverName;
				this.serverAddress = e.getValue().getServerAddress();
				this.clientPort = e.getValue().getClientPort();
				this.serverPort = e.getValue().getServerPort();
			}
		}
		
		
		
		//hard coded end
		
		//create a mainhall room
		String mainHall = "MainHall-"+this.serverName;
		ChatRoom chatRoom = new ChatRoom();
	    chatRoom.createChatRoom(mainHall, "");
	    chatRoomHashmap.put("MainHall", chatRoom);
		
	    createServer2ServerConnection();
	    
		return serverState;
	}
	
	public ConcurrentHashMap<String, ChatRoom> getChatRoomHashmap() {
		return chatRoomHashmap;
	}

	public void setChatRoomHashmap(ConcurrentHashMap<String, ChatRoom> chatRoomHashmap) {
		this.chatRoomHashmap = chatRoomHashmap;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public ConcurrentLinkedQueue<User> getIdentityList() {
		return identityList;
	}

	public void setIdentityList(ConcurrentLinkedQueue<User> identityList) {
		this.identityList = identityList;
	}
	
	public Server getServerByName(String serverName) {
		return this.serversHashmap.get(serverName);
	}
	
	public void replaceServerbByName(Server server) {
		this.serversHashmap.put(server.getServerName(), server);
	}
	
	public ConcurrentHashMap<String, Server> getServersHashmap() {
		return this.serversHashmap;
	}

	public ConcurrentHashMap<String, String> getOtherServersChatRooms() {
		return otherServersChatRooms;
	}

	public void setOtherServersChatRooms(ConcurrentHashMap<String, String> otherServersChatRooms) {
		this.otherServersChatRooms = otherServersChatRooms;
	}

	public ConcurrentHashMap<String, String> getOtherServersUsers() {
		return otherServersUsers;
	}

	public void setOtherServersUsers(ConcurrentHashMap<String, String> otherServersUsers) {
		this.otherServersUsers = otherServersUsers;
	}

	public void createServer2ServerConnection() {
		for (ConcurrentHashMap.Entry<String,Server> entry : serversHashmap.entrySet()) {
			if (!(entry.getKey().equals(this.serverName))) {
				try {
					Socket socket = new Socket(entry.getValue().getServerAddress(), entry.getValue().getServerPort());
					logger.info("Server "+ this.serverName + " is connected to Server "+entry.getValue().getServerName()
							+ " using address " +entry.getValue().getServerAddress() 
							+ " port " + entry.getValue().getServerPort());
					JSONObject obj = new JSONObject();
					obj.put("type","server-connection-request").put("server", this.serverName);
					Sender.sendRespond(socket, obj);
					Server s = entry.getValue();
					s.setServerSocketConnection(socket);
					replaceServerbByName(s);
					Server2ServerConnection s2sc = new Server2ServerConnection(socket);
					s2sc.start();
				}catch (UnknownHostException u)
		        {
		            logger.error(u.getMessage());
		        }
		        catch(IOException i)
		        {
		            logger.error(i.getMessage());
		        }
				
			}
		}
	}
	
	

}
