package dev.soli.productionWatchdog.mobileStations;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * 
 * This class handles all the connections with the mobile stations that communicate with it over TCP/IP protocol.
 * The input is a String that contains the operatorName+" "+action taken+" "(+description).
 *
 */
public class MobileStationsHandler {

	private static final int port=9999;//port number at which the mobile stations communicate with this application.
	//if you change this, you will have to change the port number in the mobile stations code.
	
	/**
	 * 
	 * Creates a new thread that listens for incoming connections on port port and binds them with a mobileStationServer.
	 * 
	 */
	public MobileStationsHandler(){
		
		
		Thread t=new Thread() {
			@SuppressWarnings("resource")
			@Override
			public void run() {
				
				ServerSocket serverSocket = null;
				try {
					serverSocket = new ServerSocket(port);
					serverSocket.setReuseAddress(true);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				HashSet<Socket> clientsList=new HashSet<Socket>();
				while(true) {
					System.out.println("Waiting for Clients...");
					Socket socket = null;
					try {
						socket = serverSocket.accept();
						socket.setReuseAddress(true);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Recieved a call from: " + socket+"\n");
					if (!clientsList.contains(socket)){
						MobileStationServer mobileStationServer=new MobileStationServer(socket);
						mobileStationServer.start();
						clientsList.add(socket);
					} else {
						System.out.println("Already connected to: "+socket);
					}
				}
				
			}
		};
		t.start();
		
	}
	
}
