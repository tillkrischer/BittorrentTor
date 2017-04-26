package dht;

public class GetPeersRequest extends Request {
	byte[] info_hash;
	
	public GetPeersRequest(byte[] id, byte[] info_hash, int port) {
		super("get_peers", id, port);
		this.info_hash = info_hash;
	}
}
