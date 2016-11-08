package dev.soli.productionWatchdog.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.swing.JFrame;

public class SimulatedMachineClient extends Thread {

	int id;

	@SuppressWarnings("resource")
	public void run() {
		//Connessione della Socket con il Server
		String ip=null;//"192.168.1.103";//ip of the server
		int port=3000+id;//port on which server listens
		Socket socket = null;
		try {
			socket = new Socket(ip,port);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//Stream di byte da passare al Socket
		DataOutputStream os = null;
		try {
			os = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String file[]=Utils.loadFileAsString("C:/Users/Public/Documents/ProductionWatchdog/test.txt").split("\\r\\n|\\n|\\r");
		while (true) {
			for (String s:file){
				String a[]=s.split(" ");
				try {
					//os.write(ByteBuffer.allocate(4).putInt(Integer.parseInt(a[0])).array());
					//os.write(ByteBuffer.allocate(4).putInt(Integer.parseInt(a[1])).array());
					//os.writeByte(Boolean.parseBoolean(a[2])==true?new Byte((byte) 0):new Byte((byte) 1));
					os.write(((ByteBuffer.allocate(9).putInt(0, Integer.parseInt(a[0]))).putInt(4, Integer.parseInt(a[1]))).put(8,Boolean.parseBoolean(a[2])==true?new Byte((byte) 1):new Byte((byte) 0)).array());
					os.flush();
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public SimulatedMachineClient(int i)  throws IOException {
		this.id=i;
	}


	public static void main(String[] args) throws Exception{
		JFrame frame=new JFrame("SimulatedMachinesClients");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		SimulatedMachineClient t=new SimulatedMachineClient(5);
		t.start();
		/*for (int i=1;i<40;i++){
		SimulatedMachineClient t=new SimulatedMachineClient(i);
		t.start();
		}*/
	}

}
