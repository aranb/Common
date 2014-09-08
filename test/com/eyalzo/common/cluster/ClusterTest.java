package com.eyalzo.common.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import com.eyalzo.common.misc.MapCounterCouple;

public class ClusterTest
{
	private int[][]						values1	= { { 1, 2, 3, 4 }, { 3, 4, 5, 6 }, { 4, 5, 6, 7 } };
	private Cluster<Member, Integer>	cluster1;
	private Cluster<Member, Integer>	cluster2;
	private int[][]						values3	= { { 1, 2 }, { 1, 3 } };
	private Cluster<Member, Integer>	cluster3;

	private class Member implements Clusterable<Integer>
	{
		private LinkedList<Integer>	values	= new LinkedList<Integer>();
		private int					serial;

		public Member(int serial, Collection<Integer> values)
		{
			this.serial = serial;
			for (int curValue : values)
				this.values.add(curValue);
		}

		public Member(int serial, int[] values)
		{
			this.serial = serial;
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

		@Override
		public String toString()
		{
			return "m" + serial;
		}
	}

	@Before
	public void setUp() throws Exception
	{
		//
		// Cluster 1
		//
		int serial = 1;
		LinkedList<Member> members = new LinkedList<Member>();
		for (int[] curValues : values1)
		{
			members.add(new Member(serial++, curValues));
		}
		// New cluster with all the members (and values)
		cluster1 = new Cluster<Member, Integer>(members);

		//
		// Cluster 2
		//
		int maxValues = 10;
		members = new LinkedList<Member>();
		for (int i = 1; i <= maxValues; i++)
		{
			LinkedList<Integer> memberValues = new LinkedList<Integer>();
			for (int j = 1; j <= i; j++)
				memberValues.add(j);
			members.add(new Member(i, memberValues));
		}
		cluster2 = new Cluster<Member, Integer>(members);

		//
		// Cluster 3
		//
		serial = 1;
		members.clear();
		for (int[] curValues : values3)
		{
			members.add(new Member(serial++, curValues));
		}
		// New cluster with all the members (and values)
		cluster3 = new Cluster<Member, Integer>(members);
	}

	@Test
	public void testGetValueGangs()
	{
		LinkedList<Cluster<Member, Integer>> gangs;

		gangs = getValueGangs(cluster1, 1);
		assertEquals(5, gangs.size());
		// Minimum 2, meaning that [7] and [1,2] are out
		gangs = getValueGangs(cluster1, 2);
		assertEquals(3, gangs.size());
		// Minimum 3, meaning that only [4] is in
		gangs = getValueGangs(cluster1, 3);
		assertEquals(1, gangs.size());
		// Minimum 4, meaning that nobody is in
		gangs = getValueGangs(cluster1, 4);
		assertEquals(0, gangs.size());

		//
		// Cluster 2
		//
		gangs = getValueGangs(cluster2, 1);
		assertEquals(10, gangs.size());
		gangs = getValueGangsSoft(cluster2, 1, 0.9);
		assertEquals(7, gangs.size());
	}

	public LinkedList<Cluster<Member, Integer>> getValueGangs(Cluster<Member, Integer> cluster, int minMembers)
	{
		// Merge similar values, meaning those with the same members
		LinkedList<Cluster<Member, Integer>> gangs = cluster.getValueGangs(minMembers);

		System.out.println("\ntestGetValueGangs(" + minMembers + ")\nValues\tMembers");
		// We expect 1+2 and 5+6 to merge
		for (Cluster<Member, Integer> curCluster : gangs)
		{
			System.out.print(curCluster.getStoredValues());
			System.out.print("\t");
			System.out.println(curCluster.getMembers());
		}

		return gangs;
	}

	@Test
	public void testGetValueGangsSoft3()
	{
		LinkedList<Cluster<Member, Integer>> gangs;

		gangs = getValueGangsSoft(cluster3, 1, 0.5);
		assertEquals(2, gangs.size());
		gangs = getValueGangsSoft(cluster3, 1, 0.25);
		assertEquals(2, gangs.size());
		gangs = getValueGangsSoft(cluster3, 1, 0.75);
		assertEquals(3, gangs.size());
	}

