import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.InputMismatchException;
import java.util.Scanner;

// Client.
public class Client {
	
	private static final String SERV_NAME = "localhost";
	private static final int SERV_PORT = 7734;
	
	private static final String VERSION = "P2P-CI/1.0";
	private static final String STAT_OK = "200 OK";
	private static final String STAT_BAD = "400 Bad Request";
	private static final String STAT_NOTF = "404 Not Found";
	private static final String STAT_VRS = "505 P2P-CI Version Not Supported";
	
	private Scanner console = new Scanner(System.in);
	
	private String hostname;
	private int upload_port;
	private ServerSocket upload_soc;
	Socket upload;
	Socket p2p_soc;
	
	// Start program.
	public static void main(String[] args) {
		Client client = new Client();
		client.start();
	}
	
	// Client main process.
	public void start() {
		
		try {
			boolean cont = true;
			
			// Get server name.
			System.out.print("Enter server name: ");
			String serv_name = console.nextLine();
			
			// Open upload port.
			System.out.print("Enter your upload port number: ");
			upload_port = console.nextInt();
			console.nextLine();
			upload_soc = new ServerSocket(upload_port);
			
			// Client thread handles P2P connections.
			ClientThread s_thd = new ClientThread(upload_soc);
			s_thd.start();
			
			// Setup connection.
			System.out.println("Connecting to " + serv_name + " on port " + SERV_PORT);
			Socket client = new Socket(serv_name, SERV_PORT);
			System.out.println("Connected to " + client.getRemoteSocketAddress());
			System.out.println();
			
			// Test
			/*
			System.out.println("Server hostname: " + client.getInetAddress().getHostName());
			System.out.println("Local address: " + client.getLocalAddress());
			System.out.println("Local socket address: " + client.getLocalSocketAddress());
			System.out.println("Local address - local host - hostname: " + client.getLocalAddress().getLocalHost().getHostName());
			System.out.println("Local address - hostname: " + client.getLocalAddress().getHostName());
			*/
			
			hostname = client.getLocalAddress().getLocalHost().getHostName(); // Updated hostname
			
			// Setup data stream.
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			
			// Send info on self to server.
			String client_info = hostname + " " + upload_port;
			out.writeUTF(client_info);
			
			// Test
			//out.writeUTF("Hello from " + client.getLocalSocketAddress());
			//System.out.println("Server says " + in.readUTF());
			
			while (cont) {
				String cmd_num;
				String rfc_num;
				String title;
				String msg;
				
				System.out.println("Commands: 1-ADD 2-LOOKUP 3-LIST 4-GET 0-EXIT");
				cmd_num = console.nextLine();
				
				if (cmd_num.length() == 1) {
					
					if (cmd_num.equals("1")) {
						// Add
						System.out.print("Enter RFC number: ");
						rfc_num = console.nextLine();
						System.out.print("Enter RFC title: ");
						title = console.nextLine();
						
						msg = addMsg(rfc_num, title);
						System.out.println("Sending ADD to server:\n" + msg);
						out.writeUTF(msg);
						
						// Receive from server.
						System.out.println("Server response:\n" + in.readUTF());
						
					} else if (cmd_num.equals("2")) {
						// Lookup
						System.out.print("Enter RFC number: ");
						rfc_num = console.nextLine();
						System.out.print("Enter RFC title: ");
						title = console.nextLine();
						
						msg = lookupMsg(rfc_num, title);
						System.out.println("Sending LOOKUP to server:\n" + msg);
						out.writeUTF(msg);
						
						// Receive from server.
						System.out.println("Server response:\n" + in.readUTF());
						
					} else if (cmd_num.equals("3")) {
						// List
						msg = listMsg();
						System.out.println("Sending LIST to server:\n" + msg);
						out.writeUTF(msg);
						
						// Receive from server.
						System.out.println("Server response:\n" + in.readUTF());
						
					} else if (cmd_num.equals("4")) {
						// Get
						System.out.println("Enter client hostname: ");
						String client_hn = console.next();
						System.out.println("Enter client upload port number: ");
						int client_p = console.nextInt();
						console.nextLine();
						System.out.println("Enter RFC number: ");
						rfc_num = console.nextLine();
						
						// Setup connection.
						System.out.println("Connecting to " + client_hn + " on port " + client_p);
						p2p_soc = new Socket(client_hn, client_p); // Consider errors
						System.out.println("Connected to " + p2p_soc.getRemoteSocketAddress());
						
						// Setup P2P data stream.
						OutputStream outToClient = p2p_soc.getOutputStream();
						DataOutputStream out_client = new DataOutputStream(outToClient);
						InputStream inFromClient = p2p_soc.getInputStream();
						DataInputStream in_client = new DataInputStream(inFromClient);
						
						msg = getMsg(rfc_num, client_hn);
						System.out.println("Sending GET to peer:\n" + msg);
						out_client.writeUTF(msg);
						
						// Receive from peer.
						String msg_in = in_client.readUTF();
						System.out.println("Peer response:\n" + msg_in);
						
						if (msg_in.contains(STAT_OK)) {
							processGetResp(msg_in, rfc_num);
						}
						
						// Close from requester side.
						out_client.close();
						
					} else if (cmd_num.equals("0")) {
						// Quit
						cont = false;
						
					} else {
						System.out.println("Enter a valid number.");
					}
				} else {
					System.out.println("Enter a valid number.");
				}
				
			} // while
			
			// Exit case.
			//out.writeUTF("EXIT " + hostname + " " + upload_port);
			
			// Receive from server - exit confirm. Not necessary.
			//System.out.println("Server response:\n" + in.readUTF());
			
			client.close();
			upload_soc.close();
			System.out.println("Exit");
			
		} catch (UnknownHostException e) {
			System.out.println("Error - Unknown Host - Exiting.");
			System.exit(0);
		} catch (ConnectException e) {
			System.out.println("Error - Connection refused - Exiting.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// v--- P2S methods ---v //
	
	// Add a locally available RFC to the server's index.
	public String addMsg(String rfc_num, String title) {
		String msg = "ADD RFC " + rfc_num + " " + VERSION + "\n";
		msg += "Host: " + hostname + "\n";
		msg += "Port: " + upload_port + "\n";
		msg += "Title: " + title + "\n";
		
		return msg;
	}
	
	// Find peers that have the specified RFC.
	public String lookupMsg(String rfc_num, String title) {
		String msg = "LOOKUP RFC " + rfc_num + " " + VERSION + "\n";
		msg += "Host: " + hostname + "\n";
		msg += "Port: " + upload_port + "\n";
		msg += "Title: " + title + "\n";
		
		return msg;
	}
	
	// Request the whole index of RFCs from the server.
	public String listMsg() {
		String msg = "LIST ALL " + VERSION + "\n";
		msg += "Host: " + hostname + "\n";
		msg += "Port: " + upload_port + "\n";
		
		return msg;
	}
	
	// Return exit message. (Not used)
	public String exitMsg() {
		String msg = "EXIT " + VERSION + "\n";
		msg += "Host: " + hostname + "\n";
		msg += "Port: " + upload_port + "\n";
		return msg;
	}
	
	// ^--- P2S methods ---^ //
	
	
	// v--- P2P methods ---v //
	
	// Build and return message for getting RFC from peer.
	public String getMsg(String rfc_num, String hostname) {
		String msg = "GET RFC " + rfc_num + " " + VERSION + "\n";
		msg += "Host: " + hostname + "\n";
		msg += "OS: " + System.getProperty("os.name") + "\n";
		
		return msg;
	}
	
	// Process response from GET to write new text file.
	public void processGetResp(String msg_in, String rfc_num) {
		Scanner msg_scan = new Scanner(msg_in);
		
		String version = msg_scan.next();
		String status = msg_scan.nextLine().trim();
		msg_scan.next(); // Date header.
		String date = msg_scan.nextLine().trim();
		msg_scan.next(); // OS header.
		String os = msg_scan.nextLine().trim();
		msg_scan.next(); // Last-Modified header.
		String last_mod = msg_scan.nextLine().trim();
		msg_scan.next(); // Content-Length header.
		String cont_len = msg_scan.nextLine().trim();
		msg_scan.next(); // Content-Type header.
		String cont_type = msg_scan.nextLine().trim();
		
		//String filename = "src/rfc" + rfc_num + ".txt"; // Assumption - filename format.
		String filename = "rfc" + rfc_num + ".txt"; // Assumption - filename format.
		File f = new File(filename);
		
		try {
			PrintStream output = new PrintStream(f);
			while (msg_scan.hasNextLine()) {
				output.print(msg_scan.nextLine() + "\n");
			}
			output.close();
			System.out.println("Download complete.\n");
		}
		catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
	
	// ^--- P2P methods ---^ //
	
	
	// Client thread.
	class ClientThread extends Thread {
		
		private ServerSocket upload_soc;
		
		public ClientThread(ServerSocket s) {
			upload_soc = s;
		}
		
		public void run() {
			System.out.println("Client thread started.");
			
			//Socket upload = null;
			try {
				boolean cont = true;
				while (cont) {
					System.out.println("Client thread waiting for connection.");
					upload = upload_soc.accept();
					
					// Test
					System.out.println("Client thread connected: " + upload.getInetAddress().getHostName());
					
					DataInputStream in = new DataInputStream(upload.getInputStream());
					DataOutputStream out = new DataOutputStream(upload.getOutputStream());
					
					// Wait for input.
					String msg_in = in.readUTF();
					System.out.println("Received message:\n" + msg_in);
					
					// Process first line in message. Gets the method.
					Scanner msg_scan = new Scanner(msg_in);
					String method = msg_scan.next();
					msg_scan.next();
					
					int rfc_num = 0;
					String version = VERSION; // Updated - in case of catch before version update.
					try {
						rfc_num = msg_scan.nextInt(); // RFC number
						version = msg_scan.next(); // Version
						
						// Test
						//System.out.println(method + "|" + rfc_num + "|" + version + "\n");
						
						// Host
						msg_scan.next();
						String host = msg_scan.nextLine().trim();
						
						// OS
						msg_scan.next();
						String os = msg_scan.nextLine().trim();
						
					} catch (InputMismatchException e) {
						method = "BAD";
					}
					if (!version.equalsIgnoreCase(VERSION)) {
						String msg_out = VERSION + " " + STAT_VRS + "\n";
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else if (method.equalsIgnoreCase("GET")) {
						String msg_out = getRFC(rfc_num);
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else {
						// Bad request
						String msg_out = VERSION + " " + STAT_BAD + "\n";
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
					}
					
					// Requester side closes connection.
					//upload.close(); 
				} // while
				
			} catch (SocketException e) {
				System.out.println("Upload socket closed.");
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
			} catch (EOFException e) {
				try {
					System.out.println("Connection to " + upload.getRemoteSocketAddress() + " closed");
					upload.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				//e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Get RFC data to send to requester. If file not found send error message.
		public String getRFC(int rfc_num) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			
			String msg_out = VERSION + " " + STAT_OK + "\n";
			LocalDateTime now = LocalDateTime.now();
			msg_out += "Date: " + now + "\n";
			msg_out += "OS: " + System.getProperty("os.name") + "\n";
			
			String filename = "rfc" + rfc_num + ".txt"; // Assumption - filename format based on downloaded RFC files.
			System.out.println("Looking for file: " + filename); // Test info
			File f = new File(filename);
			
			String data = "";
			Scanner input = null;
			try {
				input = new Scanner(f);
				while (input.hasNextLine()) {
					data += input.nextLine() + "\n";
				}
				input.close();
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				return VERSION + " " + STAT_NOTF + "\n";
			}
			
			msg_out += "Last-Modified: " + sdf.format(f.lastModified()) + "\n";
			msg_out += "Content-Length: " + f.length() + "\n";
			msg_out += "Content-Type: " + "text/plain" + "\n";
			msg_out += data;
			
			return msg_out;
		}
	}
}