package test.Server;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import vc.min.chat.Server.Server;
import vc.min.chat.Shared.Packets.Packet0Login;
import vc.min.chat.Shared.Packets.Packet1Disconnect;
import vc.min.chat.Shared.Packets.PacketHandler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServerTest {
	private static Server server;
	private static Socket client;
	private static DataOutputStream dos;
	private static DataInputStream dis;
	private static PacketHandler p;
	@Before
	public void setUp(){
		server = new Server(0, 2);
		new Thread(server).start();
		
		try {
			Thread.sleep(200); // Wait for server to spin up
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		try {
			client = new Socket("localhost", server.getPort());
			dos = new DataOutputStream(client.getOutputStream());
			dis = new DataInputStream(client.getInputStream());
			p = new PacketHandler(dis, dos);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try{
			Thread.sleep(200);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		
	}
	
	@After
	public void tearDown() throws InterruptedException{
		Thread.sleep(100);
		try {
			server.stopServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.sleep(500);
	}
	
	@Test
	public void testLogin() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		Packet0Login packet = new Packet0Login("test");
		try {
			p.writePacket(packet);
		} catch (IOException e1) {
			fail(e1.getMessage());
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		// Expect a confirmation
		try {
			byte b = dis.readByte();
			assertEquals(b, 0);
			assertEquals(dis.readUTF(), "test");
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		return;
	}
	
	@Test
	public void testDisconnect(){
		Packet1Disconnect packet255disconnect = new Packet1Disconnect("test dc");
		try {
			p.writePacket(packet255disconnect);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		try {
			byte b = dis.readByte();
			assertEquals(b, 1);
			assertEquals(dis.readUTF(), "test dc");
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return;
	}
	
	@Test
	public void testKeepAlive(){
		try {
			Thread.sleep(1002);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			byte b = dis.readByte();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
}
