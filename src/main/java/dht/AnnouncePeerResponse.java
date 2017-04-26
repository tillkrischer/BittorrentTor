package dht;

public class AnnouncePeerResponse extends Response {
	public AnnouncePeerResponse(byte[] id) {
		super("announce_peer", id);
	}
}
