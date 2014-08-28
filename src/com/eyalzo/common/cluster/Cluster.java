/**
 * Copyright 2013 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.cluster;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.eyalzo.common.misc.MapCounter;
import com.eyalzo.common.misc.MapCounterCouple;

/**
 * List of members of {@link Clusterable} type, each holding a list of values of some type.
 * 
 * @author Eyal Zohar
 * 
 * @param <M>
 *            Members class, each holding a list of values.
 * @param <V>
 *            Values class.
 */
public class Cluster<M extends Clusterable<V>, V>
{
	/**
	 * Member list, each have at least one value in its list.
	 */
	private HashSet<M>	members	= new HashSet<M>();
	/**
	 * Optional values storage, to use when it means something.
	 */
	private HashSet<V>	storedValues;

	public Cluster()
	{
	}

	/**
	 * Top-down initialization: start with all members but nothing in common, for later splits.
	 * 
	 * @param members
	 *            All members, meaning the complete set before any split.
	 */
	public Cluster(Collection<M> members)
	{
		synchronized (this.members)
		{
			this.members.addAll(members);
		}
	}

	public Cluster(Cluster<M, V> otherCluster)
	{
		this(otherCluster.members);
	}

	/**
	 * @return Only values that appear in all members. May be empty, but never null.
	 */
	public Set<V> getValuesCommonToAll()
	{
		synchronized (members)
		{
			HashSet<V> result = new HashSet<V>();
			Iterator<M> it = members.iterator();
			// If the list is empty, there are no common values
			if (!it.hasNext())
				return result;
			// Start with the first value list
			M curMember = it.next();
			Collection<V> curMemberValues = curMember.getClusteringItems();
			result.addAll(curMemberValues);
			// Loop through the other members
			while (it.hasNext())
			{
				if (result.isEmpty())
					break;
				curMember = it.next();
				curMemberValues = curMember.getClusteringItems();
				for (Iterator<V> itResult = result.iterator(); itResult.hasNext();)
				{
					// If the current so-far common value is not in the member's
					// list, then remove from the common list
					if (!curMemberValues.contains(itResult.next()))
						itResult.remove();
				}
			}
			return result;
		}
	}

	/**
	 * @param splitValue
	 *            The value that splits this cluster to two clusters, according to membership in each.
	 * @return New cluster with all the values removed from this cluster.
	 */
	public Cluster<M, V> split(V splitValue, int minMembers)
	{
		synchronized (members)
		{
			if (members == null || members.size() <= 2 * minMembers)
				return null;

			LinkedList<M> membersContains = new LinkedList<M>();
			for (M curMember : members)
			{
				if (curMember.getClusteringItems().contains(splitValue))
					membersContains.add(curMember);
			}

			int containsSize = membersContains.size();
			int othersSize = members.size() - containsSize;
			if (containsSize == 0 || othersSize == 0 || containsSize < minMembers && othersSize < minMembers)
				return null;

			members.removeAll(membersContains);
			return new Cluster<M, V>(membersContains);
		}
	}

	/**
	 * Count number of members that have a given value.
	 * 
	 * @param value
	 *            The search value.
	 * @return How many members have this value.
	 */
	public int getValueMembersCount(V value)
	{
		synchronized (members)
		{
			if (members == null)
				return 0;

			int result = 0;
			for (M curMember : members)
			{
				if (curMember.getClusteringItems().contains(value))
					result++;
			}

			return result;
		}
	}

	/**
	 * Get members that have a given value.
	 * 
	 * @param value
	 *            The search value.
	 * @return Members that have this value. Can be empty but never null.
	 */
	public HashSet<M> getValueMembers(V value)
	{
		HashSet<M> result = new HashSet<M>();

		synchronized (members)
		{
			if (members == null)
				return result;

			for (M curMember : members)
			{
				if (curMember.getClusteringItems().contains(value))
					result.add(curMember);
			}
		}

		return result;
	}

	public HashSet<M> getMembers()
	{
		return members;
	}

	/**
	 * @return Number of members in the cluster.
	 */
	public int getMemberCount()
	{
		synchronized (members)
		{
			return this.members.size();
		}
	}

	public boolean contains(M member)
	{
		synchronized (members)
		{
			return members.contains(member);
		}
	}

