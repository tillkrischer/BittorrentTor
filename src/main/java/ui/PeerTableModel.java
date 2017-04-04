package ui;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import torrent.PeerConnection;
import torrent.Torrent;
import torrent.TorrentController;

public class PeerTableModel extends AbstractTableModel {

  private TorrentController controller;
  private JTable torrentTable;

  public PeerTableModel(TorrentController controller, JTable torrentTable) {
    this.controller = controller;
    this.torrentTable = torrentTable;
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(int column) {
    String name = "??";
    switch (column) {
      case 0:
        name = "PeerId";
        break;
      case 1:
        name = "Address";
        break;
      default:
        break;
    }
    return name;
  }

  @Override
  public int getRowCount() {
    Torrent t = controller.getTorrentByIndex(torrentTable.getSelectedRow());
    if (t != null) {
      return t.getNumActivePeers();
    }
    return 0;
  }

  @Override
  public Object getValueAt(int row, int column) {
    Torrent t = controller.getTorrentByIndex(torrentTable.getSelectedRow());
    if (t != null) {
      PeerConnection pc = t.getActivePeers().get(row);
      switch (column) {
        case 0:
          return pc.getRemotePeerId();
        case 1:
          return pc.getPeer().toString();
        default:
          break;
      }
    }
    return null;
  }
}
