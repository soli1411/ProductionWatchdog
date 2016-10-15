package dev.soli.productionWatchdog.GUI;

import java.awt.BorderLayout;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
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
import org.jfree.ui.RefineryUtilities;

import dev.soli.productionWatchdog.database.MachineDataBaseHandler;

@SuppressWarnings("serial")
public class TimelineCharts extends ApplicationFrame {

	public TimelineCharts(String title,String startDate, String endDate) {
		super(title);
		TimePeriodValuesCollection timeLineDataset=createTimeLineDataset(startDate,endDate);
		showTotalErrorDuration("1");
		JFreeChart chart=ChartFactory.createXYBarChart(
				title,
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
		chartPanel.setMouseZoomable( true , true );  
		setLocationRelativeTo(null);     
		setContentPane( chartPanel );
	}

	private static String convertDuration(long duration){
		long seconds=duration/1000L;
		long minutes=seconds/60L;
		long hours=minutes/60L;
		long days=hours/24L; 
		return days+":"+hours%24L+":"+minutes%60L+":"+seconds%60L;
	}

	public static void showTotalErrorDuration(String machine_id) {
		new MachineDataBaseHandler();
		Statement statement=null;
		ResultSet rs=null;
		try{
			statement=MachineDataBaseHandler.getConnection().createStatement();
			String s = "select * from machine_"+machine_id+";";
			rs=statement.executeQuery(s);
			Vector<String> column=new Vector<String>();
			column.add("Article in production"); column.add("error description"); column.add("error duration");
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
						row.add(previousArticle);row.add(""+j);row.add(convertDuration(m.get(j)));
						data.add(row);
						row=new Vector<String>();
						System.out.println(j+" "+convertDuration(m.get(j)));
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
				row.add(previousArticle);row.add(""+j);row.add(convertDuration(m.get(j)));
				data.add(row);
				row=new Vector<String>();
				System.out.println(j+" "+convertDuration(m.get(j)));
			}
			JFrame frame=new JFrame("Machine "+machine_id+" total error durations table");
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
			frame.setLocationRelativeTo(null);
			JPanel panel=new JPanel();
			JTable table=new JTable(data,column);
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

	private TimePeriodValuesCollection createTimeLineDataset(String startDate, String endDate) 
	{
		new MachineDataBaseHandler();//TODO: remove this and refer to the local instance

		TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();      
		Statement c=null;
		try {
			c=MachineDataBaseHandler.getConnection().createStatement();
			ResultSet rs=c.executeQuery("select * from mareca.machine_1 where date>\""+startDate+"\" and date<\""+endDate+"\";");
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

	//TODO: context menu with a form that allows user to replot chart in place changing parameters.
	//TODO: form for get user input of dates and machine of interest.
	public static void main( final String[ ] args )
	{
		final String title = "Time Series Management";         
		final TimelineCharts demo = new TimelineCharts(title,"2014-9-12 18","2017-10-15 18:41:40");
		demo.pack( );         
		RefineryUtilities.positionFrameRandomly( demo );         
		demo.setVisible( true );
	}
}

