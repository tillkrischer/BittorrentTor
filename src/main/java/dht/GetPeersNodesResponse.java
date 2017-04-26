package dht;

public class GetPeersNodesResponse extends Response {
	NodeInfo[] nodes;
	
	public GetPeersNodesResponse(byte[] id) {
		super("get_peers_nodes", id);
		nodes = new NodeInfo[Node.maxBucketSize];
	}
}
