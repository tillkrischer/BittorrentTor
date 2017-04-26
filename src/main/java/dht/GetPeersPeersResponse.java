package dht;

public class GetPeersPeersResponse extends Response {
	PeerInfo[] values;
	
	public GetPeersPeersResponse(byte[] id) {
		super("get_peers_peers", id);
		values = new PeerInfo[Node.maxPeerResponseCount];
	}
}
