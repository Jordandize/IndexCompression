import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public class IndexCompressor {
	
	public static final int DEFAULT_BLOCK_SIZE = 4;
	public static final int POSTING_START_SIZE = 174752;
	public static final String OUTPUT_FILE 	   = "CompressedIndex";

	private File source;
	private int boxsize;
	
	private StringBuilder boxstore;
	private int[] 		  pointers;
	
	private int boxAmount;
	
	private ArrayList<String> posting;
	
	public IndexCompressor(File index) {
		this(index, DEFAULT_BLOCK_SIZE);
	}
	
	public IndexCompressor(File index, int length) {
		if(length < 2) throw new IllegalArgumentException("ERROR: length < 2");
		
		source   = index;
		boxsize  = length;
		pointers = new int[POSTING_START_SIZE / DEFAULT_BLOCK_SIZE];
		boxstore = new StringBuilder();
		posting  = new ArrayList<>();
		construction();
	}
	
	private void construction() {
		compression();
		saveIndex();
	}
	
	private void compression() {
		try {
			FileReader 	   in = new FileReader(source);
			BufferedReader re = new BufferedReader(in);
			
			int controlsize = 0;
			String[] box    = new String[boxsize];
			
			String[] toks = {};
			String   line = "";
			while((line = re.readLine()) != null) {
				toks = line.split(" ");
				
				compressPosting(toks);

				box[controlsize] = toks[0];
				controlsize++;
				if(controlsize == boxsize) {
					compressBox(box, controlsize);
					boxAmount++;
					controlsize = 0;
					//box 	    = new String[boxsize];
				}
			}
			compressBox(box, controlsize);
			
			in.close();
			re.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *	Compress one posting list.
	 *	Example:
	 *	---> { "box", "92", "2244" }
	 *	---> "110111000001000011101000"
	*/
	private void compressPosting(String[] tokens) {
		StringBuilder compressed = new StringBuilder();
		
		int prev = 0;
		int curr = Integer.parseInt(tokens[1]);
		
		compressed.append(VBEncode(curr - prev));
		for(int i = 2; i < tokens.length; i++) {
			prev = curr;
			curr = Integer.parseInt(tokens[i]);
			compressed.append(VBEncode(curr - prev));
		}
		
		posting.add(compressed.toString());
	}
	
	/*
	 *	Add to every block 0 (not end) or 1 (end of number).
	 *	Example:
	 *	---> 16256
	 *	---> { "01111111", "10000000" }
	*/
	private String VBEncode(int num) {
		StringBuilder varcode = new StringBuilder();
		String[] bytes = bytesBlock(Integer.toBinaryString(num));
		
		for(int i = 0; i < bytes.length - 1; i++)
			varcode.append('0').append(bytes[i]);
		varcode.append('1').append(bytes[bytes.length - 1]);
		
		return varcode.toString();
	}
	
	/*
	 *	Split binary string on blocks with 7 bits.
	 *	Example:
	 *	---> "11111110000000"
	 *	---> { "1111111", "0000000" }
	*/
	private String[] bytesBlock(String binary) {
		String[] bytes = new String[binary.length() / 8 + 1];
		int length = binary.length();
		int shift  = (length % 7 == 0) ? 7 : length % 7;
		
		StringBuilder first = new StringBuilder();
		for(int i = 0; i < 7 - shift; i++)
			first.append('0');
		bytes[0] = first.append(binary.substring(0, shift)).toString();
		
		for(int i = 1; i < bytes.length; i++)
			bytes[i] = binary.substring(shift + (i - 1) * 7, shift + i * 7);
		
		return bytes;
	}
	
	/*
	 *	Compress one block of terms.
	 *	Example:
	 *	---> { "automata", "automate", "automatic", "automation" }
	 *	---> 8automat*a1◊e2◊ic3◊ion
	 *	Than put it on a free dictionary position.
	*/
	private void compressBox(String[] box, int size) {
		StringBuilder compressed = new StringBuilder();
		String prefix = commonPrefix(box, size);
		
		int plength = prefix.length();
		
		compressed.append(box[0].length())
				  .append(prefix)
				  .append('*')
				  .append(box[0].substring(plength));
		
		String suffix = "";
		for(int i = 1; i < size; i++) {
			suffix = box[i].substring(plength);
			compressed.append(suffix.length())
					  .append('◊')
					  .append(suffix);
		}
		
		if(boxAmount == pointers.length)
			pointers = Arrays.copyOf(pointers, pointers.length * 3 / 2 + 1);
		
		pointers[boxAmount] = boxstore.length();
		boxstore.append(compressed.toString());
	}
	
	/*
	 *	Return common prefix of terms in block.
	 *	Example:
	 *	---> { "automata", "automate", "automatic", "automation" }
	 *	---> automat
	*/
	private String commonPrefix(String[] box, int size) {
		StringBuilder prefix = new StringBuilder();

		char symbol;
		int len = smallest(box, size); 
		for(int i = 0; i < len; i++) {
			symbol = box[0].charAt(i);
			
			for(int j = 1; j < size; j++) {
				if(symbol != box[j].charAt(i))
					return prefix.toString();
			}
			
			prefix.append(symbol);
		}
		
		return prefix.toString();
	}
	
	/*
	 *	Return smallest length of terms in block.
	 *	Example:
	 *	---> { "automata", "automate", "automatic", "automation" }
	 *	---> 8
	*/
	private int smallest(String[] box, int size) {
		int min = box[0].length();
		
		for(int i = 1; i < size; i++)
			if(box[i].length() < min)
				min = box[i].length();
		
		return min;
	}
	
	private void saveIndex() {
		try {
			//Dictionary
			FileWriter dstream = new FileWriter(OUTPUT_FILE + "Dictionary.txt");
			BufferedWriter dwriter = new BufferedWriter(dstream);
			
			dwriter.write(boxstore.toString());
			StringBuilder build = new StringBuilder();
			for(int i = 0; i < boxAmount; i++) {
				build.append("\n").append(pointers[i]);
			}
			dwriter.write(build.toString());
			dwriter.close();
			dstream.close();
			
			//Posting
			FileOutputStream fos = new FileOutputStream(OUTPUT_FILE + "Posting.bin");
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			build.setLength(0);
			for(String line: posting) {
				bos.write(decodeBinary(line));
				bos.write("\n".getBytes(Charset.forName("UTF-8")));
			}
			
			bos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public File getSource() {
		return source;
	}
	
	public int getBoxsize() {
		return boxsize;
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		IndexCompressor ic = new IndexCompressor(new File("Index.txt"));
		System.out.println("OK");
	}

}
