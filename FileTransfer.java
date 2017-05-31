import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class FileTransfer {
	
	public static void main(String[] args) throws Exception{
		boolean modeSelected = false;
		if(args.length >= 1){
			if(args[0].equals("makekeys") && args.length == 1){
				makeKeys();
				modeSelected = true;
			} else{
				System.out.println("Proper usage to make keys: java FileTransfer makekeys");
			}
			
			if(args[0].equals("client") && args.length == 4 ){
				String pubKey = args[1];
				String host = args[2];
				String port = args[3];
				clientMode(port, host, pubKey);
				modeSelected = true;
			} else if(!modeSelected){
				System.out.println("Proper usage to run in client mode: java FileTransfer client <public key file> <host> <port>");
			}
			
			if(args[0].equals("server") && args.length == 3){
				String privKey = args[1];
				String port = args[2];
				serverMode(privKey, port);
				modeSelected = true;
			} else if(!modeSelected){
				System.out.println("Proper usage to run in server mode: java FileTransfer server <private key file> <port>");
			}
		} else{
			System.out.println("Proper usage requires at least one command line argument.");
			System.out.println("Try one of the following options to see details on how to use: ");
			System.out.println("'java FileTransfer makekeys' this will run without any addional arguments");
			System.out.println("'java FileTransfer client' requires additional arguments");
			System.out.println("'java FileTransfer server' requires additional arguments");
		}
		
	}

	private static void serverMode(String priveKey, String port) {
		try {
			ServerSocket serSocket = new ServerSocket(Integer.parseInt(port));
			Socket socket = serSocket.accept();
			
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			InputStreamReader isr = new InputStreamReader(ois);
			BufferedInputStream bis = new BufferedInputStream(ois);
			//byte[] b = new byte[10];
 			//bis.read(b);
 			//System.out.println(b.toString());
			StartMessage s = (StartMessage)ois.readObject();
			System.out.println(s.getFile());
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			AckMessage ack = new AckMessage(0);
			oos.writeObject(ack);
			
		} catch (Exception e) { }
		
	}

	private static void clientMode(String port, String host, String pubKey) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(pubKey));
		RSAPublicKey publicKey = (RSAPublicKey) in.readObject();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey sessionKey = keyGen.generateKey();
		
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, publicKey);
		byte[] wrappedSessionKey = cipher.wrap(sessionKey);
		
		System.out.println("Enter the file path: " );
		Scanner kb = new Scanner(System.in);
		String path = kb.next();
		File file = new File(path);
		System.out.println("Enter chunk size [1024]: ");
		byte chunkSize = kb.nextByte();
		StartMessage sm = new StartMessage(file.getName(), wrappedSessionKey, chunkSize);
		try (Socket socket = new Socket(host, Integer.parseInt(port))) {
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(sm);
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			AckMessage ack = (AckMessage)ois.readObject();
			System.out.println(ack.getSeq());
		}
		
		
	}

	private static void makeKeys() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(4096); // you can use 2048 for faster key generation
			KeyPair keyPair = gen.genKeyPair();
			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();
			try (ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(new File("public.bin")))) {
				oos.writeObject(publicKey);
			}
			try (ObjectOutputStream oos = new ObjectOutputStream( 
					new FileOutputStream(new File("private.bin")))) {
				oos.writeObject(privateKey);
			}
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace(System.err);
			}
		
	}
}