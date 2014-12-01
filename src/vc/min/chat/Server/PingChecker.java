package vc.min.chat.Server;

import java.util.ArrayList;

public class PingChecker extends Thread{

	private Server server;
	
	public PingChecker(Server server){
		this.server = server;
	}
	
	public void run(){
		while(true){
			for(ClientSocket client : server.getClients()){
				if(System.currentTimeMillis() - client.lastTimeRead > 1000L && client.isRunning()){
					System.out.println("Client timed out: " + client.getUsername());
					client.close("timeout reached");
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}