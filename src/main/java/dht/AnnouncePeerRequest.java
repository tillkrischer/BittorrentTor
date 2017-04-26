package dht;

public class AnnouncePeerRequest extends Request {
	PeerInfo peer;
	byte[] info_hash;
	
	public AnnouncePeerRequest(byte[] id, byte[] info_hash, PeerInfo peer, int port) {
		super("announce_peer", id, port);
		this.info_hash = info_hash;
		this.peer = peer;
	}
}
