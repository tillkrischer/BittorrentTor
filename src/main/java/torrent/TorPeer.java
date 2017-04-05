package torrent;

public class TorPeer extends Peer {
  
  public String hostname;
  public int port;
  
  public TorPeer() {
    
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
    result = prime * result + port;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TorPeer other = (TorPeer) obj;
    if (hostname == null) {
      if (other.hostname != null)
        return false;
    } else if (!hostname.equals(other.hostname))
      return false;
    if (port != other.port)
      return false;
    return true;
  }
  
  public String toString() {
    return hostname + ":" + port;
  }
}
