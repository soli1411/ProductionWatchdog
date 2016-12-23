<html> 
<head>
<title><%=request.getParameter("machine_id")+" duration chart"%></title>
<style>
table, th, td {
    border: 2px solid black;
    border-collapse: collapse;
}
th, td {
    padding: 5px;
    text-align: center;    
}
</style>
<script src="sorttable.js"></script>
</head>
<body>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/><h2><%=request.getParameter("machine_id")%> duration chart:</h2></center></b></span><br/>
<br/>
<center>
<br/>
<table style="width:100%" class="sortable">
<tbody>
<%@ 
page import="java.sql.*,java.io.*,java.util.Arrays"
%>
<%
try {
   ProcessBuilder pb = new ProcessBuilder("java", "-jar", "SaveDurationToFile.jar", "mareca."+request.getParameter("machine_id"));
   pb.directory(new File("C:\\ProductionWatchdog"));
   Process p = pb.start();
   p.waitFor();
   StringBuilder builder = new StringBuilder();
   BufferedReader br = new BufferedReader(new FileReader("C:\\ProductionWatchdog\\Durations.txt"));
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
</tbody>
</table>
</center>
<br/>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/></center></b></span><br/>
<br/>
</body>
</html>