package dev.soli.productionWatchdog.utils;

import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.GUI.Window;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.machine.Machine;

/**
 * 
 * Class of frequently used static methods and variables that are needed for other classes.
 *
 */
public class Utils {

	public static final String machine_list_path="C:/ProductionWatchdog/machines_list.txt";
	public static final String error_list_path = "C:/ProductionWatchdog/errors_list.txt";
	public static final String read_me_path =    "C:/ProductionWatchdog/READ_ME.txt";
	public static Color[] colors;

	/**
	 * 
	 * Converts 4 bytes to an integer, where the first byte (b[0]) is the most significant.
	 * 
	 * @param b the 4 bytes to convert to INT.
	 * @return the converted integer.
	 * 
	 */
	public static int byteArrayToInt(byte[] b) {

		return   b[3] & 0xFF |
				(b[2] & 0xFF) << 8 |
				(b[1] & 0xFF) << 16 |
				(b[0] & 0xFF) << 24;

	}

	/**
	 * 
	 * @return the month and the day numbers, dash separated, as a String.
	 * 
	 */
	public static String getDayAndMonth() {

		Calendar cal = Calendar.getInstance();
		String dayOfMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
		String month = String.valueOf(cal.get(Calendar.MONTH)+1);//+1 because is 0 based.
		return month+"-"+dayOfMonth;

	}

	/**
	 * 
	 * Generates a list of n colors esthetically pleasing.
	 * 
	 * @param n, number of total colors.
	 * @return list of generated colors.
	 * 
	 */
	public static Color[] generateColors(int n) {

		Color[] colors = new Color[n];
		for(int i = 0; i < n; i++){
			colors[i] = Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
		}
		return colors;

	}

	/**
	 * 
	 * Opens the default file editor in order to allow the user to modify/read the file located at path.
	 * 
	 * @param path
	 * @param writable
	 * 
	 */
	public static void openWithFileEditor(String path, boolean writable){

		try {
			File f = new File(path);
			f.setWritable(writable);
			Desktop.getDesktop().edit(f);
		} catch (IOException e) {
			System.out.println("Desktop not supported!");
		}

	}

