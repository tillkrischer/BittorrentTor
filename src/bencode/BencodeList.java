package bencode;

import java.util.ArrayList;

public class BencodeList extends BencodeElem {
  public ArrayList<BencodeElem> list;
  
  public BencodeList() {
    list = new ArrayList<BencodeElem>();
  }
}
