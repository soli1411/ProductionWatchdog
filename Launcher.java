package dev.soli.productionWatchdog;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import dev.soli.productionWatchdog.GUI.Window;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.database.MobileStationDataBaseHandler;
import dev.soli.productionWatchdog.machine.Machine;
import dev.soli.productionWatchdog.mobileStations.MobileStationsHandler;
import dev.soli.productionWatchdog.utils.Utils;

/*
 * 
 * This controller application monitors the specified machines, showing on a GUI and saving to HD and DB 
 * the number of pieces made by each machine and the duration of the errors that each machine encountered
 * during the production process.
 * For instruction on how to use see the "READ_ME.txt" file of the application.
 *
 */

/**
 * 
 * Contains the starting point of the program. This class starts a new window, initializes resources and connections with machines and databases.
 * It also runs a routine for updating data and saving them to the local disk (log files) and a server (database).
 * 
 */
public class Launcher {
	
	//TODO: handle reset from machine(PLC).
	//TODO: make all machines communicate on the same port, they have different IPs, so it's not going to be a problem. 
	//For now use different ports, so that you can simulate all the machines on local host.
	//TODO: finish employee database and mobile stations part.
	//TODO: count running time.
	//Idea: take a PLC input as a flag that is set by the operator when the machine starts and it is reset when the machine finish or a new product is being made.

	//Port for checking only one application at time is running 
	private static final int APPLICATION_BIND_PORT=9998;
	private static ServerSocket singleInstanceServerSocket;

	//Refresh_time for the tick method (in milliseconds).
	private static final int TICK_TIME=600000;//10 minutes.

	//Log directory path
	public static final String logDirectory="C:/Users/Public/Documents/ProductionWatchdog/Logs";
	public static final int DAYSTOKEEP=32;//number of days that the logs are held into memory. If the files are older, then they are deleted.

	//TreeMap of <machine_id,machine object>, used to keep track of connected machines.
	public static Map<String,Machine> machines=new TreeMap<String, Machine>();

	//TreeMap of <error_id,error description>, used to keep track of errors
	public static NavigableMap<Integer,String> errors=new TreeMap<Integer, String>();

	//Manager for the machine related database connection and activities.
	public static MachineDataBaseHandler machineDatabaseHandler;

	//Manager for the mobile stations connections and activities.
	public static MobileStationsHandler mobileStationsHandler;

	//Manager for the mobile stations related database connections and activities.
	public static MobileStationDataBaseHandler mobileStationDatabaseHandler;

	/**
	 * 
	 * Main entrance of the program. This is the point where everything begins.
	 * 
	 * @param args, not used.
	 * 
	 */
	public static void main(String[] args) {

		//Allowing only one instance of the program at time.
		checkIfRunning();

		//Operations to do before the process is closed, so it's safe to terminate it without data loss.
		addCloseOperation();

		//Starting the GUI
		Window.createWindow();

		//Loading the list of the machines to be watched and the list of errors that the machines could came across
		Utils.loadErrorsFromFile();
		Utils.loadMachinesFromFile();

		//Starting connection with database
		machineDatabaseHandler=new MachineDataBaseHandler();
		mobileStationDatabaseHandler=new MobileStationDataBaseHandler();//TODO: needed?? where? why?

		//Creating new instances of the machines
		Utils.startMachines();
		
		//Starting connections with mobile stations
		mobileStationsHandler=new MobileStationsHandler();//TODO: finish this part
		
		//Logging and updating stored data
		Utils.deleteFilesOlderThanNdays(DAYSTOKEEP, logDirectory);//deletes log files older than daysToKeep days.

		tick();//logging and updating database periodically

	}

	/**
	 * 
	 * Checking if the application is already running: if so display massage dialog and exit with error code 2.
	 * N.B.:the shoot down hook is added after this function so that there can't be any conflict with
	 * data saved, connections and databases.
	 * 
	 */
	private static void checkIfRunning() {

		try {
			//Bind to local host adapter with a zero connection queue 
			singleInstanceServerSocket=new ServerSocket(APPLICATION_BIND_PORT,0,InetAddress.getByAddress(new byte[] {127,0,0,1}));
		}
		catch (BindException e) {
			JOptionPane.showMessageDialog(null, "Already running!");
			System.out.println("Already running!");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("Unexpected error");
			e.printStackTrace();
			try {
				singleInstanceServerSocket.close();
			} catch (IOException e1) {
				System.out.println("Couldn't close socket for the single instace");
				e1.printStackTrace();
			}
			System.exit(2);
		}

	}

	/**
	 * 
	 * Routine that runs periodically every TICK_TIME milliseconds to update the database content and to log to file.
	 * 
	 */
	private static void tick() {

		Thread t=new Thread() {
			@Override
			public void run() {

				while(true){
					try {
						sleep(TICK_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Running "+Thread.activeCount()+" Threads");
					for (Machine m:machines.values()){
						m.log("Tick - pieces="+m.number_of_pieces_label.getText()+" error="+m.error_label.getText());
						System.out.println("machine="+m.machine_id+" NUMBER OF PIECES="+m.number_of_pieces_label.getText());
					}
				}

			}
		};	
		t.start();

	}

	/**
	 * 
	 * Adds a shoot down hook to the application, so that if it is closed (even by task manager) it will save its data.
	 * 
	 */
	private static void addCloseOperation() {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				//logging and saving to database
				for (Machine m:machines.values()){
					m.log("Closing application - pieces="+m.number_of_pieces_label.getText()+" error="+m.error_label.getText());
				}
				//closing database connection
				if (MachineDataBaseHandler.is_db_connected())
					try {
						MachineDataBaseHandler.getConnection().close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				try {
					singleInstanceServerSocket.close();
				} catch (IOException e1) {
					System.out.println("Couldn't close socket for the single instace");
					e1.printStackTrace();
				}
			}
		});

	}

}
