package dev.soli.productionWatchdog.database;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import dev.soli.productionWatchdog.utils.Utils;

public class MobileStationDataBaseHandler {

	//Dedicated user, password and database;
	//private static final String URL = "jdbc:mysql://192.168.1.223/mareca_produzione?autoReconnect=true";
	//private static final String USER = "mareca";
	//private static final String PASSWORD="|VBSQQA_]_";
	//private static final String dbName="mareca_produzione";
	private static final String URL = "jdbc:mysql://127.0.0.1:3306/mareca?autoReconnect=true";
	private static final String USER = "mareca";
	private static final String PASSWORD="|VBSQQA_]_";	
	private static final String tableName="employee";
	private static final String dbName="mareca";
	private static boolean db_connected;
	private static Connection connection;

	/**
	 * 
	 * Manager for the database connection activity. 
	 * Creates a connection to the MySQL dedicated database:
	 * URL, USER and PASSWORD are specified at the top of this class.
	 * 
	 */
	public MobileStationDataBaseHandler(){

		db_connected=false;
		connection = createConnection();
		try {
			Statement statement = connection.createStatement();
			statement.execute("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			statement.execute("USE "+dbName+";");
			try {
				statement.execute("CREATE TABLE IF NOT EXISTS "+tableName+" ("
						+ "employee_id VARCHAR(15) NOT NULL PRIMARY KEY,"
						+ "number_of_actions INT SIGNED,"
						+ "date_entered TIMESTAMP"
						+");"
						);
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("Error creating table "+tableName);
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
			String s = "select * from "+tableName+";";
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
