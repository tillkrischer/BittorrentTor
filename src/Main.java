import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Main implements Runnable {
  
  private int port = 6881;
  private String downloadDir = "test/downloads/";
  public int maxConnections = 20;
  public int maxConnectionsPerTorrent = 5;
  // pretend we are deluge
  private String clientIdent = "-DE13D0-";
  
  private byte[] peerId;
  private ArrayList<Torrent> torrents;
  private HashMap<String, Torrent> infoMap;
  private int peerConnections;
  
  public Main() {
    peerId = generatePeerId(clientIdent); 
    torrents = new ArrayList<Torrent>();
    infoMap = new HashMap<String, Torrent>();
    
    Listener listen = new Listener(peerId, this, port);
    Thread listenThread = new Thread(listen);
    listenThread.start();
  }
 
  @Override
  public void run() {
    while (true) {
      int count = 0;
      for (Torrent t : torrents) {
        t.update();
      
        int peers = t.getNumActivePeers();
        count += peers;
        if (t.isDowloading() && t.hasInactivePeers() 
            && peers < maxConnectionsPerTorrent && peerConnections < maxConnections) {
          t.connectToPeer();
        }
      }
      peerConnections = count;
    }
  }
  
  public int getNumPeerConnections() {
    return peerConnections;
  }
  
  public Torrent getTorrent(byte[] info) {
    return infoMap.get(Util.byteArrayToHex(info));
  }
  
  //index in the torrents ArrayList
  public synchronized void startTorrent(int index) {
    torrents.get(index).start();
  }
  
  public synchronized void addTorrent(String filename) {
    try {
      Torrent t = new Torrent(filename);
      torrents.add(t);
      System.out.println(Arrays.toString(t.getInfoHash()));
      infoMap.put(Util.byteArrayToHex(t.getInfoHash()), t);
    } catch (FileNotFoundException e) {
      System.out.println("file not found!");
    } catch (InvalidTorrentFileException e) {
      System.out.println("invalid torrent file!");
    }
  }
  
  public static byte[] generatePeerId(String clientIdent) {
    byte[] id = new byte[20];
    int i = 0;
    while (i < clientIdent.length()) {
      id[i] = (byte) clientIdent.charAt(i);
      i++;
    }
    Random rand = new Random();
    while (i < id.length) {
      id[i] = (byte) (rand.nextInt('z' - 'a') + 'a');
      i++;
    }
    return id;
  }
  
  public static void main(String[] args) {
    Main m = new Main();
    Thread t = new Thread(m);
    t.start();
  }
}
