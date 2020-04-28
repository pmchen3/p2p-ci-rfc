import java.util.*;
import java.io.*;
import java.net.*;

// Server with central index.
public class Server {
	
	private static final int SERV_PORT = 7734;
	private static final String VERSION = "P2P-CI/1.0";
	private static final String STAT_OK = "200 OK";
	private static final String STAT_BAD = "400 Bad Request";
	private static final String STAT_NOTF = "404 Not Found";
	private static final String STAT_VRS = "505 P2P-CI Version Not Supported";
	
	List<Peer> list_peer = new ArrayList<>();
	List<RFC> list_rfc = new ArrayList<>();
	
	private ServerSocket serv_soc;
	//private Socket serv;
	//private boolean control_flag = false;
	
	// Start program
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	// Server main process.
	public void start() {
		System.out.println("Server starting up");
		
		try {
			serv_soc = new ServerSocket(SERV_PORT);
			//serv_soc.setSoTimeout(15000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while (true) {
			//System.out.println("Start of loop"); // Info
			
			//System.out.println("Local socket address: " + serv_soc.getLocalSocketAddress()); // Info
			System.out.println("Waiting for connection on port: " + serv_soc.getLocalPort());
			
			try {
				Socket serv = serv_soc.accept();
				System.out.println("Connected to: " + serv.getRemoteSocketAddress());
				
				System.out.println("Creating thread for new connection");
				ServerThread s_thd = new ServerThread(serv);
				s_thd.start();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // while
	}
	
	// Server thread.
	class ServerThread extends Thread {
		
		private Socket serv;
		
		public ServerThread(Socket s) {
			serv = s;
		}
		
		public void run() {
			// Save client info to remove from data structure on exit.
			String client_hn = "";
			int client_p = 0;
			try {
				
				//Socket soc = serv;
				
				// System.out.println("Waiting for connection on port: " + serv_soc.getLocalPort());
				// Socket serv = serv_soc.accept();
				
				//control_flag = false;
				
				DataInputStream in = new DataInputStream(serv.getInputStream());
				DataOutputStream out = new DataOutputStream(serv.getOutputStream());
				
				// Test
				//System.out.println("Serv hostname: " + serv.getInetAddress().getHostName());
				
				// Get client info.
				String client_info = in.readUTF();
				System.out.println("New Client info: " + client_info); // Test Info
				
				Scanner client_scan = new Scanner(client_info);
				client_hn = client_scan.next();
				client_p = client_scan.nextInt();
				client_scan.close(); // Close scanner
				
				Peer new_client = new Peer(client_hn, client_p);
				list_peer.add(0, new_client);
				
				boolean cont = true;
				while (cont) {
					// Wait for input.
					String msg_in = in.readUTF();
					System.out.println("Received message:\n" + msg_in);
					
					// Process first line in message. Gets the method.
					Scanner msg_scan = new Scanner(msg_in);
					String method = msg_scan.next();
					/* EXIT case not needed.
					if (method.equalsIgnoreCase("EXIT")) {
						String hostname = msg_scan.next();
						int up_port = msg_scan.nextInt();
						removeClient(hostname, up_port);
						out.writeUTF(VERSION + " " + STAT_OK + "\n");
					} else {
					*/
					int rfc_num = 0;
					String version = VERSION; // Updated - in case of catch before version update.
					String host = "";
					int port = 0;
					String title = "";
					
					try {
						msg_scan.next();
						
						// RFC number
						if (!method.equalsIgnoreCase("LIST")) {
							// LIST does not have RFC number.
							rfc_num = msg_scan.nextInt();
						}
						// Version
						version = msg_scan.next();
						
						// Test
						//System.out.println(method + "|" + rfc_num + "|" + version + "\n");
						
						// Host
						msg_scan.next(); // Header
						host = msg_scan.nextLine().trim();
						
						// Port
						msg_scan.next(); // Header
						port = msg_scan.nextInt();
						msg_scan.nextLine();
						
						// Title for ADD and LOOKUP
						if (method.equalsIgnoreCase("ADD") || method.equalsIgnoreCase("LOOKUP")) {
							msg_scan.next(); // Header
							title = msg_scan.nextLine().trim();
						}
					} catch (InputMismatchException e) {
						method = "BAD";
					}
					
					if (!version.equalsIgnoreCase(VERSION)) {
						String msg_out = VERSION + " " + STAT_VRS + "\n";
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else if (method.equalsIgnoreCase("ADD")) {
						String msg_out = addRFC(rfc_num, title, host, port);
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else if (method.equalsIgnoreCase("LOOKUP")) {
						String msg_out = lookupRFC(rfc_num, title);
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else if (method.equalsIgnoreCase("LIST")) {
						String msg_out = listRFC();
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
						
					} else {
						// Bad Request
						String msg_out = VERSION + " " + STAT_BAD + "\n";
						System.out.println("Sending response:\n" + msg_out);
						out.writeUTF(msg_out);
					}
					
					//out.writeUTF("Thank you for connecting to " + serv.getLocalSocketAddress() + "\nGoodbye!");
					//serv.close();
					//System.out.println("End of thread");
						
					//} EXIT else not needed.
				} // while
				
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
			} catch (EOFException e) {
				try {
					System.out.println("Connection to " + serv.getRemoteSocketAddress() + " closed");
					serv.close();
					
					removeClient(client_hn, client_p);
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				//e.printStackTrace();
			} catch (SocketException e) {
				try {
					System.out.println("Connection to " + serv.getRemoteSocketAddress() + " closed");
					serv.close();
					
					removeClient(client_hn, client_p);
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Remove client information from data lists. Invoked when client disconnects.
	public void removeClient(String hostname, int port) {
		RFC rfc_obj;
		for (int i = list_rfc.size() - 1; i >= 0; i--) {
			rfc_obj = list_rfc.get(i);
			if (rfc_obj.name_peer.equalsIgnoreCase(hostname) /*&& rfc_obj.port == port*/) {
				list_rfc.remove(i);
			}
		}
		Peer peer_obj;
		for (int i = list_peer.size() - 1; i >= 0; i--) {
			peer_obj = list_peer.get(i);
			if (peer_obj.name.equalsIgnoreCase(hostname) /*&& peer_obj.port == port*/) {
				//System.out.println("Remove: " + peer_obj.name + " " + peer_obj.port);
				list_peer.remove(i);
			}
		}
	}
	
	// Create new RFC object and add to list. Return response message.
	public String addRFC(int number, String title, String name, int port) {
		RFC new_rfc = new RFC(number, title, name, port);
		list_rfc.add(0, new_rfc);
		
		String msg_out = VERSION + " " + STAT_OK + "\n";
		msg_out += "RFC " + number + " " + title + " " + name + " " + port + "\n";
		return msg_out;
	}
	
	// Build and return message for looking up RFC.
	public String lookupRFC(int number, String title) {
		RFC rfc_obj;
		String msg_out = VERSION + " " + STAT_OK + "\n";
		
		boolean rfc_found = false;
		
		// This case if executing on a single localhost where clients have same hostname.
		/*
		for (int i = 0; i < list_rfc.size(); i++) {
			rfc_obj = list_rfc.get(i);
			if (rfc_obj.number == number && rfc_obj.title.equalsIgnoreCase(title)) {
				msg_out += rfc_obj.name_peer + " " + rfc_obj.port + "\n";
				rfc_found = true;
			}
		}
		*/
		
		// This case if executing on multiple hosts with different hostnames.
		String hostname;
		for (int i = 0; i < list_rfc.size(); i++) {
			rfc_obj = list_rfc.get(i);
			if (rfc_obj.number == number && rfc_obj.title.equalsIgnoreCase(title)) {
				hostname = rfc_obj.name_peer;
				
				Peer peer_obj;
				for (int j = 0; j< list_peer.size(); j++) {
					peer_obj = list_peer.get(j);
					if (peer_obj.name.equalsIgnoreCase(hostname)) {
						msg_out += hostname + " " + peer_obj.port + "\n";
						rfc_found = true;
					}
				}
			}
		}
		
		if (rfc_found == false) {
			 msg_out = VERSION + " " + STAT_NOTF + "\n";
		}
		
		return msg_out;
	}
	
	// Build and return message for listing RFCs.
	public String listRFC() {
		String msg_out = VERSION + " " + STAT_OK + "\n";
		RFC rfc_obj;
		for (int i = 0; i < list_rfc.size(); i++) {
			rfc_obj = list_rfc.get(i);
			
			for (int j = 0; j < list_peer.size(); j++) {
				if (rfc_obj.name_peer.equalsIgnoreCase(list_peer.get(j).name)) {
					msg_out += "RFC " + rfc_obj.number + " " + rfc_obj.title + " " + rfc_obj.name_peer + " " + list_peer.get(j).port + "\n";
				}
			}
		}
		return msg_out;
	}
}