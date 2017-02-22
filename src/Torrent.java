import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import bencode.BencodeByteString;
import bencode.BencodeDictionary;
import bencode.BencodeElem;
import bencode.BencodeInteger;
import bencode.BencodeList;
import bencode.BencodeParser;

public class Torrent {
  
  File torrentFile;
  private String announceUrl;
  private long pieceLength;
  private byte[] pieces;
  private ArrayList<TorrentOutputFile> files;
  private long totalSize;
  private long downloaded;
  private long uploaded;
  private byte[] infoHash;
  private int numberOfPieces;
  
  // TODO: these should be done at client startup not here
  private byte[] peerId;
  private int port = 6881;
  private String downloadDir = "test/downloads/";
  // NOTE: this is only introduced in BEP23, but required for most modern trackers
  private boolean useCompact = true;
 
  private HashSet<Peer> allPeers;
  private byte[] trackerId;
  // interval for tracker request in seconds
  private int trackerInterval;
  private int seederCount;
  private int leecherCount;
  private long lastTrackerRequestTime;
  
  private HashMap<PeerConnection, Thread> activePeers;
  
  private TorrentProgress progress;
  
  public Torrent(String filename) throws InvalidTorrentFileException, FileNotFoundException {
    torrentFile = new File(filename);
    parseTorrent();
    generatePeerId();
    activePeers = new HashMap<PeerConnection, Thread>();
    allPeers = new HashSet<Peer>();
    totalSize = calculateLength();
    downloaded = 0;
    uploaded = 0;
    numberOfPieces = (int) ((totalSize + pieceLength - 1) / pieceLength);
    progress = new TorrentProgress(numberOfPieces);
   
    System.out.println("number of pices:" + numberOfPieces);
    
    checkProgress();
  }
  
  public void checkProgress() {
    int count = 0;
    for (int i = 0; i < numberOfPieces; i++) {
      try {
        byte[] data = getPiece(i);
        if (checkHash(i, data)) {
          progress.setDownloaded(i);
          count++;
          System.out.println("piece " + i + " was correct");
        }
      } catch (IOException e) {
        //quiet this, since it will happen whenever the file doesnt exit before download
        //System.out.println("Error reading download files");
      }
    }
    System.out.println("progress: " + (double)count / numberOfPieces);
  }
  
  private boolean checkHash(int index, byte[] data) {
    byte[] recievedHash = sha1(data);
    byte[] expectedHash = getControlHash(index);
    return Arrays.equals(recievedHash, expectedHash);
  }
  
  private byte[] getControlHash(int piece) {
    byte[] hash = new byte[20];
    for (int i = 0; i < hash.length; i++) {
      hash[i] = pieces[piece * 20 + i];
    }
    return hash;
  }
  
  public void listPieceLocations() {
    for (int i = 0; i < numberOfPieces; i++) {
      long[] a = getPieceLocation(i); 
      System.out.println(i + ":");
      System.out.println("file: " + files.get((int) a[0]).path);
      System.out.println("offest: " + a[1]);
    }
  }
  
  public synchronized byte[] getPiece(int index) throws IOException {
    int size = (int) min(pieceLength, totalSize - index * pieceLength);
    byte[] data = new byte[size];
    long[] a = getPieceLocation(index);
    int fileIndex = (int) a[0];
    TorrentOutputFile outFile = files.get(fileIndex);
    long offset = a[1];
    int remainingSize = size;
    int i = 0;
    while (remainingSize + offset > outFile.length) {
      byte[] data1 = new byte[(int) (outFile.length - offset)];
      outFile.file.seek(offset);
      int read = outFile.file.read(data1);
      if (read < data1.length) {
        throw new IOException();
      }
      for (int j = 0; j < data1.length; j++) {
        data[i++] = data1[j];
      }
      offset = 0;
      fileIndex++;
      outFile = files.get(fileIndex);
      remainingSize -= data1.length;
    }
    byte[] data2 = new byte[remainingSize];
    outFile.file.seek(offset);
    int read = outFile.file.read(data2);
    if (read < data2.length) {
      throw new IOException();
    }
    for (int j = 0; j < data2.length; j++) {
      data[i++] = data2[j];
    }
    return data;
  }
  
