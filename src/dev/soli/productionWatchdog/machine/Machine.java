package dev.soli.productionWatchdog.machine;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.Arrays;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.GUI.Window;
import dev.soli.productionWatchdog.utils.Utils;

public class Machine {

	public int machine_id;	
	private String log_path;

	//Errors variables
	private enum MachineState { RUNNING, IN_ERRROR, RESET, NO_CONNECTION, APPLICATION_CLOSED, CHANGED_ARTICLE, CHANGED_MULTIPLIER };
	private MachineState state;
	public boolean errorState=false;
	private static final long TIME_FOR_RUNNING_STATE=180*1000000000L;//after this time (expressed as number of seconds * second in nanoseconds) elapses from an error event 
	//and the machine has been running, it can be considered running.
	private long timeAtLastErrorEnd=0;
	public int errorHappened=-6;
	public int error_info=-3;
	private int multiplier=1;

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

	//Graphical variables
	private JPanel panel;
	public JLabel article_in_production_label=null;
	public JLabel number_of_pieces_label=null;
	public JLabel error_label=null;
	public JLabel pieces_multiplier_label=null;

	/**
	 * 
	 * Creates an instance of an object representing a machine.
	 * It has its own port for the connection thread, its own panel in the window and its own voice in the database.
	 * 
	 * @param machine_id
	 * 
	 */
	public Machine(int machine_id) {

		this.machine_id=machine_id;
		this.state=MachineState.NO_CONNECTION;
		log_path=Launcher.logDirectory+"/Machine_"+this.machine_id+"/"+Utils.getDayAndMonth()+".txt";
		timeAtLastErrorEnd=System.nanoTime()-TIME_FOR_RUNNING_STATE;//so that the machine if when connect is running has a background of (238,238,238)

		//Graphic
		addMachineOnGui();//also retrieves value of number of pieces made from database

		//SuiteOneDatabase
		Launcher.suiteOneDataBaseHandler.addMachine(machine_id);

		//Connection
		connected=false;
		port=portOffset+machine_id;
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
		case APPLICATION_CLOSED:
			error_info=-3;
			break;
		case CHANGED_ARTICLE:
			error_info=-4;
			break;
		case CHANGED_MULTIPLIER:
			error_info=-5;
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
					log("Production started "+number_of_pieces_label.getText(),
							new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date(new Date().getTime()-TIME_FOR_RUNNING_STATE/1000000)));
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
				log("Production stopped beacuse of error "+error+" - "+errorDescription+" - "+number_of_pieces_label.getText(),
						new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
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
			Integer number_of_error=-1,number_of_pieces=0;
			Boolean running=false;
			while(connected==true) {
				try {
					socket.setSoTimeout(socket_timeout);
					byte[] a=new byte[18];//first 4 bytes first UDINT, second 4 bytes second UDINT, last bit of last byte equals !running
					dataInputStream.read(a);
					number_of_pieces=Utils.byteArrayToInt(Arrays.copyOfRange(a,0,4))*multiplier;//Type can be changed, but the change must be here and in the plc's code
					number_of_error=Utils.byteArrayToInt(Arrays.copyOfRange(a,4,8));//Type can be changed, but the change must be here and in the plc's code
					number_of_error=number_of_error==0?43:number_of_error;//Changing mapping of error 0 in the machine for possible problems avoidance
					running=(a[8]&1)==0?false:true;//Type can be changed, but the change must be here and in the plc's code
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

	/**
	 * 
	 * Closes the connection with the machine.
	 * 
	 */
	public void disconnect(){

		if (connected){
			try {
				if (socket!=null && !socket.isClosed())
					socket.close();
				if (!serverSocket.isClosed())
					serverSocket.close();
				if (dataInputStream!=null)
					dataInputStream.close();
				connected=false;
			} catch (IOException e) {
				System.out.println("Couldn't close connection with machine "+machine_id);
			}
		}
		state=MachineState.APPLICATION_CLOSED;
		error_info=-3;
		error_label.setText("Application closed");

	}

	/**
	 * 
	 * Resets the errors and the number of pieces made by the current machine because a new production may be started.
	 * 
	 */
	public void reset() {

		number_of_pieces_label.setText("0");
		errorHappened=-1;
		error_info=-1;
		errorState=false;
		state=MachineState.RESET;
		log("Reset - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
				new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));

	}

	/**
	 * 
	 * Logs to file the state of the machine: it writes the number of pieces made until now, the state of error,
	 * the current time as a TimeStamp and a description.
	 * 
	 */
	public void log(String logDescription, String timeStamp) {

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
			if (!number_of_pieces_label.getText().equals("NaN"))
				out.println(timeStamp + " machine " + machine_id +" "+logDescription);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		}
		Launcher.machineDatabaseHandler.updateDatabase(machine_id, article_in_production_label.getText(), 
				number_of_pieces_label.getText(), Integer.parseInt(pieces_multiplier_label.getText()), 
				errorHappened, error_info);

	}

	/**
	 * 
	 * Sets the article in production label to the new string passed as argument.
	 * 
	 * @param newArticle
	 * 
	 */
	public void setArticleInProduction(String newArticle){

		if (!newArticle.equals("") && !newArticle.equals(null))
			article_in_production_label.setText(newArticle);
		error_info=-4;
		log("Changed article in production - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
				new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));

	}

