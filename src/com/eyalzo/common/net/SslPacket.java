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
 * Parsing SSL packets. This class was written for the {@link #getTlsHostName(byte[], int)} only, so it does not really
 * support full SSL parsing.
 * <p>
 * Basic SSL/TLS format, RFC 4346:
 * 
 * <pre>
 * struct {
 *           ProtocolVersion client_version;
 *           Random random;
 *           SessionID session_id;
 *           CipherSuite cipher_suites<2..2^16-1>;
 *           CompressionMethod compression_methods<1..2^8-1>;
 *       } ClientHello;
 * </pre>
 * <p>
 * Extended "hello" data, RFC 3546:
 * 
 * 
 * <pre>
 * 2.3. Hello Extensions
 * 
 *    The extension format for extended client hellos and extended server
 *    hellos is:
 * 
 *       struct {
 *           ExtensionType extension_type;
 *           opaque extension_data<0..2^16-1>;
 *       } Extension;
 * 
 *    Here:
 * 
 *    - "extension_type" identifies the particular extension type.
 * 
 *    - "extension_data" contains information specific to the particular
 *    extension type.
 * 
 *    The extension types defined in this document are:
 * 
 *       enum {
 *           server_name(0), max_fragment_length(1),
 *           client_certificate_url(2), trusted_ca_keys(3),
 *           truncated_hmac(4), status_request(5), (65535)
 *       } ExtensionType;
 * </pre>
 * 
 * ...
 * 
 * <pre>
 *    In order to provide the server name, clients MAY include an extension
 *    of type "server_name" in the (extended) client hello.  The
 *    "extension_data" field of this extension SHALL contain
 *    "ServerNameList" where:
 * 
 *       struct {
 *           NameType name_type;
 *           select (name_type) {
 *               case host_name: HostName;
 *           } name;
 *       } ServerName;
 * 
 *       enum {
 *           host_name(0), (255)
 *       } NameType;
 * 
 *       opaque HostName<1..2^16-1>;
 * 
 *       struct {
 *           ServerName server_name_list<1..2^16-1>
 *       } ServerNameList;
 * </pre>
 * 
 * @author Eyal Zohar
 * 
 */
public class SslPacket
{
	/**
	 * Minimum length for a header to be processed: type (1) + version (2) + length (2).
	 */
	private static final int	SSL_HEADER_MIN_LENGTH			= 5;
	/**
	 * Minimum length for a header to be processed: type (1) + length (3).
	 */
	private static final int	SSL_TLS_MIN_LENGTH				= 4;

	//
	// ProtocolVersion
	//
	private static final int	SSL_PROTOCOL_VERSION_TLS_1_0	= 0x0301;
	private static final int	SSL_PROTOCOL_VERSION_TLS_1_1	= 0x0302;

	/**
	 * @return May be null but never empty.
	 */
	public static String getTlsHostName(byte[] buffer, int startOffset)
	{
		if (startOffset < 0)
			return null;

		int offset = startOffset;

		if (buffer == null || (buffer.length - offset) < SSL_HEADER_MIN_LENGTH)
			return null;

		//
		// Main header
		//

		// Main, field 1 (1 byte): type
		int contentType = buffer[offset] & 0x00ff;
		offset++;
		if (contentType != ContentType.handshake.value)
			return null;

		// Main, field 2 (2 bytes): version
		int protocolVersion = NetUtils.bytes2AsUnsignedInt(buffer, offset);
		offset += 2;
		if (protocolVersion != SSL_PROTOCOL_VERSION_TLS_1_0 && protocolVersion != SSL_PROTOCOL_VERSION_TLS_1_1)
			return null;

		// Main, field 3 (2 bytes): length
		int length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
		offset += 2;
		if (length <= 0 || length > (buffer.length - offset))
			return null;

		//
		// TLSPlaintext (multiple)
		//
		if ((buffer.length - offset) < SSL_TLS_MIN_LENGTH)
			return null;

		// TLS, field 1 (1 byte): type
		int handshakeType = buffer[offset] & 0x00ff;
		offset++;
		if (handshakeType != HandshakeType.client_hello.value)
			return null;

		// TLS, field 2 (3 bytes): length
		length = NetUtils.bytes3AsUnsignedInt(buffer, offset);
		offset += 3;
		// Need to read at least 35 more (see below)
		if (length < 35 || length > (buffer.length - offset))
			return null;

		//
		// ClientHello structure
		//

		// Field 1 (2 bytes): client version
		offset += 2;

		// Field 2 (32 bytes): random
		offset += 32;

		// Field 3 (1 byte): session-id length
		length = buffer[offset] & 0x00ff;
		offset++;
		// Need to read at least two more bytes
		if (length < 0 || length > (buffer.length - offset + 2))
			return null;

		// Field 4 (var bytes, can be zero): session-id
		offset += length;

		// Field 5 (2 bytes): cipher-suites length
		length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
		offset += 2;
		// Need to read at least one more byte
		if (length < 0 || length > (buffer.length - offset + 1))
			return null;

		// Field 6 (var bytes): cipher-suites
		offset += length;

		// Field 7 (1 byte): compression-method length
		length = buffer[offset] & 0x00ff;
		offset++;
		// Need to read at least two more bytes
		if (length < 0 || length > (buffer.length - offset + 2))
			return null;

		// Field 8 (var bytes): compression method
		offset += length;

		// Field 9 (2 bytes): extensions length
		length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
		offset += 2;
		// Need to read at least one more byte
		if (length <= 0 || length > (buffer.length - offset + 1))
			return null;

		// Field 10+: extensions (multiple)
		int extensionsEndOffset = offset + length;
		// Loop as long as there are bytes left to read another extension header
		while (offset <= (extensionsEndOffset - 4))
		{
			int type = NetUtils.bytes2AsUnsignedInt(buffer, offset);
			offset += 2;
			length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
			offset += 2;
			// Length safety check
			if (length <= 0 || offset + length > extensionsEndOffset)
				return null;
			// Skip extensions that are not the server_name(0)
			if (type != 0)
			{
				// Skip the opaque
				offset += length;
				continue;
			}
			// Safety check, for list length reading
			if (length < 2)
				return null;
			// Now we have a server_name in hand
			// This can be a list, so a length is read again
			length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
			offset += 2;
			// Length safety check
			if (length <= 0 || offset + length > extensionsEndOffset)
				return null;

			//
			// Host names loop
			//
			while (offset <= (extensionsEndOffset - 3))
			{
				type = buffer[offset] & 0x00ff;
				offset++;
				length = NetUtils.bytes2AsUnsignedInt(buffer, offset);
				offset += 2;
				// Length safety check (incomplete)
				if (length <= 0 || offset + length > extensionsEndOffset)
					return null;

				// No need to loop anymore, because we have a name
				String result = new String(buffer, offset, length);
				return result;
			}
		}

		return null;
	}

	/**
	 * Message content-type.
	 * <p>
	 * 
	 * @see <a href="http://tools.ietf.org/html/rfc4346">RFC 4346</a>
	 */
	private enum ContentType
	{
		change_cipher_spec(20), alert(21), handshake(22), application_data(23);
		private int	value;

		private ContentType(int value)
		{
			this.value = value;
		}
	}

	private enum HandshakeType
	{
		hello_request(0), client_hello(1), server_hello(2), certificate(11), server_key_exchange(12), certificate_request(
				13), server_hello_done(14), certificate_verify(15), client_key_exchange(16), finished(20);

		private int	value;

		private HandshakeType(int value)
		{
			this.value = value;
		}
	}
}
