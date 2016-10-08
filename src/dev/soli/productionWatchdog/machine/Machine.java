package dev.soli.productionWatchdog.machine;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.GUI.Window;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.utils.Utils;

public class Machine {

	public String machine_id;	
	private String log_path;

	//Errors variables
	private enum MachineState { RUNNING, IN_ERRROR, RESET, NO_CONNECTION };
	private MachineState state;
	public boolean errorState=true;
	private static final long TIME_FOR_RUNNING_STATE=4000000000L;//after this time (in nanoseconds) elapses from an error event 
	//and the machine has been running, it can be considered running.
	private long timeAtLastErrorEnd=0;
	public int errorHappened=-1;
	public int error_info=-1;

	//Connection variables
	private static final int portOffset=3000;//port number at which the ports starts to be assigned. 
	//if you change this, you will have to change the port number in the machine code.
	private static final int socket_timeout=60000;//60 seconds: time for client to respond to server in milliseconds 
	//before server assumes that connection is lost.
	private int port;
	private boolean connected;
	private Socket socket;
	private ServerSocket serverSocket;
	private DataInputStream dataInputStream;

	//Graphic variables
	private JPanel panel;
	public JLabel article_in_production_label=null;
	public JLabel number_of_pieces_label=null;
	public JLabel error_label=null;

	/**
	 * 
	 * Creates an instance of an object representing a machine.
	 * It has its own port for the connection thread, its own panel in the window and its own voice in the database.
	 * 
	 * @param machine_id
	 * 
	 */
	public Machine(String machine_id) {

		this.machine_id=machine_id;
		this.state=MachineState.NO_CONNECTION;
		log_path=Launcher.logDirectory+"/Machine_"+this.machine_id+"/"+Utils.getDayAndMonth()+".txt";

		//Graphic
		addMachineOnGui();//also retrieves value of number of pieces made from database

		//Connection
		connected=false;
		port=portOffset+Integer.parseInt(machine_id);
		connect();

	}

	/**
	 * 
	 * Decodes the error, passed as parameter, sets the error_label to the error and changes color to machine panel.
	 * 
	 * @param input
	 * 
	 */
	private void handleError(int error,Boolean running) {

		errorHappened=error;

		switch (state) {
		case IN_ERRROR:
			error_info=1;
			break;
		case RUNNING:
			error_info=0;
			break;
		case RESET:
			error_info=-1;
			break;
		case NO_CONNECTION:
			error_info=-2;
			break;
		default:
			break;
		}
		if (state==MachineState.RUNNING){
			if (System.nanoTime()-timeAtLastErrorEnd>TIME_FOR_RUNNING_STATE) {//Actually running
				error_label.setText("Running");
				panel.setBackground(new Color(238,238,238));
				if (errorState){
					errorState=false;
					log("Production started "+number_of_pieces_label.getText());
				}
			} else {//Running, but not sure if for testing or if it's all OK
				error_label.setText("Running, but has recently stopped");
				panel.setBackground(new Color(255,255,0));
			}
		} else if (state==MachineState.IN_ERRROR) {
			timeAtLastErrorEnd=System.nanoTime();
			String errorDescription=Launcher.errors.get(error);
			error_label.setText(errorDescription);
			panel.setBackground(Utils.colors[error]);
			if (errorState==false) {//Switched from running to stopped
				errorState=true;
				System.nanoTime();
				//Showing which error happened on GUI
				log("Production stopped beacuse of error "+error+" - "+errorDescription+" - "+number_of_pieces_label.getText());
			}

		}

	}

	/**
	 * 
	 * Connects to the machine at the port 2000+the machine id using TCP/IP protocol.
	 * 
	 */
	private void connect() {

		Thread t;
		t = new Thread() {
			@Override
			public void run() {
				try {
					serverSocket=new ServerSocket(port);//Each machine must have a different port for communication
					serverSocket.setReuseAddress(true);
					serverSocket.setSoTimeout(socket_timeout);//After socket_timeout milliseconds of a non-responding connection the socket is closed
				} catch (IOException e) {
					state=MachineState.NO_CONNECTION;
					error_label.setText("Connection failed, reconnect manually");
					System.out.println("Cannot create socket on port "+port);//Usually happens if port is already in use
				}
				System.out.println("Waiting for client "+machine_id);
				try {
					socket=serverSocket.accept();//beginning of actual connection
					connected=true;
					//Showing in the GUI that the machine is connected
					error_label.setText("Connected and running");
					panel.setBackground(new Color(238,238,238));
					System.out.println("Recieved a call from: " + socket+"\n");
					run_connection();//actually doing something with the connection made
				} catch (InterruptedIOException iioe) {
					System.out.println("Connection failed");
					connected=false;
					state=MachineState.NO_CONNECTION;
					error_label.setText("Connection failed, reconnect manually");
					panel.setBackground(new Color(255,0,0));
				} catch (IOException e1) {
					System.out.println("Connection failed");
					connected=false;
					state=MachineState.NO_CONNECTION;
					error_label.setText("Connection failed, reconnect manually");
					panel.setBackground(new Color(255,0,0));
				}
			}
		};
		t.start();

	}

