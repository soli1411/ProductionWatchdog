package dev.soli.productionWatchdog.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.database.MobileStationDataBaseHandler;
import dev.soli.productionWatchdog.utils.Utils;

public class Window {

	public static JFrame frame=null;
	private static JScrollPane scrollPane=null;
	private static JMenuBar menuBar=null;
	private static JMenu fileMenu=null,databaseMenu=null,chartMenu=null,helpMenu=null;
	public static JPanel panel=null;
	private static URL iconURL = Window.class.getResource("/icon.png");

	/**
	 * 
	 * Main window creator and handler.
	 * The window has its own frame, that automatically starts at full screen.
	 * Inside the JFrame there's a JScroll pane the contains a panel that contains every machine row panel.
	 * Each row displays the machine_ID, the number of pieces made till the last input received 
	 * and the description of the error that occurred, if there's one.
	 * The window also has its own JMenubar to control the application.
	 *
	 */
	public static void createWindow(){

		//Setting the frame
		frame = new JFrame("Production Watchdog");
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ImageIcon icon = new ImageIcon(iconURL);
		frame.setIconImage(icon.getImage());
		frame.setLayout(new BorderLayout());
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);

		//Setting the menu
		menuBar = new JMenuBar();
		fileMenu = new JMenu("Menu");
		databaseMenu = new JMenu("Database");
		chartMenu=new JMenu("Chart");
		helpMenu=new JMenu(" ? ");
		menuBar.add(fileMenu);
		menuBar.add(databaseMenu);
		menuBar.add(chartMenu);
		menuBar.add(helpMenu);

		//Setting the scroll panes
		panel = new JPanel(new GridLayout(0,1));//rows as needed, 1 column
		scrollPane = new JScrollPane(panel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVisible(true);

		//Creating the legend on the top of the window
		JPanel top=new JPanel();
		top.setBackground(new Color(255,255,0));//Yellow background
		JLabel machine_id_label=new JLabel("MACHINE_ID");
		JLabel current_article_label=new JLabel("ARTICLE");
		JLabel number_of_pieces_label=new JLabel("NUMBER OF PIECES");
		JLabel error_label=new JLabel("ERROR");
		JLabel reconnect_button_label=new JLabel("RECONNECT BUTTON");
		JLabel reset_button_label=new JLabel("RESET BUTTON");
		GridLayout layout = new GridLayout();
		top.setLayout(layout);
		top.add(machine_id_label);
		top.add(current_article_label);
		top.add(number_of_pieces_label);
		top.add(error_label);
		top.add(reconnect_button_label);
		top.add(reset_button_label);
		//Adding components
		frame.add(top,BorderLayout.NORTH);
		frame.add(scrollPane,BorderLayout.CENTER);
		frame.setJMenuBar(menuBar);
		frame.setVisible(true);

		initializeActionComponents();

	}

	/**
	 * 
	 * Initializes the resources that implement action listeners.
	 * 
	 */
	private static void initializeActionComponents(){

		//Adding elements to the menu bar
		JMenuItem menuItem;

		//Menu voice:"ADD SINGLE CLIENT". Adds a single machine to the listened ones (if it's not already listened).
		menuItem=new JMenuItem("Add single machine");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.addMachineFromUserInput();
			}
		});
		fileMenu.add(menuItem);

		//Menu voice:"ADD ALL MACHINES". Adds all the default machines ,
		//whose IDs are stored in the "machines_list.txt" file, to the watched ones.
		menuItem = new JMenuItem("Add all machines");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.loadMachinesFromFile();
			}
		});
		fileMenu.add(menuItem);

		//Menu voice: Modify machines list, allows user to modify the list of the listened machines.
		menuItem=new JMenuItem("Modify machines list");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.openWithFileEditor(Utils.machine_list_path,true);
			}
		});
		fileMenu.add(menuItem);

		//Menu voice: Modify error list, allows user to modify the list of the possible errors.
		menuItem=new JMenuItem("Modify error list");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.openWithFileEditor(Utils.error_list_path,true);
			}
		});
		fileMenu.add(menuItem);

		//Menu voice: Help!, allows the user to read the READ_ME file, where there are instruction to run the application.
		menuItem=new JMenuItem("Help!");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextArea textArea=new JTextArea(Utils.loadFileAsString(Utils.read_me_path));
				JScrollPane scrollPane=new JScrollPane(textArea);
				textArea.setLineWrap(true);  
				textArea.setWrapStyleWord(true); 
				textArea.setEditable(false);
				scrollPane.setPreferredSize(new Dimension(500, 500));
				JOptionPane.showMessageDialog(null, scrollPane, "HELP!", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		helpMenu.add(menuItem);

		//Menu voice: connect to database. Allows the user to enter the password in order to connect to the database
		menuItem=new JMenuItem("Connect to database");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						//Machine database
						if (!MachineDataBaseHandler.is_db_connected()){
							Launcher.machineDatabaseHandler=new MachineDataBaseHandler();
						} else {
							JOptionPane.showMessageDialog(null, "Already connected to machine database!");
						}
						//Employee database
						if (!MobileStationDataBaseHandler.is_db_connected()){
							Launcher.mobileStationDatabaseHandler=new MobileStationDataBaseHandler();
						} else {
							JOptionPane.showMessageDialog(null, "Already connected to mobile station database!");
						}
					}
				};
				t.start();
			}
		});
		databaseMenu.add(menuItem);

		//Menu voice: show machine's database content. Allows the user to see the machines related database content.
		//N.B.:the data displayed on GUI can be out of sync.
		menuItem=new JMenuItem("Show machine database");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						Utils.showSingleMachineDataBaseOnGui();
					}
				};
				t.start();
			}
		});
		databaseMenu.add(menuItem);

		//Menu voice: show database content. Allows the user to see the database content.
		//N.B.:the data displayed on GUI can be out of sync.
		menuItem=new JMenuItem("Show employees database");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						MobileStationDataBaseHandler.showDataBaseOnGui();
					}
				};
				t.start();
			}
		});
		databaseMenu.add(menuItem);

		//Menu voice: disconnect machine database. Allows the user to disconnect from database.
		menuItem=new JMenuItem("Disconnect machine database");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						MachineDataBaseHandler.disconnect();
					}
				};
				t.start();
			}
		});
		databaseMenu.add(menuItem);

		//Menu voice: show single machine chart. Allows the user to see the database content in a pie or a bar chart.
		//N.B.:the data displayed on GUI can be out of sync.
		menuItem=new JMenuItem("Single machine chart");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						Utils.showSingleMachineChart();
					}
				};
				t.start();
			}
		});
		chartMenu.add(menuItem);

		//Menu voice: show comparison chart. Allows the user to see the database content for the selected machines in a bar chart.
		//N.B.:the data displayed on GUI can be out of sync.
		menuItem=new JMenuItem("Multiple machines chart");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				//Creating a new thread so that the main window can continue to function.
				Thread t;
				t = new Thread() {
					public void run() {
						Utils.showMultipleMachineChart();
					}
				};
				t.start();
			}
		});
		chartMenu.add(menuItem);

	}

}
