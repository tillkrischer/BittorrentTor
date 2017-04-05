package ui;

import javax.swing.JFrame;

import torrent.TorrentController;

public class TorrentUiFrame extends JFrame {
  
  private TorrentController tc;
  
  public TorrentUiFrame(String name, TorrentController tc) {
    super(name);
    this.tc = tc;
  }

  public void dispose() {
    super.dispose();
    tc.end();
    System.exit(0);
  }
}
