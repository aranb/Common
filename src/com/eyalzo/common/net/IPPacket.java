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

/**
 * Parse IP and alter packet, optionally with an Ethernet header.
 * <p>
 * Data itself is not copied nor completely parsed immediately. Instead, separate called to getters and setters handle
 * specific fields.
 * 
 * @author Eyal Zohar
 */
public class IPPacket extends EtherPacket
{
	/**
	 * The byte offset into the raw packet where the IP packet begins. This may be due to initial offset in the given
	 * buffer and/or offset cause by Ethernet header.
	 */
	protected int				offsetIp;

	//
	// Constants
	//
	/**
	 * Minimum number of bytes in an IP header.
	 */
	private static final int	MIN_IP_HEADER_LEN				= 20;

	//
	// IP packet offsets
	//
	/**
	 * Field 1 (1): Two parts - IP version (4 bits) and header length in 32-bit units (4 bits).
	 * */
	public static final int		IP_OFFSET_VERSION_AND_LEN		= 0;
	/**
	 * Field 2 (1): Type of service.
	 * */
	public static final int		IP_OFFSET_TYPE_OF_SERVICE		= 1;
	/**
	 * Field 3 (2): Packet length in bytes, including the header.
	 */
	public static final int		IP_OFFSET_TOTAL_LENGTH			= 2;
	/**
	 * Field 4 (2): Identification.
	 */
	public static final int		IP_OFFSET_IDENTIFICATION		= 4;
	/**
	 * Field 5 (1 - 3 left bits): Flags.
	 */
	public static final int		IP_OFFSET_FLAGS					= 6;
	/**
	 * Field 7 (1): TTL.
	 */
	public static final int		IP_OFFSET_TTL					= 8;
	/**
	 * Field 8 (1): Protocol.
	 */
	public static final int		IP_OFFSET_PROTOCOL				= 9;
	/**
	 * Field 9 (2): Checksum.
	 */
	public static final int		IP_OFFSET_CHECKSUM				= 10;
	/**
	 * Field 10 (4): Source address.
	 */
	public static final int		IP_OFFSET_SOURCE_ADDRESS		= 12;
	/**
	 * Field 11 (4): Destination address.
	 */
	public static final int		IP_OFFSET_DESTINATION_ADDRESS	= 16;

	//
	// Protocols
	//
	/**
	 * Protocol constant for IPv4.
	 * */
	public static final int		IP_PROTOCOL_IP					= 0;
	/**
	 * Protocol constant for ICMP.
	 * */
	public static final int		IP_PROTOCOL_ICMP				= 1;
	/**
	 * Protocol constant for TCP.
	 * */
	public static final int		IP_PROTOCOL_TCP					= 6;
	/**
	 * Protocol constant for UDP.
	 * */
	public static final int		IP_PROTOCOL_UDP					= 17;

	/**
	 * @return The size of the IP packet, including the IP header, but without Ethernet header.
	 */
	public int getIpPacketSizeWithIpHeader()
	{
		return buffer.length - offsetIp;
	}

	/**
	 * Sets the raw packet byte array, assuming that the buffer is available for this instance only.
	 * 
	 * @param data
	 *            The raw packet byte array to wrap.
	 */
	public void setData(byte[] data)
	{
		setData(data, 0);
	}

	/**
	 * Wrap existing raw packet, to get ready for later reads of specific fields.
	 * 
	 * @param data
	 *            The buffer that holds the IP packet and/or Ethernet header. This buffer must be allocated for this
	 *            wrapping class all the time.
	 * @param offset
	 *            Offset in the buffer to the Ethernet header (if included) or IP header (if no Ethernet).
	 * @param isEthHdrIcluded
	 *            Indicates whether the raw data includes the Ethernet header.
	 */
	public void setData(byte[] data, int offset)
	{
		// The buffer just points to an already allocated buffer.
		buffer = data;

		// Indicate that there is no Ethernet header in this case
		offsetEth = offsetIp = offset;
	}

