package tor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class TorManager {
  
  private int torPort;
  private int port;
  private String dataDir = "tor-data/";
  private String torExecutable;
  private Process torProcess;
  private String hostname = "";
  private int orPort;
  private String[] dirs;

  public TorManager(int torPort, int port, String torExecutable, int orPort, String[] dirs) {
    this.dirs = dirs;
    this.orPort = orPort;
    this.port = port;
    this.torPort = torPort;
    this.torExecutable = torExecutable;
    createTorrc();
  }
  
  public boolean launchTor() {
    String cmd = "";
    cmd += torExecutable;
    cmd += " -f " + dataDir + "torrc";
    try {
      System.out.println(cmd);
      torProcess = Runtime.getRuntime().exec(cmd);
      Scanner scan = new Scanner(torProcess.getInputStream());
      while(scan.hasNext()) {
        String line = scan.nextLine();
        System.out.println(line);
        if (line.contains("100%")) {
          readHostname();
          return true;
        }
      }
      return false;
    } catch (IOException e) {
      System.out.println("error launchinig tor");
    }
    return false;
  }
  
  public void readHostname() {
    try {
      File f = new File(dataDir + "hidden_service/hostname");
      Scanner scan = new Scanner(f);
      String hostname = scan.nextLine();
      System.out.println("hostname: " + hostname);
      this.hostname = hostname;
    } catch (FileNotFoundException e) {
      System.out.println("Error reading hostname");
    }
    
  }
  
  public void createTorrc() {
    try {
      File folder = new File(dataDir);
      if(folder.mkdirs()) {
        System.out.println("created folder " + dataDir);
      }
      FileWriter fw = new FileWriter(dataDir + "torrc");
      
      ClassLoader classLoader = getClass().getClassLoader();
      //File f = new File(classLoader.getResource("torrc-template").getFile());
      //Scanner scan = new Scanner(new FileReader(f));
      InputStream in = classLoader.getResourceAsStream("torrc-template");
      Scanner scan = new Scanner(in);
      while (scan.hasNext()) {
        String line = scan.nextLine();
        line = line.replaceAll("\\$datadir", System.getProperty("user.dir") + "/" + dataDir);
        line = line.replaceAll("\\$torport", Integer.toString(torPort));
        line = line.replaceAll("\\$port", Integer.toString(port));
        line = line.replaceAll("\\$orport", Integer.toString(orPort));
        line = line.replaceAll("\\$dir1", dirs[0]);
        line = line.replaceAll("\\$dir2", dirs[1]);
        line = line.replaceAll("\\$dir3", dirs[2]);
        fw.write(line + '\n');
      }
      fw.close();
      scan.close();
    } catch (FileNotFoundException e) {
      System.out.println("file not found");
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("IOException");
    }
  }
  
  public String getHostname() {
    return this.hostname;
  }
  
  public int getTorPort() {
    return torPort;
  }

  public void killTor() {
    if(torProcess != null && torProcess.isAlive()) {
      System.out.println("killing tor process");
      torProcess.destroy();
    }
  }
}
