/**
 * Copyright 2011 Eyal Zohar. All rights reserved.
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

package com.eyalzo.common.misc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import com.eyalzo.common.webgui.DisplayTable;

/**
 * Similar to {@link MapCounter}, but counts separately the count and sum of each entry, and provides a special methods
 * that shows an average column per item.
 * 
 * @author Eyal Zohar
 */
public class MapCounter2<K> extends MapCounter<K>
{
	protected MapCounter<K>	mapCounter2	= new MapCounter<K>();

	@Override
	public synchronized long add(K key, long toAdd)
	{
		mapCounter2.inc(key);
		return super.add(key, toAdd);
	}

	/**
	 * Similar to {@link MapCounter#add(Object, long)}, but adds only to the first internal map-counter.
	 */
	public synchronized long add1(K key, long toAdd)
	{
		return super.add(key, toAdd);
	}

	/**
	 * Similar to {@link MapCounter#add(Object, long)}, but adds only to the second internal map-counter.
	 */
	public synchronized long add2(K key, long toAdd)
	{
		return mapCounter2.add(key, toAdd);
	}

	@Override
	public synchronized void addAll(Collection<K> keys, long toAdd)
	{
		mapCounter2.incAll(keys);
		super.addAll(keys, toAdd);
	}

	@Override
	public synchronized void incAll(Collection<K> keys)
	{
		mapCounter2.incAll(keys);
		super.incAll(keys);
	}

	/**
	 * @param key
	 *            The key.
	 * @return Count of items which is the number of items added, detached from their values. It is identical to
	 *         {@link MapCounter#get(Object)} if only {@link MapCounter#inc(Object)} was called.
	 */
	public synchronized long getCount(K key)
	{
		return mapCounter2.get(key);
	}

	/**
	 * Display table for this map, with optional count, sum and average columns. Default sort column is the first after
	 * the key column, in ascending order.
	 * 
	 * @param keyColName
	 *            Key column name (leftmost).
	 * @param keyTip
	 *            Optional key column tool tip.
	 * @param keyLink
	 *            Optional link for key columns. Use null if links are not required. Null key will never have a link, so
	 *            to have a link the null need to be replaced in the add methods.
	 * @param countColName
	 *            Optional name for "Count" column. Use null if the column should not be displayed. Use empty string for
	 *            default column name.
	 * @param sumColName
	 *            Optional name for "Sum" column. Use null if the column should not be displayed. Use empty string for
	 *            default column name.
	 * @param averageColName
	 *            Optional name for "Average" column. Use null if the column should not be displayed. Use empty string
	 *            for default column name.
	 * @return New display table for this map-counter. Default sort to value column is the value column.
	 */
	public synchronized DisplayTable webGuiTable2(String keyColName, String keyTip, String keyLink,
			String countColName, String sumColName, String averageColName)
	{
		DisplayTable table = new DisplayTable();

		//
		// Columns
		//
		table.addCol(keyColName, keyTip, true);
		if (countColName != null)
		{
			table.addCol(countColName.isEmpty() ? "Count" : countColName, "Number of items in line", false);
		}
		if (sumColName != null)
		{
			table.addCol(sumColName.isEmpty() ? "Sum" : sumColName, "Sum of items values in line", false);
		}
		if (averageColName != null)
		{
			table.addCol(averageColName.isEmpty() ? "Average" : averageColName, "Average of items values in line",
					false);
		}

		// Set default sort to last
		table.setDefaultSortCol(table.getColumnsCount() <= 1 ? 0 : 1);

		Iterator<Entry<K, Long>> it = mapCounter.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<K, Long> entry = it.next();
			K key = entry.getKey();
			long count = mapCounter2.get(key);

			table.addRow("");

			//
			// Key
			//
			if (keyLink != null && !keyLink.isEmpty() && key != null)
			{
				table.addCell(key, keyLink + key.toString());
			} else
			{
				table.addCell(key);
			}

			// Count
			if (countColName != null)
			{
				table.addCell(count);
			}

			// Sum
			if (sumColName != null)
			{
				table.addCell(entry.getValue().longValue());
			}

			// Average
			if (averageColName != null)
			{
				table.addCell(count <= 0 ? null : entry.getValue().longValue() / count);
			}
		}

		return table;
	}
}