  public synchronized void writePiece(int index, byte[] data) throws IOException {
    long[] a = getPieceLocation(index);
    int fileIndex = (int) a[0];
    TorrentOutputFile outFile = files.get(fileIndex);
    long offset = a[1];
    int remainingSize = data.length;
    int i = 0;
    while (remainingSize + offset > outFile.length) {
      byte[] data1 = new byte[(int) (outFile.length - offset)];
      for (int j = 0; j < data1.length; j++) {
        data1[j] = data[i++];
      }
      outFile.file.seek(offset);
      outFile.file.write(data1);
      offset = 0;
      fileIndex++;
      outFile = files.get(fileIndex);
      remainingSize -= data1.length;
    }
    byte[] data2 = new byte[remainingSize];
    for (int j = 0; j < data2.length; j++) {
      data2[j] = data[i++];
    }
    outFile.file.seek(offset);
    outFile.file.write(data2);
  }
  
  private long[] getPieceLocation(int index) {
    // a[0] is the index into the file array
    // a[1] is the offset within that file
    long[] a = new long[2];
    long locationBytes = index * pieceLength;
    long fileStart = 0;
    for (a[0] = 0; a[0] < files.size(); a[0]++) {
      long fileLength = files.get((int)a[0]).length;
      if (fileStart + fileLength > locationBytes) {
        a[1] = locationBytes - fileStart;
        return a;
      }
      fileStart += fileLength;
    }
    return a;
  }
  
  public long getTotalBytes() {
    return totalSize;
  }
  
  public long getPieceSize() {
    return pieceLength;
  }
  
  public TorrentProgress getProgress() {
    return progress;
  }
  
  public int getNumberOfPieces() {
    return numberOfPieces;
  }
  
  public void start() {
    trackerRequest("started");
  }
  
  public int getNumActivePeers() {
    return activePeers.size();
  }
  
  public void checkConnections() {
    for (Map.Entry<PeerConnection, Thread> entry : activePeers.entrySet()) {
      Thread t = entry.getValue();
      PeerConnection pc = entry.getKey();
      if (! t.isAlive()) {
        System.out.println("connection to peer " + pc.getPeer() + " ended");
      }
    }
  }
 
  // adds and inactive Peer to the active peers
  // PeerConnection asynchronously does the handshake
  // and sets peer state to bad in case of failure
  public void connectToPeer() {
    Peer p = getInactivePeer();
    if (p != null) {
      try {
        System.out.println("trying to connect to peer " + p);
        PeerConnection pc;
        pc = new PeerConnection(peerId, p);
        pc.assignTorrent(this);
        Thread th = new Thread(pc);
        th.start();
        activePeers.put(pc, th);
        p.setState(Peer.PeerState.active);
      } catch (IOException e) {
        p.setState(Peer.PeerState.bad);
        System.out.println("Error opening Socket to " + p);
      }
    } else {
      System.out.println("no inactive peers");
    }
  }
  
  public Peer getInactivePeer() {
    Peer selected = null;
    Iterator<Peer> it = allPeers.iterator();
    Peer p;
    while (it.hasNext() && selected == null) {
      p = it.next();
      if (p.getState() == Peer.PeerState.inactive) {
        selected = p;
      }
    }
    return selected;
  }
  
  public long getTotalSize() {
    return totalSize;
  }
  
  private long calculateLength() {
    long size = 0;
    for (TorrentOutputFile f : files) {
      size += f.length;
    }
    return size;
  }
  
  private void calculateinfoHash(BencodeDictionary info) throws IOException {
    int start = info.start;
    int end = info.end;
    RandomAccessFile raf = new RandomAccessFile(torrentFile, "r");
    byte[] data = new byte[end - start];
    raf.seek(start);
    raf.read(data);
    infoHash = sha1(data);
    raf.close();
  }
  
