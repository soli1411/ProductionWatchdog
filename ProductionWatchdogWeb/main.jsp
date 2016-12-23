<script type="text/javascript">
function setValue(){
document.getElementById("dropdown").value=document.getElementById("tableName").value;
document.productForm.submit();
return true;
}
</script>
<%@ 
page import="java.sql.*,java.io.*,java.util.Arrays"
%>
<%
try {
   ProcessBuilder pb = new ProcessBuilder("java", "-jar", "QueryDbNames.jar");
   pb.directory(new File("C:\\ProductionWatchdog"));
   Process p = pb.start();
   p.waitFor();
   StringBuilder builder = new StringBuilder();
   BufferedReader br = new BufferedReader(new FileReader("C:\\ProductionWatchdog\\dbNames.txt"));
   String line;
   while((line = br.readLine()) != null){
       if (!line.equals("machines"))
	    builder.append("<option value=\""+line+"\">"+line+"\n");
   }
   br.close();
%>
<center>
<h2>Show database:</h2>
<form method="post" action="showDatabase.jsp" name="productForm">
    <select id="tableName" name="tableName" onchange="return setValue();">
        <option value="machines">machines
	<%=builder.toString()%>
    </select>
    <input type="submit" value="click" name="btn_dropdown">
    </form>
<h2>Show total durations chart:</h2>
<form method="post" action="showDurationChart.jsp" name="productForm">
    <select id="machine_id" name="machine_id" onchange="return setValue();">
        <%=builder.toString()%>
    </select>
    <input type="submit" value="click" name="btn_dropdown">
    </form>
<h2>Show timeline chart:</h2>
<form method="post" action="showTimelineChart.jsp" name="productForm1">
    <select id="machine_id" name="machine_id" onchange="return setValue();">
        <%=builder.toString()%>
    </select>
    <input type="submit" value="click" name="btn_dropdown1">
    </form>
</center>
<%
}
catch (Exception e){
    out.println("An exception occurred: " + e.getMessage());
}
%>