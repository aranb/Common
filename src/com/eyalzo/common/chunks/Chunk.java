/**
 * 
 */
package com.eyalzo.common.chunks;

/**
 * @author Aran Bergman
 *
 */
public class Chunk {
	private int length;
	private long signature;
	private int startOffset;
	
	public Chunk(int length, long signature, int startOffset) {
		this.length = length;
		this.signature = signature;
		this.startOffset = startOffset;
	}
	
	public long getCode() {
		long result = ((length & PackChunking.CHUNK_LEN_MASK) << PackChunking.CHUNK_SHA1_BITS) | (PackChunking.CHUNK_SHA1_MASK & signature);
		return result;
	}

	public int getLength() {
		return length;
	}

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