  private void parseTorrent() throws InvalidTorrentFileException {
    try {
      // NOTE: we dont differentiate between single and multiple-file torrents
      //       single file torrents will just have one entry in the files list.
      files = new ArrayList<TorrentOutputFile>();
     
      FileInputStream in = new FileInputStream(torrentFile);
      BencodeParser parser = new BencodeParser(in);
      BencodeDictionary torrent;
      torrent = parser.readDictionary();
      in.close();
     
      //need an announce
      if (! torrent.dict.containsKey("announce")) {
        throw new InvalidTorrentFileException();
      }
      BencodeByteString a = (BencodeByteString) torrent.dict.get("announce");
      announceUrl = a.getValue();
     
      //info dictionary
      if (! torrent.dict.containsKey("info")) {
        throw new InvalidTorrentFileException();
      }
      BencodeDictionary info = (BencodeDictionary) torrent.dict.get("info");
      calculateinfoHash(info);
      
      //these 3 fields are required
      if ((! info.dict.containsKey("name"))
          || (! info.dict.containsKey("piece length"))
          || (! info.dict.containsKey("pieces"))) {
        throw new InvalidTorrentFileException();
      }
      BencodeByteString n = (BencodeByteString) info.dict.get("name");
      String name = n.getValue();
      BencodeInteger i = (BencodeInteger) info.dict.get("piece length");
      pieceLength = i.value;
      BencodeByteString p = (BencodeByteString) info.dict.get("pieces");
      pieces = p.data;
      
      //we need either length or files, but not both or neither
      if ((! info.dict.containsKey("length")) && (! info.dict.containsKey("files"))
          || (info.dict.containsKey("length") && info.dict.containsKey("files"))) {
        throw new InvalidTorrentFileException();
      }
      if (info.dict.containsKey("length")) {
        //Single-file torrent
        BencodeInteger l = (BencodeInteger) info.dict.get("length");
        TorrentOutputFile t = new TorrentOutputFile(name, l.value, downloadDir);
        files.add(t);
      } else {
        //Multiple-file torrent
        BencodeList filelist = (BencodeList) info.dict.get("files");
        for (BencodeElem elem : filelist.list) {
          BencodeDictionary d = (BencodeDictionary) elem;
          if ((! d.dict.containsKey("length"))
              || (! d.dict.containsKey("path"))) {
            throw new InvalidTorrentFileException();
          }
          StringBuilder path = new StringBuilder();
          path.append(name);
          BencodeList pathlist = (BencodeList) d.dict.get("path");
          if (pathlist.list.size() == 0) {
            throw new InvalidTorrentFileException();
          }
          for (BencodeElem pathElem : pathlist.list) {
            BencodeByteString s = (BencodeByteString) pathElem;
            path.append('/');
            path.append(s.getValue());
          }
          BencodeInteger l = (BencodeInteger) d.dict.get("length");
          TorrentOutputFile t = new TorrentOutputFile(path.toString(), l.value, downloadDir);
          files.add(t);
        }
      }
    } catch (IOException e) {
      throw new InvalidTorrentFileException();
    } catch (ClassCastException e) {
      throw new InvalidTorrentFileException();
    }
  }
 
