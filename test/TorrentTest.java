import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import org.junit.Test;

import torrent.InvalidTorrentFileException;
import torrent.Torrent;
import torrent.TorrentController;
import torrent.Util;

public class TorrentTest {
  

  @Test
  public void ubuntu() {
    try {
      Torrent tor = new Torrent("test/resources/ubuntu-16.10-desktop-amd64.iso.torrent");
      String info = Util.byteArrayToHex(tor.getInfoHash());
      System.out.println("infohash: " + info);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail();
    } catch (InvalidTorrentFileException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void multifile() {
    try {
      Torrent tor = new Torrent("test/resources/BittorrentTor.torrent");
      long size = tor.getTotalSize();
      System.out.println("size: " + size);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail();
    } catch (InvalidTorrentFileException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  
  @Test
  public void invalid() {
    boolean caught = false;
    try {
      Torrent tor = new Torrent("test/resources/ubuntu-invalid.torrent");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail();
    } catch (InvalidTorrentFileException e) {
      caught = true;
    }
    assertTrue(caught);
  }
  
  @Test
  public void ubuntuGetPeers() {
    try {
      Torrent tor = new Torrent("test/resources/ubuntu-16.10-desktop-amd64.iso.torrent");
      tor.trackerRequest();
      System.out.println(tor);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail();
    } catch (InvalidTorrentFileException e) {
      e.printStackTrace();
      fail();
    }
  }
 
  @Test
  public void ubuntuLocal() {
    try {
      Torrent tor = new Torrent("test/resources/local-ubuntu.torrent");
      tor.trackerRequest("started");
      tor.connectToPeer();
      tor.connectToPeer();
      while (true) {
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      fail();
    } catch (InvalidTorrentFileException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  @Test
  public void peerTest() {
    try {
      Torrent tor;
      tor = new Torrent("test/resources/local-ubuntu.torrent");
      tor.trackerRequest("started");
      tor.connectToPeer();
      tor.connectToPeer();
    } catch (FileNotFoundException | InvalidTorrentFileException e) {
      System.out.println("error");
      fail();
    }
  }
  
  @Test
  public void wallpaperTest() {
    try {
      Torrent tor;
      tor = new Torrent("test/resources/Wallpapers.torrent");
    } catch (FileNotFoundException | InvalidTorrentFileException e) {
      System.out.println("error");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
  
  @Test
  public void smallFiles() {
    try {
      Torrent tor;
      tor = new Torrent("test/resources/smallFiles.torrent");
    } catch (FileNotFoundException | InvalidTorrentFileException e) {
      System.out.println("error");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
  
  @Test
  public void mainTest() {
    try {
      try {
        TorrentController m = new TorrentController();
        Thread t = new Thread(m);
        t.start();
        m.addTorrent("test/resources/local-ubuntu.torrent");
        m.startTorrent(0);
        t.join();
      } catch (FileNotFoundException | InvalidTorrentFileException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
