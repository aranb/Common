package com.eyalzo.common.chunks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class ChunkUtils {
	/**
	 * Calculate the similarity between two collections of {@link Chunk}s, when similarity is defined as |a * b| / |a U b|. a * b is the intersection between the two sets,
	 * and |c| is the total length in bytes of all the chunks contains in c. The relation is symmetric (between a and b).
	 * 
	 * @param a first collection of Chunks
	 * @param b second collection of Chunks
	 *  
	 * @author aran
	 *
	 */
	public static double jaccardSimilarity(Collection<Chunk> a, Collection<Chunk> b) {
		if (a == null || a.isEmpty() ||
				b == null || b.isEmpty())
			return 0.0d;
		
		Collection<Chunk> biggerChunkSet;
		Collection<Chunk> smallerChunkSet;
		
		int aSize = a.size();
		int bSize = b.size();
		
		if (aSize > bSize) {
			biggerChunkSet = a;
			smallerChunkSet = b;
		} else {
			smallerChunkSet = b;
			biggerChunkSet = a;
		}
		
		// TODO: skip creating a new HashSet if the chunk list is a HashSet...
		Set<Chunk> biggerSet = new HashSet<Chunk>();
		biggerSet.addAll(biggerChunkSet);
		
		// count intersecting chunks
		int count = 0;
		for (Chunk curElement: smallerChunkSet) {
			if (biggerSet.contains(curElement))
				count+=curElement.getLength();
		}
		
		// calculate total lengths
		int aLength = 0;
		for (Chunk curChunk: a) {
			aLength += curChunk.getLength();
		}
		int bLength = 0;
		for (Chunk curChunk: b) {
			bLength += curChunk.getLength();
		}
		
		return 1.0d - ((double) count / (aLength + bLength - count));
		
	}
	
//	public static double jaccardBestSimilarity(Set<Chunk> a, Set<Chunk> b, int blockSize) {
//		if (a == null || a.isEmpty() ||
//				b == null || b.isEmpty())
//			return 0.0d;
//		
//		Collection<Chunk> biggerChunkSet;
//		Collection<Chunk> smallerChunkSet;
//		
//		
//	}
}

