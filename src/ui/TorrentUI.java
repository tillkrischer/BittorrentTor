package ui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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

import torrent.TorrentController;

public class TorrentUI implements ActionListener {
  
  private TorrentController controller;
  private JFrame frame;
  private TorrentTableModel model;
  private JTable table;
  private Timer timer;
  
  public TorrentUI() {
    controller = new TorrentController();
    model = new TorrentTableModel(controller);
    Thread t = new Thread(controller);
    t.start();
    
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
    timer = new Timer(200, this);
    timer.setActionCommand("update");
    timer.setRepeats(true);
    timer.start();
  }
  
  public JTable getTable() {
    JTable table = new JTable();
    table.setModel(model);
    table.setRowSelectionAllowed(true);
    table.getColumn("Progress").setCellRenderer(new ProgressCellRender());
    return table;
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
  
  public void addComponentsToPane(Container pane) {
    JButton button;
    pane.setLayout(new GridBagLayout());
    GridBagConstraints c;

    JPanel controls = getControlPanel();
    button = new JButton("Button 1");
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipadx = 100;
    c.weightx = 1.0;
    c.weighty = 0.0;
    pane.add(controls, c);
    
    button = new JButton("Button 2");
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.fill = GridBagConstraints.VERTICAL;
    c.ipady = 500;
    c.weightx = 0.0;
    c.weighty = 1.0;
    pane.add(button, c);

    table = getTable();
    c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 1;
    c.fill = GridBagConstraints.BOTH;
    c.ipadx = 800;
    c.weightx = 0.5;
    c.weighty = 0.5;
    pane.add(new JScrollPane(table), c);

    button = new JButton("Button 4");
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipady = 200;
    c.weightx = 1.0;
    c.weighty = 0.0;
    pane.add(button, c);
  }

  private void createAndShowGUI() {
    frame = new JFrame("Torrent");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    addComponentsToPane(frame.getContentPane());

    frame.pack();
    frame.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    switch(command) {
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
        updateUI();
        break;
    }
  }
  
  public void addTorrent() {
    JFileChooser fc = new JFileChooser();
    File file = null;
    File directory = new File(System.getProperty("user.dir"));
    fc.setCurrentDirectory(directory);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Torrent file", "torrent");
    fc.setFileFilter(filter);
    fc.showSaveDialog(frame);
    file = fc.getSelectedFile();
    if (file != null) {
      AddTorrentWorker worker = new AddTorrentWorker(controller, file.getAbsolutePath());
      worker.execute();
    }
  }
  
  public void startTorrent() {
    controller.startTorrent(table.getSelectedRow());
  }
  
  public void pauseTorrent() {
    
  }
  
  public void updateUI() {
    int selected = table.getSelectedRow();
    model.fireTableDataChanged();
    if (selected != -1) {
      table.setRowSelectionInterval(selected, selected);
    }
  }
  
  public static void main(String[] args) {
    try {
      // Set cross-platform Java L&F (also called "Metal")
      UIManager.setLookAndFeel(
          UIManager.getSystemLookAndFeelClassName());
    } 
    catch (UnsupportedLookAndFeelException e) {
      // handle exception
    }
    catch (ClassNotFoundException e) {
      // handle exception
    }
    catch (InstantiationException e) {
      // handle exception
    }
    catch (IllegalAccessException e) {
      // handle exception
    }
    new TorrentUI();
  }

}