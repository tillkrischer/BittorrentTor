import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.Test;

public class TorrentTest {
  
  public static String byteArrayToHex(byte[] a) {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  @Test
  public void ubuntu() {
    try {
      Torrent tor = new Torrent("test/resources/ubuntu-16.10-desktop-amd64.iso.torrent");
      String info = byteArrayToHex(tor.getInfoHash());
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
}
