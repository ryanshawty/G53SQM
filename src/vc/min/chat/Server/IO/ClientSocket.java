package vc.min.chat.Server.IO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import vc.min.chat.Server.Server;
import vc.min.chat.Server.Logger.LogLevel;
import vc.min.chat.Server.Logger.Logger;
import vc.min.chat.Shared.Packets.Packet;
import vc.min.chat.Shared.Packets.Packet0Login;
import vc.min.chat.Shared.Packets.Packet127Greeting;
import vc.min.chat.Shared.Packets.Packet1Disconnect;
import vc.min.chat.Shared.Packets.Packet2KeepAlive;
import vc.min.chat.Shared.Packets.Packet3Message;
import vc.min.chat.Shared.Packets.Packet4ListClients;
import vc.min.chat.Shared.Packets.Packet5PM;
import vc.min.chat.Shared.Packets.PacketHandler;

/**
 * 
 * @author Ryan Shaw
 *
 */
public class ClientSocket implements IClientSocket {
	
	/**
	 * Clients user name
	 */
	private String username;
	
	/**
	 * The client socket
	 */
	private Socket socket;

	/**
	 * Socket is running
	 */
	private boolean running;
	
	/**
	 * Input thread
	 */
	private ReaderThread rt;

	/**
	 * Main server instance
	 */
	public Server server;
	
	/**
	 * Input stream
	 */
	private DataInputStream dis;

	/**
	 * Output stream
	 */
	private DataOutputStream dos;

	/**
	 * Packet handler
	 */
	private PacketHandler packetHandler;
	
	private Long lastTimeRead;
	
	/**
	 * Constructor to create the client
	 * @param socket
	 * @throws IOException 
	 */
	public ClientSocket(Socket socket, Server server) throws IOException{
		this.socket = socket;
		this.server = server;
		running = true;
		lastTimeRead = System.currentTimeMillis();
		/* Send the greeting packet to new client */
		initIO();
		packetHandler = new PacketHandler(dis, dos);
		sendPacket(new Packet127Greeting());
	}

	/**
	 * Start the packet reader thread
	 */
	private void initIO(){
		try {
			dis = new DataInputStream(this.socket.getInputStream());
			dos = new DataOutputStream(this.socket.getOutputStream());
			rt = new ReaderThread(this);
			rt.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read and then decide what to do with the incoming packet
	 * @param Packet
	 * 		The packet object to process
	 * @throws IOException 
	 */
	public void handlePacket(Packet packet) throws IOException {
		setLastTimeRead(System.currentTimeMillis());
		int packetID = getPacketHandler().getPacketID(packet.getClass());
		// Check packets are able to be used by client.
		if(getUsername() == null && packetID > 2){
			close("only login, dc and ping packet available when not logged in");
			return;
		}
		switch(packetID){

			case 0:
				Packet0Login packet0login = (Packet0Login) packet;
				Logger.log(LogLevel.INFO, packet0login.username + " has joined");
				if(server.getClientSocketByUsername(packet0login.username) == null){
					setUsername(packet0login.username);
					sendPacket(packet0login);
				}else{
					close("already connected with that username");
				}
			break;
			case 1:
				Logger.log(LogLevel.INFO, getUsername() + " is disconnecting");
				Packet1Disconnect packet255disconnect = (Packet1Disconnect) packet;
				close(packet255disconnect.message);
			break;
			case 2:
				Packet2KeepAlive packet2keepalive = (Packet2KeepAlive) packet;
				sendPacket(packet2keepalive);
			break;
			case 3:
				Packet3Message packet3message = (Packet3Message) packet;
				sendBroadcast(this.getUsername(), packet3message.message);
			break;
			case 4:
				Packet4ListClients packet4listclients = (Packet4ListClients) packet;
				sendListClients(packet4listclients.fullList);
			break;
			case 5:
				Packet5PM packet5pm =(Packet5PM) packet;
				IClientSocket sock = server.getClientSocketByUsername(packet5pm.toUsername);
				if(sock == null){
					sendMessage("SERVER", "user not found");
				}else{
					sock.sendMessage(this.getUsername(), packet5pm.message);
				}
			break;
		}
	}
	
	/**
	 * Send a the client list to the connected client
	 * @param fullList
	 * 			if true send usernames else send client count
	 * @throws IOException 
	 */
	private void sendListClients(boolean fullList) throws IOException {
		ArrayList<IClientSocket> clients = server.getClients();
		ArrayList<String> usernames = new ArrayList<String>();
		for(IClientSocket c : clients){
			if(c.getUsername() != null){
				usernames.add(c.getUsername());
			}
		}
		Packet4ListClients packet = new Packet4ListClients(fullList, usernames);
		sendPacket(packet);
	}
	
	/**
	 * @throws IOException 
	 * Add a packet to the send queue
	 * @param packet
	 * @throws  
	 */
	public void sendPacket(Packet packet) throws IOException{
		getPacketHandler().writePacket(packet);
	}
	
	public void close(String message) throws IOException{
		Packet1Disconnect packet = new Packet1Disconnect(message);
		sendPacket(packet);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		running = false;
	}
	
	public void sendBroadcast(String from, String message) {
		server.sendBroadcast(from, message);
	}
	
	public void sendMessage(String from, String message) throws IOException{
		Packet3Message packet3message = new Packet3Message(message, from);
		sendPacket(packet3message);
	}
	
	// Getters and setters, self explanatory
	
	public PacketHandler getPacketHandler(){
		return packetHandler;
	}

	public DataInputStream getInputStream(){
		return dis;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
	
	public long getLastTimeRead(){
		return lastTimeRead;
	}
	
	public void setLastTimeRead(long time){
		this.lastTimeRead = time;
	}
}
