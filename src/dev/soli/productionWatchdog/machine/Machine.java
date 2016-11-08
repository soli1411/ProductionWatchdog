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

	public int machineId;	
	private String logPath;

	private static final long TIME_FOR_RUNNING_STATE=4*1000000000L;//TODO: set to 3 minutes//after this time (expressed as number of seconds * second in nanoseconds) elapses from an error event 
	//and the machine has been running, it can be considered running.
	private long timeAtLastErrorEnd;
	private int errorInfo;
	private boolean errorState;
	private int multiplier;

	//Connection variables
	private static final int PORT_OFFSET=3000;//port number at which the ports starts to be assigned. 
	//if you change this, you will have to change the port number in the machine code.
	private static final int SOCKET_TIMEOUT=60000;//60 seconds: time for client to respond to server in milliseconds 
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
	 * @param machineId
	 * 
	 */
	public Machine(int machine_id) {

		this.machineId=machine_id;
		errorState=true;
		errorInfo=-2;
		logPath=Launcher.logDirectory+"/Machine_"+this.machineId+"/"+Utils.getDayAndMonth()+".txt";
		timeAtLastErrorEnd=System.nanoTime()-TIME_FOR_RUNNING_STATE-1000L;//so that when the application is started a log is done.

		//Graphic
		addMachineOnGui();//also retrieves value of number of pieces made from database

		//SuiteOneDatabase
		Launcher.suiteOneDataBaseHandler.addMachine(machine_id);

		//Connection
		connected=false;
		port=PORT_OFFSET+machine_id;
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

		if (running) {
			if (System.nanoTime()-timeAtLastErrorEnd>TIME_FOR_RUNNING_STATE) {//Actually running
				errorInfo=0;
				error_label.setText("Running");
				panel.setBackground(new Color(238,238,238));
				if (errorState){
					errorState=false;
					log("Production started "+number_of_pieces_label.getText(),
							new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date(new Date().getTime()-TIME_FOR_RUNNING_STATE/1000000)));
				}
			} else {//Running, but not sure if for testing or if it's all OK -> no log required.
				error_label.setText("Running, but has recently stopped");
				panel.setBackground(new Color(255,255,0));
			}
		} else {
			timeAtLastErrorEnd=System.nanoTime();
			String errorDescription=Launcher.errors.get(error);
			errorInfo=error;
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
					serverSocket.setSoTimeout(SOCKET_TIMEOUT);//After SOCKET_TIMEOUT milliseconds of a non-responding connection the socket is closed
				} catch (IOException e) {
					errorState=true;
					errorInfo=-2;
					error_label.setText("Connection failed, reconnect manually");
					System.out.println("Cannot create socket on port "+port);//Usually happens if port is already in use
					log("Lost connection - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
				}
				System.out.println("Waiting for client "+machineId);
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
					errorState=true;
					errorInfo=-2;
					error_label.setText("Connection failed, reconnect manually");
					panel.setBackground(new Color(255,0,0));
					log("Lost connection - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
				} catch (IOException e1) {
					System.out.println("Connection failed");
					connected=false;
					errorState=true;
					errorInfo=-2;
					error_label.setText("Connection failed, reconnect manually");
					panel.setBackground(new Color(255,0,0));
					log("Lost connection - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
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
					socket.setSoTimeout(SOCKET_TIMEOUT);
					byte[] a=new byte[18];//first 4 bytes first UDINT, second 4 bytes second UDINT, last bit of last byte equals !running
					dataInputStream.read(a);
					number_of_pieces=Utils.byteArrayToInt(Arrays.copyOfRange(a,0,4));//Type can be changed, but the change must be here and in the plc's code
					number_of_error=Utils.byteArrayToInt(Arrays.copyOfRange(a,4,8));//Type can be changed, but the change must be here and in the plc's code
					number_of_error=number_of_error==0?43:number_of_error;//Changing mapping of error 0 in the machine for possible problems avoidance
					running=(a[8]&1)==0?false:true;//TODO: verify correctness of this //Type can be changed, but the change must be here and in the plc's code
				} catch (InterruptedIOException e) {
					dataInputStream.close();
					System.out.println("Client isn't responding...\nTrying to restart connection...");
					connected=false;
					errorState=true;
					errorInfo=-2;
					error_label.setText("Connection failed! Trying to restart connection...");
					panel.setBackground(new Color(255,0,0));
					log("Lost connection - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
					reconnect();//Trying to reconnect
					e.printStackTrace();
					break;
				}
				error_label.setText(Launcher.errors.get(number_of_error));
				if (number_of_pieces<(Integer.parseInt(number_of_pieces_label.getText())/multiplier)){//Reset
					errorInfo=-1;
					number_of_pieces_label.setText(""+(number_of_pieces*multiplier));
					log("Reset - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
					errorState=true;
					timeAtLastErrorEnd=System.nanoTime()-TIME_FOR_RUNNING_STATE-1000L;//so that when the restart happens a running log is done.
					continue;
				}
				number_of_pieces_label.setText(""+(number_of_pieces*multiplier));
				handleError(number_of_error,running);
			}
		} catch (IOException e) {
			System.out.println("CONNECTION INTERRUPTED...\nTrying to restart connection...");
			connected=false;
			errorState=true;
			errorInfo=-2;
			error_label.setText("Connection failed! Trying to restart connection...");
			panel.setBackground(new Color(255,0,0));
			log("Lost connection - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
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
				errorState=true;
				errorInfo=-2;
				error_label.setText("Reconnecting...");
				System.out.println("Closed connection with machine "+machineId);
				panel.setBackground(new Color(255,255,0));
				connect();
			} catch (IOException e) {
				System.out.println("Couldn't close connection with machine "+machineId);
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
	public void disconnectForClosing() {

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
				System.out.println("Couldn't close connection with machine "+machineId);
			}
		}
		errorInfo=-3;
		error_label.setText("Application closed");

	}

	/**
	 * 
	 * Sets the article in production label to the new string passed as argument.
	 * 
	 * @param newArticle
	 * 
	 */
	public void setArticleInProduction(String newArticle) {

		if (!newArticle.equals("") && !newArticle.equals(null)){
			article_in_production_label.setText(newArticle);
			errorInfo=-4;
			Launcher.machineDatabaseHandler.updateArticleInProduction(machineId, newArticle);
			logToFile("Changed article in production - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
					new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
		}

	}

	/**
	 * 
	 * Sets the multiplier for the pieces to the new multiplier.
	 * 
	 * @param newMoltiplier
	 * 
	 */
	public void setMultiplier(int newMultiplier) {

		if (newMultiplier<=0)
			return;
		number_of_pieces_label.setText(""+Integer.parseInt(number_of_pieces_label.getText())/multiplier*newMultiplier);
		multiplier=newMultiplier;
		pieces_multiplier_label.setText(""+newMultiplier);
		errorInfo=-5;
		Launcher.machineDatabaseHandler.updateMultiplier(machineId, newMultiplier);
		logToFile("Changed multiplier - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
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
	public void startNewProduction(int newMultiplier, String newArticle) {

		if (newMultiplier<=0)
			return;
		number_of_pieces_label.setText(""+Integer.parseInt(number_of_pieces_label.getText())/multiplier*newMultiplier);
		pieces_multiplier_label.setText(""+newMultiplier);
		multiplier=newMultiplier;
		if (!newArticle.equals("") && !newArticle.equals(null))
			article_in_production_label.setText(newArticle);
		number_of_pieces_label.setText("-1");
		Launcher.machineDatabaseHandler.updateMultiplier(machineId, newMultiplier);
		Launcher.machineDatabaseHandler.updateArticleInProduction(machineId, newArticle);
		logToFile("Reset - pieces="+number_of_pieces_label.getText()+" error="+error_label.getText(),
				new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));

	}

	/**
	 * 
	 * Logs to file the state of the machine: it writes the number of pieces made until now, the state of error,
	 * the current time as a TimeStamp and a description of the event that caused the log.
	 * 
	 * @param logDescription: describes the event that caused the log.
	 * @param timeStamp: time at which the event occurred.
	 * 
	 */
	private void logToFile(String logDescription, String timeStamp) {

		logPath=Launcher.logDirectory+"/Machine_"+this.machineId+"/"+Utils.getDayAndMonth()+".txt";
		File logFile = new File(logPath);
		if(!logFile.exists()) {
			try {
				logFile.getParentFile().mkdirs();
				logFile.createNewFile();
			} catch (IOException e) {
				System.out.println("Couldn't create log file for "+Utils.getDayAndMonth());
			}
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(logPath,true))) {
			if (!number_of_pieces_label.getText().equals("NaN"))
				out.println(timeStamp + " machine " + machineId +" "+logDescription);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		}

	}

	/**
	 * 
	 * Logs to file and to the database the state of the machine: it writes the number of pieces made until now, the state of error,
	 * the current time as a TimeStamp and a description.
	 * 
	 * @param logDescription: describes the event that caused the log.
	 * @param timeStamp: time at which the event occurred.
	 * 
	 */
	public void log(String logDescription, String timeStamp) {

		logToFile(logDescription,timeStamp);
		Launcher.machineDatabaseHandler.updateMachine(machineId,article_in_production_label.getText(), 
				number_of_pieces_label.getText(),Integer.parseInt(pieces_multiplier_label.getText()),errorInfo);

	}

	/**
	 * 
	 * Adds to the GUI a panel that displays info about the machine, providing:
	 * 	the machine's ID,
	 * 	the number of pieces made, accessing the database to retrieve the value (if there is one),
	 * 	the error that the machine encountered,
	 * 	a button to delete data and connection about this machine.
	 * 
	 * @param machineId
	 * @param pieces
	 * @param error_code
	 *
	 */
	private void addMachineOnGui() {

		panel=new JPanel();
		JLabel machine_id_label=new JLabel("Machine_"+machineId);
		number_of_pieces_label=new JLabel(""+Launcher.machineDatabaseHandler.getNumberOfPieces(machineId));
		error_label=new JLabel("Connecting...");
		article_in_production_label=new JLabel(Launcher.machineDatabaseHandler.getArticleInProduction(machineId));
		article_in_production_label.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				setArticleInProduction(JOptionPane.showInputDialog("Please enter the new article:"));
			}
		});
		pieces_multiplier_label=new JLabel(Launcher.machineDatabaseHandler.getMultiplier(machineId));
		multiplier=Integer.parseInt(pieces_multiplier_label.getText());
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
		panel.add(machine_id_label);
		panel.add(article_in_production_label);
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
		panel.setBorder(BorderFactory.createTitledBorder("Machine " + machineId));
		GridLayout layout = new GridLayout();
		panel.setLayout(layout);
		Window.panel.add(panel);
		Window.panel.revalidate();

	}

}
