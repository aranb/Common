/**
 * 
 */
package com.eyalzo.common.chunks;

/**
 * Chunk class is a variation on the chunks as represented by a long primitive data type (or its Long wrapper), to create a more OO design. 
 * Can later be replaced by the "long" version due to speed requirements, while keeping the OO design.
 * 
 * @author Aran Bergman
 *
 */
public class Chunk {
	/**
	 * Length of the chunk in bytes.
	 */
	private int length;
	/**
	 * The signature of the chunk.
	 */
	private long signature;
	/**
	 * The offset of this chunk in the original text.
	 */
	private int startOffset;
	
	/**
	 * Create a chunk with the specified parameters.
	 * 
	 * @param length
	 * @param signature
	 * @param startOffset
	 */
	public Chunk(int length, long signature, int startOffset) {
		this.length = length;
		this.signature = signature;
		this.startOffset = startOffset;
	}
	
	/**
	 * @return the  "code" of the chunk - i.e., its length combined with its signature. Can be used to compare chunks (prefer the {@link #equals} method).
	 */
	public long getCode() {
		long result = ((length & PackChunking.CHUNK_LEN_MASK) << PackChunking.CHUNK_SHA1_BITS) | (PackChunking.CHUNK_SHA1_MASK & signature);
		return result;
	}

	/** 
	 * 
	 * @return chunk length in bytes.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * 
	 * @return The offset of the chunk in the original text.
	 */
	public int getStartOffset() {
		return startOffset;
	}

	@Override
	public String toString() {
		return String.format("Chunk [length=$,7d, signature=%012x, startOffset= %,7d]", length, signature, startOffset);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result + (int) (signature ^ (signature >>> 32));
		result = prime * result + startOffset;
		return result;
	}

	/**
	 * Compare two {@link Chunk}s (excluding their offsets)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Chunk other = (Chunk) obj;
		if (length != other.length)
			return false;
		if (signature != other.signature)
			return false;
		return true;
	}
	
	
}
