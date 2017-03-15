package ui;

import javax.swing.table.AbstractTableModel;

import torrent.Torrent;
import torrent.TorrentController;

public class TorrentTableModel extends AbstractTableModel {
  
  private TorrentController controller;

  public TorrentTableModel(TorrentController tc) {
    controller = tc;
  }
  
  @Override
  public int getColumnCount() {
    // TODO Auto-generated method stub
    return 3;
  }

  @Override
  public int getRowCount() {
    // TODO Auto-generated method stub
    if(controller == null) {
      System.out.println("null");
      return 0;
    }
    return controller.getNumberOfTorrents();
  }

  @Override
  public Object getValueAt(int row, int column) {
    Torrent t = controller.getTorrentByIndex(row);
    switch(column) {
      case 0:
        return t.getName();
      case 1:
        return t.getTotalBytes();
      case 2:
        return (float) t.getProgressPercent();
    }
    return null;
  }
 
  @Override
  public String getColumnName(int column) {
      String name = "??";
      switch (column) {
          case 0:
              name = "Name";
              break;
          case 1:
              name = "Size";
              break;
          case 2:
              name = "Progress";
              break;
      }
      return name;
  }
}
