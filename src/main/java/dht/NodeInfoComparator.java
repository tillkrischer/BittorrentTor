package dht;
import java.math.BigInteger;
import java.util.Comparator;

public class NodeInfoComparator implements Comparator<NodeInfo> {

	BigInteger findId;
	
	public NodeInfoComparator(BigInteger findId) {
		this.findId = findId;
	}

	@Override
	public int compare(NodeInfo o1, NodeInfo o2) {
		BigInteger dist1 = findId.xor(Node.arrayToBigIntUnsigned(o1.id)).abs();
		BigInteger dist2 = findId.xor(Node.arrayToBigIntUnsigned(o2.id)).abs();
		
		return dist1.compareTo(dist2);
	}
}
