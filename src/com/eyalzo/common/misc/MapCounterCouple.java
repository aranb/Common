/**
 * Copyright 2011 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.misc;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

import com.eyalzo.common.webgui.DisplayTable;

/**
 * Counter of items per key. Use it to count instances in list or map.
 * 
 * @author Eyal Zohar
 */
public class MapCounterCouple<K>
{
	protected Map<K, Couple<Long>>	mapCounter	= Collections.synchronizedMap(new HashMap<K, Couple<Long>>());

	/**
	 * Add to counter of this key.
	 * 
	 * @param key
	 *            Key.
	 * @param toAdd
	 *            How much to add to this key's counter.
	 * @return Updated count, after add.
	 */
	public synchronized Couple<Long> add(K key, long toAdd1, long toAdd2)
	{
		Couple<Long> couple = mapCounter.get(key);

		//
		// If key is new put for the first time
		//
		if (couple == null)
		{
			couple = new Couple<Long>(toAdd1, toAdd2);
			mapCounter.put(key, couple);
			return couple;
		}

		//
		// Key already exists, to add to current
		//
		couple.value1 += toAdd1;
		couple.value2 += toAdd2;
		return couple;
	}

	/**
	 * Add to counter of these keys.
	 * 
	 * @param keys
	 *            Collection of keys to be handled with iterator.
	 * @param toAdd
	 *            How much to add to keys' counter.
	 */
	public synchronized void addAll(Collection<K> keys, long toAdd1, long toAdd2)
	{
		Iterator<K> it = keys.iterator();
		while (it.hasNext())
		{
			K key = it.next();
			this.add(key, toAdd1, toAdd2);
		}
	}

