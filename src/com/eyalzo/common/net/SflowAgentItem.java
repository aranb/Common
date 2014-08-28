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
package com.eyalzo.common.net;

import java.util.LinkedList;

import com.eyalzo.common.webgui.DisplayTable;

/**
 * @author Eyal Zohar
 * 
 */
public class SflowAgentItem
{
	int							agentAddr;
	int							subAgentId;
	boolean						isDirDownstream;
	/**
	 * UDP datagrams that carry sflow.
	 */
	long						statDatagramsCount;
	long						statSflowFlowSamples;
	/**
	 * Total count of bytes inside the sFlow samples, in the Ethernet level.
	 */
	long						statEtherBytesSamples;
	/**
	 * Similar to {@link #statEtherBytesSamples}, but also considers the sampling rate (and ignores dropped bytes).
	 */
	long						statEtherBytesEst;
	long						lastSeen;
	int							lastDatagramSeq;
	/**
	 * Last seen sampling rate, which is the number of packets resembled by a single sample.
	 */
	int							statLastSamplingRate;
	long						deviceUptime;

	//
	// Packets per second
	//
	private long				statPpsPrevTime;
	private long				statPpsDatagramsPrev;
	private long				statPpsEtherBytesPrev;
	private long				statPpsNextTime;
	private static final long	PPS_INTERVAL_MILLIS			= 5 * 1000;
	private static final int	PPS_MEMORY					= 100;
	/**
	 * Datagrams per second history list.
	 */
	private LinkedList<Integer>	ppsListDatagrams			= new LinkedList<Integer>();
	/**
	 * Similar to {@link #ppsListDatagrams} and synchronized by it, but holds traffic Ethernet bytes.
	 */
	private LinkedList<Integer>	ppsListEtherBitsPerSecond	= new LinkedList<Integer>();

	public SflowAgentItem(int agentAddr, int subAgentId, boolean isDirDownstream)
	{
		this.agentAddr = agentAddr;
		this.subAgentId = subAgentId;
		this.isDirDownstream = isDirDownstream;
		// Pps
		statPpsPrevTime = System.currentTimeMillis();
		statPpsNextTime = statPpsPrevTime + PPS_INTERVAL_MILLIS;
	}

	void addPacket(SflowPacket packet)
	{
		statDatagramsCount++;
		statSflowFlowSamples += packet.getSflowFlowSamplesCount();
		long now = System.currentTimeMillis();
		lastSeen = now;
		deviceUptime = packet.deviceUptimeMillis;
		lastDatagramSeq = packet.datagramSeq;

		//
		// Count bytes
		//
		SflowFlowSample firstSample = packet.getFlowSample(0);
		if (firstSample != null && firstSample.flowData != null)
		{
			// Ethernet starts at the saved buffer
			int curEtherBytes = firstSample.flowData.length;
			statEtherBytesSamples += curEtherBytes;
			// Similarly, but this time consider also dropped packets
			long curEtherBytesEst = curEtherBytes * (firstSample.samplingRate > 1 ? firstSample.samplingRate : 1);
			statEtherBytesEst += curEtherBytesEst;
			statLastSamplingRate = firstSample.samplingRate;
		}

		//
		// Packets per second
		//
		if (now >= statPpsNextTime)
		{
			synchronized (ppsListDatagrams)
			{
				// Datagrams
				int curPps = (int) (statDatagramsCount - statPpsDatagramsPrev);
				ppsListDatagrams.add(curPps);
				// Bytes
				long curEtherBytes = statEtherBytesEst - statPpsEtherBytesPrev;
				ppsListEtherBitsPerSecond.add((int) (curEtherBytes * 8 * 1000 / PPS_INTERVAL_MILLIS));
				// Check for overflow
				if (ppsListDatagrams.size() > PPS_MEMORY)
				{
					ppsListDatagrams.removeFirst();
					ppsListEtherBitsPerSecond.removeFirst();
				}
				// Get ready for next time
				statPpsDatagramsPrev = statDatagramsCount;
				statPpsEtherBytesPrev = statEtherBytesEst;
				statPpsNextTime += PPS_INTERVAL_MILLIS;
			}
		}
	}

	@Override
	public String toString()
	{
		return NetUtils.ipAsString(agentAddr) + "-" + subAgentId;
	}

	/**
	 * @return
	 */
	public boolean dirDownstream()
	{
		return this.isDirDownstream;
	}

	/**
	 * @return The number of packets per second on the last complete interval ({@link #PPS_INTERVAL_MILLIS} mSec).
	 */
	public int getPacketsPerSecond()
	{
		synchronized (ppsListDatagrams)
		{
			if (ppsListDatagrams.isEmpty())
				return 0;
			return ppsListDatagrams.getLast();
		}
	}

	/**
	 * @return The number of packets per second on the last complete interval ({@link #PPS_INTERVAL_MILLIS} mSec).
	 */
	public int getTrafficBitsPerSecond()
	{
		synchronized (ppsListDatagrams)
		{
			if (ppsListDatagrams.isEmpty())
				return 0;
			return ppsListEtherBitsPerSecond.getLast();
		}
	}

	public static long getPacketsPerSecondIntervalMillis()
	{
		return PPS_INTERVAL_MILLIS;
	}

	/**
	 * Calculate the key, based on stored agent address and agent-sub-id.
	 * 
	 * @return Key that is used for the agent list reference.
	 */
	public long getKey()
	{
		return (((long) agentAddr) << 32) | subAgentId;
	}

	/**
	 * @return Table with 3 columns that contain the time, packet-per-second per interval {@link #PPS_INTERVAL_MILLIS}
	 *         and traffic bps.
	 */
	public DisplayTable webGuiPacketsPerSecond()
	{
		DisplayTable table = new DisplayTable();

		table.addColTime("Time", null, true, false, true, true);
		table.addCol("Packets per Second", null);
		table.addCol("Traffic (Kbps)", null);

		synchronized (ppsListDatagrams)
		{
			long time = System.currentTimeMillis() - ppsListDatagrams.size() * PPS_INTERVAL_MILLIS;
			int index = 0;

			for (int curPps : ppsListDatagrams)
			{
				table.addRow(null);
				table.addCell(time);
				table.addCell(curPps);
				table.addCell(ppsListEtherBitsPerSecond.get(index) / 1000);

				index++;
				time += PPS_INTERVAL_MILLIS;
			}
		}

		return table;
	}
}