	/**
	 * @return All the values and how many members have them.
	 */
	public MapCounter<V> getValuesAndMemberCount()
	{
		MapCounter<V> result = new MapCounter<V>();

		synchronized (members)
		{
			for (M curMember : members)
			{
				result.incAll(curMember.getClusteringItems());
			}
		}

		return result;
	}

	/**
	 * @return All the values and the members that have each of them.
	 */
	public HashMap<V, Cluster<M, V>> getValuesAndMembers()
	{
		HashMap<V, Cluster<M, V>> result = new HashMap<V, Cluster<M, V>>();

		synchronized (members)
		{
			for (M curMember : members)
			{
				Collection<V> curMemberValues = curMember.getClusteringItems();
				for (V curValue : curMemberValues)
				{
					Cluster<M, V> curValueMembers = result.get(curValue);
					// If this value does not have a member-list yet
					if (curValueMembers == null)
					{
						curValueMembers = new Cluster<M, V>();
						result.put(curValue, curValueMembers);
					}
					// Add the current member to the value's member-list
					curValueMembers.add(curMember);
				}
			}
		}

		return result;
	}

	private void add(M curMember)
	{
		synchronized (members)
		{
			members.add(curMember);
		}
	}

	/**
	 * @return All the values and the members that have each of them. Sorted in ascending order of number of members,
	 *         and then by values (as strings).
	 */
	public TreeMap<V, Cluster<M, V>> getValuesAndMembersSortedAsc(int minMembers)
	{
		final HashMap<V, Cluster<M, V>> valuesAndMembers = getValuesAndMembers();

		TreeMap<V, Cluster<M, V>> result = new TreeMap<V, Cluster<M, V>>(new Comparator<V>()
		{
			public int compare(V o1, V o2)
			{
				Cluster<M, V> val1 = valuesAndMembers.get(o1);
				Cluster<M, V> val2 = valuesAndMembers.get(o2);
				int result = ((Integer) (val1.getMemberCount())).compareTo(val2.getMemberCount());
				// Very important - when counters are equal, return order by
				// key, because otherwise items will be considered identical
				if (result == 0)
					result = (o1.toString()).compareTo(o2.toString());
				return result;
			}
		});

		// Add only items with minimal given count
		for (Entry<V, Cluster<M, V>> entry : valuesAndMembers.entrySet())
		{
			Cluster<M, V> members = entry.getValue();
			if (members.getMemberCount() >= minMembers)
				result.put(entry.getKey(), members);
		}

		return result;
	}

	/**
	 * @return All the values and the members that have each of them. Sorted in descending order of number of members,
	 *         and then by values (as strings).
	 */
	public TreeMap<V, Cluster<M, V>> getValuesAndMembersDupSortedDesc(int minMembers)
	{
		final HashMap<V, Cluster<M, V>> valuesAndMembers = getValuesAndMembers();

		TreeMap<V, Cluster<M, V>> result = new TreeMap<V, Cluster<M, V>>(new Comparator<V>()
		{
			public int compare(V o1, V o2)
			{
				Cluster<M, V> val1 = valuesAndMembers.get(o1);
				Cluster<M, V> val2 = valuesAndMembers.get(o2);
				int result = ((Integer) (val1.getMemberCount())).compareTo(val2.getMemberCount());
				// Very important - when counters are equal, return order by
				// key, because otherwise items will be considered identical
				if (result == 0)
					result = (o1.toString()).compareTo(o2.toString());
				return -result;
			}
		});

		// Add only items with minimal given count
		for (Entry<V, Cluster<M, V>> entry : valuesAndMembers.entrySet())
		{
			Cluster<M, V> members = entry.getValue();
			if (members.getMemberCount() >= minMembers)
				result.put(entry.getKey(), members);
		}

		return result;
	}

	/**
	 * @return Gangs of values, which are values that have the same members.
	 */
	public LinkedList<Cluster<M, V>> getValueGangs(int minMembers)
	{
		// All values above the minimum, sorted by number of members
		TreeMap<V, Cluster<M, V>> valuesAndMembersSortedDesc = getValuesAndMembersDupSortedDesc(minMembers);

		LinkedList<Cluster<M, V>> result = new LinkedList<Cluster<M, V>>();

		// Start with the first empty gang
		Cluster<M, V> curCluster = null;

		for (Entry<V, Cluster<M, V>> entry : valuesAndMembersSortedDesc.entrySet())
		{
			V curValue = entry.getKey();
			Cluster<M, V> curValueMembers = entry.getValue();

			// To merge with previous value(s)?
			if (curCluster != null && curCluster.getMemberCount() == curValueMembers.getMemberCount()
					&& curCluster.members.containsAll(curValueMembers.members))
			{
				curCluster.addValue(curValue);
				continue;
			}

			curCluster = new Cluster<M, V>(curValueMembers);
			curCluster.addValue(curValue);
			result.add(curCluster);
		}

		return result;
	}

