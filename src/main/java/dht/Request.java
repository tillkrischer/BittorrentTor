package dht;

public class Request {
	String type;
	byte[] id;
	int port;
	
	public Request(String type, byte[] id, int port) {
		this.port = port;
		this.type = type;
		this.id = id;
	}
}
