package dev.soli.productionWatchdog;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import dev.soli.productionWatchdog.GUI.Window;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.database.MobileStationDataBaseHandler;
import dev.soli.productionWatchdog.database.SuiteOneDataBaseHandler;
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

	//TODO: documentation in read_me.txt about the usage for the users.
	//TODO: finish employee database and mobile stations part.

	//Port for checking only one application at time is running 
	private static final int APPLICATION_BIND_PORT=9998;
	private static ServerSocket singleInstanceServerSocket;

	//Refresh_time for tick methods (in milliseconds).
	private static final int TICK_TIME=600000;//10 minutes
	private static final int TICK_SUITEONE_TIME=15000;//15 seconds

	//Log directory path
	public static final String logDirectory="C:/Users/Public/Documents/ProductionWatchdog/Logs";
	public static final int DAYS_TO_KEEP=32;//number of days that the logs are held into memory. If the files are older, then they are deleted.

	//TreeMap of <machine_id,machine object>, used to keep track of connected machines.
	public static Map<Integer,Machine> machines=new TreeMap<Integer, Machine>();

	//TreeMap of <error_id,error description>, used to keep track of errors
	public static NavigableMap<Integer,String> errors=new TreeMap<Integer, String>();

	//Manager for the machine related database connection and activities.
	public static MachineDataBaseHandler machineDatabaseHandler;

	//Manager for the SuiteOne dedicated database connection and activities.
	public static SuiteOneDataBaseHandler suiteOneDataBaseHandler;

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
		mobileStationDatabaseHandler=new MobileStationDataBaseHandler();
		suiteOneDataBaseHandler=new SuiteOneDataBaseHandler();

		//Creating new instances of the machines
		Utils.startMachines();

		//Starting connections with mobile stations
		mobileStationsHandler=new MobileStationsHandler();

		//Logging and updating stored data
		Utils.deleteLogsOlderThanNdays(DAYS_TO_KEEP, logDirectory);//deletes log files & database entries if they are older than daysToKeep days.

		tickSuiteOne();
		tick();//logging last state to file and updating machine tables in the database periodically

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
	 * Routine that runs periodically every TICK_SUITEONE_TIME milliseconds to update the database content
	 * 
	 */
	private static void tickSuiteOne(){

		Thread t=new Thread() {
			@Override
			public void run() {

				while(true){
					try {
						sleep(TICK_SUITEONE_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("TICK SUITEONE");
					for (Machine m:machines.values()){
						suiteOneDataBaseHandler.updateNumberOfPieces(m.machine_id,m.number_of_pieces_label.getText());
					}
				}

			}
		};
		t.start();

	}

	/**
	 * 
	 * Routine that runs periodically every TICK_TIME milliseconds to update the machine table in the database content and to log to file.
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
						m.log("Tick - pieces="+m.number_of_pieces_label.getText()+" error="+m.error_label.getText(),
								new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
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
					m.disconnect();
					m.log("Closing application - pieces="+m.number_of_pieces_label.getText()+" error="+m.error_label.getText(),
							new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
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
