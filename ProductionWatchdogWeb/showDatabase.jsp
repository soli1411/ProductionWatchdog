<!DOCTYPE html>
<html> 
 <head>
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
<title><%=request.getParameter("tableName")+" table"%></title>
</head>
<body>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/><h2><%=request.getParameter("tableName")%> TABLE:</h2></center></b></span><br/>
<table style="width:100%">
<%@ 
page import="java.sql.*,java.io.*,java.util.Arrays"
%>
<%
try {
   ProcessBuilder pb = new ProcessBuilder("java", "-jar", "Query.jar", "mareca."+request.getParameter("tableName"));
   pb.directory(new File("C:\\ProductionWatchdog"));
   Process p = pb.start();
   p.waitFor();
   StringBuilder builder = new StringBuilder();
   BufferedReader br = new BufferedReader(new FileReader("C:\\ProductionWatchdog\\query.txt"));
   String line;
   while((line = br.readLine()) != null){
	builder.append(line);
   }
   br.close();
%>
<%=builder.toString()%>
</table>
<%
}
catch (Exception e){
    out.println("An exception occurred: " + e.getMessage());
}
%>
<br/>
<span style="text-transform: uppercase;"><b><center><a href="main.jsp">menu</a><br/></center></b></span><br/>
<br/>
</body>
</html>