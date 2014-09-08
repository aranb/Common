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

public class TCPPacket extends IPPacket
{

	/**
	 * The byte offset of the place where TCP header starts in the raw Ethernet/IP packet.
	 * */
	private int					offsetTcp;

	//
	// TCP header structure offsets.
	//
	/**
	 * Field 1 (2): source port.
	 */
	private static final int	TCP_OFFSET_SOURCE_PORT		= 0;
	/**
	 * Field 2 (2): destination port.
	 */
	private static final int	TCP_OFFSET_DESTINATION_PORT	= 2;
	/**
	 * Field 3 (4): sequence number.
	 */
	private static final int	TCP_OFFSET_SEQUENCE			= 4;
	/**
	 * Field 4 (4): ack number.
	 */
	private static final int	TCP_OFFSET_ACK				= 8;
	/**
	 * Field 5 (1): (4 left bits) Specifies the size of the TCP header in 32-bit words. The minimum size header is 5
	 * words and the maximum is 15 words thus giving the minimum size of 20 bytes and maximum of 60 bytes, allowing for
	 * up to 40 bytes of options in the header. This field gets its name from the fact that it is also the offset from
	 * the start of the TCP segment to the actual data.
	 */
	private static final int	TCP_OFFSET_DATA_OFFSET		= 12;
	/**
	 * Field 6 (1): control flags.
	 */
	private static final int	TCP_OFFSET_CONTROL			= 13;
	/**
	 * Field 7 (2): window size.
	 */
	private static final int	TCP_OFFSET_WINDOW_SIZE		= 14;
	/**
	 * Field 8 (2): checksum.
	 */
	private static final int	TCP_OFFSET_CHECKSUM			= 16;
	/** Offset into the TCP packet of the URG pointer. */
	@SuppressWarnings("unused")
	private static final int	TCP_OFFSET_URG_POINTER		= 18;

	//
	// Constants
	//
	private static final int	TCP_MIN_HEADER_LEN			= 20;

	//
	// Flags from the control field
	//
	/** A mask for extracting the FIN bit from the control header. */
	public static final int		MASK_FIN					= 0x01;
	/** A mask for extracting the SYN bit from the control header. */
	public static final int		MASK_SYN					= 0x02;
	/** A mask for extracting the reset bit from the control header. */
	public static final int		MASK_RST					= 0x04;
	/** A mask for extracting the push bit from the control header. */
	public static final int		MASK_PSH					= 0x08;
	/** A mask for extracting the ACK bit from the control header. */
	public static final int		MASK_ACK					= 0x10;
	/** A mask for extracting the urgent bit from the control header. */
	public static final int		MASK_URG					= 0x20;

	//
	// Options
	//
	/** A byte value for TCP options indicating end of option list. */
	public static final byte	KIND_EOL					= 0;
	/** A byte value for TCP options indicating no operation. */
	public static final byte	KIND_NOP					= 1;
	/**
	 * A byte value for TCP options identifying a selective acknowledgment option.
	 */
	public static final byte	KIND_SACK					= 4;

	/**
	 * @param mask
	 *            The bit mask to check.
	 * @return True only if all of the bits in the mask are set.
	 */
	public boolean isSet(int mask)
	{
		return ((buffer[offsetTcp + TCP_OFFSET_CONTROL] & mask) == mask);
	}

	/**
	 * @param mask
	 *            The bit mask to check.
	 * @return True if any of the bits in the mask are set.
	 */
	public boolean isSetAny(int mask)
	{
		return ((buffer[offsetTcp + TCP_OFFSET_CONTROL] & mask) != 0);
	}

	/**
	 * @param mask
	 *            The bit mask to check.
	 * @return True only if all of the bits in the mask are set and ONLY the bits in the mask are set.
	 */
	public boolean isSetOnly(int mask)
	{
		int flags = buffer[offsetTcp + TCP_OFFSET_CONTROL] & 0xff;
		return ((flags & mask) == flags);
	}

	/**
	 * Sets the specified control bits without altering any other bits in the control header.
	 * 
	 * @param mask
	 *            The bits to set.
	 */
	public void addControlFlags(int mask)
	{
		int flags = buffer[offsetTcp + TCP_OFFSET_CONTROL] & 0xff;
		flags |= mask;
		buffer[offsetTcp + TCP_OFFSET_CONTROL] = (byte) (flags & 0xff);
	}