	@Test
	public void testGetValueGangsSoft()
	{
		LinkedList<Cluster<Member, Integer>> gangs;

		double similarityFactor = 1.0;

		//
		// Full match, like in testGetValueGangs()
		//

		// No limits
		gangs = getValueGangsSoft(cluster1, 1, similarityFactor);
		assertEquals(5, gangs.size());
		// Minimum 2, meaning that [7] and [1,2] are out
		gangs = getValueGangsSoft(cluster1, 2, similarityFactor);
		assertEquals(3, gangs.size());
		// Minimum 3, meaning that only [4] is in
		gangs = getValueGangsSoft(cluster1, 3, similarityFactor);
		assertEquals(1, gangs.size());
		// Minimum 4, meaning that nobody is in
		gangs = getValueGangsSoft(cluster1, 4, similarityFactor);
		assertEquals(0, gangs.size());

		//
		// Allow freedom of 1/3
		//
		similarityFactor = 2.0 / 3;

		// No limits, so [3,4] merges with [5,6]
		gangs = getValueGangsSoft(cluster1, 1, similarityFactor);
		assertEquals(3, gangs.size());
		// Minimum 2, meaning that [7] and [1,2] are out
		gangs = getValueGangsSoft(cluster1, 2, similarityFactor);
		assertEquals(2, gangs.size());
		// Minimum 3, meaning that only [4] is in
		gangs = getValueGangsSoft(cluster1, 3, similarityFactor);
		assertEquals(1, gangs.size());
		// Minimum 4, meaning that nobody is in
		gangs = getValueGangsSoft(cluster1, 4, similarityFactor);
		assertEquals(0, gangs.size());

		//
		// Allow freedom of 2/3
		//
		similarityFactor = 1.0 / 3;

		// No limits, so everybody merge
		gangs = getValueGangsSoft(cluster1, 1, similarityFactor);
		assertEquals(3, gangs.size());
		assertEquals(1, gangs.getFirst().getMemberCount());
	}

	public LinkedList<Cluster<Member, Integer>> getValueGangsSoft(Cluster<Member, Integer> cluster, int minMembers,
			double similarityFactor)
	{
		// Merge similar values, meaning those with the same members
		LinkedList<Cluster<Member, Integer>> gangs = cluster.getValueGangsSoft(minMembers, similarityFactor);

		System.out.println(String.format("\ntestGetValueGangsSoft(%,d,%.2f)\nValues\tMembers", minMembers,
				similarityFactor));
		// We expect 1+2 and 5+6 to merge
		for (Cluster<Member, Integer> curCluster : gangs)
		{
			System.out.print(curCluster.getStoredValues());
			System.out.print("\t");
			System.out.println(curCluster.getMembers());
		}

		return gangs;
	}

	@Test
	public void testGetValuesCommonToAll()
	{
		Set<Integer> commonValues = cluster1.getValuesCommonToAll();
		assertFalse(commonValues.contains(1));
		assertFalse(commonValues.contains(2));
		assertFalse(commonValues.contains(3));
		assertTrue(commonValues.contains(4));
		assertFalse(commonValues.contains(5));
		assertFalse(commonValues.contains(6));
		assertFalse(commonValues.contains(7));
	}

