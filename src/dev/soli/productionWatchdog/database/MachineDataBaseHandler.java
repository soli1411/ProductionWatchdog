package dev.soli.productionWatchdog.database;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.utils.Utils;

/**
 * 
 * Database manager class. The database is made so that each machine as its own table, with columns made like these:
 * date (DateTime, not null, primary key) | article_in_production VARCHAR(50) | number_of_pieces INT UNSIGNED |
 * error_code INT | error_state BOOLEAN
 * The entries older than Launcher.daysToKeep days are deleted.
 *
 */
public class MachineDataBaseHandler {

	//Dedicated user, password and database;
	private static final String URL = "jdbc:mysql://192.168.1.223/mareca_produzione";
	private static final String dbName="mareca_produzione";
	//private static final String URL = "jdbc:mysql://127.0.0.1:3306/";
	//private static final String dbName="mareca";
	private static final String USER = "mareca";
	private static final String PASSWORD="|VBSQQA_]_";
	private static boolean db_connected;
	private static Connection connection;

	/**
	 * 
	 * Manager for the database connection activity. 
	 * Creates a connection to the MySQL dedicated database:
	 * URL, USER and PASSWORD are specified at the top of this class.
	 * 
	 */
	public MachineDataBaseHandler() {

		db_connected=false;
		connection = createConnection();
		try {
			Statement statement = connection.createStatement();
			statement.execute("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			statement.execute("USE "+dbName+";");
			try {
				for (int machine_id:Launcher.machines.keySet()) {
					statement.execute("CREATE TABLE IF NOT EXISTS machine_"+machine_id+" ("
							+ "date DATETIME NOT NULL PRIMARY KEY,"
							+ "article_in_production VARCHAR(50),"
							+ "number_of_pieces INT UNSIGNED,"
							+ "error_code INT,"
							+ "error_state TINYINT"
							+ ");"
							);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("Error creating table machine");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * Creates connection with MySQL server.
	 * 
	 * @return Connection created
	 * 
	 */
	private static Connection createConnection() {

		Connection connection = null;
		try {
			//Establishing Java-MySQL connection
			connection = DriverManager.getConnection(URL, USER, Utils.lolledalotwiththisname(PASSWORD));
			db_connected=true;
		} catch (SQLException e) {
			System.out.println("ERROR: Unable to Connect to Database.");
			JOptionPane.showMessageDialog(null, "ERROR: Unable to Connect to Database.");
			e.printStackTrace();
			db_connected=false;
		}
		return connection;

	}

	/**
	 * 
	 * @param machine_id
	 * @returns the latest number of pieces saved in the database for the specified machine.
	 * 
	 */
	public int getNumberOfPieces(String machine_id) {

		ResultSet rs=null;
		try {
			Statement statement=connection.createStatement();
			rs = statement.executeQuery("select * from "+dbName+".machine_"+machine_id+" order by date desc limit 1;");//gets the last entry in order of date
		} catch (SQLException e) {
			System.out.println("Couldn't retrieve the number of pieces for the machine "+machine_id);
			e.printStackTrace();
		}
		int res=-1;
		try {
			while (rs.next()){
				res = rs.getInt("number_of_pieces");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;

	}

	/**
	 * 
	 * @param machine_id
	 * @returns the number of pieces saved in the database for the specified machine.
	 * 
	 */
	public String getArticleInProduction(String machine_id){

		ResultSet rs=null;
		try {
			Statement statement=connection.createStatement();
			rs = statement.executeQuery("SELECT article_in_production FROM "+dbName+".machine_"+machine_id+";");
		} catch (SQLException e) {
			System.out.println("Couldn't retrieve the article in production for the machine "+machine_id);
			e.printStackTrace();
		}
		String res="NONE";
		try {
			while (rs.next()){
				res = rs.getString("article_in_production");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;

	}

	/**
	 * 
	 * Adds a new entry to the database of the machine specified that logs the data received as parameters.
	 * 
	 * @param machine_id
	 * @param article_in_production
	 * @param number_of_pieces
	 * @param error_code
	 * @param error_state
	 * 
	 */
	public void updateDatabase(int machine_id, String article_in_production, String number_of_pieces, int error_code, int error_state) {

		Date dt = new Date();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime=sdf.format(dt);
		Statement statement=null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			statement.execute("INSERT INTO "+dbName+".machine_"+machine_id+" (date,article_in_production,number_of_pieces,error_code,error_state) "
					+ "VALUES('"+currentTime+"','"+article_in_production+"',"+number_of_pieces+","+error_code+","+error_state+");");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Couldn't update database!");
		}

	}

	/**
	 * 
	 * @returns database connection status.
	 * 
	 */
	public static boolean is_db_connected() {

		return db_connected;

	}

	/**
	 * 
	 * @returns database Connection.
	 * 
	 */
	public static Connection getConnection() {

		return connection;

	}

	/**
	 * 
	 * @param daysBack
	 *
	 */
	public static void deleteEntriesOlderThanNDays(long daysBack){

		Statement statement=null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			String date=new SimpleDateFormat("yyyy.MM.dd HH-mm-ss").format(new Timestamp(System.currentTimeMillis() - (daysBack * 24L * 60L * 60L * 1000L)));
			for (Integer machine_id:Launcher.machines.keySet()){
				statement.execute("delete from "+dbName+".machine_"+machine_id+" where date<"+date);
			}
		} catch (SQLException e) {
			System.out.println("Couldn't delete entries from database!");
		}

	}

	/**
	 * 
	 * Shows the database content in a JTable.
	 * 
	 */
	public static void showDataBaseOnGui(String machine_id) {

		Statement statement=null;
		ResultSet rs=null;
		try{
			statement = connection.createStatement();
			String s = "select * from machine_"+machine_id+";";
			rs = statement.executeQuery(s);
			ResultSetMetaData rsmt = rs.getMetaData();
			int c = rsmt.getColumnCount();
			Vector<String> column = new Vector<String>(c);
			for(int i = 1; i <= c; i++){
				column.add(rsmt.getColumnName(i));
			}
			Vector<Vector<String>> data = new Vector<Vector<String>>();
			Vector<String> row = new Vector<String>();
			while(rs.next()){
				row = new Vector<String>(c);
				for(int i = 1; i <= c; i++){
					row.add(rs.getString(i));
				}
				data.add(row);
			}
			JFrame frame = new JFrame();
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
			frame.setLocationRelativeTo(null);
			JPanel panel = new JPanel();
			JTable table = new JTable(data,column);
			JScrollPane jsp = new JScrollPane(table);
			panel.setLayout(new BorderLayout());
			panel.add(jsp,BorderLayout.CENTER);
			frame.setContentPane(panel);
			frame.setVisible(true);
		}catch(Exception e){
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "ERROR");
		}finally{
			try{
				statement.close();
				rs.close();
			}catch(Exception e){
				JOptionPane.showMessageDialog(null, "ERROR CLOSE");
			}
		}

	}

}