	/**
	 * Clears the specified control bits.
	 * 
	 * @param mask
	 *            The bits to unset.
	 */
	public void removeControlFlags(int mask)
	{
		int flags = buffer[offsetTcp + TCP_OFFSET_CONTROL] & 0xff;
		flags |= mask;
		flags ^= mask;
		buffer[offsetTcp + TCP_OFFSET_CONTROL] = (byte) (flags & 0xff);
	}

	/**
	 * Sets the control header to the specified value.
	 * 
	 * @param mask
	 *            The new control header bit mask.
	 */
	public void setControlFlags(int mask)
	{
		buffer[offsetTcp + TCP_OFFSET_CONTROL] = (byte) (mask & 0xff);
	}

	@Override
	public void setData(byte[] data)
	{
		super.setData(data);
		offsetTcp = getIPHeaderOffset() + getIPHeaderLength();
	}

	/**
	 * Sets the source port.
	 * 
	 * @param port
	 *            The new source port.
	 */
	public final void setSourcePort(int port)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetTcp + TCP_OFFSET_SOURCE_PORT, port);
	}

	/**
	 * Sets the destination port.
	 * 
	 * @param port
	 *            The new destination port.
	 */
	public final void setDestinationPort(int port)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetTcp + TCP_OFFSET_DESTINATION_PORT, port);
	}

	/**
	 * @return The source port.
	 */
	public final int getSourcePort()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetTcp + TCP_OFFSET_SOURCE_PORT);
	}

	/**
	 * @return The destination port.
	 */
	public final int getDestinationPort()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetTcp + TCP_OFFSET_DESTINATION_PORT);
	}

	/**
	 * Sets the sequence number.
	 * 
	 * @param seq
	 *            The new sequence number.
	 */
	public final void setSequenceNumber(long seq)
	{
		NetUtils.setBytes4Int(buffer, offsetTcp + TCP_OFFSET_SEQUENCE, seq & 0xffffffff);
	}

	/**
	 * @return The sequence number.
	 */
	public final long getSequenceNumber()
	{
		return NetUtils.bytes4AsInt(buffer, offsetTcp + TCP_OFFSET_SEQUENCE);
	}

	/**
	 * Sets the ack number.
	 * 
	 * @param seq
	 *            The new ack number.
	 */
	public final void setAckNumber(long seq)
	{
		NetUtils.setBytes4Int(buffer, offsetTcp + TCP_OFFSET_ACK, seq & 0xffffffff);
	}

	/**
	 * @return The acknowledgment number.
	 */
	public final long getAckNumber()
	{
		return NetUtils.bytes4AsInt(buffer, offsetTcp + TCP_OFFSET_ACK);
	}

	@Override
	public void setIPHeaderLength(int length)
	{
		super.setIPHeaderLength(length);
		offsetTcp = getIPHeaderOffset() + getIPHeaderLength();
	}

	/**
	 * Sets the TCP header length (i.e., the data offset field) in 32-bit words.
	 * 
	 * @param length
	 *            The TCP header length in 32-bit words.
	 */
	public final void setTCPHeaderLength(int lengthInWords)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetTcp + TCP_OFFSET_DATA_OFFSET, lengthInWords);
	}

	/**
	 * @return The TCP header length in bytes, although originally written as 32-bit words.
	 */
	public final int getTCPHeaderLength()
	{
		// Get the MSB 4 bits, and then multiply by 4 (shift two back)
		return (buffer[offsetTcp + TCP_OFFSET_DATA_OFFSET] & 0x00ff) >> 2;
	}

	/**
	 * Sets the TCP window size.
	 * <p>
	 * Does not take into account window scaling.
	 * 
	 * @param window
	 *            The TCP window size in bytes or x-bytes (if scaling is used).
	 */
	public final void setWindowSize(int window)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetTcp + TCP_OFFSET_WINDOW_SIZE, window);
	}

	/**
	 * @return The TCP window size.
	 */
	public final int getWindowSize()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetTcp + TCP_OFFSET_WINDOW_SIZE);
	}

	/**
	 * @return The TCP checksum.
	 */
	public final int getTCPChecksum()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetTcp + TCP_OFFSET_CHECKSUM);
	}

	/**
	 * @return The TCP packet length in bytes, includes the TCP header. This is the size of the IP packet minus the size
	 *         of the IP header.
	 * @see #getTCPPayloadLength()
	 */
	public final int getTCPPacketByteLength()
	{
		return getIPPacketLength() - getIPHeaderLength();
	}

	/**
	 * @return The sum of all headers lengths in bytes, meaning Ethernet (if included), IP and TCP.
	 * @see #getEthHeaderByteLength()
	 * @see #getIPAndTCPHeaderByteLength()
	 */
	public final int getCombinedHeaderByteLength()
	{
		return getEthHeaderByteLength() + getIPHeaderLength() + getTCPHeaderLength();
	}

	/**
	 * @return The IP header length plus the TCP header length in bytes.
	 * @see #getCombinedHeaderByteLength()
	 */
	public final int getIPAndTCPHeaderLength()
	{
		return getIPHeaderLength() + getTCPHeaderLength();
	}

	/**
	 * Sets the length of the TCP data payload.
	 * 
	 * @param length
	 *            The length of the TCP data payload in bytes.
	 */
	public final void setTCPDataByteLength(int length)
	{
		if (length < 0)
			length = 0;

		setIPPacketLength(getIPAndTCPHeaderLength() + length);
	}

	/**
	 * @return Data (payload) length in bytes.
	 * @see #getTCPPacketByteLength()
	 */
	public final int getTCPPayloadLength()
	{
		return getIPPacketLength() - getIPAndTCPHeaderLength();
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
		return s1 + s2 + d1 + d2 + getProtocol() + getTCPPacketByteLength();
	}

	/**
	 * Computes the TCP checksum, optionally updating the TCP checksum header.
	 * 
	 * @param update
	 *            Specifies whether or not to update the TCP checksum header after computing the checksum. A value of
	 *            true indicates the header should be updated, a value of false indicates it should not be updated.
	 * @return The computed TCP checksum.
	 */
	public final int computeTCPChecksum(boolean update)
	{
		return computeChecksum(offsetTcp, offsetTcp + TCP_OFFSET_CHECKSUM, getIPHeaderOffset() + getIPPacketLength(),
				__getVirtualHeaderTotal(), update);
	}

	public String flagsToString()
	{
		String result = "[";

		if (isSet(MASK_SYN))
			result += "SYN ";
		if (isSet(MASK_RST))
			result += "RST ";
		if (isSet(MASK_FIN))
			result += "FIN ";
		if (isSet(MASK_ACK))
			result += "ACK ";

		// TODO cover all
		return (result.length() == 1 ? result : result.substring(0, result.length() - 1)) + "]";
	}

	@Override
	public String toString()
	{
		return getSequenceNumber() + "/" + getAckNumber();
	}

	public TCPPacket(IPPacket ipPacket)
	{
		this(ipPacket.buffer, ipPacket.offsetEth);
	}

	/**
	 * Create a TCP packet with Ethernet frame and IP packet to wrap an existing raw frame buffer.
	 * 
	 * @param buffer
	 *            Raw frame to wrap.
	 * @param offsetEth
	 *            Offset of Ethernet header in the given buffer.
	 */
	public TCPPacket(byte[] buffer, int offsetEth)
	{
		// Prepare the Ethernet and IP packet fields
		super(buffer, offsetEth);
		if (!isValid)
			return;

		isValid = false;

		// Calculate UDP offset, based on Ethernet and IP headers
		this.offsetTcp = offsetIp + getIPHeaderLength();

		// Sanity check
		if (buffer == null || offsetTcp < 0 || (buffer.length - offsetTcp) < TCP_MIN_HEADER_LEN)
			return;

		// Check if Ethernet fields indicates that it carries an IP packet
		if (getProtocol() != IP_PROTOCOL_TCP)
			return;

		isValid = true;
	}

	/**
	 * @return True if the packet start with Skype stamp.
	 */
	public boolean isSkype()
	{
		// Sanity check
		if (buffer == null || offsetTcp < 0)
			return false;

		int payloadOffset = offsetTcp + getTCPHeaderLength();
		if (buffer.length - payloadOffset < 4)
			return false;

		return buffer[payloadOffset] == 0x17 && buffer[payloadOffset + 1] == 0x03 && buffer[payloadOffset + 2] == 0x01
				&& buffer[payloadOffset + 3] == 0x00;
	}

	/**
	 * @return True if the packet start with BitTorrent stamp.
	 */
	public boolean isBittorrent()
	{
		// Sanity check
		if (buffer == null || offsetTcp < 0)
			return false;

		int payloadOffset = offsetTcp + getTCPHeaderLength();
		int tcpPayloadLength = buffer.length - payloadOffset;
		if (tcpPayloadLength != 68)
			return false;

		// Not a full check
		return buffer[payloadOffset] == 19 && buffer[payloadOffset + 1] == 'B';
	}
}
