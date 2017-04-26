package dht;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class Demo {
	public static void main(String[] args) {
//		HashMap<String, Connection> connections = new HashMap<>();
//		for(int i = 0; i < 100; i++) {
//			PeerInfo p = new PeerInfo();
//			p.address = "address" + i;
//			p.port = 22;
//			Connection c = new Connection("127.0.0.1", 6000 + i, p);
//			connections.put("node" + i, c);
//		}
//
//		for(int i = 1; i < 100; i++) {
//			Connection c = connections.get("node" + i);
//			c.ping("127.0.0.1", 6000);
//		}
//	
//		for(int i = 0; i < 100; i++) {
//			Connection c = connections.get("node" + i);
//			c.bootstrap();
//		}
//		for(int i = 0; i < 100; i++) {
//			Connection c = connections.get("node" + i);
//			c.bootstrap();
//		}
//	
//		connections.get("node1").info();
//		
//		byte[] target = Node.randomId();
//		System.out.println("target: " + Node.arrayToBigIntUnsigned(target));
//		
//		
//		HashSet<PeerInfo> peers = connections.get("node1").announce(target);
//		System.out.println("node1 peers: " + peers.size());
//		
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) { }
//		
//		peers = connections.get("node2").announce(target);
//		System.out.println("node2 peers: " + peers.size());
//		
//		for(PeerInfo p : peers) {
//			System.out.println(p.address + ":" + p.port);
//		}
//		
//		peers = connections.get("node1").announce(target);
//		System.out.println("node1 peers: " + peers.size());
//		
//		for(PeerInfo p : peers) {
//			System.out.println(p.address + ":" + p.port);
//		}
	  PeerInfo p1 = new PeerInfo();
	  p1.address = "address";
	  p1.port = 22;
	  Connection con1 = new Connection("127.0.0.1", 6000, p1);
	  Connection con2 = new Connection("127.0.0.1", 6001, p1);
	  
	  con1.ping("127.0.0.1", 6000);
	  con1.bootstrap();
	  
	  con2.ping("127.0.0.1", 6000);
	  con2.bootstrap();
	  
      byte[] target = Node.randomId();
	  con1.announce(target);
	  con2.announce(target);
	}
	
	public static void nodeTest() {
		Node n = new Node("127.0.0.1", 5000);
		NodeInfo[] inserts = new NodeInfo[10];
		for(int i = 0; i < 10; i++) {
			inserts[i] =  new NodeInfo(Node.randomId(), "127.0.0.1", 5001 + i);
		}
		Random rand = new Random();
		for(int i = 0; i < 100; i++) {
			n.addNode(inserts[rand.nextInt(10)]);
		}
		n.printBuckets();
	}
	
}
