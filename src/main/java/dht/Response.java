package dht;

public class Response {
	String type;
	byte[] id;
	
	public Response(String type, byte[] id) {
		this.type = type;
		this.id = id;
	}
}
