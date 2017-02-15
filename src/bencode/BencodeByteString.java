package bencode;

import java.io.UnsupportedEncodingException;

public class BencodeByteString extends BencodeElem {
  public byte[] data;

  public String getValue() {
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException exception) {
      exception.printStackTrace();
      System.out.println("unsupported encoding");
    }
    return "";
  }
}
