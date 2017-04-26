package torrent;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashSet;

import dht.Connection;
import dht.PeerInfo;

public class DhtPeerProvider implements PeerProvider {
  
  private Connection con;
  private boolean torMode;
  
  public DhtPeerProvider(int dhtport, String address, int port, boolean torMode) throws UnknownHostException {
    this.torMode = torMode;
    PeerInfo pinfo = new PeerInfo();
    pinfo.address = address;
    pinfo.port = port;
    this.con = new Connection(Inet4Address.getLocalHost().getHostAddress().toString(), dhtport, pinfo);
  }
  
  public void bootstrap(String address, int port) {
    con.ping(address, port);
    con.bootstrap();
  }

  @Override
  public HashSet<Peer> announce(String status, Torrent torrent) {
    HashSet<Peer> results = new HashSet<Peer>();
    con.bootstrap();
    HashSet<PeerInfo> pInfos = con.announce(torrent.getInfoHash());
    for(PeerInfo pinfo : pInfos) {
      if (torMode) {
        TorPeer p = new TorPeer();
        p.hostname = pinfo.address;
        p.port = pinfo.port;
        results.add(p);
      } else {
        IpPeer p = new IpPeer();
        try {
          p.address = Inet4Address.getByName(pinfo.address);
        } catch (UnknownHostException e) {
          System.out.println("unknown host exception");
        }
        p.port = pinfo.port;
        results.add(p);
      }
    }
    return results;
  }
  
}
