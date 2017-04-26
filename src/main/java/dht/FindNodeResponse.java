package dht;

public class FindNodeResponse extends Response {
	NodeInfo[] nodes;
	
	public FindNodeResponse(byte[] id) {
		super("find_node", id);
		nodes = new NodeInfo[Node.maxBucketSize];
	}
	
}
