package dht;

public class FindNodeRequest extends Request {
	byte[] targetId;
	
	public FindNodeRequest(byte[] id, byte[] targetId, int port) {
		super("find_node", id, port);
		this.targetId = targetId;
	}
}
