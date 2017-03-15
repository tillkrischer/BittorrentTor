package torrent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class TorrentOutputFile {
  String path;
  long length;
  RandomAccessFile file;
  
  public TorrentOutputFile(String path, long length, String downloadDir) {
    try {
      this.path = path;
      this.length = length;
      openFile(downloadDir);
    } catch (FileNotFoundException e) {
      System.out.println("Unable to open download file");
      e.printStackTrace();
    }
  }
  
  public void openFile(String downloadDir) throws FileNotFoundException {
    String fullPath = downloadDir + path;
    File folder = new File(fullPath.substring(0, fullPath.lastIndexOf('/')));
    if (folder.mkdirs()) {
      System.out.println("created folder: " + folder.getPath());
    }
    file = new RandomAccessFile(fullPath, "rw");
  }
}
