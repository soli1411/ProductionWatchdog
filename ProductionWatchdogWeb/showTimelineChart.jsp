<html> 
<head>
<title><%=request.getParameter("machine_id")+" timeline chart"%></title>
</head>
<body>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/><h2><%=request.getParameter("machine_id")%> timeline chart:</h2></center></b></span><br/>
<br/>
<center>
<script type="text/javascript" src="chartsloader.js"></script>
<script type="text/javascript">
  google.charts.load("current", {packages:["timeline"]});
  google.charts.setOnLoadCallback(drawChart);
  function drawChart() {
    var container = document.getElementById('timelineChart');
    var chart = new google.visualization.Timeline(container);
    var dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ type: 'string', id: 'Error code' });
    dataTable.addColumn({ type: 'string', id: 'Article in production' });
    dataTable.addColumn({ type: 'datetime', id: 'Start' });
    dataTable.addColumn({ type: 'datetime', id: 'End' });
    dataTable.addRows([
<%@ 
page import="java.sql.*,java.io.*,java.util.Arrays"
%>
<%
try {
   ProcessBuilder pb = new ProcessBuilder("java", "-jar", "SaveTimelineToFile.jar", request.getParameter("machine_id"));
   pb.directory(new File("C:\\ProductionWatchdog"));
   Process p = pb.start();
   p.waitFor();
   StringBuilder builder = new StringBuilder();
   BufferedReader br = new BufferedReader(new FileReader("C:\\ProductionWatchdog\\Timeline.txt"));
   String line;
   while((line = br.readLine()) != null){
	builder.append(line);
   }
   br.close();
%>
<%=builder.toString()%>
<%
}
catch (Exception e){
    out.println("An exception occurred: " + e.getMessage());
}
%>
    ]);
    var options = {
    hAxis: {
        format: 'dd/MM/yyyy hh:mm:ss'
    },
    timeline: { colorByRowLabel: true }
    };
    chart.draw(dataTable, options);
  }
</script>
<div id="timelineChart" style="height: 400px;"></div>
<br/>
</center>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/></center></b></span><br/>
<br/>
</body>
</html>