package dev.soli.productionWatchdog.GUI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Second;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import dev.soli.productionWatchdog.database.MachineDataBaseHandler;

@SuppressWarnings("serial")
public class TimelineCharts extends ApplicationFrame 
{
	public TimelineCharts( final String title )
	{
		super( title );         
		final TimePeriodValuesCollection dataset = createDataset();         
		final JFreeChart chart = ChartFactory.createXYBarChart(
	            title,
	            "Timeline->",
	            true,
	            "Error state",
	            dataset,
	            PlotOrientation.VERTICAL,
	            true,
	            false,
	            false
	        );       
		final ChartPanel chartPanel = new ChartPanel( chart );         
		chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 370 ) );         
		chartPanel.setMouseZoomable( true , false );         
		setContentPane( chartPanel );
	}

	@SuppressWarnings("deprecation")
	private TimePeriodValuesCollection createDataset( ) 
	{
		final TimePeriodValues  series = new TimePeriodValues ( "Time series" );         
		new MachineDataBaseHandler();
		Statement c=null;
		try {
			c=MachineDataBaseHandler.getConnection().createStatement();
			ResultSet rs=c.executeQuery("select * from mareca.machine_1;");
			ArrayList<Second> sec=new ArrayList<Second>();
			ArrayList<Integer> states=new ArrayList<Integer>();
			while (rs.next()){
				Timestamp ts=rs.getTimestamp("date");
				sec.add(Second.parseSecond(ts.toString()));
				states.add(rs.getBoolean("error_state")==true?1:0);
			}
			for (int i=0;i<sec.size()-1;i++){
				series.add(new SimpleTimePeriod(sec.get(i).getStart(),sec.get(i+1).getStart()),states.get(i));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();
		dataset.addSeries(series);
		dataset.setDomainIsPointsInTime(false);
		return dataset;
	}

	private JFreeChart createChart( final XYDataset dataset ) 
	{
		return ChartFactory.createTimeSeriesChart(             
				"Computing Test", 
				"Seconds",              
				"Value",              
				dataset,             
				false,              
				false,              
				false);
	}

	public static void main( final String[ ] args )
	{
		final String title = "Time Series Management";         
		final TimelineCharts demo = new TimelineCharts( title );         
		demo.pack( );         
		RefineryUtilities.positionFrameRandomly( demo );         
		demo.setVisible( true );
	}
}

