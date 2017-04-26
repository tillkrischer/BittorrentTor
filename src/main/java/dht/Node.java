package dht;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

public class Node {
	
	final byte[] max = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
	final byte[] zero = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	public static final int maxBucketSize = 8;
	public static final int maxPeerResponseCount = 100;
	private byte[] nodeId;
	private NodeInfo nodeInfo;
	private ArrayList<Bucket> buckets;
	private HashMap<String, LinkedList<PeerInfo>> peers;
	
	public Node(String ip, int port) {
		this.nodeId = randomId();
		peers = new HashMap<String, LinkedList<PeerInfo>>();
		buckets = new ArrayList<Bucket>();
		Bucket b = new Bucket(arrayToBigIntUnsigned(zero), arrayToBigIntUnsigned(max));
		nodeInfo = new NodeInfo(this.nodeId, ip, port);
		b.nodes.add(this.nodeInfo);
		buckets.add(b);
	}
	
	public static byte[] randomId() {
		Random rand = new Random();
		byte[] temp = new byte[20];
		for (int i = 0; i < 20; i++) {
			temp[i] = (byte) rand.nextInt(256);
		}
		return temp;
	}
	
	public static BigInteger arrayToBigIntUnsigned(byte[] num) {
		byte[] temp = new byte[num.length + 1]; 
		temp[0] = 0;
		for (int i = 0; i < num.length; i++) {
			temp[i+1] = num[i];
		}
		return new BigInteger(temp);
	}
	
	public synchronized boolean addNode(NodeInfo targetNodeInfo) {
		BigInteger idValue = arrayToBigIntUnsigned(targetNodeInfo.id);
		int bucketIndex =  findBucket(idValue, 0, buckets.size());
		Bucket b = buckets.get(bucketIndex);
		if (b.size() < maxBucketSize) {
			return b.nodes.add(targetNodeInfo);
		} else {
			if (b.nodes.contains(this.nodeInfo)) {
				BigInteger middle = b.start.add(b.end).divide(BigInteger.valueOf(2));
				Bucket b2 = new Bucket(middle.add(BigInteger.ONE), b.end);
				b.end = middle;
				Iterator<NodeInfo> it = b.nodes.iterator();
				while(it.hasNext()) {
					NodeInfo i = it.next();
					if(arrayToBigIntUnsigned(i.id).compareTo(middle) > 0) {
						b2.nodes.add(i);
						it.remove();
					}
				}
				buckets.add(bucketIndex + 1, b2);
				return addNode(targetNodeInfo);
			}
		}
		return false;
	}
	
	public byte[] getNodeId() {
		return this.nodeId;
	}
	
	public NodeInfo getNodeInfo() {
		return this.nodeInfo;
	}
	
	//binary search 
	//start inclusive, end exclusive
	private int findBucket(BigInteger id, int startIndex, int endIndex) {
		int middleIndex = (startIndex + endIndex)  / 2;
		Bucket b = buckets.get(middleIndex);
		if (id.compareTo(b.start) < 0) {
			return findBucket(id, startIndex, middleIndex);
		}
		if (id.compareTo(b.end) > 0) {
			return findBucket(id, middleIndex + 1, endIndex);
		}
		return middleIndex;
	}
	
	public void printBuckets() {
		for (int i = 0; i < buckets.size(); i++) {
			Bucket b = buckets.get(i);
			System.out.println("bucket" + i + " size(" + b.size() + ") (" + b.start + ":" + b.end + ")");
			for (NodeInfo info : b.nodes) {
				System.out.println("\t " + info.ip + ":" + info.port + "  " + arrayToBigIntUnsigned(info.id));
			}
		}
	}
	
	public synchronized LinkedList<NodeInfo> findNode(byte[] findId) {
		NodeInfoComparator comp = new NodeInfoComparator(arrayToBigIntUnsigned(findId));
		LinkedList<NodeInfo> top8 = new LinkedList<NodeInfo>();
		
		for (Bucket b : buckets) {
			for (NodeInfo i : b.nodes) {
				if(top8.size() < maxBucketSize || comp.compare(i, top8.getLast()) < 0) {
					top8.add(i);
					top8.sort(comp);
					if(top8.size() > maxBucketSize) {
						top8.removeLast();
					}
				}
			}
		}
		
		return top8;
	}
	
	public LinkedList<PeerInfo> getPeers(byte[] info_hash) {
		String key = Arrays.toString(info_hash);
		if (peers.containsKey(key)) {
			return peers.get(key);
		}
		return null;
	}

	public void addPeer(byte[] info_hash, PeerInfo peer) {
		String key = Arrays.toString(info_hash);
		if (! peers.containsKey(key)) {
			peers.put(key, new LinkedList<PeerInfo>());
		}
		peers.get(key).add(peer);
	}
}
