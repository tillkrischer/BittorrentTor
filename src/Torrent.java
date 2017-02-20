import bencode.BencodeByteString;
import bencode.BencodeDictionary;
import bencode.BencodeElem;
import bencode.BencodeInteger;
import bencode.BencodeList;
import bencode.BencodeParser;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

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
  
  // TODO: these should be done at client startup not here
  private byte[] peerId;
  private int port = 6881;
  // NOTE: this is only introduced in BEP23, but required for most modern trackers
  private boolean useCompact = true;
 
  private HashSet<Peer> allPeers;
  private byte[] trackerId;
  // interval for tracker request in seconds
  private int trackerInterval;
  private int seederCount;
  private int leecherCount;
  private long lastTrackerRequestTime;
  
  public Torrent(String filename) throws InvalidTorrentFileException, FileNotFoundException {
    torrentFile = new File(filename);
    parseTorrent();
    generatePeerId();
    allPeers = new HashSet<Peer>();
    totalSize = calculateLength();
    downloaded = 0;
    uploaded = 0;
  }
  
  public void start() {
    trackerRequest("started");
    
    // add a main class that handles peer distribution
    Peer p = getInactivePeer();
    if(p != null) {
      System.out.println(p);
    } else {
      System.out.println("none found");
    }
  }
  
  public Peer getInactivePeer() {
    Peer selected = null;
    try {
      Iterator<Peer> it = allPeers.iterator();
      Peer p;
      while(it.hasNext() && selected == null) {
        p = it.next();
        if (p.state == Peer.PeerState.inactive) {
          // Dont connect to ourself
          // TODO: test this with non local addresses
          //       do we actually need this? will the connection just fail on this case ?
          String ip = p.address.getHostAddress();
          String localIp = InetAddress.getLocalHost().getHostAddress();
          if ((ip.equals(localIp) || ip.equals("127.0.0.1")) && p.port == port) {
            p.state = Peer.PeerState.bad;
            System.out.println("ignoring self");
          } else {
            selected = p;
          }
        }
      }
    } catch (UnknownHostException e) {
      System.out.println("UnknownHostException");
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
        TorrentOutputFile t = new TorrentOutputFile();
        BencodeInteger l = (BencodeInteger) info.dict.get("length");
        t.length = l.value;
        t.path = name;
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
          TorrentOutputFile t = new TorrentOutputFile();
          BencodeInteger l = (BencodeInteger) d.dict.get("length");
          t.length = l.value;
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
          t.path = path.toString();
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
 
  // This parses the "compact" tracker response
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
            p.port = (int) bigEndianToInt(port);
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
            if((! (entry.dict.containsKey("ip"))) || (! (entry.dict.containsKey("port")))) {
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
  
  public static long bigEndianToInt(byte[] data) {
    long value = 0;
    for (int i = 0; i < data.length; i++) {
      value += ((int)(data[i] & 0x000000ff)) << (data.length - i - 1) * 8;
    }
    return value;
  }
}