  // TODO: maybe put this somewhere else
  public static byte[] sha1(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] hash = md.digest(data);
      return hash;
    } catch (NoSuchAlgorithmException e) {
      System.out.println("sha1 not found");
      return (new byte[20]);
    } 
  }

  public byte[] getInfoHash() {
    return infoHash;
  }
  
  private void generatePeerId() {
    peerId = new byte[20];
    int i = 0;
    // TODO: pretend that we are deluge for now
    String clientIdent = "-DE13D0-";
    while (i < clientIdent.length()) {
      peerId[i] = (byte) clientIdent.charAt(i);
      i++;
    }
    Random rand = new Random();
    while (i < peerId.length) {
      peerId[i] = (byte) (rand.nextInt('z' - 'a') + 'a');
      i++;
    }
  }
  
  public void trackerRequest() {
    trackerRequest("");
  }
  
  public void trackerRequest(String event) {
    try {
      HashMap<String, String> parameters = new HashMap<String, String>();
      parameters.put("info_hash", percentEncode(infoHash));
      parameters.put("peer_id", percentEncode(peerId));
      parameters.put("port", Integer.toString(port));
      parameters.put("uploaded", Long.toString(uploaded));
      parameters.put("downloaded", Long.toString(downloaded));
      parameters.put("left", Long.toString(totalSize - downloaded));
      if (useCompact) {
        parameters.put("compact", "1");
      } else {
        parameters.put("compact", "0");
      }
      if (trackerId != null) {
        parameters.put("trackerid", percentEncode(trackerId));
      }
      if (! event.equals("")) {
        parameters.put("event", event);
      }
      String s = constructUrl(announceUrl, parameters);
      
      URL url = new URL(s);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("GET");
      InputStream in = connection.getInputStream();
      BencodeParser parser = new BencodeParser(in);
      BencodeDictionary response = parser.readDictionary();
      parseTrackerResponse(response);
      lastTrackerRequestTime = System.currentTimeMillis();
    } catch (IOException e) {
      System.out.println("tracker request error");
      System.out.println(e.getMessage());
    }  
  }
  
  private void parseTrackerResponse(BencodeDictionary response) {
    try {
      if (response.dict.containsKey("failure reason")) {
        System.out.println("Tracker request failure:");
        BencodeByteString reason = (BencodeByteString) response.dict.get("failure reason");
        System.out.println(reason.getValue());
        return;
      }
      if (response.dict.containsKey("interval")) {
        BencodeInteger interv = (BencodeInteger) response.dict.get("interval");
        trackerInterval = interv.value;
      }
      if (response.dict.containsKey("tracker id")) {
        BencodeByteString id = (BencodeByteString) response.dict.get("tracker id");
        trackerId = id.data;
      }
      if (response.dict.containsKey("complete")) {
        BencodeInteger completeCount = (BencodeInteger) response.dict.get("complete");
        seederCount = completeCount.value;
      }
      if (response.dict.containsKey("incomplete")) {
        BencodeInteger incompleteCount = (BencodeInteger) response.dict.get("incomplete");
        leecherCount = incompleteCount.value;
      }
      if (response.dict.containsKey("peers")) {
        BencodeElem peers = response.dict.get("peers");
        updatePeers(peers);
      } else {
        throw new InvalidTrackerResponseException();
      }
    } catch (ClassCastException exception) {
      System.out.println("Error parsing tracker response");
    } catch (InvalidTrackerResponseException e) {
      System.out.println("Error parsing tracker response");
    }
  }
 
  private void updatePeers(BencodeElem peers) throws InvalidTrackerResponseException {
    try {
      if (peers instanceof BencodeByteString) {
        //compact response
        byte[] peerData = ((BencodeByteString) peers).data;
        if ((peerData.length % 6) != 0) {
          throw new InvalidTrackerResponseException();
        }
        for (int i = 0; i < peerData.length / 6; i++) {
          try {
            Peer p = new Peer();
            byte[] ip = new byte[4];
            for (int j = 0; j < ip.length; j++) {
              ip[j] = peerData[i * 6 + j];
            }
            p.address = InetAddress.getByAddress(ip);
            byte[] port = new byte[2];
            for (int j = 0; j < port.length; j++) {
              port[j] = peerData[i * 6 + 4 + j];
            }
            p.port = (int) Util.bigEndianToInt(port);
            allPeers.add(p);
          } catch (UnknownHostException e) {
            System.out.println("UnknownHostException");
            e.printStackTrace();
          }
        }
      } else if (peers instanceof BencodeList) {
        // TODO: find a tracker that doesn't force compact to actually test this
        BencodeList peerList = (BencodeList) peers;
        for (BencodeElem elem : peerList.list) {
          try {
            BencodeDictionary entry = (BencodeDictionary) elem;
            if ((! (entry.dict.containsKey("ip"))) || (! (entry.dict.containsKey("port")))) {
              throw new InvalidTrackerResponseException();
            }
            BencodeByteString ip = (BencodeByteString) entry.dict.get("ip");
            BencodeInteger port = (BencodeInteger) entry.dict.get("port");
            Peer p = new Peer();
            p.address = InetAddress.getByName(ip.getValue());
            p.port = port.value;
          } catch (UnknownHostException e) {
            System.out.println("UnknowHostException");
          }
        }
        
      } else {
        throw new InvalidTrackerResponseException();
      }
    } catch (ClassCastException exception) {
      System.out.println("Error parsing peers");
      throw new InvalidTrackerResponseException();
    }
  }
        
  private String constructUrl(String base, HashMap<String, String> parameters) {
    StringBuilder url = new StringBuilder();
    url.append(base);
    url.append('?');
    boolean first = true;
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (! first) {
        url.append('&');
      }
      url.append(entry.getKey());
      url.append('=');
      url.append(entry.getValue());
      first = false;
    }
    return url.toString();
  }
  
  private static String percentEncode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    for (byte b : data) {
      if (Character.isAlphabetic(b) || Character.isDigit(b) 
          || b == '.' || b == '-' || b == '_' || b == '~' || b == '$' || b == '+' 
          || b == '!' || b == '*' || b == '\'' || b == '(' || b == ')' || b == ',') {
        sb.append((char)b);
      } else {
        sb.append("%" + String.format("%02x", b));
      }
    }
    return sb.toString();
  }
  
  public static long min(long a, long b) {
    if (a < b) {
      return a;
    } else {
      return b;
    }
  }
}
