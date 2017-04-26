package ui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import torrent.Torrent;
import torrent.TorrentController;

public class TorrentUi implements ActionListener {

  public static void main(String[] args) {
    
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (UnsupportedLookAndFeelException e) {
      // handle exception
    } catch (ClassNotFoundException e) {
      // handle exception
    } catch (InstantiationException e) {
      // handle exception
    } catch (IllegalAccessException e) {
      // handle exception
    }
    new TorrentUi();
  }

  private TorrentController controller;
  private JFrame frame;
  private TorrentTableModel torrentTableModel;
  private JTable torrentTable;
  private PeerTableModel peerTableModel;
  private JTable peerTable;

  private Timer timer;

  public TorrentUi() {
    controller = new TorrentController();
    torrentTableModel = new TorrentTableModel(controller);
    torrentTable = getTorrentTable();
    peerTableModel = new PeerTableModel(controller, torrentTable);
    peerTable = getPeerTable();
    Thread t = new Thread(controller);
    t.start();

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        createAndShowGui();
      }
    });
    timer = new Timer(500, this);
    timer.setActionCommand("update");
    timer.setRepeats(true);
    timer.start();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    switch (command) {
      case "add":
        addTorrent();
        break;
      case "start":
        startTorrent();
        break;
      case "pause":
        pauseTorrent();
        break;
      case "update":
        updateUi();
        break;
      default:
        break;
    }
  }

  public void addComponentsToPane(Container pane) {
    pane.setLayout(new GridBagLayout());
    GridBagConstraints c;

    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipadx = 100;
    c.weightx = 1.0;
    c.weighty = 0.0;
    JPanel controls = getControlPanel();
    pane.add(controls, c);

    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.fill = GridBagConstraints.VERTICAL;
    c.ipady = 500;
    c.weightx = 0.0;
    c.weighty = 1.0;
    JButton button = new JButton("Button 2");
    pane.add(button, c);

    c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 1;
    c.fill = GridBagConstraints.BOTH;
    c.ipadx = 800;
    c.weightx = 0.5;
    c.weighty = 0.5;
    pane.add(new JScrollPane(torrentTable), c);

    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipady = 200;
    c.weightx = 1.0;
    c.weighty = 0.0;
    pane.add(new JScrollPane(peerTable), c);
  }

  public void addTorrent() {
    JFileChooser fc = new JFileChooser();
    File directory = new File(System.getProperty("user.dir"));
    fc.setCurrentDirectory(directory);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Torrent file", "torrent");
    fc.setFileFilter(filter);
    fc.showSaveDialog(frame);
    File file = fc.getSelectedFile();
    if (file != null) {
      AddTorrentWorker worker = new AddTorrentWorker(controller, file.getAbsolutePath(), torrentTableModel);
      worker.execute();
    }
  }

  private void createAndShowGui() {
    frame = new TorrentUiFrame("Torrent", controller);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    addComponentsToPane(frame.getContentPane());

    frame.pack();
    frame.setVisible(true);
  }

  public JPanel getControlPanel() {
    JPanel controls = new JPanel();
    controls.setLayout(new FlowLayout(FlowLayout.LEFT));

    JButton add = new JButton("add");
    add.setActionCommand("add");
    add.addActionListener(this);
    controls.add(add);

    JButton start = new JButton("start");
    start.setActionCommand("start");
    start.addActionListener(this);
    controls.add(start);

    JButton pause = new JButton("pause");
    pause.setActionCommand("pause");
    pause.addActionListener(this);
    controls.add(pause);

    return controls;
  }

  public JTable getPeerTable() {
    JTable table = new JTable();
    table.setModel(peerTableModel);
    return table;
  }

  public JTable getTorrentTable() {
    JTable table = new JTable();
    table.setModel(torrentTableModel);
    table.setRowSelectionAllowed(true);
    table.getColumn("Progress").setCellRenderer(new ProgressCellRender());
    return table;
  }

  public void pauseTorrent() {

  }

  public void startTorrent() {
    Torrent t = controller.getTorrentByIndex(torrentTable.getSelectedRow());
    if (t != null && ! t.isChecking()) {
      controller.startTorrent(torrentTable.getSelectedRow());
    }
  }

  public void updateUi() {
    int selected = torrentTable.getSelectedRow();
    torrentTableModel.update();
    peerTableModel.update();
    peerTable.repaint();
    if (selected != -1) {
      torrentTable.setRowSelectionInterval(selected, selected);
    }
  }
}