	/**
	 * Add counters from another map-counter.
	 * 
	 * @param other
	 *            Another map-counter. map-counter, so a local copy better be
	 *            given and not a live object.
	 */
	public synchronized void addAll(MapCounterCouple<K> other)
	{
		Iterator<Entry<K, Couple<Long>>> it = other.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Couple<Long>> entry = it.next();
			Couple<Long> couple = entry.getValue();
			this.add(entry.getKey(), couple.value1, couple.value2);
		}
	}

	/**
	 * Add 1 to counter1 of this key.
	 * 
	 * @param key
	 *            Key.
	 * @return Updated count, after add.
	 */
	public synchronized Couple<Long> inc1(K key)
	{
		return this.add(key, 1L, 0);
	}

	/**
	 * Add 1 to counter2 of this key.
	 * 
	 * @param key
	 *            Key.
	 * @return Updated count, after add.
	 */
	public synchronized Couple<Long> inc2(K key)
	{
		return this.add(key, 0, 1L);
	}

	/**
	 * Sub 1 from counter of this key.
	 * 
	 * @param key
	 *            Key.
	 * @return Updated count, after sub.
	 */
	public synchronized Couple<Long> dec(K key)
	{
		return this.add(key, -1L, -1L);
	}

	/**
	 * Add 1 to counter of these keys.
	 * 
	 * @param keys
	 *            Collection of keys to be handled with iterator.
	 */
	public synchronized void incAll(Collection<K> keys)
	{
		this.addAll(keys, 1L, 1L);
	}

	/**
	 * @return The internal map with counter for every key. Must be synchronized
	 *         for safe access.
	 */
	public synchronized Map<K, Couple<Long>> getMap()
	{
		return mapCounter;
	}

	/**
	 * @return A duplicate of the internal map with counter for every key. No
	 *         need to synchronize.
	 */
	public synchronized Map<K, Couple<Long>> getMapDup()
	{
		return new HashMap<K, Couple<Long>>(mapCounter);
	}

	/**
	 * @return Entry set, for iterator over the original internal map.
	 */
	public synchronized Set<Entry<K, Couple<Long>>> entrySet()
	{
		return mapCounter.entrySet();
	}

	/**
	 * @return Key set, for iterator over the original internal map.
	 */
	public synchronized Set<K> keySet()
	{
		return mapCounter.keySet();
	}

	/**
	 * Gets the numeric value stored for this key, or zero if not found.
	 * 
	 * @param key
	 *            The given key.
	 * @return The couple stored for this key, or null if not found.
	 */
	public synchronized Couple<Long> get(K key)
	{
		Couple<Long> value = mapCounter.get(key);
		if (value == null)
			return null;
		return value;
	}

	/**
	 * @return Sum of all counts.
	 */
	public synchronized long getSum1()
	{
		long sum = 0;

		for (Couple<Long> val : mapCounter.values())
		{
			sum += val.value1;
		}

		return sum;
	}

	/**
	 * @return Sum of all counts.
	 */
	public synchronized long getSum2()
	{
		long sum = 0;

		for (Couple<Long> val : mapCounter.values())
		{
			sum += val.value2;
		}

		return sum;
	}

	/**
	 * @return Average count per key, by sum divided by number of keys.
	 */
	public synchronized float getAverage1()
	{
		if (mapCounter.isEmpty())
			return 0;

		return (float) (((double) (this.getSum1())) / mapCounter.size());
	}

	/**
	 * @return Average of all value1/value2.
	 */
	public synchronized double getAverageWeighted()
	{
		if (mapCounter.isEmpty())
			return 0;

		double sum = 0;
		int count = 0;

		for (Couple<Long> curCouple : mapCounter.values())
		{
			if (curCouple.value2 <= 0)
				continue;

			count++;
			sum += ((double) curCouple.value1) / curCouple.value2;
		}

		return sum / count;
	}

	/**
	 * @return Average count per key, by sum divided by number of keys.
	 */
	public synchronized float getAverage2()
	{
		if (mapCounter.isEmpty())
			return 0;

		return (float) (((double) (this.getSum1())) / mapCounter.size());
	}

	/**
	 * @return Number of keys in this map.
	 */
	public int size()
	{
		return mapCounter.size();
	}

	@Override
	public synchronized String toString()
	{
		return toString("\n", "\t");
	}

	/**
	 * @param keySeparator
	 *            If not null, keys will be returned, and each line (except for
	 *            the last) will end with this string.
	 * @param valueSeparator
	 *            If not null, values will be returned, and separated from keys
	 *            (or other values, if keys are not returned) with this string.
	 */
	public synchronized String toString(String keySeparator, String valueSeparator)
	{
		return toString(this.mapCounter, keySeparator, valueSeparator, 0, 0);
	}

	/**
	 * @param map
	 *            The map to use: can be the internal map or a sorted one.
	 * @param keySeparator
	 *            If not null, keys will be returned, and each line (except for
	 *            the last) will end with this string.
	 * @param valueSeparator
	 *            If not null, values will be returned, and separated from keys
	 *            (or other values, if keys are not returned) with this string.
	 */
	private synchronized String toString(Map<K, Couple<Long>> map, String keySeparator, String valueSeparator,
			long sum1, long sum2)
	{
		StringBuffer buffer = new StringBuffer(1000);
		boolean first = true;

		Iterator<Entry<K, Couple<Long>>> it = map.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Couple<Long>> entry = it.next();

			if (keySeparator != null)
			{
				if (first)
				{
					first = false;
				} else
				{
					buffer.append(keySeparator);
				}
				buffer.append(entry.getKey() == null ? "(null)" : entry.getKey().toString());
			}

			// Values
			if (valueSeparator != null)
			{
				buffer.append(valueSeparator);

				// Value1 itself
				Couple<Long> couple = entry.getValue();
				buffer.append(String.format("%,d", couple.value1));

				if (sum1 > 0)
				{
					buffer.append(valueSeparator);
					// Percents
					buffer.append(String.format("%.3f%%", (double) couple.value1 / sum1));
				}

				buffer.append(valueSeparator);

				// Value2 itself
				buffer.append(String.format("%,d", couple.value2));

				if (sum2 > 0)
				{
					buffer.append(valueSeparator);
					// Percents
					buffer.append(String.format("%.3f%%", (double) couple.value2 / sum2));
				}
			}
		}

		return buffer.toString();
	}

	/**
	 * Clear all items and their counters.
	 */
	public synchronized void clear()
	{
		mapCounter.clear();
	}

	/**
	 * @param minValue
	 *            Keys with value below this will be removed.
	 */
	public synchronized void cleanupMin(long minValue1, long minValue2)
	{
		Iterator<Couple<Long>> it = mapCounter.values().iterator();
		while (it.hasNext())
		{
			Couple<Long> couple = it.next();
			if (couple.value1 < minValue1 || couple.value2 < minValue2)
			{
				it.remove();
			}
		}
	}

	public synchronized boolean isEmpty()
	{
		return mapCounter.isEmpty();
	}

	/**
	 * @return The highest value1, or {@Link Long#MIN_VALUE} if empty.
	 */
	public synchronized long getMaxValue1()
	{
		long max = Long.MIN_VALUE;

		for (Couple<Long> curCouple : mapCounter.values())
		{
			if (curCouple.value1 > max)
			{
				max = curCouple.value1;
			}
		}

		return max;
	}

	/**
	 * @return The highest value2, or {@Link Long#MIN_VALUE} if empty.
	 */
	public synchronized long getMaxValue2()
	{
		long max = Long.MIN_VALUE;

		for (Couple<Long> curCouple : mapCounter.values())
		{
			if (curCouple.value2 > max)
			{
				max = curCouple.value2;
			}
		}

		return max;
	}

	/**
	 * @return The key with the highest value attached to it, or null if empty.
	 */
	public synchronized K getMaxKey1()
	{
		K result = null;
		long max = Long.MIN_VALUE;

		Iterator<Entry<K, Couple<Long>>> it = mapCounter.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Couple<Long>> entry = it.next();
			Couple<Long> couple = entry.getValue();

			if (couple.value1 > max)
			{
				result = entry.getKey();
				max = couple.value1;
			}
		}

		return result;
	}

	/**
	 * @return The key with the highest value attached to it, or null if empty.
	 */
	public synchronized K getMaxKey2()
	{
		K result = null;
		long max = Long.MIN_VALUE;

		Iterator<Entry<K, Couple<Long>>> it = mapCounter.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Couple<Long>> entry = it.next();
			Couple<Long> couple = entry.getValue();

			if (couple.value2 > max)
			{
				result = entry.getKey();
				max = couple.value2;
			}
		}

		return result;
	}

	/**
	 * @param keyToClear
	 *            Key to remove from the map.
	 */
	public void clear(K keyToClear)
	{
		synchronized (mapCounter)
		{
			mapCounter.remove(keyToClear);
		}
	}

	public boolean containsKey(K searchKey)
	{
		return this.mapCounter.containsKey(searchKey);
	}

	/**
	 * Default table for this map.
	 * 
	 * @return New display table for this map-counter. Default sort to counter
	 *         column is the first value column.
	 */
	public synchronized DisplayTable webGuiTable(String keyName, String keyTip, String keyLink, String valueName1,
			String valueTip1, String valueName2, String valueTip2)
	{
		DisplayTable table = new DisplayTable();

		//
		// Columns
		//
		table.addCol(keyName, keyTip, true);
		table.addCol(valueName1, valueTip1, false);
		// Set default sort to counter column
		table.setDefaultSortCol(1);
		table.addCol(valueName2, valueTip2, false);

		Iterator<Entry<K, Couple<Long>>> it = mapCounter.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Couple<Long>> entry = it.next();
			K key = entry.getKey();
			Couple<Long> couple = entry.getValue();

			table.addRow("");

			// Key
			if (keyLink != null && !keyLink.isEmpty() && key != null)
			{
				table.addCell(key, keyLink + key.toString());
			} else
			{
				table.addCell(key);
			}
			// Value1
			table.addCell(couple.value1);
			// Value2
			table.addCell(couple.value2);
		}

		return table;
	}

	public void filterMinMax(long minCount1, long minCount2, long maxCount1, long maxCount2)
	{
		synchronized (mapCounter)
		{
			for (Iterator<Couple<Long>> it = mapCounter.values().iterator(); it.hasNext();)
			{
				Couple<Long> couple = it.next();
				if (couple.value1 < minCount1 || couple.value2 < minCount2 || couple.value1 > maxCount1
						|| couple.value2 > maxCount2)
					it.remove();
			}
		}
	}

	public void filterMin(long minCount1, long minCount2)
	{
		synchronized (mapCounter)
		{
			for (Iterator<Couple<Long>> it = mapCounter.values().iterator(); it.hasNext();)
			{
				Couple<Long> couple = it.next();
				if (couple.value1 < minCount1 || couple.value2 < minCount2)
					it.remove();
			}
		}
	}

	/**
	 * Use with care!!! the sorted map does not follow the equals() rules, and
	 * in case different keys return the same toString() the keys may merge.
	 * 
	 * @return New map, sorted by the counters, in ascending order. In case of
	 *         equality, order is random. Note: can be easily scanned in reverse
	 *         order by using {@link TreeMap#descendingMap()}.
	 */
	public synchronized TreeMap<K, Long> getSortedByCountDup1(long minCount)
	{
		TreeMap<K, Long> result = new TreeMap<K, Long>(new Comparator<K>()
		{
			public int compare(K o1, K o2)
			{
				Long val1 = mapCounter.get(o1).value1;
				Long val2 = mapCounter.get(o2).value1;
				int result = val1.compareTo(val2);
				// Very important - when counters are equal, return order by
				// key, because otherwise items will be
				// considered identical
				if (result == 0)
					result = (o1.toString()).compareTo(o2.toString());
				return result;
			}
		});

		// Add only items with minimal given count
		for (Entry<K, Couple<Long>> entry : mapCounter.entrySet())
		{
			long value = entry.getValue().value1;
			if (value >= minCount)
				result.put(entry.getKey(), value);
		}

		return result;
	}

	/**
	 * Use with care!!! the sorted map does not follow the equals() rules, and
	 * in case different keys return the same toString() the keys may merge.
	 * 
	 * @return New map, sorted by the counters, in ascending order. In case of
	 *         equality, order is random. Note: can be easily scanned in reverse
	 *         order by using {@link TreeMap#descendingMap()}.
	 */
	public synchronized TreeMap<K, Long> getSortedByCountDup2(long minCount)
	{
		TreeMap<K, Long> result = new TreeMap<K, Long>(new Comparator<K>()
		{
			public int compare(K o1, K o2)
			{
				Long val1 = mapCounter.get(o1).value2;
				Long val2 = mapCounter.get(o2).value2;
				int result = val1.compareTo(val2);
				// Very important - when counters are equal, return order by
				// key, because otherwise items will be
				// considered identical
				if (result == 0)
					result = (o1.toString()).compareTo(o2.toString());
				return result;
			}
		});

		// Add only items with minimal given count
		for (Entry<K, Couple<Long>> entry : mapCounter.entrySet())
		{
			long value = entry.getValue().value2;
			if (value >= minCount)
				result.put(entry.getKey(), value);
		}

		return result;
	}
}
