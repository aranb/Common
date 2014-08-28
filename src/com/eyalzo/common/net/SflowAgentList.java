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

import java.util.HashMap;
import java.util.LinkedList;

import com.eyalzo.common.webgui.DisplayTable;

/**
 * @author Eyal Zohar
 * 
 */
public class SflowAgentList
{
	// Key is IP and sub-agent-id
	private HashMap<Long, SflowAgentItem>	agentList	= new HashMap<Long, SflowAgentItem>();

	/**
	 * Add packet statistics to the right agent.
	 * 
	 * @param packet
	 *            The received parsed sflow datagram.
	 */
	public SflowAgentItem addPacket(SflowPacket packet)
	{
		// Sanity check
		if (!packet.isLegal)
			return null;

		SflowAgentItem agentItem = null;

		// Calculate the key for this agent-id
		long key = calcKey(packet.agentAddr, packet.subAgentId);
		synchronized (agentList)
		{
			agentItem = agentList.get(key);
			if (agentItem == null)
			{
				agentItem = new SflowAgentItem(packet.agentAddr, packet.subAgentId, packet.isDirDownstream);
				agentList.put(key, agentItem);
			}
			agentItem.addPacket(packet);
		}

		return agentItem;
	}

	/**
	 * Calculate the key, based on given agent address and agent-sub-id.
	 * 
	 * @param agentAdd
	 * @param subAgentId
	 * @return Key that can be used for the agent list reference.
	 */
	private static long calcKey(int agentAdd, int subAgentId)
	{
		return (((long) agentAdd) << 32) | subAgentId;
	}

	public DisplayTable webGuiAgents(String agentLink)
	{
		DisplayTable table = new DisplayTable();

		table.addCol("Agent", "Agent IP address and sub-agent-id", true);
		table.addCol("Dir", "Traffic direction - down means from Internet to clients", true);
		table.addCol("Datagrams", "Number of UDP datagrams carrying sFlow, not necessarily with samples", false);
		table.addCol(
				"Datagrams<br>per second",
				String.format("Measured during the last %,d mSec interval",
						SflowAgentItem.getPacketsPerSecondIntervalMillis()), false);
		table.addColNum(
				"Traffic",
				String.format("Measured during the last %,d mSec interval",
						SflowAgentItem.getPacketsPerSecondIntervalMillis()), false, true, true, null, " Kbps");
		table.addCol("Samples", "Number of parsed and saved sFlow \"Flow Sample\" objects", false);
		table.addColNum("Rate", "Last sample's reported rate", false, true, true, "1:", null);
		table.addCol("Bytes", "Ethernet level bytes in parsed sFlow samples", false);
		table.addCol("Estimated bytes", "Like \"Bytes\", but considrs the sample rate in each sample", false);
		table.addCol("Last seq", "sFlow sequence of the last packet received", false);
		table.addColTime("Last seen", "Last time a legal sFlow packet was received", false, false, true, true);

		synchronized (agentList)
		{
			for (SflowAgentItem curAgent : agentList.values())
			{
				table.addRow(null);

				// Agent
				table.addCell(curAgent.toString(), agentLink + curAgent.getKey());
				// Dir
				table.addCell(curAgent.dirDownstream() ? "Down" : "Up");
				// Datagrams
				table.addCell(curAgent.statDatagramsCount);
				// Datagrams per second
				table.addCell(curAgent.getPacketsPerSecond());
				// Traffic
				table.addCell(curAgent.getTrafficBitsPerSecond() / 1000);
				// Samples
				table.addCell(curAgent.statSflowFlowSamples);
				// rate
				table.addCell(curAgent.statLastSamplingRate);
				// Bytes
				table.addCell(curAgent.statEtherBytesSamples);
				// Estimated bytes
				table.addCell(curAgent.statEtherBytesEst);
				// Last seq
				table.addCell(curAgent.lastDatagramSeq);
				// Last seen
				table.addCell(curAgent.lastSeen);
			}
		}

		return table;
	}

	/**
	 * @return Number of agents by address and sub-agent-id.
	 */
	public int size()
	{
		synchronized (agentList)
		{
			return agentList.size();
		}
	}

	/**
	 * @param agentId
	 *            Number that represents the IP and sub-agent-id of the desired agent.
	 * @return Null if not found.
	 */
	public SflowAgentItem getAgent(long agentId)
	{
		synchronized (agentList)
		{
			return agentList.get(agentId);
		}
	}

	/**
	 * @param agentId
	 *            Number that represents the IP and sub-agent-id of the desired agent.
	 * @return Null if not found or found an agent with direction upstream.
	 */
	public SflowAgentItem getAgentDirDown(long agentId)
	{
		synchronized (agentList)
		{
			SflowAgentItem agent = agentList.get(agentId);
			return (agent == null || !agent.isDirDownstream) ? null : agent;
		}
	}

	public LinkedList<SflowAgentItem> getAgentsDup()
	{
		synchronized (agentList)
		{
			return new LinkedList<SflowAgentItem>(agentList.values());
		}
	}
}
