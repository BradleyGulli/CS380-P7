import java.net.*;
import java.io.*;
import java.util.*;

public class ServerListener extends Thread {
	private Socket socket;
	
	public ServerListener(Socket socket){
		this.socket = socket;
	}
	
	public void run(){
		try{
			 InputStream is = socket.getInputStream();
			 ObjectInputStream ois = new ObjectInputStream(is);
			 InputStreamReader isr = new InputStreamReader(ois);
			 BufferedReader br = new BufferedReader(isr);
			 
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
}
