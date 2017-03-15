package torrent;

import torrent.Torrent.TorrentState;

public class ProgressChecker implements Runnable {
 
  Torrent torrent;
  
  public ProgressChecker(Torrent t) {
    torrent = t;
  }

  @Override
  public void run() {
    TorrentState oldState = torrent.getState();
    torrent.setState(TorrentState.Checking);
    torrent.checkProgress();
    if (torrent.getState() == TorrentState.Checking) {
      torrent.setState(oldState);
    }
  }
}
