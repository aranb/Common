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

/**
 * Parse an "Sflow Datagram" in order to get samples containing Ethernet frames (starting as the MAC addresses).
 * <p>
 * The typical use is to initialize with {@link #SflowPacket(byte[], int, int)}, validate with
 * {@link SflowPacket#isLegal} and then call {@link #getEthernetSampleFlowData()}.
 * 
 * @author Eyal Zohar
 */
public class SflowPacket
{
	/**
	 * List of "Flow Sample", although in practice each datagram holds only a single sample right now.
	 */
	private LinkedList<SflowFlowSample>	sampleList			= new LinkedList<SflowFlowSample>();
	/**
	 * True only is the packet was parsed correctly and it contains at least one legal sample.
	 */
	public boolean						isLegal;
	/**
	 * The total sFlow packet length, which is the UDP payload length, including all sFlow headers.
	 */
	public int							packetLength;

	//
	// Header fields
	//
	/**
	 * Field 1 (4 bytes): version of sflow. Only accepted value is {@link #SFLOW_VERSION}.
	 */
	private int							sflowVersion;
	/**
	 * Field 2 (4 bytes): agent IP address type, where 1=IPv4, 2=IPv6.
	 */
	public boolean						agentAddrIpv4;
	/**
	 * Field 3 (4 or 16 bytes): the LSB bytes of the agent IP address.
	 */
	public int							agentAddr;
	/**
	 * Field 4 (4 bytes): very useful and important for this application, as it shows the direction of the traffic, and
	 * in the future it will distinguish between regular traffic and specific traffic like DNS.
	 */
	public int							subAgentId;
	/**
	 * Determined by the caller's configuration and {@link #subAgentId}.
	 */
	public boolean						isDirDownstream;
	/**
	 * Field 5 (4 bytes): sequence number of the datagram. Each agent and sub-agent has its own sequence number. The
	 * application is supposed to get all sequence numbers in a raw, without any gaps. The sequence represents all
	 * samples in the datagram.
	 */
	public int							datagramSeq;
	/**
	 * Field 6 (4 bytes): device uptime in mSec.
	 */
	public int							deviceUptimeMillis;

	//
	// Constants
	//
	/**
	 * The only supported version by this class.
	 */
	private static final int			SFLOW_VERSION		= 5;
	/**
	 * Minimal expected header length, with agent IPv4 and without samples.
	 */
	private static final int			MIN_UDP_PAYLOAD_LEN	= 7 * 4;
	/**
	 * Additional address bytes when reading IPv6 instead of IPv4.
	 */
	private static final int			IPV6_MORE_BYTES		= 16 - 4;
	/**
	 * Agent IP address type IPv4, according to sflow specifications.
	 */
	private static final int			AGENT_IP_TYPE_V4	= 1;
	/**
	 * Agent IP address type IPv6, according to sflow specifications.
	 */
	private static final int			AGENT_IP_TYPE_V6	= 2;

	/**
	 * @param subAgentIdDownstreamOdd
	 *            True if the value of sub-agent-id that marks downstream packets is odd. Otherwise, it is even.
	 */
	public SflowPacket(byte[] packet, int udpPayloadOffset, int udpPayloadLen, boolean subAgentIdDownstreamOdd)
	{
		isLegal = false;

		// Sanity check
		if (packet == null || udpPayloadOffset < 0 || udpPayloadLen < 0)
			return;

		packetLength = udpPayloadLen;

		// Verify that the header can be read safely
		int maxOffset = udpPayloadOffset + udpPayloadLen;
		if (packet.length < maxOffset || udpPayloadLen < MIN_UDP_PAYLOAD_LEN)
			return;

		//
		// Read fields
		//
		int offset = udpPayloadOffset;

		// Field 1 (4 bytes): version of sflow. Only accepted value is {@link #SFLOW_VERSION}.
		sflowVersion = NetUtils.bytes4AsInt(packet, offset);
		if (sflowVersion != SFLOW_VERSION)
			return;

		// Field 2 (4 bytes): agent IP address type, where 1=IPv4, 2=IPv6.
		offset += 4;
		int agentIpType = NetUtils.bytes4AsInt(packet, offset);
		if (agentIpType == AGENT_IP_TYPE_V4)
		{
			agentAddrIpv4 = true;
		} else if (agentIpType == AGENT_IP_TYPE_V6)
		{
			agentAddrIpv4 = false;
			// Verify that the header can be read safely
			if (udpPayloadLen < (MIN_UDP_PAYLOAD_LEN + IPV6_MORE_BYTES))
				return;

			// Skip the extra bytes before reading the address
			offset += IPV6_MORE_BYTES;
		} else
			return;

		// Field 3 (4 or 16 bytes): the LSB bytes of the agent IP address.
		offset += 4;
		agentAddr = NetUtils.bytes4AsInt(packet, offset);

		// Field 4 (4 bytes): very useful and important for this application, as it shows the direction of the traffic,
		// and in the future it will distinguish between regular traffic and specific traffic like DNS.
		offset += 4;
		subAgentId = NetUtils.bytes4AsInt(packet, offset);
		isDirDownstream = (subAgentId % 2) == (subAgentIdDownstreamOdd ? 1 : 0);

		// Field 5 (4 bytes): sequence number of the datagram. Each agent and sub-agent has its own sequence number. The
		// application is supposed to get all sequence numbers in a raw, without any gaps. The sequence represents all
		// samples in the datagram.
		offset += 4;
		datagramSeq = NetUtils.bytes4AsInt(packet, offset);

		// Field 6 (4 bytes): device uptime in mSec.
		offset += 4;
		deviceUptimeMillis = NetUtils.bytes4AsInt(packet, offset);

		// Field 7 (4 bytes): sample count
		offset += 4;
		int sampleCount = NetUtils.bytes4AsInt(packet, offset);
		if (sampleCount <= 0)
			return;

		// Parse all sflow samples
		offset += 4;
		if (!parseSamples(packet, sampleCount, offset, maxOffset))
			return;

		// Mark sflow parsing as successful
		isLegal = true;
	}

