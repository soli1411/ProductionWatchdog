package dev.soli.productionWatchdog.GUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Second;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.ui.ApplicationFrame;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;
import dev.soli.productionWatchdog.utils.Utils;

public class TimelineCharts {

	/**
	 * 
	 * Displays on the GUI a time line chart of the errors that the machines have encountered from startDate to endDate.
	 * @param machinesToChart
	 * @param startDate
	 * @param endDate
	 * 
	 */
	private static void displayTimelineCharts(ArrayList<String> machinesToChart,String startDate, String endDate) {
		for (String machine_id:machinesToChart){
			JFrame f=new JFrame("Machine_"+machine_id+" timeline chart");
			TimePeriodValuesCollection timeLineDataset=createTimeLineDataset(machine_id,startDate,endDate);
			JFreeChart chart=ChartFactory.createXYBarChart(
					"Machine_"+machine_id+" timeline chart",
					"Timeline->",
					true,
					"Error state",
					timeLineDataset,
					PlotOrientation.VERTICAL,
					true,
					true,
					false
					);
			chart.getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setMouseZoomable(true,true);
			f.setDefaultCloseOperation(ApplicationFrame.DISPOSE_ON_CLOSE);
			f.setLocationRelativeTo(null);  
			f.setContentPane(chartPanel);
			f.setExtendedState(ApplicationFrame.MAXIMIZED_BOTH);
			f.setVisible(true);
		}
	}

	/**
	 * 
	 * @param duration
	 * @return the duration converted to days:hours:minutes:seconds.
	 * 
	 */
	private static String convertDuration(long duration){
		long seconds=duration/1000L;
		long minutes=seconds/60L;
		long hours=minutes/60L;
		long days=hours/24L; 
		return String.format("%02d : %02d : %02d : %02d",days,hours%24L,minutes%60L,seconds%60L);
	}

