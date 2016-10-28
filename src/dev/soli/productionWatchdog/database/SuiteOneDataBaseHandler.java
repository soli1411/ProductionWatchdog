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

public class SuiteOneDataBaseHandler {

	//Dedicated user, password and database;
	private static final String URL = "jdbc:mysql://192.168.1.223/mareca_produzione";
	private static final String dbName="mareca_produzione";
	//private static final String URL = "jdbc:mysql://127.0.0.1:3306/";
	//private static final String dbName="mareca";
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
	public SuiteOneDataBaseHandler() {

		db_connected=false;
		connection = createConnection();
		try {
			Statement statement = connection.createStatement();
			statement.execute("CREATE DATABASE IF NOT EXISTS "+dbName+";");
			statement.execute("USE "+dbName+";");
			try {
				statement.execute("CREATE TABLE IF NOT EXISTS "+tableName+" ("
						+ "Codice_Macchina INT NOT NULL PRIMARY KEY,"
						+ "Conta_Pezzi INT UNSIGNED"
						+ ");"
						);
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
	 * Adds the passed machine ID to the table.
	 * @param machine_id
	 * 
	 */
	public void addMachine(int machine_id){
	
		Statement s=null;
		try {
			s=connection.createStatement();
			s.execute("INSERT IGNORE INTO "+dbName+"."+tableName+" (Codice_Macchina, Conta_Pezzi) VALUES ("+machine_id+","+0+");");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 * @param machine_id
	 * @param number_of_pieces
	 * 
	 */
	public void updateNumberOfPieces(int machine_id, String number_of_pieces){
		
		Statement s=null;
		try {
			s=connection.createStatement();
			s.execute("update "+dbName+"."+tableName+" machines set Conta_Pezzi="+number_of_pieces+" where Codice_Macchina="+machine_id+";");
		} catch (Exception e) {
			e.printStackTrace();
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
	 * Shows the database content in a JTable.
	 * 
	 */
	public static void showDataBaseOnGui() {

		Statement statement=null;
		ResultSet rs=null;
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
