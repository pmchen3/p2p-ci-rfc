// RFC info.
public class RFC {
	
	int number;
	String title;
	String name_peer;
	//int port; // Port is passed in but not used.
	
	// Constructor.
	public RFC() {
		
	}
	
	// Constructor.
	public RFC(int number, String title, String name_peer, int port) {
		this.number = number;
		this.title = title;
		this.name_peer = name_peer;
		//this.port = port;
	}
}