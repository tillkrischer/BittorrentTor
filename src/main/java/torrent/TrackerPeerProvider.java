package torrent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import bencode.BencodeByteString;
import bencode.BencodeDictionary;
import bencode.BencodeElem;
import bencode.BencodeInteger;
import bencode.BencodeList;
import bencode.BencodeParser;

public class TrackerPeerProvider implements PeerProvider {

  private static String percentEncode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    for (byte b : data) {
      if (Character.isAlphabetic(b) || Character.isDigit(b) || (b == '.') || (b == '-')
          || (b == '_') || (b == '~') || (b == '$') || (b == '+') || (b == '!') || (b == '*')
          || (b == '\'') || (b == '(') || (b == ')') || (b == ',')) {
        sb.append((char) b);
      } else {
        sb.append("%" + String.format("%02x", b));
      }
    }
    return sb.toString();
  }

  private Torrent torrent;
  private boolean torMode;
  private boolean useCompact;
  private String announceUrl;
  private long lastTrackerRequestTime;
  private int trackerInterval;
  private byte[] trackerId;

  public TrackerPeerProvider(Torrent t, boolean torMode) {
    this.torMode = torMode;
    this.torrent = t;
    useCompact = true;
    announceUrl = torrent.getAnnounceUrl();
    lastTrackerRequestTime = 0;
  }

  private String constructUrl(String base, HashMap<String, String> parameters) {
    StringBuilder url = new StringBuilder();
    url.append(base);
    url.append('?');
    boolean first = true;
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (!first) {
        url.append('&');
      }
      url.append(entry.getKey());
      url.append('=');
      url.append(entry.getValue());
      first = false;
    }
    return url.toString();
  }

  public HashSet<Peer> trackerRequest(String event, Torrent torrent) {
    HashSet<Peer> peerSet = new HashSet<Peer>();
    try {
      HashMap<String, String> parameters = new HashMap<String, String>();
      parameters.put("info_hash", percentEncode(torrent.getInfoHash()));
      parameters.put("peer_id", percentEncode(torrent.getPeerId()));
      parameters.put("port", Integer.toString(torrent.getPort()));
      parameters.put("uploaded", Long.toString(torrent.getUploaded()));
      parameters.put("downloaded", Long.toString(torrent.downloaded()));
      parameters.put("left", Long.toString(torrent.getTotalSize() - torrent.downloaded()));
      if (torMode) {
        parameters.put("address", torrent.getTorMan().getHostname());
      }
      if (useCompact) {
        parameters.put("compact", "1");
      } else {
        parameters.put("compact", "0");
      }
      if (torrent.getTrackerId() != null) {
        parameters.put("trackerid", percentEncode(torrent.getTrackerId()));
      }
      if (!event.equals("")) {
        parameters.put("event", event);
      }
      String s = constructUrl(announceUrl, parameters);
      System.out.println(s);

      URL url = new URL(s);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      InputStream in = connection.getInputStream();
      BencodeParser parser = new BencodeParser(in);
      BencodeDictionary response = parser.readDictionary();
      peerSet.addAll(parseTrackerResponse(response));
      lastTrackerRequestTime = System.currentTimeMillis() / 1000;
    } catch (IOException e) {
      System.out.println("tracker request error");
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
    return peerSet;
  }


  private HashSet<Peer> parseTrackerResponse(BencodeDictionary response) {
    HashSet<Peer> peerSet = new HashSet<Peer>();
    try {
      if (response.dict.containsKey("failure reason")) {
        System.out.println("Tracker request failure:");
        BencodeByteString reason = (BencodeByteString) response.dict.get("failure reason");
        System.out.println(reason.getValue());
        return peerSet;
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
        torrent.setSeederCount(completeCount.value);
      }
      if (response.dict.containsKey("incomplete")) {
        BencodeInteger incompleteCount = (BencodeInteger) response.dict.get("incomplete");
        torrent.setLeecherCount(incompleteCount.value);
      }
      if (response.dict.containsKey("peers")) {
        BencodeElem peers = response.dict.get("peers");
        peerSet.addAll(updatePeers(peers));
      } else {
        throw new InvalidTrackerResponseException();
      }
    } catch (ClassCastException exception) {
      System.out.println("Error parsing tracker response");
    } catch (InvalidTrackerResponseException e) {
      System.out.println("Error parsing tracker response");
    }
    return peerSet;
  }

  private HashSet<Peer> updatePeers(BencodeElem peers) throws InvalidTrackerResponseException {
    HashSet<Peer> peersSet = new HashSet<Peer>();
    try {
      if (peers instanceof BencodeByteString) {
        // compact response
        byte[] peerData = ((BencodeByteString) peers).data;
        if ((peerData.length % 6) != 0) {
          throw new InvalidTrackerResponseException();
        }
        for (int i = 0; i < (peerData.length / 6); i++) {
          try {
            IpPeer p = new IpPeer();
            byte[] ip = new byte[4];
            for (int j = 0; j < ip.length; j++) {
              ip[j] = peerData[(i * 6) + j];
            }
            p.address = InetAddress.getByAddress(ip);
            byte[] port = new byte[2];
            for (int j = 0; j < port.length; j++) {
              port[j] = peerData[(i * 6) + 4 + j];
            }
            p.port = (int) Util.bigEndianToInt(port);
            peersSet.add(p);
          } catch (UnknownHostException e) {
            System.out.println("UnknownHostException");
            e.printStackTrace();
          }
        }
      } else if (peers instanceof BencodeList) {
        BencodeList peerList = (BencodeList) peers;
        for (BencodeElem elem : peerList.list) {
          BencodeDictionary entry = (BencodeDictionary) elem;
          if (!(entry.dict.containsKey("port"))) {
            throw new InvalidTrackerResponseException();
          }
          BencodeInteger port = (BencodeInteger) entry.dict.get("port");
          if (entry.dict.containsKey("ip")) {
            if (! torMode) {
              try {
                BencodeByteString ip = (BencodeByteString) entry.dict.get("ip");
                IpPeer p = new IpPeer();
                p.address = InetAddress.getByName(ip.getValue());
                p.port = port.value;
                peersSet.add(p);
              } catch (UnknownHostException e) {
                System.out.println("unknown host exception");
              }
            }
          } else if(entry.dict.containsKey("address")) {
            if (torMode) {
              BencodeByteString address = (BencodeByteString) entry.dict.get("address");
              TorPeer p = new TorPeer();
              p.hostname = address.getValue();
              p.port = port.value;
              peersSet.add(p);
            }
          } else {
            throw new InvalidTrackerResponseException();
          }
        }
      } else {
        throw new InvalidTrackerResponseException();
      }
    } catch (ClassCastException exception) {
      System.out.println("Error parsing peers");
      throw new InvalidTrackerResponseException();
    }
    return peersSet;
  }

  @Override
  public HashSet<Peer> announce(String status, Torrent torrent) {
    return trackerRequest(status, torrent);
  }
}
