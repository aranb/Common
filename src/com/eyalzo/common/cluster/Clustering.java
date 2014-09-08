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
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Clustering of members, according to each member's values.
 * 
 * @author Eyal Zohar
 */
public class Clustering
{
	/**
	 * 
	 * @param members
	 *            A collection of members to be divided into clusters.
	 * @param membershipThreshold
	 *            A threshold in the range of 0.0 to 1.0. A member belongs to the same cluster only if it has a match
	 *            score of at least this number with the cluster's representative member.
	 * @param minClusterMembers
	 *            Optional. Clusters with less members will be filtered out. Ignored if not positive.
	 * @param minIntersectionSize
	 *            Optional. To use if the compare method requires additional information.
	 * @param membershipThresholdMax
	 *            Optional, relevant only if less-equal 1. This is the upper boundary for a match, non-inclusive. Use
	 *            2.0 if not needed. Useful when need to find specific matches without the stronger matches.
	 * @return All the given items ordered in list of clusters, where each cluster is made of list of items. Each item
	 *         belongs to exactly one cluster.
	 */
	public static <V, M extends Clusterable<V>> LinkedList<LinkedList<M>> clusterByCouplesCompare(
			Collection<M> members, double membershipThreshold, double membershipThresholdMax, int maxClusters,
			int minClusterMembers, int minIntersectionSize)
	{
		LinkedList<LinkedList<M>> result = clusterByCouplesCompare(members, membershipThreshold,
				membershipThresholdMax, maxClusters, minIntersectionSize);
		if (minClusterMembers > 0)
		{
			for (Iterator<LinkedList<M>> it = result.iterator(); it.hasNext();)
			{
				LinkedList<M> list = it.next();
				if (list.size() < minClusterMembers)
					it.remove();
			}
		}
		return result;
	}

	/**
	 * 
	 * @param members
	 *            A collection of members to be divided into clusters.
	 * @param membershipThreshold
	 *            A threshold in the range of 0.0 to 1.0. A member belongs to the same cluster only if it has a match
	 *            score of at least this number with the cluster's representative member.
	 * @param membershipThresholdMax
	 *            Optional, relevant only if less-equal 1. This is the upper boundary for a match, non-inclusive.
	 * @return All the given items ordered in list of clusters, where each cluster is made of list of items. Each item
	 *         belongs to exactly one cluster.
	 */
	private static <V, M extends Clusterable<V>> LinkedList<LinkedList<M>> clusterByCouplesCompare(
			Collection<M> members, double membershipThreshold, double membershipThresholdMax, int maxClusters,
			int minIntersectionSize)
	{
		LinkedList<LinkedList<M>> result = new LinkedList<LinkedList<M>>();

		// For each new item, try to match with existing clusters
		for (M curItem : members)
		{
			// For now, the new item is not matched with any cluster
			boolean matched = false;

			// Compare with existing clusters
			for (LinkedList<M> curCluster : result)
			{
				M repItem = curCluster.getFirst();
				double matchScore = curItem.compareTo(repItem, minIntersectionSize);
				// If it is a good match with the current cluster
				if (matchScore >= membershipThreshold && matchScore < membershipThresholdMax)
				{
					curCluster.add(curItem);
					matched = true;
					break;
				}
			}

			// If not matched, then add a new cluster
			if (!matched)
			{
				// Find a cluster with a single mail
				boolean canAddCluster = false;
				if (result.size() < maxClusters)
				{
					canAddCluster = true;
				} else
				{
					for (Iterator<LinkedList<M>> it = result.iterator(); it.hasNext();)
					{
						LinkedList<M> curCluster = it.next();
						if (curCluster.size() == 1)
						{
							it.remove();
							canAddCluster = true;
							break;
						}
					}
				}

				// Add only if removed a cluster
				if (canAddCluster)
				{
					// Create new cluster
					LinkedList<M> curCluster = new LinkedList<M>();
					// Add the single item to the cluster, as future
					// representative
					curCluster.add(curItem);
					// Add the new cluster itself
					result.add(curCluster);
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param items
	 *            A collection of n items to be compared with the given representative item and maybe added to the
	 *            result.
	 * @param positiveThreshold
	 *            A threshold in the range of 0.0 to 1.0. An item belongs to the same cluster only if it has a match
	 *            score of at least this number with the cluster's representative item.
	 * @return List of items that match the given representative in the boundaries given by the threshold.
	 */
	public static <V, K extends Clusterable<V>> LinkedList<K> clusterOfItem(Collection<K> items,
			double positiveThreshold, K repItem, int minIntersectionSize)
	{
		LinkedList<K> result = new LinkedList<K>();

		// For each new item, try to match with existing clusters
		for (K curItem : items)
		{
			double matchScore = curItem.compareTo(repItem, minIntersectionSize);
			// If it is a good match with the current cluster
			if (matchScore >= positiveThreshold && curItem != repItem)
			{
				result.add(curItem);
			}
		}

		return result;
	}

	/**
	 * @param members
	 *            Full member list, each have value list in {@link Clusterable#getClusteringItems()}.
	 * @param values
	 *            The values to use for clustering.
	 * @param minMembers
	 *            Do not try to split clusters that have this number of members. Can still produce such clusters if the
	 *            opposite cluster is big enough.
	 * @return Cluster list.
	 */
	public static <V, K extends Clusterable<V>> Collection<Cluster<K, V>> clusterByValues(Collection<K> members,
			Collection<V> values, int minMembers)
	{
		LinkedList<Cluster<K, V>> result = new LinkedList<Cluster<K, V>>();
		LinkedList<Cluster<K, V>> temp = new LinkedList<Cluster<K, V>>();

		result.add(new Cluster<K, V>(members));

		// Try to split for each value
		for (V curValue : values)
		{
			// Try to split each existing cluster
			for (Cluster<K, V> curCluster : result)
			{
				// Minimum number of members in order to try to split
				Cluster<K, V> splitedNewCluster = curCluster.split(curValue, minMembers);
				// If a new cluster was created, then add it, as the current was
				// already updated (removed members that moved to the new one)
				if (splitedNewCluster != null)
					temp.add(0, splitedNewCluster);
			}

			// If new clusters were created
			if (!temp.isEmpty())
			{
				result.addAll(temp);
				temp.clear();
			}
		}

		return result;
	}
}
