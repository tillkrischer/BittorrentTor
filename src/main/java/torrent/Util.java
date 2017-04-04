package torrent;

public class Util {
  public static long bigEndianToInt(byte[] data) {
    long value = 0;
    for (int i = 0; i < data.length; i++) {
      value += (data[i] & 0x000000ff) << (data.length - i - 1) * 8;
    }
    return value;
  }
  
  public static byte[] intToBigEndian(long value, int bytes) {
    byte[] data = new byte[bytes];
    for (int i = 0; i < bytes; i++) {
      data[bytes - i - 1] = (byte) (value & 0x000000ff);
      value >>= 8;
    }
    return data;
  }
  
  public static String byteArrayToHex(byte[] a) {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }
}
