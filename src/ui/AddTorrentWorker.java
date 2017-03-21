package ui;

import java.io.FileNotFoundException;

import javax.swing.SwingWorker;

import torrent.InvalidTorrentFileException;
import torrent.TorrentController;

public class AddTorrentWorker extends SwingWorker<Boolean, Object> {
  
  private TorrentController controller;
  private String filename;

  public AddTorrentWorker(TorrentController tc, String filename) {
    this.controller = tc;
    this.filename = filename;
  }
  
  @Override
  protected Boolean doInBackground() throws Exception {
    try {
      controller.addTorrent(filename);
    } catch (FileNotFoundException e) {
      System.out.println("file note found");
    } catch (InvalidTorrentFileException e) {
      System.out.println("invalid torrent file");
    }
    return false;
  }
}