	/**
	 * Copies a raw packet data into the internal buffer.
	 * <p>
	 * If the array is too small to hold the data, the data is truncated.
	 * 
	 * @param srcRawPacket
	 *            The raw packet byte array to wrap.
	 */
	public void getData(byte[] srcRawPacket, int srcRawPacketOffset)
	{
		System.arraycopy(buffer, offsetEth, srcRawPacket, srcRawPacketOffset, srcRawPacket.length - srcRawPacketOffset);
	}

	/**
	 * @return Size of raw packet in bytes, including Ethernet header if included.
	 */
	public int size()
	{
		return buffer.length - offsetEth;
	}

	/**
	 * Sets the IP version header value.
	 * 
	 * @param version
	 *            A 4-bit unsigned integer.
	 */
	public final void setIPVersion(int version)
	{
		buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] &= 0x0f;
		buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] |= ((version << 4) & 0xf0);
	}

	/**
	 * Returns the IP version header value.
	 * 
	 * @return The IP version header value.
	 */
	public final int getIPVersion()
	{
		return ((buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] & 0xf0) >> 4);
	}

	/**
	 * Sets the IP header length field. At most, this can be a four-bit value. The high order bits beyond the fourth bit
	 * will be ignored.
	 * 
	 * @param length
	 *            The length of the IP header in 32-bit words.
	 */
	public void setIPHeaderLength(int length)
	{
		// Clear the 4 low order (right) bits
		buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] &= 0xf0;
		// Set the 4 bits we just cleared
		buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] |= (length & 0x0f);
	}

	/**
	 * @return The length of the IP header in 32-bit words.
	 */
	private final int getIPHeaderWordsLength()
	{
		return (buffer[offsetIp + IP_OFFSET_VERSION_AND_LEN] & 0x0f);
	}

	/**
	 * @return The length of the IP header in bytes, which is 4 times the size written in the header, which uses 32-bit
	 *         words.
	 */
	public final int getIPHeaderLength()
	{
		return getIPHeaderWordsLength() << 2;
	}

	/**
	 * Sets the IP type of service header value. You have to set the individual service bits yourself. Convenience
	 * methods for setting the service bit fields directly may be added in a future version.
	 * 
	 * @param service
	 *            An 8-bit unsigned integer.
	 */
	public final void setTypeOfService(int service)
	{
		buffer[offsetIp + IP_OFFSET_TYPE_OF_SERVICE] = (byte) (service & 0xff);
	}

	/**
	 * Returns the IP type of service header value.
	 * 
	 * @return The IP type of service header value.
	 */
	public final int getTypeOfService()
	{
		return (buffer[offsetIp + IP_OFFSET_TYPE_OF_SERVICE] & 0xff);
	}

	/**
	 * Sets the IP packet total length header value.
	 * 
	 * @param length
	 *            The total IP packet length in bytes.
	 */
	public final void setIPPacketLength(int length)
	{
		buffer[offsetIp + IP_OFFSET_TOTAL_LENGTH] = (byte) ((length >> 8) & 0xff);
		buffer[offsetIp + IP_OFFSET_TOTAL_LENGTH + 1] = (byte) (length & 0xff);
	}

	/**
	 * @return The IP packet total length from the header value.
	 */
	public final int getIPPacketLength()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetIp + IP_OFFSET_TOTAL_LENGTH);
	}

	/**
	 * Sets the IP identification header value.
	 * 
	 * @param id
	 *            A 16-bit unsigned integer.
	 */
	public void setIdentification(int id)
	{
		NetUtils.setBytes2UnsignedInt(buffer, offsetIp + IP_OFFSET_IDENTIFICATION, id);
	}

	/**
	 * Returns the IP identification header value.
	 * 
	 * @return The IP identification header value.
	 */
	public final int getIdentification()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetIp + IP_OFFSET_IDENTIFICATION);
	}

	/**
	 * Sets the IP flags header value. You have to set the individual flag bits yourself. Convenience methods for
	 * setting the flag bit fields directly may be added in a future version.
	 * 
	 * @param flags
	 *            A 3-bit unsigned integer.
	 */
	public final void setIPFlags(int flags)
	{
		buffer[offsetIp + IP_OFFSET_FLAGS] &= 0x1f;
		buffer[offsetIp + IP_OFFSET_FLAGS] |= ((flags << 5) & 0xe0);
	}

	/**
	 * Returns the IP flags header value.
	 * 
	 * @return The IP flags header value.
	 */
	public final int getIPFlags()
	{
		return ((buffer[offsetIp + IP_OFFSET_FLAGS] & 0xe0) >> 5);
	}

	/**
	 * Sets the fragment offset header value. The offset specifies a number of octets (i.e., bytes).
	 * 
	 * @param offset
	 *            A 13-bit unsigned integer.
	 */
	public void setFragmentOffset(int offset)
	{
		buffer[offsetIp + IP_OFFSET_FLAGS] &= 0xe0;
		buffer[offsetIp + IP_OFFSET_FLAGS] |= ((offset >> 8) & 0x1f);
		buffer[offsetIp + IP_OFFSET_FLAGS + 1] = (byte) (offset & 0xff);
	}

	/**
	 * Returns the fragment offset header value.
	 * 
	 * @return The fragment offset header value.
	 */
	public final int getFragmentOffset()
	{
		return (((buffer[offsetIp + IP_OFFSET_FLAGS] & 0x1f) << 8) | (buffer[offsetIp + IP_OFFSET_FLAGS + 1] & 0xff));
	}

	/**
	 * Sets the protocol number.
	 * 
	 * @param protocol
	 *            The protocol number.
	 */
	public final void setProtocol(int protocol)
	{
		buffer[offsetIp + IP_OFFSET_PROTOCOL] = (byte) protocol;
	}

	/**
	 * @return The protocol number.
	 */
	public final int getProtocol()
	{
		return buffer[offsetIp + IP_OFFSET_PROTOCOL];
	}

	/**
	 * Sets the time to live value in hop-count.
	 * 
	 * @param ttl
	 *            The time to live value in hop-count.
	 */
	public final void setTTL(int ttl)
	{
		buffer[offsetIp + IP_OFFSET_TTL] = (byte) ttl;
	}

	/**
	 * @return The time to live value in hop-count.
	 */
	public final int getTTL()
	{
		return buffer[offsetIp + IP_OFFSET_TTL];
	}

	/**
	 * Calculates checksums assuming the checksum is a 16-bit header field. This method is generalized to work for IP,
	 * ICMP, UDP, and TCP packets given the proper parameters.
	 */
	protected int computeChecksum(int startOffset, int checksumOffset, int length, int virtualHeaderTotal,
			boolean update)
	{
		int total = 0;
		int i = startOffset;
		int imax = checksumOffset;

		while (i < imax)
			total += (((buffer[i++] & 0xff) << 8) | (buffer[i++] & 0xff));

		// Skip existing checksum.
		i = checksumOffset + 2;

		imax = length - (length % 2);

		while (i < imax)
			total += (((buffer[i++] & 0xff) << 8) | (buffer[i++] & 0xff));

		if (i < length)
			total += ((buffer[i] & 0xff) << 8);

		total += virtualHeaderTotal;

		// Fold to 16 bits
		while ((total & 0xffff0000) != 0)
			total = (total & 0xffff) + (total >>> 16);

		total = (~total & 0xffff);

		if (update)
		{
			buffer[checksumOffset] = (byte) (total >> 8);
			buffer[checksumOffset + 1] = (byte) (total & 0xff);
		}

		return total;
	}

	/**
	 * Computes the IP checksum, optionally updating the IP checksum header.
	 * 
	 * @param update
	 *            Specifies whether or not to update the IP checksum header after computing the checksum. A value of
	 *            true indicates the header should be updated, a value of false indicates it should not be updated.
	 * @return The computed IP checksum.
	 */
	public final int computeIPChecksum(boolean update)
	{
		return computeChecksum(offsetIp, offsetIp + IP_OFFSET_CHECKSUM, offsetIp + getIPHeaderLength(), 0, update);
	}

	/**
	 * @return The IP checksum header value.
	 */
	public final int getIPChecksum()
	{
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetIp + IP_OFFSET_CHECKSUM);
	}

	/**
	 * @return The source IP address as 4-byte integer.
	 */
	public final int getSourceAsInt()
	{
		return NetUtils.bytes4AsInt(buffer, offsetIp + IP_OFFSET_SOURCE_ADDRESS);
	}

	/**
	 * @return The source IP address as "x.x.x.x".
	 */
	public final String getSourceAsString()
	{
		return NetUtils.ipAsString(getSourceAsInt());
	}

	/**
	 * @return The destination IP address as 4-byte integer.
	 */
	public final int getDestinationAsInt()
	{
		return NetUtils.bytes4AsInt(buffer, offsetIp + IP_OFFSET_DESTINATION_ADDRESS);
	}

	/**
	 * @return The destination IP address as "x.x.x.x".
	 */
	public final String getDestinationAsString()
	{
		return NetUtils.ipAsString(getDestinationAsInt());
	}

	/**
	 * Get the offset of the IP header from the start of the raw packet data. Will be non zero in case the raw data
	 * includes additional data like the Ethernet header or if the buffer contains additional arbitrary data.
	 * 
	 * @return
	 */
	public int getIPHeaderOffset()
	{
		return offsetIp;
	}

	/**
	 * Indicates whether the raw data from which this packet is constructed contains also the Ethernet layer.
	 * 
	 * @return True if Ethernet data is included in packet (actually a frame).
	 */
	public boolean isEthHdrIncluded()
	{
		return offsetIp > offsetEth;
	}

	/**
	 * @return Zero if Ethernet data is not included in packet, or Ethernet header length if it does.
	 */
	public final int getEthHeaderByteLength()
	{
		return offsetIp > offsetEth ? (offsetIp - offsetEth) : 0;
	}

	/**
	 * @param ethPacket
	 *            Ethernet packet that (maybe) contains an IP packet.
	 * @return New IP packet object, or null if too small or malformed.
	 */
	public IPPacket(EtherPacket ethPacket)
	{
		this(ethPacket.buffer, ethPacket.offsetEth);
	}

	/**
	 * Create an IP packet with Ethernet frame to wrap an existing raw frame buffer.
	 * 
	 * @param buffer
	 *            Raw frame to wrap.
	 * @param offsetEth
	 *            Offset of Ethernet header in the given buffer.
	 * @param offsetIp
	 *            Offset of the IP packet in the given buffer.
	 */
	public IPPacket(byte[] buffer, int offsetEth)
	{
		// Parse Ethernet first
		super(buffer, offsetEth);
		if (!isValid)
			return;

		isValid = false;

		// IP offset can be calculated now, based on Ethernet frame fields
		this.offsetIp = offsetEth + ethHeaderLength;

		// Sanity check
		if (buffer == null || offsetIp < 0 || (offsetIp - offsetEth) < ETH_MIN_HEADER_LEN
				|| (buffer.length - offsetIp) < MIN_IP_HEADER_LEN)
			return;

		// Check if Ethernet fields indicates that it carries an IP packet
		if (!isEtherTypeIp())
			return;

		isValid = true;
	}

	@Override
	public String toString()
	{
		return String.format("%s-%s len=%,d header=%,d protocol=%s", getSourceAsString(), getDestinationAsString(),
				getIPPacketLength(), getIPHeaderLength(), getProtocolName());
	}

	/**
	 * @return Protocol name if known, or the number if not.
	 */
	private String getProtocolName()
	{
		int protocol = this.getProtocol();

		if (protocol == IP_PROTOCOL_ICMP)
			return "ICMP";
		if (protocol == IP_PROTOCOL_IP)
			return "IP";
		if (protocol == IP_PROTOCOL_TCP)
			return "TCP";
		if (protocol == IP_PROTOCOL_UDP)
			return "UDP";

		return Integer.toString(protocol);
	}

	public boolean isProtocolTcp()
	{
		return getProtocol() == IP_PROTOCOL_TCP;
	}

	public boolean isProtocolUdp()
	{
		return getProtocol() == IP_PROTOCOL_UDP;
	}
}
