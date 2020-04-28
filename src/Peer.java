// Peer info.
public class Peer {

	String name;
	int port;
	
	// Constructor.
	public Peer() {
		
	}
	
	// Constructor.
	public Peer(String hostname, int port) {
		this.name = hostname;
		this.port = port;
	}
}