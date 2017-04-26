package dht;

public class PingResponse extends Response {
	
	public PingResponse(byte[] id) {
		super("ping", id);
	}
}
