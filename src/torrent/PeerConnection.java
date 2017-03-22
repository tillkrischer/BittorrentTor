package torrent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class PeerConnection implements Runnable {

  private final int blocksize = 16384;
  private final int requestQueueSize = 1;

  private Socket socket;
  private BufferedInputStream in;
  private BufferedOutputStream out;
  private Torrent torrent;
  private byte[] myPeerId;
  private byte[] remotePeerId;
  private final String protocolString = "BitTorrent protocol";
  private boolean amInterested;
  private boolean amChocking;
  private boolean peerInterested;
  private boolean peerChocking;
  private Peer peer;
  private boolean isConnected;
  private boolean[] peerPieces;
  private TorrentProgress progress;
  private int seedingPieceIndex;
  private byte[] seedingPieceData;
  private Piece downloadingPiece;

  public PeerConnection(Socket s, byte[] peerId, Peer p) throws IOException {
    socket = s;
    in = new BufferedInputStream(socket.getInputStream(), blocksize * requestQueueSize);
    out = new BufferedOutputStream(socket.getOutputStream());
    myPeerId = peerId;
    peer = p;

    amChocking = true;
    peerChocking = true;

    isConnected = false;

    remotePeerId = new byte[20];
  }

  public void assignTorrent(Torrent t) {
    torrent = t;
    progress = torrent.getProgress();
    peerPieces = new boolean[torrent.getNumberOfPieces()];
  }

  public void downloadPiece() throws IOException, InvalidMessageException {
    int i = progress.claimForDownload(peerPieces);
    if (i != -1) {
      downloadingPiece = new Piece(i, torrent.getTotalBytes(), blocksize, torrent.getPieceSize());
      log("Downloading piece " + i);
      for (int j = 0; (j < requestQueueSize) && (downloadingPiece.queueSize() > 0); j++) {
        int[] a = downloadingPiece.popRequest();
        int blockindex = a[0];
        int length = a[1];
        sendRequest(downloadingPiece.getIndex(), blockindex, length);
      }
      while (!downloadingPiece.recievedAllBlocks()) {
        recieveMessage();
      }
      byte[] a = downloadingPiece.getData();
      if (torrent.writePiece(downloadingPiece.getIndex(), a)) {
        progress.setDownloaded(downloadingPiece.getIndex());
        torrent.sendHave(downloadingPiece.getIndex());
      } else {
        progress.setMissing(downloadingPiece.getIndex());
      }
      downloadingPiece = null;
    }
  }

  public void endConnection() {
    if (downloadingPiece != null) {
      progress.setMissing(downloadingPiece.getIndex());
    }
    torrent.deactivateConncetion(this);
    torrent.markPeerInactive(peer);
    try {
      out.close();
      in.close();
    } catch (IOException e) {
      System.out.println("error closing streams");
    }
    log("connection ended");
  }

  public Peer getPeer() {
    return peer;
  }

  public boolean isConnected() {
    return isConnected;
  }

  public boolean isLeecher() {
    for (boolean b : peerPieces) {
      if (!b) {
        return true;
      }
    }
    return false;
  }

  private void log(String msg) {
    System.out.println("Peer " + peer + " - " + msg);
  }

  public void processRequest(int index, int begin, int length) throws IOException {
    if (seedingPieceIndex != index) {
      seedingPieceData = torrent.getPiece(index);
      seedingPieceIndex = index;
    }
    byte[] block = new byte[length];
    for (int i = 0; i < block.length; i++) {
      block[i] = seedingPieceData[begin + i];
    }
    sendPiece(index, begin, block);
  }

  private byte read() throws IOException {
    int i = in.read();
    if (i == -1) {
      throw new EOFException();
    }
    return (byte) i;
  }

  private void read(byte[] data) throws IOException {
    // TODO: timeout ?
    int i = in.read(data);
    if (i == -1) {
      throw new EOFException();
    }
  }

  public void recieveHandshake(byte[] infohash, byte[] peerid) throws IOException {
    int length = read();
    byte[] pstr = new byte[length];
    read(pstr);
    String s = new String(pstr, "UTF-8");
    if (!s.equals(protocolString)) {
      throw new IOException("protocol wrong");
    }
    in.skip(8);
    read(infohash);
    read(peerid);
    for (int i = 0; i < 20; i++) {
      remotePeerId[i] = peerid[i];
    }
    remotePeerId = peerid;
  }

  public void recieveMessage() throws IOException, InvalidMessageException {
    byte[] number = new byte[4];
    read(number);
    int len = (int) Util.bigEndianToInt(number);
    if (len == 0) {
      // keepalive
      log("recived keepalive");
    } else {
      int messageId = read();
      switch (messageId) {
        case 0: {
          // choke
          log("recived choke");
          peerChocking = true;
          break;
        }
        case 1: {
          // unchoke
          log("recived unchoke");
          peerChocking = false;
          break;
        }
        case 2: {
          // interested
          log("recived interested");
          peerInterested = true;
          break;
        }
        case 3: {
          // not interested
          log("recived not interested");
          peerInterested = false;
          break;
        }
        case 4: {
          // have
          log("recived have");
          read(number);
          int pieceindex = (int) Util.bigEndianToInt(number);
          setHaveBit(pieceindex);
          updateInterested();
          break;
        }
        case 5: {
          // bitfield
          log("recived bitfield");
          byte[] bitfield = new byte[len - 1];
          read(bitfield);
          log("bit field with length " + bitfield.length + ": " + Arrays.toString(bitfield));
          setHaveBits(bitfield);
          updateInterested();
          break;
        }
        case 6: {
          // request
          read(number);
          int index = (int) Util.bigEndianToInt(number);
          read(number);
          int begin = (int) Util.bigEndianToInt(number);
          read(number);
          int length = (int) Util.bigEndianToInt(number);
          log("recived request: index: " + index + " begin: " + begin + " length: " + length);
          processRequest(index, begin, length);
          break;
        }
        case 7: {
          // piece
          read(number);
          int index;
          index = (int) Util.bigEndianToInt(number);
          read(number);
          int begin;
          begin = (int) Util.bigEndianToInt(number);
          if (len < 9) {
            throw new InvalidMessageException();
          }
          byte[] data = new byte[len - 9];
          read(data);
          // more quiet
          log("received piece: index: " + index + " begin " + begin + " length " + (len - 9));
          if (index == downloadingPiece.getIndex()) {
            downloadingPiece.putBlock(begin, data);
            if (downloadingPiece.queueSize() > 0) {
              int[] a = downloadingPiece.popRequest();
              sendRequest(downloadingPiece.getIndex(), a[0], a[1]);
            }
          }
          break;
        }
        // last two are not implemented
        case 8:
          // cancel
          log("recived cancel");
          System.out.println("invalid message cancel");
          throw new InvalidMessageException();
          // break;
        case 9:
          // port
          log("recived port");
          System.out.println("invalid message port");
          throw new InvalidMessageException();
          // break;
        default:
          System.out.println("invalid message default id: " + messageId);
          throw new InvalidMessageException();
      }
    }
  }

  @Override
  public void run() {
    if (!isConnected) {
      try {
        sendHandshake();
        byte[] responseInfoHash = new byte[20];
        byte[] peerId = new byte[20];
        recieveHandshake(responseInfoHash, peerId);
        if (!Arrays.equals(responseInfoHash, torrent.getInfoHash())) {
          throw new IOException();
        }
        System.out.println("succesful handshake with " + peer + " " + new String(peerId));
        isConnected = true;
      } catch (IOException e) {
        System.out.println("IOError during handshake");
        e.printStackTrace();
        torrent.markPeerBad(peer);
        endConnection();
      }
    }
    try {
      sendBitFiled();
      do {
        if (amInterested && amChocking) {
          sendUnchoke();
        } else if (peerInterested && amChocking) {
          sendUnchoke();
        } else if (amInterested && !peerChocking) {
          downloadPiece();
        } else {
          recieveMessage();
        }
      } while (amInterested || isLeecher());
    } catch (IOException e) {
      log("IOException");
    } catch (InvalidMessageException e) {
      log("InvalidMessageException");
    }
    endConnection();
  }

  public synchronized void sendBitFiled() throws IOException {
    int x = (torrent.getNumberOfPieces() + 7) / 8;
    byte[] len = Util.intToBigEndian(1 + x, 4);
    out.write(len);
    out.write(5);
    byte[] bitfield = new byte[x];
    for (int i = 0; i < x; i++) {
      bitfield[i] = 0;
    }
    for (int i = 0; i < torrent.getNumberOfPieces(); i++) {
      if (progress.isDownloaded(i)) {
        bitfield[i / 8] |= 1 << (8 - 1 - (i % 8));
      }
    }
    out.write(bitfield);
    out.flush();
    log("send bitfield " + Arrays.toString(bitfield));
  }

  public synchronized void sendChoke() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(0);
    amChocking = true;
    out.flush();
    log("send choke");
  }

  public synchronized void sendHandshake() throws IOException {
    log("sending handshake");
    out.write((byte) 19);
    String pstr = protocolString;
    out.write(pstr.getBytes());
    for (int i = 0; i < 8; i++) {
      out.write(0);
    }
    out.write(torrent.getInfoHash());
    out.write(myPeerId);
    out.flush();
  }

  public synchronized void sendHave(int i) throws IOException {
    byte[] len = Util.intToBigEndian(5, 4);
    out.write(len);
    out.write(4);
    byte[] piece = Util.intToBigEndian(i, 4);
    out.write(piece);
    out.flush();
    log("send have");
  }

  public synchronized void sendInterested() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(2);
    amInterested = true;
    out.flush();
    log("send interested");
  }

  public synchronized void sendPiece(int index, int begin, byte[] block) throws IOException {
    byte[] len = Util.intToBigEndian(9 + block.length, 4);
    out.write(len);
    out.write(7);
    byte[] indexBytes = Util.intToBigEndian(index, 4);
    byte[] beginBytes = Util.intToBigEndian(begin, 4);
    out.write(indexBytes);
    out.write(beginBytes);
    out.write(block);
    out.flush();
    // to noisy
    log("send piece: index: " + index + " begin: " + begin);
  }

  public synchronized void sendRequest(int index, int block, int length) throws IOException {
    byte[] len = Util.intToBigEndian(13, 4);
    out.write(len);
    out.write(6);
    int begin = blocksize * block;
    byte[] indexBytes = Util.intToBigEndian(index, 4);
    byte[] beginBytes = Util.intToBigEndian(begin, 4);
    byte[] lengthBytes = Util.intToBigEndian(length, 4);
    out.write(indexBytes);
    out.write(beginBytes);
    out.write(lengthBytes);
    out.flush();
    // to noisy
    log("send request: index: " + index + " begin: " + begin + " length: " + length);
  }

  public synchronized void sendUnchoke() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(1);
    amChocking = false;
    out.flush();
    log("send unchoke");
  }

  public synchronized void sendUninterested() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(3);
    amInterested = false;
    out.flush();
    log("send uninterested");
  }

  public void setConnected() {
    isConnected = true;
  }

  private void setHaveBit(int index) throws InvalidMessageException {
    if ((index >= 0) && (index < torrent.getNumberOfPieces())) {
      peerPieces[index] = true;
    } else {
      throw new InvalidMessageException();
    }
  }

  private void setHaveBits(byte[] field) throws InvalidMessageException {
    if (field.length == ((torrent.getNumberOfPieces() + 7) / 8)) {
      for (int i = 0; i < peerPieces.length; i++) {
        peerPieces[i] = (field[i / 8] & (1 << (8 - 1 - (i % 8)))) > 0;
      }
    } else {
      throw new InvalidMessageException();
    }
  }

  public void updateInterested() throws IOException {
    int i = progress.getAvailableMissing(peerPieces);
    if ((i != -1) && !amInterested) {
      sendInterested();
    } else if ((i == -1) && amInterested) {
      sendUninterested();
    }
  }
}
