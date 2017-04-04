package torrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TorrentProgress {

  HashSet<Integer> missing;
  HashSet<Integer> downloading;
  HashSet<Integer> downloaded;
  int pieces;
  Random rand;

  public TorrentProgress(int pieces) {
    rand = new Random();
    missing = new HashSet<Integer>();
    downloading = new HashSet<Integer>();
    downloaded = new HashSet<Integer>();
    this.pieces = pieces;
    for (int i = 0; i < pieces; i++) {
      missing.add(i);
    }
  }

  public synchronized int claimForDownload(HashSet<Integer> peerPieces) {
    int piece = getAvailableMissing(peerPieces);
    if (piece != -1) {
      setDownloading(piece);
    }
    return piece;
  }

  public synchronized int getAvailableMissing(HashSet<Integer> peerPieces) {
    ArrayList<Integer> intersection = new ArrayList<Integer>(peerPieces);
    intersection.retainAll(missing);
    if (intersection.size() > 0) {
      return intersection.get(rand.nextInt(intersection.size()));
    } else {
      return -1;
    }
//    for(int i : missing) {
//      if(peerPieces.contains(i)) {
//        return i;
//      }
//    }
//    return -1;
  }

  public synchronized boolean isDone() {
    return (downloaded.size() == pieces);
  }

  public synchronized boolean isDowloading(int index) {
    return downloading.contains(index);
  }

  public synchronized boolean isDownloaded(int index) {
    return downloaded.contains(index);
  }

  public synchronized boolean isMissing(int index) {
    return missing.contains(index);
  }

  public synchronized int piecesDownloaded() {
    return downloaded.size();
  }

  public synchronized int piecesDownloading() {
    return downloading.size();
  }

  public synchronized int piecesMissing() {
    return missing.size();
  }

  public synchronized void setDownloaded(int index) {
    if (downloading.contains(index)) {
      downloading.remove(index);
    }
    if (missing.contains(index)) {
      missing.remove(index);
    }
    downloaded.add(index);
  }

  public synchronized void setDownloading(int index) {
    if (downloaded.contains(index)) {
      downloaded.remove(index);
    }
    if (missing.contains(index)) {
      missing.remove(index);
    }
    downloading.add(index);
  }

  public synchronized void setMissing(int index) {
    if (downloading.contains(index)) {
      downloading.remove(index);
    }
    if (downloaded.contains(index)) {
      downloaded.remove(index);
    }
    missing.add(index);
  }
}
