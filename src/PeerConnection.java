import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class PeerConnection implements Runnable {
  
  private final int blocksize = 16384; 
  private final int requestQueueSize = 3; 
  
  private Socket socket;
  private BufferedInputStream in;
  private BufferedOutputStream out;
  private Torrent torrent;
  private byte[] myPeerId;
  private byte[] remotePeerId;
  private final String PROTOCOL = "BitTorrent protocol";
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
  
  public PeerConnection(byte[] peerId, Peer p) throws IOException {
    socket = new Socket(p.address, p.port);
    in = new BufferedInputStream(socket.getInputStream());
    out = new BufferedOutputStream(socket.getOutputStream());
    myPeerId = peerId;
    peer = p;
    
    isConnected = false;
  }
  
  public Peer getPeer() {
    return peer;
  }
  
  public void assignTorrent(Torrent t) {
    torrent = t;
    progress = torrent.getProgress();
    peerPieces = new boolean[torrent.getNumberOfPieces()];
  }

  @Override
  public void run() {
    if (torrent != null) {
      try {
        sendHandshake();
        byte[] responseInfoHash = recieveHandshake();
        if (! Arrays.equals(responseInfoHash, torrent.getInfoHash())) {
          throw new IOException();
        }
        System.out.println("succesful handshake with " + peer);
      } catch (IOException e) {
        System.out.println("IOError during handshake");
        e.printStackTrace();
        peer.setState(Peer.PeerState.bad);
        return;
      }
    } else {
      // TODO: case where we get connected to
      //recieveHandshake()
      //sendHandshake()
    }
    isConnected = true;
    // TODO: maybe not use infinite loop ?
    while (true) {
      try {
        sendBitFiled();
        while (amInterested || isLeecher()) {
          if (amInterested && amChocking) {
            sendUnchoke();
          } else if (peerInterested && amChocking) {
            sendUnchoke();
          } else if (amInterested && ! peerChocking) {
            downloadPiece();
          } else {
            recieveMessage();
          }
        }
      } catch (IOException e) {
        log("IOException");
        break;
      } catch (InvalidMessageException e) {
        log("InvalidMessageException");
        break;
      } 
    }
    
    peer.setState(Peer.PeerState.inactive);
  }
  
  public boolean isLeecher() {
    for (boolean b : peerPieces) {
      if (! b) {
        return true;
      }
    }
    return false;
  }
 
  public boolean isConnected() {
    return isConnected;
  }
  
  public void recieveMessage() throws IOException, InvalidMessageException {
    byte[] number = new byte[4]; 
    read(number);
    int len = (int) Util.bigEndianToInt(number);
    if (len == 0) {
      //keepalive
      log("recived keepalive");
    } else {
      int messageId = read();
      switch (messageId) {
        case 0: {
          //choke
          log("recived choke");
          peerChocking = true;
          break;
        } 
        case 1: {
          //unchoke
          log("recived unchoke");
          peerChocking = false;
          break;
        }
        case 2: {
          //interested
          log("recived interested");
          peerInterested = true;
          break;
        }
        case 3: {
          //not interested
          log("recived not interested");
          peerInterested = false;
          break;
        }
        case 4: {
          //have
          log("recived have");
          read(number);
          int pieceindex = (int) Util.bigEndianToInt(number);
          setHaveBit(pieceindex);
          updateInterested(); 
          break;
        }
        case 5: {
          //bitfield
          log("recived bitfield");
          byte[] bitfield = new byte[len - 1];
          read(bitfield);
          log("bit field: " + Arrays.toString(bitfield));
          setHaveBits(bitfield);
          updateInterested();
          break;
        }
        case 6: {
          //request
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
          //piece
          read(number);
          int index = (int) Util.bigEndianToInt(number);
          read(number);
          int begin = (int) Util.bigEndianToInt(number);
          byte[] data = new byte[len - 9];
          read(data);
          log("recieved piece: index: " + index + " begin " + begin);
          if (index == downloadingPiece.getIndex()) {
            downloadingPiece.putBlock(begin, data);
            if (downloadingPiece.queueSize() > 0) {
              int[] a = downloadingPiece.popRequest();
              sendRequest(downloadingPiece.getIndex(), a[0], a[1]);
            }
          }
          break;
        }
        //last two are not implemented
        case 8:
          //cancel
          log("recived cancel");
          System.out.println("invalid message cancel");
          throw new InvalidMessageException();
          //break;
        case 9:
          //port
          log("recived port");
          System.out.println("invalid message port");
          throw new InvalidMessageException();
          //break;
        default:
          System.out.println("invalid message default");
          throw new InvalidMessageException();
      }
    }
  }
  
  public synchronized void sendHandshake() throws IOException {
    out.write((byte)19);
    String pstr = PROTOCOL;
    out.write(pstr.getBytes());
    for (int i = 0; i < 8; i++) {
      out.write(0);
    }
    out.write(torrent.getInfoHash());
    out.write(myPeerId);
    out.flush();
  }
  
  public byte[] recieveHandshake() throws IOException {
    int length = read();
    byte[] pstr = new byte[length];
    read(pstr);
    String s = new String(pstr, "UTF-8");
    if (! s.equals(PROTOCOL)) {
      throw new IOException("protocol wrong");
    }
    byte[] infohash = new byte[20];
    byte[] peerid = new byte[20];
    in.skip(8);
    read(infohash);
    read(peerid);
    remotePeerId = peerid;
    return infohash;
  }
  
  public synchronized void sendChoke() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(0);
    amChocking = true;
    out.flush();
    log("send choke");
  }
  
  public synchronized void sendUnchoke() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(1);
    amChocking = false;
    out.flush();
    log("send unchoke");
  }
  
  public synchronized void sendInterested() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(2);
    amInterested = true;
    out.flush();
    log("send interested");
  }
  
  public synchronized void sendUninterested() throws IOException {
    byte[] len = Util.intToBigEndian(1, 4);
    out.write(len);
    out.write(3);
    amInterested = false;
    out.flush();
    log("send uninterested");
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
        bitfield[i / 8] |= 1 << (8 - 1 - i % 8);
      }
    }
    out.write(bitfield);
    out.flush();
    log("send bitfield " + Arrays.toString(bitfield));
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
    //to noisy
    //log("send request: index: " + index + " begin: " + begin + " length: " + length);
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
    //to noisy
    //log("send piece: index: " + index + " begin: " + begin);
  }
  
  public void downloadPiece() throws IOException, InvalidMessageException {
    int i = progress.claimForDownload(peerPieces);
    if (i != -1) {
      downloadingPiece = new Piece(i, torrent.getTotalBytes(), blocksize, torrent.getPieceSize());
      log("Downloading piece " + i);
      for (int j = 0; j < requestQueueSize && downloadingPiece.queueSize() > 0; j++) {
        int[] a = downloadingPiece.popRequest();
        int blockindex = a[0];
        int length = a[1];
        sendRequest(downloadingPiece.getIndex(), blockindex, length);
      }
      while (! downloadingPiece.recievedAllBlocks()) {
        recieveMessage();
      }
      byte[] a = downloadingPiece.getData();
      torrent.writePiece(downloadingPiece.getIndex(), a);
      downloadingPiece = null;
    }
  }
  
  private void setHaveBit(int index) throws InvalidMessageException {
    if (index >= 0 && index < torrent.getNumberOfPieces()) {
      peerPieces[index] = true;
    } else {
      throw new InvalidMessageException();
    }
  }
  
  private void setHaveBits(byte[] field) throws InvalidMessageException {
    if (field.length == (torrent.getNumberOfPieces() + 7) / 8) {
      for (int i = 0; i < peerPieces.length; i++) {
        peerPieces[i] = (field[i / 8] & (1 << (8 - 1 - i % 8))) > 0;
      }
    } else {
      throw new InvalidMessageException();
    }
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
  
  public void updateInterested() throws IOException {
    int i = progress.getAvailableMissing(peerPieces);
    if (i != -1 && ! amInterested) {
      sendInterested();
    } else if (i == -1 && amInterested) {
      sendUninterested();
    }
  }
  
  private byte read() throws IOException {
    int i = in.read();
    if (i == -1) {
      throw new EOFException();
    }
    return (byte)i;
  }
  
  private void read(byte[] data) throws IOException {
    // TODO: timeout ?
    int i = in.read(data);
    if (i == -1) {
      throw new EOFException();
    }
  }
  
  private void log(String msg) {
    System.out.println("Peer " + peer + " - " + msg);
  }  
}