	@Test
	public void testGetValuesAndMembersSorted()
	{
		//
		// Test size after filtering by min-members
		//
		assertEquals(7, cluster1.getValuesAndMembersSortedAsc(0).size());
		assertEquals(7, cluster1.getValuesAndMembersSortedAsc(1).size());
		assertEquals(4, cluster1.getValuesAndMembersSortedAsc(2).size());
		assertEquals(1, cluster1.getValuesAndMembersSortedAsc(3).size());
		assertEquals(0, cluster1.getValuesAndMembersSortedAsc(4).size());

		//
		// Test number of members after count and sort
		//
		TreeMap<Integer, Cluster<Member, Integer>> map = cluster1.getValuesAndMembersSortedAsc(0);
		assertEquals(1, map.get(1).getMemberCount());
		assertEquals(1, map.get(2).getMemberCount());
		assertEquals(2, map.get(3).getMemberCount());
		assertEquals(3, map.get(4).getMemberCount());
		assertEquals(2, map.get(5).getMemberCount());
		assertEquals(2, map.get(6).getMemberCount());
		assertEquals(1, map.get(7).getMemberCount());

		//
		// Test ascending order
		//
		map = cluster1.getValuesAndMembersSortedAsc(0);
		Iterator<Integer> it = map.keySet().iterator();
		assertEquals(1, it.next().intValue());
		assertEquals(2, it.next().intValue());
		assertEquals(7, it.next().intValue());
		assertEquals(3, it.next().intValue());
		assertEquals(5, it.next().intValue());
		assertEquals(6, it.next().intValue());
		assertEquals(4, it.next().intValue());

		//
		// Test ascending order
		//
		map = cluster1.getValuesAndMembersSortedAsc(0);
		it = map.keySet().iterator();
		assertEquals(1, it.next().intValue());
		assertEquals(2, it.next().intValue());
		assertEquals(7, it.next().intValue());
		assertEquals(3, it.next().intValue());
		assertEquals(5, it.next().intValue());
		assertEquals(6, it.next().intValue());
		assertEquals(4, it.next().intValue());

		//
		// Test ascending order
		//
		map = cluster1.getValuesAndMembersDupSortedDesc(0);
		it = map.keySet().iterator();
		assertEquals(4, it.next().intValue());
		assertEquals(6, it.next().intValue());
		assertEquals(5, it.next().intValue());
		assertEquals(3, it.next().intValue());
		assertEquals(7, it.next().intValue());
		assertEquals(2, it.next().intValue());
		assertEquals(1, it.next().intValue());
	}

	@Test
	public void testGetValuesWithSameMembers()
	{
		assertEquals(2, cluster1.getValuesWithSameMembers(1).size());
		assertEquals(2, cluster1.getValuesWithSameMembers(2).size());
		assertEquals(1, cluster1.getValuesWithSameMembers(3).size());
		assertEquals(1, cluster1.getValuesWithSameMembers(4).size());
		assertEquals(2, cluster1.getValuesWithSameMembers(5).size());
		assertEquals(2, cluster1.getValuesWithSameMembers(6).size());
		assertEquals(1, cluster1.getValuesWithSameMembers(7).size());

		// 1 by 3,4
		assertEquals(2, cluster1.getValuesWithMoreMembers(1).size());
		// 2 by 3,4
		assertEquals(2, cluster1.getValuesWithMoreMembers(2).size());
		assertEquals(1, cluster1.getValuesWithMoreMembers(3).size());
		assertEquals(0, cluster1.getValuesWithMoreMembers(4).size());
		// 5 by 4
		assertEquals(1, cluster1.getValuesWithMoreMembers(5).size());
		// 6 by 4
		assertEquals(1, cluster1.getValuesWithMoreMembers(6).size());
		// 7 by 4,5,6
		assertEquals(3, cluster1.getValuesWithMoreMembers(7).size());
	}

	@Test
	public void testGetValuesRepresentativesStats()
	{
		MapCounterCouple<Integer> result = cluster1.getValuesRepresentativesStats(0, cluster1.getMemberCount());

		System.out.println("\ntestGetValuesRepresentativesStats()");
		System.out.println("Value\tCover\tMembers");
		System.out.println(result);

		assertEquals(5, result.size());
		// Highest number of represented members per value
		assertEquals(2, result.getMaxValue1());
		// Highest number of members per value
		assertEquals(3, result.getMaxValue2());
		assertEquals(7, result.getSum1());
		assertEquals(9, result.getSum2());
	}
}
