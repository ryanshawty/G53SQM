package vc.min.chat.Server;

import java.io.IOException;

import vc.min.chat.Server.IO.ClientSocket;
import vc.min.chat.Server.IO.IClientSocket;
import vc.min.chat.Server.Logger.LogLevel;
import vc.min.chat.Server.Logger.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;



/**
 * Simple chat server based on the protocol listed in README.md
 * 
 * @author Ryan Shaw
 *
 */
public class Server extends Thread implements IServer {
	
	/** 
	 * Start program with: java -jar server.jar --port=n --max=n
	 * Port defaults to 6111
	 * Max defaults to 20
	 * @param args
	 */
	public static void main(String[] args){
		int port = 6111;
		int max = 20;
		if(args.length != 0){
			for(String c : args){
				if(c.startsWith("--port=")){
					String temp = c.substring(7);
					port = Integer.parseInt(temp);
				}else if(c.startsWith("--max=")){
					String temp = c.substring(6);
					max = Integer.parseInt(temp);
				}
			}
		}
		new Thread(new Server(port, max)).start();
		
	}
		
	/**
	 * Thread running field
	 */
	private boolean running;
	
	/**
	 * The server port
	 */
	private int port;
	
	/**
	 * Maximum number of connections to allow
	 */
	private int maxConnections;
	
	/**
	 * The server socket
	 */
	private ServerSocket serverSocket;
	
	/**
	 * Holds the client sockets
	 */
	private ArrayList<IClientSocket> clientSockets;

	private boolean accepting;
	
	/**
	 * Convenience constructor
	 * 
	 * @param port
	 * @param maxConnections
	 */
	public Server(int port, int maxConnections){
		Logger.log(LogLevel.INFO, "Starting server on port " + port + " with max connections of " + maxConnections);
		this.port = port;
		this.maxConnections = maxConnections;
		this.running = true;
		clientSockets = new ArrayList<IClientSocket>();
		/* Thread to check clients are alive */
		new PingChecker(this).start();
	}

	public void stopServer() throws IOException{
		Logger.log(LogLevel.INFO, "Shutting down server...");
		running = false;
		for(IClientSocket client : clientSockets){
			client.close("server stopping");
		}
		serverSocket.close();
	}
	
	public ArrayList<IClientSocket> getClients(){
		return clientSockets;
	}

	public int getPort() {
		return serverSocket.getLocalPort();
	}
	
	public synchronized boolean isAccepting(){
		return accepting;
	}
	
	/**
	 * Remove dead sockets
	 * @throws IOException 
	 */
	public void removeDead() throws IOException{
		
		ListIterator<IClientSocket> li = clientSockets.listIterator();
		ArrayList<IClientSocket> remove = new ArrayList<IClientSocket>();
		while(li.hasNext()){
			IClientSocket client = li.next();
			if(!client.isRunning())
				remove.add(client);
		}
		for(IClientSocket c : remove){
			try{
				c.close("dead");
			}catch(IOException e){
				// If already terminated.
			}
			clientSockets.remove(c);
		}
	}
	
	/**
	 * Send broadcast message to all connected clients
	 * @param message
	 * 			message to send
	 */
	public void sendBroadcast(String from, String message){
		ListIterator<IClientSocket> li = clientSockets.listIterator();
		
		while(li.hasNext()){
			IClientSocket client = li.next();
			if(client.isRunning() && client.getUsername() != null)
				try{
					client.sendMessage(from, message);
				}catch(IOException e){
					Logger.log(LogLevel.ERROR, "error sending broadcast message to " + client.getUsername());
				}
		}
	}
	
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(this.port);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to start server on port " + this.port);
			return;
		}
		while(running){
			accepting = true;
			try {
				Socket clientSocket = serverSocket.accept();
				try{
					removeDead();
				}catch(IOException e){
					Logger.log(LogLevel.ERROR, "error occured on removal of dead sockets");
				}
				IClientSocket clientThread = new ClientSocket(clientSocket, this);
				if(clientSockets.size() >= maxConnections){
					clientThread.close("max connections reached");
				}else{
					clientSockets.add(clientThread);
					Logger.log(LogLevel.INFO, "Added client, client count: " + clientSockets.size());
				}
			} catch (IOException e) {
				if(running)
					Logger.log(LogLevel.ERROR, "Failed to accept client: " + e.getMessage());
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public IClientSocket getClientSocketByUsername(String username) {
		if(username == null) return null;
		ListIterator<IClientSocket> li = clientSockets.listIterator();
		while(li.hasNext()){
			IClientSocket client = li.next();
			if(client.getUsername() == null) continue;
			if(client.getUsername().equals(username)){
				return client;
			}
		}
		return null;
	}

}