	/**
	 * Split the cluster to gangs, which are clusters made of several values with similar member-group. The member-group
	 * similarity is determined by the given similarity-factor.
	 * <p>
	 * Unlike {@link #getValueGangs(int)}, the output here is not deterministic, because it depends on the order in
	 * which the values are examined.
	 * 
	 * @param similarityFactor
	 *            Number in the range 0.0-1.0 that determines how similar the member groups need to be. For example, the
	 *            factor 0.75 requires the gangs to share 0.75 of the member list.
	 * @return Gangs of values, which are values that have about the same members.
	 */
	public LinkedList<Cluster<M, V>> getValueGangsSoft(int minMembers, double similarityFactor)
	{
		// All values above the minimum, sorted by number of members
		TreeMap<V, Cluster<M, V>> valuesAndMembersSortedDesc = getValuesAndMembersDupSortedDesc(minMembers);

		LinkedList<Cluster<M, V>> result = new LinkedList<Cluster<M, V>>();

		// Minimal number of members still acceptable for the current cluster
		int clusterMinMembers = 0;

		// Create a duplicate list of values, sorted by number of members
		LinkedList<V> valuesSortedDesc = new LinkedList<V>(valuesAndMembersSortedDesc.keySet());

		// Iterate over the duplicate, so we can use the original for scan-ahead
		for (V repValue : valuesSortedDesc)
		{
			// Remove the current cluster so we won't use it again
			Cluster<M, V> curCluster = valuesAndMembersSortedDesc.remove(repValue);

			// This will happen when encountering a value that already joined a
			// gang in scan-ahead
			if (curCluster == null)
				continue;

			// For now, only the representative value
			curCluster.addValue(repValue);

			// The minimal number of members
			clusterMinMembers = Math.max(1, (int) Math.round(similarityFactor * curCluster.getMemberCount()));

			// Walk through the values with same or less members
			for (Iterator<Entry<V, Cluster<M, V>>> it = valuesAndMembersSortedDesc.entrySet().iterator(); it.hasNext();)
			{
				Entry<V, Cluster<M, V>> joinEntry = it.next();
				Cluster<M, V> joinMembers = joinEntry.getValue();
				// We reached a value that do not have enough members
				// Since values are sorted, there is no need to continue
				if (joinMembers.getMemberCount() < clusterMinMembers)
					break;
				// To merge with previous value(s)?
				if (curCluster.intersectionSizeAtLeast(joinMembers, clusterMinMembers))
				{
					// Remove the members not in the intersection, otherwise it
					// may join several values that are not close enough to each
					// other
					curCluster.removeMembersNotInIntersection(joinMembers);
					// Store the value, for the returned final result
					curCluster.addValue(joinEntry.getKey());
					// Remove because it should not merge again later
					it.remove();
				}
			}

			// Add to result, after it may was merged with other
			result.add(curCluster);
		}

		return result;
	}

	/**
	 * Remove all members that the other cluster do not have.
	 * 
	 * @param o
	 *            The other cluster.
	 */
	private void removeMembersNotInIntersection(Cluster<M, V> o)
	{
		synchronized (members)
		{
			for (Iterator<M> it = members.iterator(); it.hasNext();)
			{
				// If the other one does not have this member
				if (!o.members.contains(it.next()))
					it.remove();
			}
		}
	}

	/**
	 * @return True if the members intersection size is at least in the given size.
	 */
	private boolean intersectionSizeAtLeast(Cluster<M, V> o, int clusterMinMembers)
	{
		synchronized (members)
		{
			int maxAllowedMissing = members.size() - clusterMinMembers;
			for (M curMember : members)
			{
				if (maxAllowedMissing < 0)
					return false;
				// If the other one does not have this member
				if (!o.members.contains(curMember))
					maxAllowedMissing--;
			}
			return maxAllowedMissing >= 0;
		}
	}

