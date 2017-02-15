package bencode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BencodeParser {

  int index;
  InputStream in;
  
  public BencodeParser(InputStream in) {
    this.in = in;
    index = 0;
  }
  
  public BencodeElem readElement() throws IOException {
    char character = nextChar();
    if (Character.isDigit(character)) {
      return readString(character);
    } else if (character == 'i') { 
      return readIntegerWithoutI();
    } else if (character == 'l') {
      return readListWithoutL();
    } else if (character == 'd') {
      return readDictionaryWithoutD();
    } else if (character == 'e') {
      return new BencodeEnd();
    } else {
      throw new IOException();
    }
  }

  public BencodeByteString readString() throws IOException {
    char character = nextChar();
    if (! Character.isDigit(character)) {
      throw new IOException();
    }
    return readString(character);
  }

  public BencodeByteString readString(char firstDigit) throws IOException  {
    BencodeByteString bytestring = new BencodeByteString();
    bytestring.start = index - 1;
    StringBuilder sb = new StringBuilder();
    sb.append(firstDigit);
    char character = nextChar();
    while (Character.isDigit(character)) {
      sb.append(character);
      character = nextChar();
    }
    if (character != ':') {
      throw new IOException();
    }
    int length = Integer.parseInt(sb.toString());
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) {
      int next = in.read();
      index++;
      if (next == -1) {
        throw new IOException();
      }
      data[i] = (byte)next;
    }
    bytestring.data = data;
    bytestring.end = index;
    return bytestring;
  }

  public BencodeInteger readInteger() throws IOException {
    char character = nextChar();
    if (character != 'i') {
      throw new IOException();
    }
    return readIntegerWithoutI();
  }
  
  public BencodeInteger readIntegerWithoutI() throws IOException {
    BencodeInteger bint = new BencodeInteger();
    bint.start = index - 1;
    StringBuilder sb = new StringBuilder();
    char character = nextChar();
    while (character != 'e') {
      sb.append(character);
      character = nextChar();
    }
    bint.value = Integer.parseInt(sb.toString());
    bint.end = index;
    return bint;
  }
  
  public BencodeList readList() throws IOException {
    char character = nextChar();
    if (character != 'l') {
      throw new IOException();
    }
    return readListWithoutL();
  }
  
  public BencodeList readListWithoutL() throws IOException {
    BencodeList blist = new BencodeList();
    blist.start = index - 1;
    BencodeElem elem;
    elem = readElement();
    while (!(elem instanceof BencodeEnd)) {
      blist.list.add(elem);
      elem = readElement();
    }
    blist.end = index;
    return blist;
  }
  
  public BencodeDictionary readDictionary() throws IOException {
    char character = nextChar();
    if (character != 'd') {
      throw new IOException();
    }
    return readDictionaryWithoutD();
  }
  
  public BencodeDictionary readDictionaryWithoutD() throws IOException {
    BencodeDictionary bdict = new BencodeDictionary();
    bdict.start = index - 1;
    BencodeElem e = readElement();
    while (! (e instanceof BencodeEnd)) {
      if (e instanceof BencodeByteString) {
        BencodeByteString key = (BencodeByteString) e;
        BencodeElem value = readElement();
        if (value instanceof BencodeEnd) {
          throw new IOException();
        }
        bdict.dict.put(key.getValue(), value);
        e = readElement();
      } else {
        throw new IOException();
      }
    }
    bdict.end = index;
    return bdict;
  }
  
  public char nextChar() throws IOException {
    int c = in.read();
    index++;
    if (c == -1) {
      throw new EOFException();
    }
    return (char) c;
  }
}