	/**
	 * @param packet
	 *            The datagram itself, where the sflow header was read before this call.
	 * @param offset
	 *            Offset where to start reading the first sample.
	 * @param maxOffset
	 *            The offset right after the last byte of the datagram.
	 * @return True only if all the samples were in the right format and the total length matches the samples and their
	 *         lengths.
	 */
	private boolean parseSamples(byte[] packet, int sampleCount, int offset, int maxOffset)
	{
		for (int i = 0; i < sampleCount; i++)
		{
			// Sanity check - need to read at least two integers
			if ((maxOffset - offset) < 8)
				return false;

			int enterpriseAndFormat = NetUtils.bytes4AsInt(packet, offset);
			// Part 1: MSB 20 bits
			int enterprise = (enterpriseAndFormat & 0xfffff000) >>> 12;
			// Part 2: LSB 12 bits
			int format = (enterpriseAndFormat & 0xfff);

			// Field 2: length from right after this field.
			offset += 4;
			int length = NetUtils.bytes4AsInt(packet, offset);

			// Get ready for the data block
			offset += 4;
			if (length < 0 || length > (maxOffset - offset))
				return false;

			// Parse a "Flow Sample", which is only one type of sample, but the only type we care about: enterprise=0
			// and format=1
			if (enterprise == 0 && format == 1)
			{
				// Parse the sample, in order to read a single Ethernet sample that is stored deep inside the structure
				SflowFlowSample sample = SflowFlowSample.parse(packet, offset, length);
				if (sample != null)
				{
					sampleList.add(sample);
				}
			}

			// Skip the block
			offset += length;
		}

		return (offset == maxOffset);
	}

	@Override
	public String toString()
	{
		if (!isLegal)
			return "(illegal)";

		return String.format("Sflow %d-%d: len=%,d seq=%,d samples=%,d", agentAddr, subAgentId, packetLength,
				datagramSeq, sampleList.size());
	}

	/**
	 * @return Number of parsed and saved sflow "Flow Sample".
	 */
	public long getSflowFlowSamplesCount()
	{
		return sampleList.size();
	}

	/**
	 * 
	 * @param index
	 *            0-based index.
	 * @return Sample, or null if index too high.
	 */
	SflowFlowSample getFlowSample(int index)
	{
		synchronized (sampleList)
		{
			if (index >= sampleList.size())
				return null;

			return sampleList.get(index);
		}
	}

	/**
	 * @return The first Ethernet frame found in this sFlow packet, or null if no such record exists.
	 */
	public byte[] getEthernetSampleFlowData()
	{
		SflowFlowSample flowSample = getFlowSample(0);
		if (flowSample == null)
			return null;

		return flowSample.flowData;
	}

	/**
	 * @return The sampling rate (1+) of the first Ethernet frame found in this sFlow packet, or 0 if not even one
	 *         sample is found.
	 */
	public int getEthernetSampleSamplingRate()
	{
		SflowFlowSample flowSample = getFlowSample(0);
		if (flowSample == null)
			return 0;

		return flowSample.samplingRate;
	}
}
