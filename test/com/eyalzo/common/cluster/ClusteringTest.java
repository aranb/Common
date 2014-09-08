package com.eyalzo.common.cluster;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

public class ClusteringTest
{
	private class Member implements Clusterable<Integer>
	{
		private LinkedList<Integer>	values	= new LinkedList<Integer>();

		public Member(int[] values)
		{
			for (int curValue : values)
				this.values.add(curValue);
		}

		@Override
		public <K extends Clusterable<?>> double compareTo(K o, int minIntersectionSize) throws ClassCastException
		{
			return 0;
		}

		@Override
		public Collection<Integer> getClusteringItems()
		{
			return values;
		}
	}

	@Test
	public void testClusterByValues()
	{
		int[][] values = { { 1, 2, 3, 4 }, { 3, 4, 5, 6 }, { 4, 5, 6, 7 } };
		LinkedList<Member> members = new LinkedList<ClusteringTest.Member>();
		for (int[] curValues : values)
		{
			members.add(new Member(curValues));
		}

		Collection<Cluster<Member, Integer>> clusters = Clustering.clusterByValues(members, Arrays.asList(1), 0);
		assertEquals(2, clusters.size());

		clusters = Clustering.clusterByValues(members, Arrays.asList(8), 0);
		assertEquals(1, clusters.size());

		clusters = Clustering.clusterByValues(members, Arrays.asList(4), 0);
		assertEquals(1, clusters.size());

		clusters = Clustering.clusterByValues(members, Arrays.asList(1, 2, 3, 4), 0);
		assertEquals(3, clusters.size());

		clusters = Clustering.clusterByValues(members, Arrays.asList(3, 4), 0);
		assertEquals(2, clusters.size());

		clusters = Clustering.clusterByValues(members, Arrays.asList(3, 5), 0);
		assertEquals(3, clusters.size());

		//
		// Test min-members parameter
		//
		clusters = Clustering.clusterByValues(members, Arrays.asList(3, 5), 1);
		assertEquals(3, clusters.size());

		// Now we expect only two clusters, because it does not split clusters
		// with two members
		clusters = Clustering.clusterByValues(members, Arrays.asList(3, 5), 2);
		assertEquals(2, clusters.size());
		clusters = Clustering.clusterByValues(members, Arrays.asList(1, 2, 3, 4), 2);
		assertEquals(2, clusters.size());
	}
}
