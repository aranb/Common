package com.eyalzo.common.chunks;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class ChunkUtilsTest {
	private Set<Chunk> set1;
	private ArrayList<Chunk> set2;

	@Before
	public void setUp() throws Exception {
		set1 = new HashSet<Chunk>();
		set2 = new ArrayList<Chunk>();
		
		set1.add(new Chunk(10, 1, 0));
		set1.add(new Chunk(11, 2, 0));
		set1.add(new Chunk(3, 3, 0));
		set1.add(new Chunk(3, 4, 0));
		set1.add(new Chunk(3, 5, 0));
		set1.add(new Chunk(4, 5, 0));
		set1.add(new Chunk(10, 5, 0));
		
		set2.add(new Chunk(10, 1, 0));
		set2.add(new Chunk(11, 2, 0));
		set2.add(new Chunk(3, 3, 0));
		set2.add(new Chunk(3, 4, 0));
		set2.add(new Chunk(3, 5, 0));
		set2.add(new Chunk(4, 5, 0));
		set2.add(new Chunk(10, 5, 0));
	}

	@Test
	public void testJaccardSimilarity1() {
		assertEquals(1.0d, ChunkUtils.jaccardSimilarity(set1, set2), 1e-10d);
	}

	@Test
	public void testJaccardSimilarity2() {
		set2.remove(1);
		assertEquals(0.75d, ChunkUtils.jaccardSimilarity(set1, set2), 1e-10d);
	}
}