	/**
	 * 
	 * Sets the multiplier for the pieces to the new multiplier.
	 * 
	 * @param newMoltiplier
	 * 
	 */
	public void setMultiplier(int newMultiplier){

		if (newMultiplier<=0)
			return;
		pieces_multiplier_label.setText(""+newMultiplier);
		multiplier=Integer.parseInt(pieces_multiplier_label.getText());
		error_info=-5;
		log("Changed multiplier - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
				new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));

	}

	/**
	 * 
	 * Starts a new production specified with the new multiplier and the new article received as parameters.
	 * 
	 * @param newMultiplier
	 * @param newArticle
	 * 
	 */
	public void startNewProduction(int newMultiplier, String newArticle){

		if (newMultiplier<=0)
			return;
		pieces_multiplier_label.setText(""+newMultiplier);
		multiplier=Integer.parseInt(pieces_multiplier_label.getText());
		if (!newArticle.equals("") && !newArticle.equals(null))
			article_in_production_label.setText(newArticle);
		reset();

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
		number_of_pieces_label=new JLabel(""+Launcher.machineDatabaseHandler.getNumberOfPieces(""+machine_id));
		if (number_of_pieces_label.getText().equals("") || number_of_pieces_label.getText().equals(null))
			number_of_pieces_label.setText("0");
		error_label=new JLabel("Connecting...");
		panel.add(machine_id_label);
		article_in_production_label=new JLabel(Launcher.machineDatabaseHandler.getArticleInProduction(""+machine_id));
		if (article_in_production_label.getText().equals("") || article_in_production_label.getText().equals(null))
			article_in_production_label.setText("NONE");
		//on click edit text
		article_in_production_label.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				setArticleInProduction(JOptionPane.showInputDialog("Please enter the new article:"));
			}
		});
		panel.add(article_in_production_label);
		pieces_multiplier_label=new JLabel(Launcher.machineDatabaseHandler.getMultiplier(""+machine_id));
		if (pieces_multiplier_label.getText().equals("") || pieces_multiplier_label.getText().equals(null))
			pieces_multiplier_label.setText("1");
		multiplier=Integer.parseInt(pieces_multiplier_label.getText());
		//on click set multiplier
		pieces_multiplier_label.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				try {
					setMultiplier(Integer.parseUnsignedInt((JOptionPane.showInputDialog("Please enter the new multiplier:"))));
				} catch (NumberFormatException nfe){
					nfe.printStackTrace();
					JOptionPane.showMessageDialog(null,"Error: not a valid number!");
				}
			}
		});
		panel.add(pieces_multiplier_label);
		panel.add(number_of_pieces_label);
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
				if (JOptionPane.showConfirmDialog(null, "Are you sure you want to start a new production?", "STARTING NEW PRODUCTION", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
					startNewProduction(Integer.parseInt(JOptionPane.showInputDialog(null,"Please enter the new multiplier","1")),JOptionPane.showInputDialog(null,"Please enter the new article name","ARTICLE_NAME"));
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
