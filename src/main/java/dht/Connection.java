package dht;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class Connection {

  private Node node;
  private Listener listen;
  private Gson gson;
  private PeerInfo peerInfo;
  private int port;

  public Connection(String ip, int port, PeerInfo peerInfo) {
    this.peerInfo = peerInfo;
    this.port = port;
    node = new Node(ip, port);
    listen = new Listener(port, this);
    Thread t = new Thread(listen);
    t.start();
    gson = new Gson();
  }

  public Response makeRequest(String ip, int port, Request request) throws IOException, InvalidResponseException {
    log("making " + request.getClass());
    Socket s = new Socket(ip, port);
    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

    //send request
    out.println(gson.toJson(request));
    //receive response
    String line = in.readLine();
    s.close();
    JsonParser parser = new JsonParser();
    Response r = null;
    try {
      JsonObject obj = parser.parse(line).getAsJsonObject();
      if (! obj.has("type")) {
        throw new InvalidResponseException();
      }
      switch (obj.get("type").getAsString()) {
        case "ping":
          r = gson.fromJson(obj, PingResponse.class);
          break;
        case "find_node":
          r = gson.fromJson(obj, FindNodeResponse.class);
          break;
        case "get_peers_nodes":
          r = gson.fromJson(obj, GetPeersNodesResponse.class);
          break;
        case "get_peers_peers":
          r = gson.fromJson(obj, GetPeersPeersResponse.class);
          break;
        case "announce_peer":
          r = gson.fromJson(obj, AnnouncePeerResponse.class);
          break;
        default:
          throw new InvalidResponseException();
      }
      NodeInfo nodeinfo = new NodeInfo(r.id, ip, port);
      node.addNode(nodeinfo);
    } catch (JsonSyntaxException e) {
      throw new InvalidResponseException();
    }
    return r;
  }

  public PingRequest createPingRequest() {
    PingRequest r = new PingRequest(node.getNodeId(), port);
    return r;
  }

  public PingResponse createPingResponse(PingRequest request, String ip, int remotePort) {
    NodeInfo nodeInfo = new NodeInfo(request.id, ip, remotePort);
    node.addNode(nodeInfo);
    PingResponse r = new PingResponse(node.getNodeId());
    return r;
  }

  public FindNodeRequest createFindNodeRequest(byte[] targetId) {
    FindNodeRequest r = new FindNodeRequest(node.getNodeId(), targetId, port);
    return r;
  }

  public FindNodeResponse createFindNodeResponse(FindNodeRequest request, String ip, int remotePort) {
    NodeInfo nodeInfo = new NodeInfo(request.id, ip, remotePort);
    node.addNode(nodeInfo);
    FindNodeResponse r = new FindNodeResponse(node.getNodeId());
    List<NodeInfo> nodes = node.findNode(request.targetId);
    for (int i = 0; i < nodes.size() && i < Node.maxBucketSize; i++) {
      r.nodes[i] = nodes.get(i);
    }
    return r;
  }

  public GetPeersRequest createGetPeersRequest(byte[] info_hash) {
    GetPeersRequest r = new GetPeersRequest(node.getNodeId(), info_hash, port);
    return r;
  }

  public Response createGetPeersResponse(GetPeersRequest request, String ip, int remotePort) {
    NodeInfo nodeInfo = new NodeInfo(request.id, ip, remotePort);
    node.addNode(nodeInfo);
    Response r = null;
    LinkedList<PeerInfo> peers = node.getPeers(request.info_hash);
    if (peers != null) {
      GetPeersPeersResponse peerR = new GetPeersPeersResponse(node.getNodeId());
      for(int i = 0; i < peers.size() && i < Node.maxPeerResponseCount; i++) {
        peerR.values[i] = peers.get(i);
      }
      r = peerR;
    } else {
      GetPeersNodesResponse nodesR = new GetPeersNodesResponse(node.getNodeId());
      List<NodeInfo> nodes = node.findNode(request.info_hash);
      for (int i = 0; i < nodes.size() && i < Node.maxBucketSize; i++) {
        nodesR.nodes[i] = nodes.get(i);
      }
      r = nodesR;
    }
    return r;
  }

  public AnnouncePeerRequest createAnnouncePeerRequest(byte[] info_hash) {
    AnnouncePeerRequest r = new AnnouncePeerRequest(node.getNodeId(), info_hash, peerInfo, port);
    return r;
  }

  public AnnouncePeerResponse createAnnouncePeerResponse(AnnouncePeerRequest request, String ip, int remotePort) {
    NodeInfo nodeInfo = new NodeInfo(request.id, ip, remotePort);
    node.addNode(nodeInfo);
    node.addPeer(request.info_hash, request.peer);
    AnnouncePeerResponse r = new AnnouncePeerResponse(node.getNodeId());
    return r;
  }

  public void info() {
    node.printBuckets();
  }

  public void ping(String ip, int port) {
    PingRequest r = createPingRequest();
    try {
      makeRequest(ip, port, r);
    } catch (IOException | InvalidResponseException e) {
      log("error sending ping");
    }
  }

  public void bootstrap() {
    LinkedList<NodeInfo> queue = node.findNode(node.getNodeId());
    while(! queue.isEmpty()) {
      NodeInfo info = queue.removeFirst();
      if(! info.equals(node.getNodeInfo())) {
        try {
          log("request to " + info.ip + ":" + info.port);
          FindNodeRequest r = createFindNodeRequest(node.getNodeId());
          FindNodeResponse res = (FindNodeResponse) makeRequest(info.ip, info.port, r);
          for (NodeInfo result : res.nodes) {
            if (result != null && node.addNode(result)) {
              queue.addLast(result);
            }
          }
        } catch (IOException | InvalidResponseException e) {
          log("error sending find_node");
        }
      }
    }
  }

  public HashSet<PeerInfo> announce(byte[] info_hash) {
    HashSet<NodeInfo> queried = new HashSet<NodeInfo>();
    NodeInfoComparator comp = new NodeInfoComparator(Node.arrayToBigIntUnsigned(info_hash));
    LinkedList<NodeInfo> top8 = node.findNode(info_hash);

    top8.sort(comp);

    while (! queried.containsAll(top8)) {
      NodeInfo unqueried = null;
      Iterator<NodeInfo> it = top8.iterator();
      while(unqueried == null && it.hasNext()) {
        NodeInfo i = it.next();
        if(! queried.contains(i)) {
          unqueried = i;
        }
      }
      if(unqueried != null) {
        try {
          FindNodeRequest r = createFindNodeRequest(info_hash);
          FindNodeResponse res = (FindNodeResponse) makeRequest(unqueried.ip, unqueried.port, r);
          for (NodeInfo result : res.nodes) {
            if(result != null) {
              node.addNode(result);
              if (! top8.contains(result)) {
                top8.add(result);
              }
            }
          }
        } catch (IOException | InvalidResponseException e) {
          System.out.println("error sending find_node");
        }
        queried.add(unqueried);
      }
      top8.sort(comp);
      while(top8.size() > Node.maxBucketSize) {
        top8.removeLast();
      }
    }

    HashSet<PeerInfo> results = new HashSet<PeerInfo>();
    for (NodeInfo i : top8) {
      try {
        GetPeersRequest req = createGetPeersRequest(info_hash);
        Response res = makeRequest(i.ip, i.port, req);
        if(res instanceof GetPeersPeersResponse) {
          for (PeerInfo pInfo : ((GetPeersPeersResponse)res).values) {
            if (pInfo != null) {
              results.add(pInfo);
            }
          }
        }
        try {
          AnnouncePeerRequest annouceReq = createAnnouncePeerRequest(info_hash);
          makeRequest(i.ip, i.port, annouceReq);
        } catch (IOException | InvalidResponseException e) {
          System.out.println("error sending announce_peer Request");
        }
      } catch (IOException | InvalidResponseException e) {
        System.out.println("error sending getPeer Request");
      }
    }
    System.out.println("dht found " + results.size() + " peers");
    return results;
  }

  public void log(String s) {
    System.out.println(port + ": " + s);
  }
}