	/**
	 * Count for each "unique" value how many other values it can represent, because it appears in the same members'
	 * values.
	 * 
	 * @return List of "unique" values and 1) how many other values (not in the returned list) are represented by each
	 *         returned value and 2) how many members have this value.
	 */
	public MapCounterCouple<V> getValuesRepresentativesStats(int minMembersPerValue, int maxMembersPerValue)
	{
		MapCounterCouple<V> result = new MapCounterCouple<V>();

		// Values and how many members have them, sorted
		MapCounter<V> temp = getValuesAndMemberCount();
		temp.filterMinMax(minMembersPerValue, maxMembersPerValue);
		NavigableMap<V, Long> valuesByMemebers = temp.getSortedByCountDup().descendingMap();

		while (!valuesByMemebers.isEmpty())
		{
			//
			// Use the first (currently most popular) value and remove it
			//
			Entry<V, Long> theEntry = valuesByMemebers.firstEntry();
			V theValue = theEntry.getKey();
			long theMemebersCount = theEntry.getValue();
			// For now it represents one value
			result.add(theValue, 1, theMemebersCount);
			valuesByMemebers.remove(theValue);

			// Walk through the other entries to look for matches
			HashSet<M> theMembers = getValueMembers(theValue);
			for (Iterator<Entry<V, Long>> it = valuesByMemebers.entrySet().iterator(); it.hasNext();)
			{
				Entry<V, Long> curEntry = it.next();
				// Value to compare with
				long curCount = curEntry.getValue();
				// Break here, because the list is sorted so no more values can
				// match
				if (curCount < theMemebersCount)
					break;
				// Continue because we need to compare with each value that has
				// the same count
				V curValue = curEntry.getKey();
				HashSet<M> curMembers = getValueMembers(curValue);
				if (theMembers.containsAll(curMembers))
				{
					it.remove();
					// Now it represents one more value, while members count
					// remains the same
					result.inc1(theValue);
				}
			}
		}

		return result;
	}

	/**
	 * @return List of values that appear in the exact same members' value-list as the given value.
	 */
	public HashSet<V> getValuesWithSameMembers(V theValue)
	{
		HashSet<V> result = new HashSet<V>();

		HashSet<M> theMembers = getValueMembers(theValue);
		// It may happen because of the multi-threading
		if (theMembers.isEmpty())
			return result;

		M firstMember = theMembers.iterator().next();
		Collection<V> firstMemberValues = firstMember.getClusteringItems();

		for (V curValue : firstMemberValues)
		{
			HashSet<M> curMembers = getValueMembers(curValue);
			if (theMembers.size() == curMembers.size() && theMembers.containsAll(curMembers))
				result.add(curValue);
		}

		return result;
	}

	/**
	 * @return List of values that appear in same members' value-list as the given value, and at least one more member.
	 */
	public HashSet<V> getValuesWithMoreMembers(V theValue)
	{
		HashSet<V> result = new HashSet<V>();

		HashSet<M> theMembers = getValueMembers(theValue);
		if (!theMembers.iterator().hasNext())
			return result;

		M firstMember = theMembers.iterator().next();
		Collection<V> firstMemberValues = firstMember.getClusteringItems();

		for (V curValue : firstMemberValues)
		{
			HashSet<M> curMembers = getValueMembers(curValue);
			if (theMembers.size() < curMembers.size() && curMembers.containsAll(theMembers))
				result.add(curValue);
		}

		return result;
	}

	/**
	 * @param value
	 *            The new value to add.
	 * @return False if the value was already in the (optional) value list.
	 */
	public boolean addValue(V value)
	{
		if (storedValues == null)
			storedValues = new HashSet<V>();

		synchronized (storedValues)
		{
			return storedValues.add(value);
		}
	}

	/**
	 * @return Optional stored values. May be null.
	 */
	public HashSet<V> getStoredValues()
	{
		return this.storedValues;
	}

	@Override
	public String toString()
	{
		String result = "";
		synchronized (members)
		{
			for (M curMember : members)
			{
				if (!result.isEmpty())
					result += ",";
				result += curMember.toString();
			}
		}

		if (storedValues != null)
		{
			synchronized (storedValues)
			{
				String values = "";
				for (V curValue : storedValues)
				{
					if (values.isEmpty())
						values += " {";
					else
						values += ",";
					values += curValue.toString();
				}
				if (!values.isEmpty())
					result += values + "}";
			}
		}

		return result;
	}
}
