package torrent;

import java.util.HashSet;

public interface PeerProvider {
  public void update(String status);
  public HashSet<Peer> announce();
}
