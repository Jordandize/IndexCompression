import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

public class BitOutputStream extends FilterOutputStream {

  private int bits = 0;
  private int n = 0;
  private long totalBits = 0;

  public BitOutputStream(OutputStream out) {
    super(out);
  }

  private void writeSingleBit(int bit) throws IOException {
    bits = (bits << 1) | (bit & 1);
    n++;
    totalBits++;
    if (n == 8) {
      super.write(bits);
      bits = 0;
      n = 0;
    }
  }

  /**
   * Writes the <i>numberOfBits</i> lower bits of <i>bitsToWrite</i> to the
   * output stream, starting with the most significant bit.
   */
  public void writeBits(int bitsToWrite, int numberOfBits) throws IOException {
    for (int i = numberOfBits - 1; i >= 0; i--) {
      int bit = bitsToWrite >> i;
      writeSingleBit(bit);
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    for (int i = 0; i < len; i++)
      writeBits(b[off + i], 8);
  }
  
  static byte[] decodeBinary(String s) {
	    if (s.length() % 8 != 0) throw new IllegalArgumentException(
	        "Binary data length must be multiple of 8");
	    byte[] data = new byte[s.length() / 8];
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        if (c == '1') {
	            data[i >> 3] |= 0x80 >> (i & 0x7);
	        } else if (c != '0') {
	            throw new IllegalArgumentException("Invalid char in binary string");
	        }
	    }
	    return data;
	}

  @Override
  public final void write(int b) throws IOException {
    writeBits(b, 8);
  }

  @Override
  public final void flush() throws IOException {
    writeBits(0, (8 - n) & 0x07);
  }

  /**
   * Returns the number of bits that have been written to this bitstream.
   */
  public long getTotalBits() {
    return totalBits;
  }
  
  public static void main(String[] args) throws FileNotFoundException, IOException {
	  /*BitOutputStream bos;
	try {
		bos = new BitOutputStream(new FileOutputStream(new File("test5.bin")));

	    bos.writeBits(0x00, 2);
	    bos.writeBits(0x01, 2);
	    bos.writeBits(0x02, 2);
	    bos.writeBits(0x02, 2);
	    bos.writeBits(0x03, 2);
	    bos.close();
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}*/
	  String num = "10110101010101100000111111110000";
	  
	  byte[] b = new BigInteger(num, 2).toByteArray();
	  byte[] c = num.getBytes();
	  byte[] s = decodeBinary(num);
	  
	  System.out.println(Arrays.toString(b));
	  System.out.println(Arrays.toString(c));

	  FileOutputStream fos = new FileOutputStream("sfile1.bin");
	  fos.write(b);
	  fos.close();
	  
	  FileOutputStream fos2 = new FileOutputStream("sfile2.bin");
	  fos2.write(c);
	  fos2.close();
	  
	  FileOutputStream fos3 = new FileOutputStream("sfile3.bin");
	  BufferedOutputStream bos = new BufferedOutputStream(fos3);
	  bos.write(s);
	  bos.close();
	  

	  
	  java.nio.file.Files.write(new File("file55.bin").toPath(), s);
  }
  
}