	/**
	 * 
	 * Returns the content of the file, located at the path passed as parameter, as a string.
	 * 
	 * @param path to the file
	 * @return file as string
	 *
	 */
	public static String loadFileAsString(String path){

		StringBuilder builder = new StringBuilder();
		try{
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line;
			while((line = br.readLine()) != null){
				builder.append(line + "\n");
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		return builder.toString();

	}

	/**
	 * 
	 * Loads all the errors, form the file located at error_list_path, in the errors map.
	 * The file is made of different rows each starting with an integer, indicating the error_id,
	 * followed by a String description of the error.
	 * This method also updates the database if any new error is encountered.
	 * It makes sure that same error isn't added twice and the content of the file is then sorted.
	 * 
	 */
	public static void loadErrorsFromFile(){

		String file=Utils.loadFileAsString(error_list_path);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		for (String s:textStr){
			String[] line_parts = s.split(" ");
			if (line_parts.length > 1){
				StringBuilder desc = new StringBuilder(line_parts[1]);
				for (int i = 2; i < line_parts.length; i++){
					desc.append(" "+line_parts[i]);
				}
				if (!Launcher.errors.containsKey(Integer.parseInt(line_parts[0]))){
					Launcher.errors.put(new Integer(line_parts[0]), desc.toString());
				}
			}
		}
		//Rewriting the content of the file so that it's sorted and with no repetition.
		try (PrintStream out = new PrintStream(new FileOutputStream(error_list_path))) {
			for (Integer key: Launcher.errors.keySet()){
				out.println(key + " " + Launcher.errors.get(key));
			}
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		}
		//Generating a list of colors for differentiate error labels backgrounds.
		colors=generateColors(Launcher.errors.lastEntry().getKey()+1);

	}

	/**
	 * 
	 * Loads all the machines, form the file located at machine_list_path, in the machine map.
	 * The file is made of different rows each made of an integer, indicating the machineId.
	 * This method also updates the database if any new machine is encountered.
	 * It makes sure that same machine isn't added twice and the content of the file is then sorted.
	 * 
	 */
	public static void loadMachinesFromFile(){

		String file=loadFileAsString(machine_list_path);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		Set<Integer> set=new TreeSet<Integer>();
		for (String s:textStr){
			try {
				set.add(Integer.parseInt(s));
			} catch(NumberFormatException e){
				System.out.println("Error reading file, not an int!");
			}
		}
		//Rewriting the content of the file so that it's sorted and with no repetition and adding new machines to the list of the listened ones.
		try (PrintStream out = new PrintStream(new FileOutputStream(machine_list_path))) {
			for (Integer i:set){
				if (!Launcher.machines.containsKey(i)){//prevents that the duplicated key
					Launcher.machines.put(i, null);//creating a new instance of machine
				}
				out.println(i);
			}
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		}

	}

	/**
	 * 
	 * Creates a new instance of every machine in Launcher.machines
	 * 
	 */
	public static void startMachines(){

		for (int i:Launcher.machines.keySet()){
			Launcher.machines.put(i, new Machine(i));
		}

	}

	/**
	 * 
	 * Loads a machine, selected by the user form the file located at machine_list_path, in the machine map.
	 * The file is made of different rows each made of an integer, indicating the machineId.
	 * This method also updates the database if any new machine is encountered.
	 * It makes sure that same machine isn't added twice and the content of the file is then sorted.
	 * 
	 */
	public static void addMachineFromUserInput(){

		String machine_list=machine_list_path;
		File f = new File(machine_list);
		String file=null;
		if(f.exists() && !f.isDirectory())
			file=Utils.loadFileAsString(machine_list);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		Integer machine_id = Integer.parseInt((String) JOptionPane.showInputDialog(null,"Choose a machine to add:",
				"Machine to add",JOptionPane.QUESTION_MESSAGE,null,textStr,textStr[1])); 
		if (Launcher.machines.containsKey(machine_id)){
			JOptionPane.showMessageDialog(Window.frame,"Already added!");
		} else if ((machine_id!=null)){
			Launcher.machines.put(machine_id, new Machine(machine_id));
		}
		loadMachinesFromFile();

	}

	/**
	 * 
	 * Magic happens here.
	 * 
	 * @param s
	 * @return a secret enchantment
	 * 
	 */
	public static String lolledalotwiththisname(String s){

		String mymomisfat="170620soli";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++)
			sb.append((char)(s.charAt(i) ^ mymomisfat.charAt(i % mymomisfat.length())));//requires a lot of MANA.
		String result = sb.toString();
		return result;

	}

	/**
	 * 
	 * Deletes files older than daysBack days located at the directory dirWay.
	 * 
	 * @param daysBack
	 * @param dirWay
	 *
	 */
	public static void deleteLogsOlderThanNdays(long daysBack, String dirWay) {

		MachineDataBaseHandler.deleteEntriesOlderThanNDays(daysBack);
		File directory = new File(dirWay);
		if(directory.exists()){
			File[] listFiles = directory.listFiles();
			long purgeTime = System.currentTimeMillis() - (daysBack * 24L * 60L * 60L * 1000L);
			for (File listFile : listFiles) {
				if (listFile.isDirectory()) {
					deleteLogsOlderThanNdays(daysBack,dirWay+"\\"+listFile.getName());//recursion to explore all sub folders
				} else {
					if (listFile.lastModified() < purgeTime) {
						if (!listFile.delete())
							System.out.println("Error deleting file: "+listFile.getName());
					}
				}
			}
		}
	}

	/**
	 * 
	 * Opens the default application associated with that file format.
	 * 
	 * @param path
	 * 
	 */
	public static void openFile(String path) {

		Desktop desktop = Desktop.getDesktop();
		if (!desktop.isSupported(Desktop.Action.EDIT)) {
			System.err.println("EDIT not supported");
			return;
		}
		try {
			desktop.open(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * Writes a JTable to an excel file.
	 * 
	 * @param table
	 * @param path
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @return the path if the file is saved successfully.
	 * 
	 */
	public static String writeToExcell(JTable table, String path) throws FileNotFoundException, IOException {

		new WorkbookFactory();
		Workbook wb = new XSSFWorkbook(); //Excel workbook
		Sheet sheet = wb.createSheet(); //WorkSheet
		Row row = sheet.createRow(2); //Row created at line 3
		TableModel model = table.getModel(); //Table model
		Row headerRow = sheet.createRow(0); //Create row at line 0
		for(int headings = 0; headings < model.getColumnCount(); headings++){ //For each column
			headerRow.createCell(headings).setCellValue(model.getColumnName(headings));//Write column name
		}
		for(int rows = 0; rows < model.getRowCount(); rows++){ //For each table row
			for(int cols = 0; cols < table.getColumnCount(); cols++){ //For each table column
				row.createCell(cols).setCellValue(model.getValueAt(rows, cols).toString()); //Write value
			}
			//Set the row to the next one in the sequence 
			row = sheet.createRow((rows + 3)); 
		}
		wb.write(new FileOutputStream(path));//Save the file     
		wb.close();
		return path;

	}

	/**
	 * 
	 * Displays on the GUI the content of the database related to the user selected machine.
	 * 
	 */
	public static void showSingleMachineDataBaseOnGui() {
		//letting the user choose which machine to display
		String machine_list=Utils.machine_list_path;
		File f = new File(machine_list);
		String file=null;
		if(f.exists() && !f.isDirectory())
			file=Utils.loadFileAsString(machine_list);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		String machine_id = (String) JOptionPane.showInputDialog(null,"Choose a machine for the table:",
				"Choose a machine",JOptionPane.QUESTION_MESSAGE,null,textStr,textStr[1]);
		if (!(machine_id==null)){
			MachineDataBaseHandler.showDataBaseOnGui(machine_id);
		}

	}

}