	/**
	 * 
	 * Runs the connection between the server and the machine entering an infinite while loop for listening to the data input stream.
	 * The while loop is broken when the read blocks for timeout milliseconds.
	 * 
	 */
	private void run_connection() {

		try {
			dataInputStream = new DataInputStream(socket.getInputStream());
			Integer number_of_error=-1,number_of_pieces=-1;
			Boolean running=false;
			while(connected==true) {
				try {
					socket.setSoTimeout(socket_timeout);
					number_of_pieces=dataInputStream.readInt(); //Type can be changed, but the change must be here and in the plc's code
					number_of_error=dataInputStream.readInt();//Type can be changed, but the change must be here and in the plc's code
					running=dataInputStream.readBoolean();//Type can be changed, but the change must be here and in the plc's code
				} catch (InterruptedIOException e) {
					dataInputStream.close();
					System.out.println("Client isn't responding...\nTrying to restart connection...");
					connected=false;
					state=MachineState.NO_CONNECTION;
					error_label.setText("Connection failed! Trying to restart connection...");
					panel.setBackground(new Color(255,0,0));
					reconnect();//Trying to reconnect
					e.printStackTrace();
					break;
				}
				state=running?MachineState.RUNNING:MachineState.IN_ERRROR;
				number_of_pieces_label.setText(number_of_pieces.toString());
				error_label.setText(Launcher.errors.get(number_of_error));
				handleError(number_of_error,running);
			}
		} catch (IOException e) {
			System.out.println("CONNECTION INTERRUPTED...\nTrying to restart connection...");
			connected=false;
			state=MachineState.NO_CONNECTION;
			error_label.setText("Connection failed! Trying to restart connection...");
			panel.setBackground(new Color(255,0,0));
			reconnect();//Trying to reconnect automatically because it can be that the connection isn't stable
		}

	}

	/**
	 * 
	 * Tries to reconnect to the machine.
	 * 
	 */
	private void reconnect() {

		if (connected==false 
				&& !error_label.getText().equals("Reconnecting...") 
				&& !error_label.getText().equals("Connecting...")){
			try {
				if (socket!=null && !socket.isClosed())
					socket.close();
				if (!serverSocket.isClosed())
					serverSocket.close();
				if (dataInputStream!=null)
					dataInputStream.close();
				connected=false;
				state=MachineState.NO_CONNECTION;
				System.out.println("Closed connection with machine "+machine_id);
				error_label.setText("Reconnecting...");
				panel.setBackground(new Color(255,255,0));
				connect();
			} catch (IOException e) {
				System.out.println("Couldn't close connection with machine "+machine_id);
			}
		} else {
			System.out.println("already connected, can't reconnect");
		}

	}

	//TODO: finish this method with the new database tables configuration.
	/**
	 * 
	 * @param machine_id
	 * @returns a HashMap containing the error descriptions as keys and the error durations as values.
	 * 
	 */
	public static HashMap<String,Long> getErrorDurations(String machine_id){

		HashMap<String, Long> errorDurations=new HashMap<String, Long>();//HashMap for holding error description and error duration

		return errorDurations;

	}

	/**
	 * 
	 * Resets the errors duration and the number of pieces made by the current machine.
	 * 
	 */
	public void reset() {

		article_in_production_label.setText("NOTHING");
		number_of_pieces_label.setText(""+-1);
		errorHappened=-1;
		errorState=false;
		state=MachineState.RESET;
		log("Reset - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText());

	}

	/**
	 * 
	 * Logs to file the state of the machine: it writes the number of pieces made until now and the state of error if there's one.
	 * 
	 */
	public void log(String logDescription) {

		log_path=Launcher.logDirectory+"/Machine_"+this.machine_id+"/"+Utils.getDayAndMonth()+".txt";
		File logFile = new File(log_path);
		if(!logFile.exists()) {
			try {
				logFile.getParentFile().mkdirs();
				logFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Couldn't create log file for "+Utils.getDayAndMonth());
			}
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(log_path,true))) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			if (!number_of_pieces_label.getText().equals("NaN"))
				out.println(timeStamp + " machine " + machine_id +" "+logDescription);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		}
		Launcher.machineDatabaseHandler.updateDatabase(machine_id, article_in_production_label.getText(), number_of_pieces_label.getText(), errorHappened, error_info);

	}

	/**
	 * 
	 * Adds to the GUI a panel that displays info about the machine, providing:
	 * 	the machine's ID,
	 * 	the number of pieces made, accessing the database to retrieve the value (if there is one),
	 * 	the error that the machine encountered,
	 * 	a button to delete data and connection about this machine.
	 * 
	 * @param machine_id
	 * @param pieces
	 * @param error_code
	 *
	 */
	private void addMachineOnGui() {

		panel=new JPanel();
		JLabel machine_id_label=new JLabel("Machine_"+machine_id);
		panel.add(machine_id_label);
		article_in_production_label=new JLabel(Launcher.machineDatabaseHandler.getArticleInProduction(MachineDataBaseHandler.is_db_connected()?machine_id:"Couldn't retrieve value from db"));
		panel.add(article_in_production_label);
		number_of_pieces_label=new JLabel("NaN");
		//getting data from database
		number_of_pieces_label.setText(""+Launcher.machineDatabaseHandler.getNumberOfPieces(machine_id));//sets the label to "-1" if there were no fata in database
		panel.add(number_of_pieces_label);
		error_label=new JLabel("Connecting...");
		panel.add(error_label);
		panel.setBackground(new Color(255,255,0));
		JButton button=new JButton("RECONNECT!");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if (connected==false){
					reconnect();
					System.out.println("Reconnecting...");
				}
			}
		});
		panel.add(button);
		button=new JButton("RESET!");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Resetting...");
				reset();
			}
		});
		panel.add(button);
		panel.setBorder(BorderFactory.createTitledBorder("Machine " + machine_id));
		GridLayout layout = new GridLayout();
		panel.setLayout(layout);
		Window.panel.add(panel);
		Window.panel.revalidate();

	}

}
