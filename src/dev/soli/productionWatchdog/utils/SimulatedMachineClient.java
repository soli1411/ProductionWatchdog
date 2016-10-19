package dev.soli.productionWatchdog.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
					os.writeInt(Integer.parseInt(a[0]));
					os.writeInt(Integer.parseInt(a[1]));
					os.writeBoolean(Boolean.parseBoolean(a[2]));
					os.flush();
					Thread.sleep(900);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		/*int i=0,j=0;
		while(true){
			try {
				i=Random.class.newInstance().nextInt(100010);
				j=Random.class.newInstance().nextInt(42);
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
			try {
				System.out.println(i+" "+j+" "+i%2);
				os.writeInt(i);
				os.writeInt(j);
				os.writeBoolean(i%2==0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try{
				Thread.sleep(1000);
			} catch(Exception e){
				e.printStackTrace();
			}
		}*/
	}

	public SimulatedMachineClient(int i)  throws IOException {
		this.id=i;
	}


	public static void main(String[] args) throws Exception{
		JFrame frame=new JFrame("SimulatedMachinesClients");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		//for (int i=1;i<40;i++){
		SimulatedMachineClient t=new SimulatedMachineClient(5);
		t.start();
		//}
	}

}
