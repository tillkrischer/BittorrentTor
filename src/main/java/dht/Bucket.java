package dht;
import java.math.BigInteger;
import java.util.HashSet;

public class Bucket {

	public HashSet<NodeInfo> nodes;
	public BigInteger start;
	public BigInteger end;

	public Bucket(BigInteger start, BigInteger end) {
		this.start = start;
		this.end = end;
		nodes = new HashSet<NodeInfo>();
	}

	public int size() {
		return nodes.size();
	}
}
