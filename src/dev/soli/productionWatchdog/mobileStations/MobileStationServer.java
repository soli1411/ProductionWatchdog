package dev.soli.productionWatchdog.mobileStations;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class MobileStationServer extends Thread{
//FIXME all!
	private Socket socket=null;
	
	/**
	 * 
	 * Creates a listener over the specified socket for a mobile station.
	 * 
	 * @param socket
	 * 
	 */
	public MobileStationServer(Socket socket){
		this.socket=socket;
	}
	
	/**
	 * 
	 * Gets the string data sent by the mobile station over TCP/IP protocol and handles it.
	 * 
	 */
	@Override
	public void run(){
		InetAddress address = socket.getInetAddress();
		String client = address.getHostName();
		int porta = socket.getPort();
		System.out.println("Connected with client: "+ client + " porta: " + porta);
		try {
			new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedReader d = null;
		try {
			d = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			String input = null;
			try {
				input = d.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(input);
			if (input==null){
				System.out.println("Connection interrupted with client: "+ client + " porta: " + porta);
				break;
			} else {
				handleInput(input);
			}
		}
		try {
			this.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Handles received input from the mobile station.
	 * 
	 * @param input
	 * 
	 */
	private void handleInput(String input) {
		//TODO: database related part.
		String[] inputParts=input.split(" ");
		String role=inputParts[0];
		String id_number=inputParts[1];
		String actionTaken=inputParts[2];
		StringBuilder desc = new StringBuilder(inputParts[3]);
		for (int i = 3; i < inputParts.length; i++){
			desc.append(" "+inputParts[i]);
		}
		String description=desc.toString();
		System.out.println("Agent="+role+" "+id_number);
		System.out.println("Action="+actionTaken);
		System.out.println("Description="+description);
		
	}
	
}
