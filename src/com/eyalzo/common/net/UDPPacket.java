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

public class UDPPacket extends IPPacket
{

	/**
	 * The byte offset of the place where UDP header starts in the raw Ethernet/IP packet.
	 * */
	private int					offsetUdp;

	//
	// UDP header structure offsets.
	//
	/**
	 * Field 1 (2): source port.
	 */
	private static final int	UDP_OFFSET_SOURCE_PORT		= 0;
	/**
	 * Field 2 (2): destination port.
	 */
	private static final int	UDP_OFFSET_DESTINATION_PORT	= 2;
	/**
	 * Field 3 (4): total length (minimum is 8). This field is practically redundant.
	 */
	@SuppressWarnings("unused")
	private static final int	UDP_OFFSET_HEADER_LENGTH	= 4;
	/**
	 * Field 4 (2): checksum.
	 */
	private static final int	UDP_OFFSET_CHECKSUM			= 6;

	//
	// Constants
	//
	private static final int	UDP_MIN_HEADER_LEN			= 8;

	@Override
	public void setData(byte[] data)
	{
		super.setData(data);
		offsetUdp = getIPHeaderOffset() + getIPHeaderLength();
	}

	/**
	 * Sets the source port.
	 * 
	 * @param port
	 *            The new source port.
	 */
	public final void setSourcePort(int port)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetUdp + UDP_OFFSET_SOURCE_PORT, port);
	}

	/**
	 * Sets the destination port.
	 * 
	 * @param port
	 *            The new destination port.
	 */
	public final void setDestinationPort(int port)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetUdp + UDP_OFFSET_DESTINATION_PORT, port);
	}

	/**
	 * @return The source port.
	 */
	public final int getSourcePort()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetUdp + UDP_OFFSET_SOURCE_PORT);
	}

	/**
	 * @return The destination port.
	 */
	public final int getDestinationPort()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetUdp + UDP_OFFSET_DESTINATION_PORT);
	}

	@Override
	public void setIPHeaderLength(int length)
	{
		super.setIPHeaderLength(length);
		offsetUdp = getIPHeaderOffset() + getIPHeaderLength();
	}

	/**
	 * @return Always 8, as the length field in UDP is redundant and the header is always at the same size.
	 */
	public final int getUDPHeaderLength()
	{
		return 8;
	}

	/**
	 * @return The UDP checksum.
	 */
	public final int getUDPChecksum()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetUdp + UDP_OFFSET_CHECKSUM);
	}

	/**
	 * @return The sum of all headers lengths in bytes, meaning Ethernet (if included), IP and UDP.
	 * @see #getEthHeaderByteLength()
	 * @see #getIPAndUDPHeaderByteLength()
	 */
	public final int getCombinedHeaderByteLength()
	{
		return getEthHeaderByteLength() + getIPHeaderLength() + getUDPHeaderLength();
	}

	/**
	 * @return Data (payload) length in bytes.
	 * @see #getUDPPacketLength()
	 */
	public final int getUDPPayloadLength()
	{
		return getUDPPacketLength() - getUDPHeaderLength();
	}

	private final int __getVirtualHeaderTotal()
	{
		int s1 = ((buffer[getIPHeaderOffset() + IP_OFFSET_SOURCE_ADDRESS] & 0xff) << 8)
				| (buffer[getIPHeaderOffset() + IP_OFFSET_SOURCE_ADDRESS + 1] & 0xff);
		int s2 = ((buffer[getIPHeaderOffset() + IP_OFFSET_SOURCE_ADDRESS + 2] & 0xff) << 8)
				| (buffer[getIPHeaderOffset() + IP_OFFSET_SOURCE_ADDRESS + 3] & 0xff);
		int d1 = ((buffer[getIPHeaderOffset() + IP_OFFSET_DESTINATION_ADDRESS] & 0xff) << 8)
				| (buffer[getIPHeaderOffset() + IP_OFFSET_DESTINATION_ADDRESS + 1] & 0xff);
		int d2 = ((buffer[getIPHeaderOffset() + IP_OFFSET_DESTINATION_ADDRESS + 2] & 0xff) << 8)
				| (buffer[getIPHeaderOffset() + IP_OFFSET_DESTINATION_ADDRESS + 3] & 0xff);
		return s1 + s2 + d1 + d2 + getProtocol() + getUDPPacketLength();
	}

	/**
	 * Computes the UDP checksum, optionally updating the UDP checksum header.
	 * 
	 * @param update
	 *            Specifies whether or not to update the UDP checksum header after computing the checksum. A value of
	 *            true indicates the header should be updated, a value of false indicates it should not be updated.
	 * @return The computed UDP checksum.
	 */
	public final int computeUDPChecksum(boolean update)
	{
		return computeChecksum(offsetUdp, offsetUdp + UDP_OFFSET_CHECKSUM, getIPHeaderOffset() + getIPPacketLength(),
				__getVirtualHeaderTotal(), update);
	}

	@Override
	public String toString()
	{
		return String.format("%,d-%,d len=%,d header=%,d", getSourcePort(), getDestinationPort(), getUDPPacketLength(),
				getUDPHeaderLength());
	}

	/**
	 * Create a UDP packet with Ethernet frame and IP packet to wrap an existing raw frame buffer.
	 * 
	 * @param buffer
	 *            Raw frame to wrap.
	 * @param offsetEth
	 *            Offset of Ethernet header in the given buffer.
	 * @param offsetUdp
	 *            Offset of the UDP packet in the given buffer.
	 */
	public UDPPacket(byte[] buffer, int offsetEth)
	{
		// Prepare the Ethernet and IP packet fields
		super(buffer, offsetEth);
		if (!isValid)
			return;

		isValid = false;

		// Calculate UDP offset, based on Ethernet and IP headers
		this.offsetUdp = offsetIp + getIPHeaderLength();

		// Sanity check
		if (buffer == null || offsetUdp < 0 || (buffer.length - offsetUdp) < UDP_MIN_HEADER_LEN)
			return;

		// Check if Ethernet fields indicates that it carries an IP packet
		if (getProtocol() != IP_PROTOCOL_UDP)
			return;

		isValid = true;
	}

	/**
	 * @return The UDP packet length in bytes, including the UDP header. This is the size of the IP packet minus the
	 *         size of the IP header.
	 * @see #getUDPPayloadLength()
	 */
	public final int getUDPPacketLength()
	{
		return getIPPacketLength() - getIPHeaderLength();
		// Can also be retrieved this way (almost always works):
		// return NetUtils.bytes2AsUnsignedInt(buffer, offsetUdp + UDP_OFFSET_HEADER_LENGTH);
	}

	/**
	 * @param ethPacket
	 *            Ethernet packet that (maybe) contains an IP packet.
	 * @return New IP packet object, or null if too small or corrupted.
	 */
	public UDPPacket(IPPacket ipPacket)
	{
		this(ipPacket.buffer, ipPacket.offsetEth);
	}
}
