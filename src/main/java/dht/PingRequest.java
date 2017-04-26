package dht;

public class PingRequest extends Request {
	public PingRequest(byte[] id, int port) {
		super("ping", id, port);
	}
}
