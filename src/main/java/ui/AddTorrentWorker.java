package ui;

import java.io.FileNotFoundException;

import javax.swing.SwingWorker;

import torrent.InvalidTorrentFileException;
import torrent.TorrentController;

public class AddTorrentWorker extends SwingWorker<Boolean, Object> {
  
  private TorrentController controller;
  private String filename;
  private TorrentTableModel ttmodel;

  public AddTorrentWorker(TorrentController tc, String filename, TorrentTableModel ttmodel) {
    this.controller = tc;
    this.filename = filename;
    this.ttmodel = ttmodel;
  }
  
  @Override
  protected Boolean doInBackground() throws Exception {
    try {
      controller.addTorrent(filename);
      ttmodel.fireTableRowsInserted(ttmodel.getRowCount() - 1, ttmodel.getRowCount() - 1);
    } catch (FileNotFoundException e) {
      System.out.println("file note found");
    } catch (InvalidTorrentFileException e) {
      System.out.println("invalid torrent file");
    }
    return false;
  }
}