	/**
	 * 
	 * Displays a JTable containing the sum of the errors duration divided by article in production.
	 * @param machine_id
	 * 
	 */
	private static void showTotalErrorDuration(int machine_id) {
		Statement statement=null;
		ResultSet rs=null;
		try{
			statement=MachineDataBaseHandler.getConnection().createStatement();
			String s = "select * from machine_"+machine_id+";";
			rs=statement.executeQuery(s);
			Vector<String> column=new Vector<String>();
			column.add("Article in production"); column.add("error code"); column.add("error description"); column.add("error duration (dd:hh:mm:ss");
			Vector<Vector<String>> data=new Vector<Vector<String>>();
			Vector<String> row=new Vector<String>();
			ArrayList<Timestamp> sec=new ArrayList<Timestamp>();
			ArrayList<Integer> states=new ArrayList<Integer>();
			Map<Integer,Long> m=new TreeMap<Integer,Long>();
			String previousArticle="";
			String currentArticle="";
			while(rs.next()){
				currentArticle=rs.getString("article_in_production");
				if (!currentArticle.equals(previousArticle)){
					for (int k=0;k<states.size()-1;k++){
						if (m.containsKey(states.get(k)))
							m.put(states.get(k),m.get(states.get(k))+sec.get(k+1).getTime()-sec.get(k).getTime());
						else
							m.put(states.get(k), sec.get(k+1).getTime()-sec.get(k).getTime());
					}
					for (int j:m.keySet()){
						row.add(previousArticle); row.add(""+j); row.add(Launcher.errors.get(j)); row.add(convertDuration(m.get(j)));
						data.add(row);
						row=new Vector<String>();
					}
					sec=new ArrayList<Timestamp>();
					states=new ArrayList<Integer>();
					m=new TreeMap<Integer,Long>();
				}
				previousArticle=currentArticle;
				int error_state=(int) rs.getByte("error_state");
				states.add(error_state==1?rs.getInt("error_code"):error_state);
				sec.add(rs.getTimestamp("date"));
			}
			for (int k=0;k<states.size()-1;k++){
				if (m.containsKey(states.get(k)))
					m.put(states.get(k),m.get(states.get(k))+sec.get(k+1).getTime()-sec.get(k).getTime());
				else
					m.put(states.get(k), sec.get(k+1).getTime()-sec.get(k).getTime());
			}
			for (int j:m.keySet()){
				row.add(previousArticle); row.add(""+j); row.add(Launcher.errors.get(j)); row.add(convertDuration(m.get(j)));
				data.add(row);
				row=new Vector<String>();
			}
			JFrame frame=new JFrame("Machine "+machine_id+" total error durations table");
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
			frame.setLocationRelativeTo(null);
			JPanel panel=new JPanel();
			JTable table=new JTable(data,column);
			table.setAutoCreateRowSorter(true);
			JScrollPane jsp=new JScrollPane(table);
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

	/**
	 * 
	 * @param machine_id
	 * @param startDate
	 * @param endDate
	 * @return a TimePeriodValuesCollection that is be used to plot the chart of the errors time line.
	 * 
	 */
	private static TimePeriodValuesCollection createTimeLineDataset(String machine_id,String startDate, String endDate) {
		TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();
		Statement c=null;
		try {
			c=MachineDataBaseHandler.getConnection().createStatement();
			ResultSet rs=c.executeQuery("select * from mareca.machine_"+machine_id+" where date>\""+startDate+"\" and date<\""+endDate+"\";");
			TimePeriodValues series=null;
			ArrayList<Second> sec=null;
			ArrayList<Integer> states=null;
			String previousArticle="";
			String currentArticle="";
			while (rs.next()) {
				currentArticle=rs.getString("article_in_production");
				if (!previousArticle.equals(currentArticle)){
					for (int i=0;series!=null && i<sec.size()-1;i++){
						series.add(new SimpleTimePeriod(sec.get(i).getStart(),sec.get(i+1).getStart()),states.get(i));
					}
					if (series!=null) {
						series.add(new SimpleTimePeriod(sec.get(sec.size()-1).getStart(),sec.get(sec.size()-1).getStart()),states.get(sec.size()-1));
						dataset.addSeries(series);
					}
					series=new TimePeriodValues(currentArticle);
					sec=new ArrayList<Second>();
					states=new ArrayList<Integer>();
				}
				previousArticle=currentArticle;
				sec.add(Second.parseSecond(rs.getTimestamp("date").toString()));
				int error_state=(int) rs.getByte("error_state");
				states.add(error_state==1?rs.getInt("error_code"):error_state);
			}
			for (int i=0;series!=null && i<sec.size()-1;i++){
				series.add(new SimpleTimePeriod(sec.get(i).getStart(),sec.get(i+1).getStart()),states.get(i));
			}
			if (series!=null) {
				series.add(new SimpleTimePeriod(sec.get(sec.size()-1).getStart(),sec.get(sec.size()-1).getStart()),states.get(sec.size()-1));
				dataset.addSeries(series);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dataset;
	}

	/**
	 * 
	 * Displays a JTable of the error durations of a user selected machines in a separated JFrame.
	 * 
	 */
	public static void showDurationTable() {

		//letting the user choose which machines to display
		String machine_list=Utils.machine_list_path;
		File f = new File(machine_list);
		String file=null;
		if(f.exists() && !f.isDirectory())
			file=Utils.loadFileAsString(machine_list);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		//creating GUI to select machines
		JFrame frame = new JFrame("Choose machines for the chart");
		frame.setLayout(new GridLayout(0,1));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.add(new JLabel("Choose machines for the chart"));
		ArrayList<String> machinesToChart=new ArrayList<String>();//holds the temporary list of the IDs of users's selected machines
		for (String s:textStr){
			JCheckBox cb=new JCheckBox(s);
			ActionListener actionListener = new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
					boolean selected = abstractButton.getModel().isSelected();
					if (selected){
						machinesToChart.add(s);
					} else {
						machinesToChart.remove(s);
					}
				}
			};
			cb.addActionListener(actionListener);
			frame.add(cb);
		}
		JButton button=new JButton("Done!");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				for (String machine_id:machinesToChart){
					showTotalErrorDuration(Integer.parseInt(machine_id));
				}
			}
		});
		frame.add(button);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);


	}

	/**
	 * 
	 * Displays a chart (that can be print or saved as .PNG) of the error durations of user selected machines.
	 * 
	 */
	public static void timeLineChart() {

		//letting the user choose which machines to display
		String machine_list=Utils.machine_list_path;
		File f = new File(machine_list);
		String file=null;
		if(f.exists() && !f.isDirectory())
			file=Utils.loadFileAsString(machine_list);
		String textStr[] = file.split("\\r\\n|\\n|\\r");
		//creating GUI to select machines
		JFrame frame = new JFrame("Choose machines for the chart");
		frame.setLayout(new GridLayout(0,1));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.add(new JLabel("Choose machines for the chart"));
		ArrayList<String> machinesToChart=new ArrayList<String>();//holds the temporary list of the IDs of users's selected machines
		for (String s:textStr){
			JCheckBox cb=new JCheckBox(s);
			ActionListener actionListener = new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
					boolean selected = abstractButton.getModel().isSelected();
					if (selected){
						machinesToChart.add(s);
					} else {
						machinesToChart.remove(s);
					}
				}
			};
			cb.addActionListener(actionListener);
			frame.add(cb);
		}
		JButton button=new JButton("Done!");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				String startDate=JOptionPane.showInputDialog("Please insert a start date in yyyy-MM-dd hh:mm:ss format.\nSelect help for more informations");
				if (!startDate.equals(null) && !startDate.equals("")){
					String endDate=JOptionPane.showInputDialog("Please insert an end date in yyyy-MM-dd hh:mm:ss format.\nSelect help for more informations");
					if (!endDate.equals(null) && !endDate.equals("")){
						displayTimelineCharts(machinesToChart,startDate,endDate);
					}
				}
			}
		});
		frame.add(button);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

}
