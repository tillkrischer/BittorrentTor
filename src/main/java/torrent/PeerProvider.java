package torrent;

import java.util.HashSet;

public interface PeerProvider {
  public HashSet<Peer> announce(String status, Torrent torrent);
}
