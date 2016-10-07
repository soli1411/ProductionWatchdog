package dev.soli.productionWatchdog.database;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import dev.soli.productionWatchdog.utils.Utils;

public class MachineDataBaseHandler {

	//Dedicated user, password and database;
	//private static final String URL = "jdbc:mysql://192.168.1.223/mareca_produzione";
	//private static final String dbName="mareca_produzione";
	private static final String URL = "jdbc:mysql://127.0.0.1:3306/mareca";
	private static final String dbName="mareca";
	private static final String USER = "mareca";
	private static final String PASSWORD="|VBSQQA_]_";
	private static final String tableName="machines";
	private static boolean db_connected;
	private static Connection connection;

	/**
	 * 
	 * Manager for the database connection activity. 
	 * Creates a connection to the MySQL dedicated database:
	 * URL, USER and PASSWORD are specified at the top of this class.
	 * 
	 */
	public MachineDataBaseHandler(){

		db_connected=false;
		connection = createConnection();
		try {
			Statement statement = connection.createStatement();
			statement.execute("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			statement.execute("USE "+dbName+";");
			try {
				statement.execute("CREATE TABLE IF NOT EXISTS "+tableName+" ("
						+ "machine_id INT NOT NULL PRIMARY KEY,"
						+ "article_in_production VARCHAR(50),"
						+ "number_of_pieces INT SIGNED,"
						+ "date_entered TIMESTAMP"
						+");"
						);
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("Error creating table "+tableName);
			}
		} catch (SQLException e) {
			System.out.println("asd");
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
			//JOptionPane.showMessageDialog(null, "Database connected sucessfully!");
			db_connected=true;
		} catch (SQLException e) {
			System.out.println("ERROR: Unable to Connect to Database.");
			JOptionPane.showMessageDialog(null, "ERROR: Unable to Connect to Database.");
			db_connected=false;
		}
		return connection;

	}

	/**
	 * 
	 * Creates a row in the machine table dedicated to the machine specified by the machine_id.
	 * If that row doesn't already exist, the number_of_pieces is set to -1.
	 * 
	 * @param machine_id
	 * 
	 */
	public void add_machine(String machine_id) {

		try {
			Timestamp timestamp = new Timestamp(new Date().getTime());
			Statement statement=connection.createStatement();
			statement.execute("INSERT IGNORE INTO "+dbName+"."+tableName+" (machine_id, article_in_production, number_of_pieces, date_entered) VALUES("+machine_id+", 'NOTHING',"+-1+",'"+timestamp+"');");
			System.out.println("Machine "+machine_id+" inserted into database");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error adding machine to db");//Probably because the voice already existed in the database
		}

	}

	/**
	 * 
	 * @param machine_id
	 * @returns the number of pieces saved in the database for the specified machine.
	 * 
	 */
	public int getNumberOfPieces(String machine_id){

		ResultSet rs=null;
		try {
			Statement statement=connection.createStatement();
			rs = statement.executeQuery("SELECT number_of_pieces FROM "+dbName+"."+tableName+" WHERE machine_id="+machine_id+";");
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
			rs = statement.executeQuery("SELECT article_in_production FROM "+dbName+"."+tableName+" WHERE machine_id="+machine_id+";");
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
	 * @param machine_id
	 * @param error
	 * @return the total duration for the error error of machine machine_id
	 * 
	 */
	public static long getErrorDuration(String machine_id,int error){

		ResultSet rs=null;
		try {
			Statement statement=connection.createStatement();
			rs = statement.executeQuery("SELECT error_"+error+" FROM "+dbName+"."+tableName+" WHERE machine_id="+machine_id+";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		long res=0;
		try {
			while (rs.next()){
				res = rs.getLong("error_"+error);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;

	}

	/**
	 * 
	 * Updates the duration of the error occurred to machine machine_id with the sum of the previous stored value and the errorDuration
	 * and also updates the number_of_pieces made by the machine and the current article that is being produced.
	 * 
	 * @param machine_id
	 * @param article_in_production
	 * @param number_of_pieces
	 * @param error
	 * @param errorDuration
	 * 
	 */
	public void updateDatabase(String machine_id, String article_in_production, String number_of_pieces, int error, long errorDuration) {

		Statement statement=null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			statement.execute("UPDATE "+dbName+"."+tableName+" SET article_in_production = '"+article_in_production+"' WHERE machine_id = "+machine_id+";");
			statement.execute("UPDATE "+dbName+"."+tableName+" SET error_"+error+" = error_"+error+" + "+errorDuration+", number_of_pieces="+number_of_pieces+" WHERE machine_id = "+machine_id+";");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Couldn't update database!");
		}

	}
	
	/**
	 * 
	 * Creates a column for the error passed as parameter in the data base.
	 * 
	 * @param error
	 * 
	 */
	public static void addError(int error){

		try {
			Statement statement=connection.createStatement();
			statement.execute("ALTER TABLE "+dbName+"."+tableName+" ADD error_"+error+" BIGINT SIGNED DEFAULT 0;");//the field is of type long so the duration is stored as seconds elapsed
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Couldn't add column to table "+tableName);//probably because it was already added
		}

	}

	/**
	 * 
	 * @returns database connection status.
	 * 
	 */
	public static boolean is_db_connected(){

		return db_connected;

	}

	/**
	 * 
	 * @returns database Connection.
	 * 
	 */
	public static Connection getConnection(){

		return connection;

	}

	/**
	 * 
	 * Shows the database content in a JTable.
	 * 
	 */
	public static void showDataBaseOnGui() {

		Statement statement = null;
		ResultSet rs = null;
		try{
			statement = connection.createStatement();
			String s = "select * from machines;";
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
