package dev.soli.productionWatchdog.mobileStations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.machine.Machine;

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
		BufferedReader in=null;
		try {
			in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			String input=null;
			try {
				input=in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Request from station "+client+": "+input);
			if (input==null){
				System.out.println("Connection interrupted with client: "+ client + " porta: " + porta);
				break;
			} else {
				String response=handleInput(input);
				out.println(response);
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
	private String handleInput(String input) {
		//TODO: employee-actions database related part.

		//EmployeeId - ActivityName - Request data
		//employeeId - NewProductionActivity - machineId - multiplier - newArticle
		//employeeId - CheckQuotes TODO:implement this operation and ask for specifications.
		//employeeId - GetMachinesState - ',' separated machine_ids values //TODO: eliminate this activity
		//employeeId - GetAllMachinesState
		String[] inputParts=input.split(" ");
		String employeeId=inputParts[0];
		String activityName=inputParts[1];
		StringBuilder desc=new StringBuilder();
		for (int i=2;i<inputParts.length;i++){
			desc.append(" "+inputParts[i]);
		}
		String description=desc.toString();
		if (activityName.equals("NewProductionActivity")){
			Machine m=Launcher.machines.get(Integer.parseInt(inputParts[2]));
			System.out.println(m.machineId+" i=3 "+inputParts[3]+" i=4 "+inputParts[4]);
			m.setMultiplier(Integer.parseInt(inputParts[3]));
			m.setArticleInProduction(inputParts[4]);
			return "DONE!";
			//TODO: handle employee action.
		} else if (activityName.equals("GetAllMachinesState")){
			String response="";
			for (Machine m:Launcher.machines.values()){
				response+=m.machineId+" "+m.article_in_production_label.getText()+" "+m.pieces_multiplier_label.getText()+" "+m.number_of_pieces_label.getText()
				+" "+m.error_label.getText()+";";
			}
			return response;
		} else if (activityName.equals("CheckQuotes")){

		}
		return "FAIL";

	}

}
