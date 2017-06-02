import java.security.*;
import java.security.interfaces.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

import javax.crypto.*;
public class FileTransfer {
	
	public static void main(String[] args) throws Exception{
		boolean modeSelected = false;
		if(args.length >= 1){
			if(args[0].equals("makekeys") && args.length == 1){
				makeKeys();
				modeSelected = true;
			} else if(modeSelected){
				System.out.println("Proper usage to make keys: java FileTransfer makekeys");
			}
			
			if(args[0].equals("client") && args.length == 4 ){
				String pubKey = args[1];
				String host = args[2];
				String port = args[3];
				clientMode(port, host, pubKey);
				modeSelected = true;
			} else if(modeSelected){
				System.out.println("Proper usage to run in client mode: java FileTransfer client <public key file> <host> <port>");
			}
			
			if(args[0].equals("server") && args.length == 3){
				String privKey = args[1];
				String port = args[2];
				serverMode(privKey, port);
				modeSelected = true;
			} else if(modeSelected){
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

	private static void serverMode(String privKey, String port) {
		try {
			ServerSocket serSocket = new ServerSocket(Integer.parseInt(port));
			Socket socket = serSocket.accept();
			
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			DataInputStream dis = new DataInputStream(is);
			
			StartMessage s = (StartMessage)ois.readObject();
			byte[] wrappedSessionKey = s.getEncryptedKey();
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(privKey));
			RSAPrivateKey rsa = (RSAPrivateKey) in.readObject();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, rsa);
			Key key = cipher.unwrap(wrappedSessionKey, "AES", Cipher.SECRET_KEY);
			System.out.println(s.getFile());
			
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			AckMessage ack = new AckMessage(0);
			oos.writeObject(ack);
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			String message = "";
			
			int x = 16;
			while((x % s.getChunkSize()) == x){
				x *=2;
			}
			int size = s.getChunkSize();
			size += (x % s.getChunkSize());
			byte[] b = new byte[size];
			byte[] decrypted = null;
			while(dis.read(b) != -1){
				decrypted = cipher.doFinal(b);
				message += new String(decrypted);
				b= new byte[size];
			}
			System.out.println(message);
		} catch (Exception e) { 
			System.out.println("error");
			e.printStackTrace();
		}
		
	}

	private static void clientMode(String port, String host, String pubKey) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(pubKey));
		RSAPublicKey publicKey = (RSAPublicKey) in.readObject();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		SecretKey sessionKey = keyGen.generateKey();
		
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, publicKey);
		byte[] wrappedSessionKey = cipher.wrap(sessionKey);
		
		System.out.println("Enter the file path: " );
		Scanner kb = new Scanner(System.in);
		String filePath = kb.next();
		Path path = Paths.get(filePath);
		byte[] data = Files.readAllBytes(path);
		System.out.println(new String(data));

		System.out.println("Enter chunk size [1024]: ");
		int chunkSize = kb.nextInt();
		StartMessage sm = new StartMessage(path.getFileName().toString(), wrappedSessionKey, chunkSize);
		try (Socket socket = new Socket(host, Integer.parseInt(port))) {
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			DataOutputStream dos = new DataOutputStream(os);
			oos.writeObject(sm);
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			AckMessage ack = (AckMessage)ois.readObject();
			System.out.println(ack.getSeq());
			int leftOver = data.length % chunkSize;
			int chunks = data.length / chunkSize;
			int placement = 0;
		    cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, sessionKey);  
			for(int i = 0; i < chunks; i++){
				byte[] toSend = new byte[chunkSize];
				byte[] encrypted = null; 
				for(int j = 0, k = placement; j < chunkSize; j++, k++){
					toSend[j] = data[k];
				}
				
			    encrypted = cipher.doFinal(toSend);
			    System.out.println(encrypted.length);
				//System.out.println(new String(toSend));
				dos.write(encrypted);
				placement += chunkSize;
			}
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