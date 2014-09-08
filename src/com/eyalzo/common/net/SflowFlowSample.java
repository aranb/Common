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

import java.util.Arrays;

/**
 * Sflow sample type "Flow Sample", that is expected to host a single "Flow Record" that hosts a single "Flow Data".
 * When created, the object is expected to hold a single Ethernet frame in {@link #flowData}.
 * <p>
 * Flow samples format: http://www.sflow.org/developers/diagrams/sFlowV5Sample.pdf<br>
 * Records format: http://www.sflow.org/developers/diagrams/sFlowV5FlowData.pdf<br>
 * 
 * @author Eyal Zohar
 */
public class SflowFlowSample
{
	//
	// Fields
	//
	public int					samplingRate;
	public int					samplePool;
	public int					drops;
	public int					inputIfIndex;
	public int					outputIfIndex;
	/**
	 * The single "Flow Data" that is expected to be found in each "Flow Sample". Practically, it can be an Ethernet
	 * frame only.
	 */
	public byte[]				flowData;

	//
	// Constants
	//
	/**
	 * Minimal expected header length, without flows.
	 */
	private static final int	MIN_HEADER_LEN				= 8 * 4;
	/**
	 * Related to "Flow Data" header, field 1.
	 */
	private static final int	HEADER_PROTOCOL_ETHERNET	= 1;

	/**
	 * Parse "Flow Sample".
	 * 
	 * @param packet
	 *            The byte array of the entire UDP datagram that carries the sflow.
	 * @param startOffset
	 *            Offset where the sample starts, meaning the byte after the sample length field.
	 * @param length
	 *            Length of the sample, which is the number of bytes to read from the offset until the next sample's
	 *            header.
	 * @return New object if the sample was parsed and added to the sample list. Null if something went wrong, like
	 *         length is too short, no records or any of the fields holds an illegal value.
	 */
	public static SflowFlowSample parse(byte[] packet, int startOffset, int length)
	{
		// Sanity check
		if (length < MIN_HEADER_LEN)
			return null;

		int offset = startOffset;

		SflowFlowSample sample = new SflowFlowSample();

		// Field 1 (4): sample sequence number
		offset += 4;

		// Field 2 (4): source id (two parts)
		offset += 4;

		// Field 3 (4): sampling rate
		sample.samplingRate = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Field 4 (4): sample pool
		sample.samplePool = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Field 5 (4): drops
		sample.drops = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Field 6 (4): input ifIndex
		sample.inputIfIndex = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Field 7 (4): output ifIndex
		sample.outputIfIndex = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Field 8 (4): flow records count
		int flowRecordsCount = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;

		// Not interested in samples that do not have even one record or multiple records
		// TODO change in the future if more records will be saved in a single sample
		if (flowRecordsCount != 1)
			return null;

		// Parse the single record and save its data in this instance
		if (!parseRecord(sample, packet, offset, length - (offset - startOffset)))
			return null;

		return sample;
	}

	/**
	 * Parse "Flow Record" and then "Flow Data", and save the captured data (actually an Ethernet frame) in the
	 * "Flow Sample".
	 * 
	 * @param flowSample
	 *            The target "Flow Sample", where the "Flow Data" will be saved on successful exit.
	 * @param packet
	 *            The byte array of the entire UDP datagram that carries the sflow.
	 * @param startOffset
	 *            Offset where the record starts, meaning the byte after the "number of records" field.
	 * @param length
	 *            Bytes left in datagram.
	 * @return True if record was parsed in saved in this instance.
	 */
	private static boolean parseRecord(SflowFlowSample flowSample, byte[] packet, int startOffset, int length)
	{
		// Sanity check
		if (length < 8)
			return false;

		int offset = startOffset;

		// Field 1 (4): two parts - 20 bits enterprise and 12 bits format
		int enterpriseAndFormat = NetUtils.bytes4AsInt(packet, offset);
		// Part 1: MSB 20 bits
		int enterprise = (enterpriseAndFormat & 0xfffff000) >>> 12;
		// Part 2: LSB 12 bits
		int format = (enterpriseAndFormat & 0xfff);
		// Accept only normal records
		if (enterprise != 0 || format != 1)
			return false;

		offset += 4;

		// Field 2 (4): flow data length
		int flowDataLength = NetUtils.bytes4AsInt(packet, offset);
		offset += 4;
		// Verify that length matches the remaining bytes count
		if (flowDataLength != (length - (offset - startOffset)))
			return false;

		//
		// Parsing "Flow Data"
		//

		// Field 1 (4): Header protocol (Ethernet, IPv4, IPv6, etc...)
		int headerProtocol = NetUtils.bytes4AsInt(packet, offset);
		if (headerProtocol != HEADER_PROTOCOL_ETHERNET)
			return false;

		// Field 2 (4): Frame length.
		offset += 4;
		int frameLengthBeforeStripped = NetUtils.bytes4AsInt(packet, offset);
		if (frameLengthBeforeStripped < 0)
			return false;

		// Field 3 (4): Stripped bytes.
		offset += 4;
		int strippedBytes = NetUtils.bytes4AsInt(packet, offset);
		if (strippedBytes > 0)
			return false;

		// Field 4 (4): Actual data length.
		offset += 4;
		int actualDataLength = NetUtils.bytes4AsInt(packet, offset);

		// Check length
		offset += 4;
		// It can also be 2 bytes smaller, due to padding
		if (actualDataLength > length - (offset - startOffset))
			return false;

		// Copy the flow data, which is actually an Ethernet frame
		flowSample.flowData = Arrays.copyOfRange(packet, offset, offset + actualDataLength);

		return true;
	}
}
