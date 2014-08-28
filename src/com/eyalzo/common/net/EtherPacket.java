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
 * Ethernet packet parsing of an existing (allocated) buffer.
 * <p>
 * Basically, an Ethernet frame does not have length but only a header-length. Further parsing may reveal that the
 * carried IP packet does not reach the end of the buffer.
 * 
 * @author Eyal Zohar
 * 
 */
public class EtherPacket
{
	/**
	 * Raw packet data, as a pointer only. We assume that the buffer itself was already allocated outside this class.
	 * */
	protected byte[]			buffer;
	/**
	 * The byte offset into the raw packet where the Ethernet packet begins. This may be due to initial offset in the
	 * given buffer. If the Ethernet header is not included, then it points to the IP header.
	 */
	protected int				offsetEth;
	/**
	 * Ethernet header length, based on frame content, since there are two options - with or without VLAN tag.
	 */
	protected int				ethHeaderLength;
	/**
	 * True if frame has a VLAN tag.
	 */
	protected boolean			hasVlanTag;
	protected boolean			isValid;

	//
	// Ethernet frame offsets
	//
	/**
	 * Ethernet field 1 (6): Destination MAC address.
	 * */
	private static final int	ETH_OFFSET_MAC_DESTINATION		= 0;
	/**
	 * Ethernet field 2 (6): Source MAC address.
	 * */
	private static final int	ETH_OFFSET_MAC_SOURCE			= 6;
	/**
	 * Ethernet field 3 (2): Ethernet type, meaning the protocol carried in the frame. This is where an indication for
	 * VLAN can be found, then the following fields are moved, including this one.
	 */
	private static final int	ETH_OFFSET_ETHER_TYPE_NO_VLAN	= 12;
	/**
	 * Similar to {@link #ETH_OFFSET_ETHER_TYPE_NO_VLAN} but when VLAN tag is present.
	 */
	private static final int	ETH_OFFSET_ETHER_TYPE_WITH_VLAN	= 16;
	/**
	 * Offset of VLAN tag in Ethernet frame, just in case the type indicates that it is a VLAN frame.
	 * */
	private static final int	ETH_OFFSET_VLAN_TAG				= 14;

	//
	// Ethernet related constants
	//
	/**
	 * Normal size in bytes of an Ethernet II header, without a VLAN tag.
	 */
	public static final int		ETH_MIN_HEADER_LEN				= 14;
	/**
	 * Size in bytes of a MAC address (source or destination).
	 * */
	private static final int	ETH_MAC_LEN						= 6;
	/**
	 * Value of "ether type" when the Ethernet frame contains VLAN.
	 */
	private static final byte	ETHER_TYPE_VLAN_MSB				= (byte) 0x81;
	private static final byte	ETHER_TYPE_VLAN_LSB				= (byte) 0x00;
	/**
	 * Size of VLAN tag in Ethernet frame.
	 * */
	private static final int	VLAN_TAG_LEN					= 4;

	/**
	 * Wrap existing raw Ethernet frame, to get ready for later reads of specific fields.
	 * 
	 * @param data
	 *            The buffer that holds the IP packet and/or Ethernet header. This buffer must be allocated for this
	 *            wrapping class all the time.
	 * @param offsetEth
	 *            Offset in the buffer to the first byte of the Ethernet header.
	 */
	public EtherPacket(byte[] data, int offsetEth)
	{
		initEthernet(data, offsetEth);
	}

	/**
	 * Wrap existing raw Ethernet frame, to get ready for later reads of specific fields.
	 * 
	 * @param data
	 *            The buffer that holds the IP packet and/or Ethernet header. This buffer must be allocated for this
	 *            wrapping class all the time.
	 * @param offsetEth
	 *            Offset in the buffer to the first byte of the Ethernet header.
	 */
	public boolean initEthernet(byte[] data, int offsetEth)
	{
		// Sanity check
		if (data == null || offsetEth < 0 || (data.length - offsetEth) < ETH_MIN_HEADER_LEN)
		{
			this.isValid = false;
			return false;
		}

		// The buffer just points to an already allocated buffer.
		this.buffer = data;

		this.offsetEth = offsetEth;

		// Check VLAN tag
		this.hasVlanTag = data[offsetEth + ETH_OFFSET_ETHER_TYPE_NO_VLAN] == ETHER_TYPE_VLAN_MSB
				&& data[offsetEth + ETH_OFFSET_ETHER_TYPE_NO_VLAN + 1] == ETHER_TYPE_VLAN_LSB;

		// Look for VLAN
		if (this.hasVlanTag)
		{
			// Ethernet frame contains a VLAN tag, i.e. additional 4 bytes
			this.ethHeaderLength = ETH_MIN_HEADER_LEN + VLAN_TAG_LEN;
		} else
		{
			this.ethHeaderLength = ETH_MIN_HEADER_LEN;
		}

		this.isValid = true;

		return true;
	}

	/**
	 * Return the source MAC address if the Ethernet header was included in the raw packet.
	 * 
	 * @return Source MAC address, or null if not available.
	 */
	public byte[] getSourceMacAddress()
	{
		byte[] srcMac = new byte[6];
		System.arraycopy(buffer, ETH_OFFSET_MAC_SOURCE, srcMac, 0, ETH_MAC_LEN);
		return srcMac;
	}

	/**
	 * Return the destination MAC address if the Ethernet header was included in the raw packet.
	 * 
	 * @return Destination MAC address, or null if not available.
	 */
	public byte[] getDestinationMacAddress()
	{
		byte[] dstMac = new byte[6];
		System.arraycopy(buffer, ETH_OFFSET_MAC_DESTINATION, dstMac, 0, ETH_MAC_LEN);
		return dstMac;
	}

	/**
	 * @return Ether-type value, as read from the Ethernet frame header.
	 */
	public int getEtherType()
	{
		int offset = hasVlanTag ? ETH_OFFSET_ETHER_TYPE_WITH_VLAN : ETH_OFFSET_ETHER_TYPE_NO_VLAN;
		return NetUtils.bytes2AsUnsignedInt(buffer, offsetEth + offset);
	}

	/**
	 * 
	 * @return True if ether-type is IP, meaning that the frame contains an IP packet.
	 */
	public boolean isEtherTypeIp()
	{
		return getEtherType() == 0x0800;
	}

	/**
	 * Return the VLAN tag if the Ethernet header was included in the raw packet and the Ethernet frame is tagged with a
	 * VLAN.
	 * 
	 * @return VLAN tag or zero if not available.
	 */
	public int getVlanTag()
	{
		if (!hasVlanTag)
			return 0;

		return NetUtils.bytes4AsInt(buffer, ETH_OFFSET_VLAN_TAG);
	}

	/**
	 * @return Ethernet header length, which depands on the question if the frame contains VLAN tag.
	 */
	public int getEthHeaderLength()
	{
		return ethHeaderLength;
	}

	/**
	 * @return False if frame is surely invalid, usually due to length problems.
	 */
	public boolean isValid()
	{
		return isValid;
	}
}
