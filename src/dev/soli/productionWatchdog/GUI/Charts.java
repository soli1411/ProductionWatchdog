package dev.soli.productionWatchdog.GUI;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import dev.soli.productionWatchdog.Launcher;
import dev.soli.productionWatchdog.database.MachineDataBaseHandler;

public class Charts {

	/**
	 * 
	 * @param chartName
	 * @param pieDataset
	 * @return a ChartPanel containing the pie chart made using the pieDataset.
	 * 
	 */
	public static ChartPanel createPiePanel(String chartName, PieDataset pieDataset) {

		JFreeChart pieChart = createPieChart(chartName, pieDataset);
		return new ChartPanel(pieChart);

	}

	/**
	 * 
	 * @param chartName
	 * @param dataset
	 * @return the pie chart created using the PieDataSet parameter
	 * 
	 */
	private static JFreeChart createPieChart(String chartName, PieDataset dataset) {

		JFreeChart chart = ChartFactory.createPieChart(chartName,  // chart title
				dataset,             // data
				true,               // include legend
				true,
				true
				);
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setSectionOutlinesVisible(false);
		plot.setSimpleLabels(true);
		plot.setNoDataMessage("No data available!\nProbably because no error has happened yet.");
		PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator("{0}: {1} ({2})", new DecimalFormat("0"), new DecimalFormat("0%"));
		plot.setLabelGenerator(gen);
		return chart;

	}

	/**
	 * 
	 * Constructs a PieDataset based on the map it takes as parameter.
	 * 
	 * @param map
	 * @return PieDataset constructed
	 * 
	 */
	private static PieDataset createPieDataset(Map<String,Long> m) {

		DefaultPieDataset dataset = new DefaultPieDataset();
		for (String key:m.keySet()){
			dataset.setValue(key, m.get(key));
		}
		return dataset;

	}

	/**
	 * 
	 * @param chartName
	 * @param pieDataset
	 * @returns a ChartPanel containing the bar chart made using the pieDataset.
	 * 
	 */
	public static ChartPanel createBarPanel(String chartName, CategoryDataset pieDataset) {

		JFreeChart barChart = createBarChart(chartName, pieDataset);
		return new ChartPanel(barChart);

	}

	/**
	 * 
	 * @param chartName
	 * @param dataset
	 * @return the bar chart created
	 *
	 */
	private static JFreeChart createBarChart(String chartName, CategoryDataset dataset) {

		JFreeChart chart = ChartFactory.createBarChart(
				chartName,         // chart title
				chartName,               // domain axis label
				"Error duration (nanosec)",                  // range axis label
				dataset,                  // data
				PlotOrientation.VERTICAL, // orientation
				true,                     // include legend
				true,                     // TOOLTIPS?
				false                     // URLS?
				);
		CategoryPlot plot=chart.getCategoryPlot();
		plot.setNoDataMessage("No data available!\nProbably because no error has happened yet.");
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		return chart;

	}

	/**
	 * 
	 * @param machine_id
	 * @param m
	 * @return the CategoryDataset created
	 * 
	 */
	private static CategoryDataset createBarDataset(String machine_id,Map<String,Long> m) {

		String category1 = "machine "+machine_id;
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (String key:m.keySet()){
			dataset.addValue(m.get(key),key,category1);
		}
		return dataset;

	}

	/**
	 * 
	 * Creates a robot that right clicks at the (x,y) coordinate on the screen.
	 * 
	 * @param x
	 * @param y
	 * @throws AWTException
	 * 
	 */
	public static void click(double x, double y) throws AWTException{

		Robot bot = new Robot();
		bot.mouseMove((int)x, (int)y);
		bot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		bot.mouseRelease(InputEvent.BUTTON3_MASK);

	}

	/**
	 * 
	 * Creates a pie and a bar chart of the data in a separated JFrame for the specified machine_id.
	 * 
	 * @param machine_id
	 * @param Map<String,Long> errorDurations
	 * 
	 */
	public static void showSingleMachineChart(String machine_id, Map<String, Long>errorDurations){

		JFrame frame=new JFrame("Chart of errors of machine "+machine_id);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);

		PieDataset pieDataset=createPieDataset(errorDurations);
		ChartPanel piePanel=createPiePanel("Machine "+machine_id+" errors", pieDataset);
		CategoryDataset barDataset=createBarDataset(machine_id,errorDurations);
		ChartPanel barPanel=createBarPanel("Machine "+machine_id+" errors", barDataset);

		JScrollPane p=new JScrollPane();
		p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		p.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		p.getViewport().add(piePanel);
		p.setVisible(true);

		//Creating menu to display save options.
		JMenuBar menuBar=new JMenuBar();
		JMenu menu=new JMenu("File");
		JMenuItem menuItem=new JMenuItem("Show options");
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					click(p.getLocationOnScreen().getX()+1,p.getLocationOnScreen().getY()+1);
				} catch (AWTException e1) {
					e1.printStackTrace();
				}
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);		

		JPanel top=new JPanel();	
		top.setLayout(new GridLayout());
		JButton button=new JButton("Pie chart");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				p.getViewport().removeAll();
				p.getViewport().add(piePanel);
			}
		});
		top.add(button);
		button=new JButton("Bar chart");
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				p.getViewport().removeAll();
				p.getViewport().add(barPanel);
			}
		});
		top.add(button);

		frame.add(top, BorderLayout.NORTH);
		frame.add(p,BorderLayout.CENTER);
		frame.setVisible(true);

	}

	/**
	 * 
	 * @param machinesToChart
	 * @return a CategoryDataset built on the machines errors specified in the parameter
	 * 
	 */
	private static CategoryDataset createBarDataset(ArrayList<String> machinesToChart) {

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();//add(error_duaration,machine_id,error_id)
		for (Integer error_id:Launcher.errors.keySet()){
			String error_desc=Launcher.errors.get(error_id);
			for (String machine_id:machinesToChart){
				dataset.addValue(MachineDataBaseHandler.getErrorDuration(machine_id, error_id),machine_id,error_desc);
			}
		}
		return dataset;

	}

	/**
	 * 
	 * Creates a comparison bar chart in a separated JFrame for the specified machinesToChart.
	 * 
	 * @param machinesToChart
	 * 
	 */
	public static void showMultipleMachineChart(ArrayList<String> machinesToChart) {

		JFrame frame=new JFrame("Comparison chart of errors durations");
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);//Full screen
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);

		CategoryDataset barDataset=createBarDataset(machinesToChart);
		ChartPanel barPanel=createBarPanel("Comparison chart of errors durations", barDataset);

		JScrollPane p=new JScrollPane();
		p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		p.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		p.getViewport().add(barPanel);
		p.setVisible(true);

		//Creating menu to display save options.
		JMenuBar menuBar=new JMenuBar();
		JMenu menu=new JMenu("File");
		JMenuItem menuItem=new JMenuItem("Show options");
		//Action on menu button pressed
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					click(p.getLocationOnScreen().getX()+1,p.getLocationOnScreen().getY()+1);
				} catch (AWTException e1) {
					e1.printStackTrace();
				}
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);		
		frame.add(p,BorderLayout.CENTER);
		frame.setVisible(true);

	}

}
