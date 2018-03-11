import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
//https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
public class Block {
	
	private String hash;
	private String prevHash;
	private double data;
	private String timeStamp;
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");//date formattter
	LocalDateTime currentTime = LocalDateTime.now();
	
	/**
	 * Constructor defines a block with the following attributes
	 * @param data - data to give to block
	 * @param prevHash - reference to the last block
	 */
	public Block(double data, String prevHash) {//block creates a hash but must also have a link to prevHash
		this.data = data;
		this .prevHash = prevHash;
		this.timeStamp = dtf.format(currentTime);
		this.hash = calculatedHash();
	}
	/**
	 * Calculates hash by considering those things in the current
	 * block we do not want tampered. 
	 * @return calculatedHash - by applyingSha256 to the given blocks attributes
	 */
	public String calculatedHash() {
		String calculatedHash = StringUtil.applySha256(	getPrevHash() 
														+ getTimeStamp() 
														+ getData()) ;//getter methods which access the privte blok attributes
		return calculatedHash;
	}
	/**
	 * Prints block in unique way
	 * @param block - the block we are trying to print
	 */
	public  void printBlock() {//might need a counter if printing in a loop (Block b, int blockNumber)
		System.out.println("{" + /*Hash: " + block.hash +*/ "\n\tprevHash: " + 
							prevHash + "\n\thash: " + hash + "\n\tdata: " + data + "º degrees" +  "\n\ttimeStamp: " + timeStamp + 
							"\n}");
	}
	/**
	 * Get class object's hash
	 * @return
	 */
	public String getHash() {
		return this.hash;
	}
	/**
	 * Get class object's previous hash
	 * @return
	 */
	public String getPrevHash() {
		return this.prevHash;
	}
	/**
	 * Get class object's data
	 * @return
	 */
	public double getData() {
		return this.data;
	}
	/**
	 * Get class object's time stamp
	 * @return
	 */
	public String getTimeStamp() {
		return this.timeStamp;
	}
	/**
	 * Overwrites Arduino's value with the weather one if the web source was accurate (within range)
	 * @param data - web source's weather value for that time
	 */
	public void setData(double data) {
		this.data = data;
	}
	
}
	