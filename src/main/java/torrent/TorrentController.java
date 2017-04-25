package torrent;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import config.Config;
import config.ConfigManager;
import tor.TorManager;

public class TorrentController implements Runnable {

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

  private Config conf;
  private int port;
  private String downloadDir;
  public int maxConnections = 20;
  public int maxConnectionsPerTorrent = 5;

  // pretend we are deluge
  private String clientIdent = "-DE13D0-";
  private byte[] peerId;
  private ArrayList<Torrent> torrents;
  private HashMap<String, Torrent> infoMap;
  private TorManager torManager;

  private int peerConnections;

  public TorrentController() {
    peerId = generatePeerId(clientIdent);
    torrents = new ArrayList<Torrent>();
    infoMap = new HashMap<String, Torrent>();
    
    ConfigManager confmanager = new ConfigManager("config.json");
    conf = confmanager.getConfig();
    
    if (conf.useTor) {
      torManager = new TorManager(conf.torPort, conf.port, conf.torBinary, conf.orPort);
      torManager.launchTor();
    }
    
    port = conf.port;
    downloadDir = conf.downloadDir;

    Listener listen = new Listener(peerId, this, port);
    Thread listenThread = new Thread(listen);
    listenThread.start();
  }
  
  public void end() {
    if (torManager != null) {
      torManager.killTor();
    }
  }

  public synchronized void addTorrent(String filename)
      throws FileNotFoundException, InvalidTorrentFileException {
    Torrent t;
    if (conf.useTor) {
      t = new Torrent(filename, peerId, port, downloadDir, torManager);
    } else {
      t = new Torrent(filename, peerId, port, downloadDir);
    }
    torrents.add(t);
    System.out.println(Arrays.toString(t.getInfoHash()));
    infoMap.put(Util.byteArrayToHex(t.getInfoHash()), t);
  }

  public int getNumberOfTorrents() {
    return torrents.size();
  }

  public int getNumPeerConnections() {
    return peerConnections;
  }

  public Torrent getTorrent(byte[] info) {
    return infoMap.get(Util.byteArrayToHex(info));
  }

  public Torrent getTorrentByIndex(int i) {
    try {
      return torrents.get(i);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  @Override
  public void run() {
    while (true) {
      updateTorrents();
    }
  }

  // index in the torrents ArrayList
  public synchronized void startTorrent(int index) {
    torrents.get(index).start();
  }

  public synchronized void updateTorrents() {
    int count = 0;
    for (Torrent t : torrents) {
      t.update();

      int peers = t.getNumActivePeers();
      count += peers;
      if (t.isDowloading() && t.hasInactivePeers() && (peers < maxConnectionsPerTorrent)
          && (peerConnections < maxConnections)) {
        System.out.println("add a new peer");
        t.connectToPeer();
      }
    }
    peerConnections = count;
  }
}
