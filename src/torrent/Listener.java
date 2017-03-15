package torrent;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Listener implements Runnable {
  
  private byte[] peerId;
  private TorrentController main;
  private int port;
  
  public Listener(byte[] peerId, TorrentController main, int port) {
    this.peerId = peerId;
    this.main = main;
    this.port = port;
  }

  @Override
  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket(port);
      while(true) {
        Socket clientSocket = serverSocket.accept();
        Peer p = new Peer();
        p.address = clientSocket.getInetAddress();
        p.port = clientSocket.getPort();
        System.out.println("client connected from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        try {
          // TODO: asynchronous !!
          PeerConnection pc = new PeerConnection(clientSocket, peerId, p);
          byte[] info = pc.recieveHandshake();
          Torrent t = main.getTorrent(info);
          if (t != null) {
            if ((t.isSeeding() || t.isDowloading())
                && t.getNumActivePeers() < main.maxConnectionsPerTorrent 
                && main.getNumPeerConnections() < main.maxConnections) {
              pc.assignTorrent(t);
              pc.sendHandshake();
              pc.setConnected();
              t.addConnection(pc);
            } else {
              System.out.println("cant accept leecher, right now");
            }
          } else {
            System.out.println("dont know about this torrent");
          }
        } catch (IOException e) {
          System.out.println("error opening peerconnection");
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      System.out.println("unable to listen on port");
    }
  } 
}