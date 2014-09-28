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
package com.eyalzo.common.net;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import com.eyalzo.common.misc.Couple;

/**
 * Tools to process a TCP payload that carries HTTP header (both request or response).
 * 
 * @author Eyal Zohar
 */
public class HttpTcpPayload
{
	//
	// Constants
	//
	private static final String		HTTP_REQUEST_HOST					= "\nHost: ";
	private static final byte[]		HTTP_REQUEST_HOST_BYTES				= HTTP_REQUEST_HOST.getBytes();
	private static final int		HTTP_RESPONSE_HOST_LEN				= HTTP_REQUEST_HOST.length();
	private static final String		HTTP_REQUEST_REFERER				= "\nReferer: http://";
	private static final byte[]		HTTP_REQUEST_REFERER_BYTES			= HTTP_REQUEST_REFERER.getBytes();
	private static final int		HTTP_REQUEST_REFERER_LEN			= HTTP_REQUEST_REFERER.length();
	private static final String		HTTP_COMMAND_GET_PREFIX				= "GET ";
	private static final int		HTTP_COMMAND_GET_PREFIX_LEN			= HTTP_COMMAND_GET_PREFIX.length();
	private static final String		HTTP_COMMAND_POST_PREFIX			= "POST ";
	private static final int		HTTP_COMMAND_POST_PREFIX_LEN		= HTTP_COMMAND_POST_PREFIX.length();
	private static final String		HTTP_COMMAND_SUFFIX					= " HTTP/1.";
	private static final int		HTTP_COMMAND_SUFFIX_LEN				= HTTP_COMMAND_SUFFIX.length() + 1;
	private static final String		HTTP_RESPONSE_CONTENT_LENGTH		= "\nContent-Length: ";
	private static final byte[]		HTTP_RESPONSE_CONTENT_LENGTH_BYTES	= HTTP_RESPONSE_CONTENT_LENGTH.getBytes();
	private static final int		HTTP_RESPONSE_CONTENT_LENGTH_LEN	= HTTP_RESPONSE_CONTENT_LENGTH.length();
	private static final String		HTTP_RESPONSE_CONTENT_RANGE			= "\nContent-Range: ";
	private static final byte[]		HTTP_RESPONSE_CONTENT_RANGE_BYTES	= HTTP_RESPONSE_CONTENT_RANGE.getBytes();
	private static final int		HTTP_RESPONSE_CONTENT_RANGE_LEN		= HTTP_RESPONSE_CONTENT_RANGE.length();
	private static final String		HTTP_RESPONSE_CONTENT_TYPE			= "\nContent-Type: ";
	private static final byte[]		HTTP_RESPONSE_CONTENT_TYPE_BYTES	= HTTP_RESPONSE_CONTENT_TYPE.getBytes();
	private static final int		HTTP_RESPONSE_CONTENT_TYPE_LEN		= HTTP_RESPONSE_CONTENT_TYPE.length();
	private static final String		HTTP_RESPONSE_ATTACH_FILE1			= "\nContent-Disposition: Attachment; filename=";
	private static final byte[]		HTTP_RESPONSE_ATTACH_FILE1_BYTES	= HTTP_RESPONSE_ATTACH_FILE1.getBytes();
	private static final String		HTTP_RESPONSE_ATTACH_FILE2			= "\nContent-Disposition: attachment; filename=";
	private static final byte[]		HTTP_RESPONSE_ATTACH_FILE2_BYTES	= HTTP_RESPONSE_ATTACH_FILE2.getBytes();
	private static final String		HTTP_RESPONSE_ETAG					= "\nETag: ";
	private static final byte[]		HTTP_RESPONSE_ETAG_BYTES			= HTTP_RESPONSE_ETAG.getBytes();
	private static final byte[]		NEWLINE_BYTES						= "\r".getBytes();
	private static final byte[]		SLASHN_BYTES						= "\n".getBytes();
	public static final byte[]		DOUBLE_QUOTE_BYTES					= "\"".getBytes();
	public static final byte[]		QUOTE_BYTES							= "'".getBytes();
	private static final Pattern	hostPattern							= Pattern
																				.compile("(([a-z0-9][a-z0-9_-]*)(\\.[a-z0-9][a-z0-9_-]*)+)");
	private static final byte[]		DOUBLE_NEW_LINE_BYTES				= "\r\n\r\n".getBytes();
	public static final byte[][]	URL_PARAM_BYTES_LINKS_AND_IMAGES	= { "src=".getBytes(), "SRC=".getBytes(),
			"href=".getBytes(), "HREF=".getBytes()						};
	public static final byte[][]	URL_PARAM_BYTES_LINKS				= { "href=".getBytes(), "HREF=".getBytes() };

	/**
	 * Get the URL part from a TCP payload in HTTP. The first line is expected to start with "GET" or "POST" and end
	 * with "HTTP/1.1" or "HTTP/1.0". The returned URL may differ from the original, as it may be decoded, especially
	 * when some of the characters are expressed as "%xy" like "%20" for space etc.
	 * 
	 * @param payload
	 *            The full TCP payload, expected to be in an HTTP format.
	 * @param decode
	 *            True if the URL should be decoded to UTF-8.
	 * @return Full URL from a GET/POST method (optionally decoded to UTF-8), or null if no such thing was detected.
	 */
	public static String getUrlFromPayload(String payload, boolean decode)
	{
		if (payload == null)
			return null;

		int httpCommandPrefixLen = 0;
		if (payload.startsWith(HTTP_COMMAND_GET_PREFIX))
		{
			httpCommandPrefixLen = HTTP_COMMAND_GET_PREFIX_LEN;
		} else if (payload.startsWith(HTTP_COMMAND_POST_PREFIX))
		{
			httpCommandPrefixLen = HTTP_COMMAND_POST_PREFIX_LEN;
		} else
		{
			return null;
		}

		// New line must appear right after "GET ... HTTP/1.x" (x is 0 or 1)
		int newLinePos = payload.indexOf('\r', httpCommandPrefixLen);
		if (newLinePos < 14)
			return null;

		// Make sure the line ends correctly
		int spacePos = newLinePos - HTTP_COMMAND_SUFFIX_LEN;
		if (payload.indexOf(HTTP_COMMAND_SUFFIX, spacePos) != spacePos)
			return null;

		String subStr = payload.substring(httpCommandPrefixLen, spacePos);

		if (decode)
		{
			try
			{
				String result = URLDecoder.decode(subStr, "UTF-8");
				return result;
			}
			// May catch UnsupportedEncodingException and
			// IllegalArgumentException
			catch (Exception e)
			{
			}
		}

		return subStr;
	}

	/**
	 * @return The decoded URL by UTF-8, or the original if something went wrong.
	 */
	public static String decodeUrl(String original)
	{
		try
		{
			String result = URLDecoder.decode(original, "UTF-8");
			return result;
		}
		// May catch UnsupportedEncodingException and IllegalArgumentException
		catch (Exception e)
		{
		}

		return original;
	}

	/**
	 * Get the host name from a TCP payload carrying an HTTP header.
	 * <p>
	 * The host name must follow the pattern. It may fail if the TCP payload is not complete and/or some kind of
	 * encoding was used.
	 * 
	 * @return Host name, or null if something went wrong.
	 */
	public static String getHostFromPayload(String payload)
	{
		if (payload == null)
			return null;

		String hostName = null;

		//
		// Get the position where the host name is written
		//
		int hostPos = payload.indexOf(HTTP_REQUEST_HOST);
		if (hostPos < 0)
			return null;
		hostPos += HTTP_RESPONSE_HOST_LEN;

		//
		// Make sure the host line ends with a new-line, and it contains a
		// minimal number of characters
		//
		int newLinePos = payload.indexOf('\r', hostPos);
		if (newLinePos < (hostPos + 4))
			return null;

		//
		// Isolate the host name
		//
		hostName = payload.substring(hostPos, newLinePos);

		//
		// Remove port number if mentioned
		//
		int portIndex = hostName.indexOf(':');
		if (portIndex > 0)
		{
			hostName = hostName.substring(0, portIndex);
		}

		// Make sure it matches the host name rules
		if (!hostPattern.matcher(hostName).matches())
			return null;

		return hostName;
	}

	/**
	 * @return Length of HTTP header assuming that the header start at the given offset, or -1 if the header end was not
	 *         found.
	 */
	public static int getHttpHeaderLenFromPayloadStart(byte[] data, int startOffset)
	{
		// Needed for all the fields we search for in the header
		int newlineOffset = indexOfBytesInBytes(data, DOUBLE_NEW_LINE_BYTES, startOffset, data.length);
		return newlineOffset == -1 ? -1 : newlineOffset + DOUBLE_NEW_LINE_BYTES.length - startOffset;
	}

	/**
	 * Get the content length from a TCP payload carrying an HTTP response.
	 * 
	 * @return Host name, or null if something went wrong.
	 */
	public static long getContentLengthFromPayload(String payload)
	{
		if (payload == null)
			return 0;

		//
		// Get the position where the content length is written
		//
		int fieldPos = payload.indexOf(HTTP_RESPONSE_CONTENT_LENGTH);
		if (fieldPos < 0)
			return 0;
		fieldPos += HTTP_RESPONSE_CONTENT_LENGTH_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = payload.indexOf('\r', fieldPos);
		if (newLinePos < (fieldPos + 1))
			return 0;

		//
		// Isolate the number
		//
		String numString = payload.substring(fieldPos, newLinePos);
		try
		{
			long num = Long.parseLong(numString);
			return num;
		} catch (Exception e)
		{
		}

		return 0;
	}

	/**
	 * Get the content length from a TCP payload carrying an HTTP response.
	 * 
	 * @param payload
	 *            HTTP content payload
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return Content length, or zero if anything went wrong.
	 */
	public static long getContentLengthFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_RESPONSE_CONTENT_LENGTH_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return 0;

		fieldPos += HTTP_RESPONSE_CONTENT_LENGTH_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return 0;

		long result = bytesToLong(payload, fieldPos, newLinePos);
		if (result < 0)
			return 0;

		return result;
	}

	/**
	 * 
	 * Purpose: Get the content range from value from the TCP payload carrying an HTTP response.
	 * 
	 * @param payload
	 *            HTTP content payload
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return from range, or zero if anything went wrong.
	 */
	public static long getContentRangeFromFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_RESPONSE_CONTENT_RANGE_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return 0;

		fieldPos += HTTP_RESPONSE_CONTENT_RANGE_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return 0;

		// reset the position to the dash char (-)
		int dashPos = indexOfBytesInBytes(payload, "-".getBytes(), fieldPos, newLinePos);

		int spacePos = dashPos;
		while (payload[spacePos] != ' ')
			spacePos--;

		long result = bytesToLong(payload, spacePos + 1, dashPos);
		if (result < 0)
			return 0;

		return result;
	}

	/**
	 * 
	 * Purpose: Get the content range to value from the TCP payload carrying an HTTP response.
	 * 
	 * @param payload
	 *            HTTP content payload
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return to range or total range if not exists, or zero if anything went wrong.
	 */
	public static long getContentRangeToFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_RESPONSE_CONTENT_RANGE_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return 0;

		fieldPos += HTTP_RESPONSE_CONTENT_RANGE_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return 0;

		int dashPos = indexOfBytesInBytes(payload, "-".getBytes(), fieldPos, newLinePos);
		if (dashPos < 0)
			return 0;

		int slashPos = indexOfBytesInBytes(payload, "/".getBytes(), fieldPos, newLinePos);
		if (slashPos < 0)
			return 0;

		long result;

		// try to get the to, if slash and dash are one after the other, the to
		// is equal to the total
		// set it accordingly (this is an impossible scenario in incoming
		// response but can happen in outgoing if controlled)
		if (dashPos == slashPos - 1)
		{
			result = bytesToLong(payload, slashPos + 1, newLinePos);
		} else
		{
			result = bytesToLong(payload, dashPos + 1, slashPos);
		}

		if (result < 0)
			return 0;

		return result;
	}

	/**
	 * 
	 * Purpose: Get the content total range length from value from the TCP payload carrying an HTTP response.
	 * 
	 * @param payload
	 *            HTTP content payload
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return total range length, or zero if anything went wrong.
	 */
	public static long getContentRangeTotalFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_RESPONSE_CONTENT_RANGE_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return 0;

		fieldPos += HTTP_RESPONSE_CONTENT_RANGE_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return 0;

		int slashPos = indexOfBytesInBytes(payload, "/".getBytes(), fieldPos, newLinePos);
		if (slashPos < 0)
			return 0;

		long result = bytesToLong(payload, slashPos + 1, newLinePos);
		if (result < 0)
			return 0;

		return result;
	}

	/**
	 * Get the host name ("Host:" field) from a TCP payload carrying an HTTP request.
	 * 
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return Host name, or null if something went wrong.
	 */
	public static String getHostFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_REQUEST_HOST_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return null;

		fieldPos += HTTP_RESPONSE_HOST_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return null;

		String result = new String(payload, fieldPos, newLinePos - fieldPos);
		return result;
	}

	/**
	 * Get the referrer ("Referer:" field with "http://" URL prefix) from a TCP payload carrying an HTTP request.
	 * 
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return Full URL of referrer without the "http://" prefix, or null if not found or something went wrong.
	 */
	public static String getRefererFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_REQUEST_REFERER_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return null;

		fieldPos += HTTP_REQUEST_REFERER_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return null;

		String result = new String(payload, fieldPos, newLinePos - fieldPos);
		return result;
	}

	/**
	 * 
	 * Purpose: Extract the value of the header from the packet payload (up to the next new line \r)
	 * 
	 * Notes: This should be called in all the other 'specific' header string extractions
	 * 
	 * @param payload
	 * @param header
	 * @param startOffset
	 * @param endOffset
	 * @return
	 */
	public static String getHeaderValueFromPayload(byte[] payload, byte[] header, int startOffset, int endOffset)
	{
		int fieldPos = indexOfBytesInBytes(payload, header, startOffset, endOffset);
		if (fieldPos < 0)
			return null;

		fieldPos += header.length;

		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 1))
			return null;

		String result = new String(payload, fieldPos, newLinePos - fieldPos);
		return result;
	}

	/**
	 * Get the content-type from a TCP payload carrying an HTTP response.
	 * 
	 * @return Content-type or null if not found.
	 */
	public static String getContentTypeFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, HTTP_RESPONSE_CONTENT_TYPE_BYTES, startOffset, endOffset);
		if (fieldPos < 0)
			return null;

		fieldPos += HTTP_RESPONSE_CONTENT_TYPE_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 3))
			return null;

		String result = new String(payload, fieldPos, newLinePos - fieldPos);
		return result;
	}

	/**
	 * Get the attachment file name from a TCP payload carrying an HTTP response.
	 * 
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return File name or null if not found.
	 */
	public static String getAttachmentFileNameFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		String result = getValueFromPayload(HTTP_RESPONSE_ATTACH_FILE1_BYTES, payload, startOffset, endOffset);

		if (result == null)
			return getValueFromPayload(HTTP_RESPONSE_ATTACH_FILE2_BYTES, payload, startOffset, endOffset);

		return result;
	}

	/**
	 * Get the ETag value from a TCP payload carrying an HTTP response.
	 * 
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return Etag value or null if not found.
	 */
	public static String getEtagFromPayload(byte[] payload, int startOffset, int endOffset)
	{
		return getValueFromPayload(HTTP_RESPONSE_ETAG_BYTES, payload, startOffset, endOffset);
	}

	/**
	 * @param startOffset
	 *            0-based inclusive.
	 * @param endOffset
	 *            0-based exclusive. Should point to the first byte that should not be scanned. The byte before should
	 *            contain the last newline to scan, meaning that is should point to the second new line (\r\n).
	 * @return Value if field was found, or null if not. If the value is surounded with " or ', they are removed.
	 */
	private static String getValueFromPayload(byte[] fieldName, byte[] payload, int startOffset, int endOffset)
	{
		//
		// Get the position where the content length is written
		//
		int fieldPos = indexOfBytesInBytes(payload, fieldName, startOffset, endOffset);
		if (fieldPos < 0)
			return null;

		fieldPos += fieldName.length;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = indexOfBytesInBytes(payload, NEWLINE_BYTES, fieldPos, endOffset);
		if (newLinePos < (fieldPos + 3))
			return null;

		// If the name is in quote marks, then remove them
		if (payload[fieldPos] == '"' && payload[newLinePos - 1] == '"' || payload[fieldPos] == '\''
				&& payload[newLinePos - 1] == '\'')
		{
			fieldPos++;
			newLinePos--;
		}

		String result = new String(payload, fieldPos, newLinePos - fieldPos);
		return result;
	}

	/**
	 * Get the content-type from a TCP payload carrying an HTTP response.
	 * 
	 * @return Content-type or null if not found.
	 */
	public static String getContentTypeFromPayload(String payload)
	{
		if (payload == null)
			return null;

		//
		// Get the position where the content length is written
		//
		int fieldPos = payload.indexOf(HTTP_RESPONSE_CONTENT_TYPE);
		if (fieldPos < 0)
			return null;
		fieldPos += HTTP_RESPONSE_CONTENT_TYPE_LEN;

		//
		// Make sure the line ends with a new-line, and it contains a minimal
		// number of characters
		//
		int newLinePos = payload.indexOf('\r', fieldPos);
		if (newLinePos < (fieldPos + 1))
			return null;

		return payload.substring(fieldPos, newLinePos);
	}

	/**
	 * Get the referer host name from a TCP payload carrying an HTTP header.
	 * <p>
	 * The host name must follow the pattern. It may fail if the TCP payload is not complete and/or some kind of
	 * encoding was used.
	 * 
	 * @return Host name, or null if something went wrong.
	 */
	public static String getRefererHostFromPayload(String payload)
	{
		if (payload == null)
			return null;

		String hostName = null;

		//
		// Get the position where the host name is written
		//
		int hostPos = payload.indexOf(HTTP_REQUEST_REFERER);
		if (hostPos < 0)
			return null;
		hostPos += HTTP_REQUEST_REFERER_LEN;

		//
		// Make sure the referer line has a path
		//
		int slashPos = payload.indexOf('/', hostPos);
		if (slashPos < (hostPos + 4))
			return null;

		//
		// Isolate the host name
		//
		hostName = payload.substring(hostPos, slashPos);

		//
		// Remove port number if mentioned
		//
		int portIndex = hostName.indexOf(':');
		if (portIndex > 0)
		{
			hostName = hostName.substring(0, portIndex);
		}

		// Make sure it matches the host name rules
		if (!hostPattern.matcher(hostName).matches())
			return null;

		return hostName;
	}

	/**
	 * @param withHttpHeader
	 *            If the payload contains an HTTP header that needs to be skipped before the hash is calculated.
	 * @return Hash code of the first content bytes, or -1 if the packet contains all the header but only it, or zero if
	 *         it does not contain enough content or the header may continue on the next packet.
	 */
	public static long getStampFromPayload(byte[] payload, int stampBytes, boolean withHttpHeader)
	{
		int pos = 0;

		if (withHttpHeader)
		{
			int limit = payload.length - stampBytes - 4;
			for (int i = 30; i < limit; i++)
			{
				if (payload[i] == '\r' && payload[i + 1] == '\n' && payload[i + 2] == '\r' && payload[i + 3] == '\n')
				{
					pos = i + 4;
					break;
				}
			}

			// Did not find the end
			if (pos == 0)
			{
				// If the packet ends with the header-end
				if (payload[payload.length - 4] == '\r' && payload[payload.length - 3] == '\n'
						&& payload[payload.length - 2] == '\r' && payload[payload.length - 1] == '\n')
					return -1;

				return 0;
			}
		} else
		{
			// Make sure we have the minimum number of bytes in payload
			if (stampBytes > payload.length)
				return 0;
		}

		int stamp = Arrays.hashCode(Arrays.copyOfRange(payload, pos, pos + stampBytes));
		return 0x00000000ffffffffL & stamp;
	}

	/**
	 * Search for byte array in another byte array.
	 * 
	 * @param data
	 *            Payload bytes, with or without a header. In case of a header, use start offset parameter.
	 * @param startOffset
	 *            0-based start offset. Should be zero if the payload start at the beginning of the given array, or a
	 *            positive number that points to the payload start in the given array.
	 * @return HTTP response code (2xx to 5xx), or -1 if this can not be a legal HTTP response.
	 */
	public static int getHttpResponseCode(byte[] data, int startOffset)
	{
		// Verify that the given parameters make sense
		if (data == null || (data.length - startOffset) < 12)
			return -1;

		// Verify start with "HTTP/x.x " and then a space after 3 digits
		if (data[startOffset] != 'H' || data[startOffset + 1] != 'T' || data[startOffset + 2] != 'T'
				|| data[startOffset + 3] != 'P' || data[startOffset + 4] != '/' || data[startOffset + 6] != '.'
				|| data[startOffset + 8] != ' ' || data[startOffset + 12] != ' ')
			return -1;

		long result = bytesToLong(data, startOffset + 9, startOffset + 12);
		if (result < 200 || result >= 600)
			return -1;

		return (int) result;
	}

	/**
	 * Search for byte array in another byte array.
	 * 
	 * @param data
	 *            Payload bytes, with or without a header. In case of a header, use start offset parameter.
	 * @param searchBytes
	 *            The bytes to search for in the payload.
	 * @param startOffset
	 *            0-based start offset. Should be zero if the payload start at the beginning of the given array, or a
	 *            positive number that points to the payload start in the given array.
	 * @param endOffset
	 *            0-based end offset, exclusive. The total length of the payload is (end - start).
	 * @return 0-based offset of the search, or -1 if not found.
	 */
	public static int indexOfBytesInBytes(byte[] data, byte[] searchBytes, int startOffset, int endOffset)
	{
		// Verify that the given parameters make sense
		if (data == null || searchBytes == null || searchBytes.length <= 0 || data.length < endOffset
				|| (endOffset - startOffset) < searchBytes.length)
			return -1;

		//
		// Search
		//
		int lastPossibleParamPos = endOffset - searchBytes.length;
		boolean matched = false;
		for (int curOffset = startOffset; curOffset <= lastPossibleParamPos && !matched; curOffset++)
		{
			//
			// Loop to compare all the bytes
			//
			for (int i = 0; i < searchBytes.length; i++)
			{
				if (data[i + curOffset] != searchBytes[i])
				{
					matched = false;
					break;
				}
				matched = true;
			}

			// The loop was ended, because of a full match or no match
			if (matched)
				return curOffset;
		}

		return -1;
	}

	/**
	 * Search for byte array in another byte array.
	 * 
	 * @param data
	 *            Payload bytes, with or without a header. In case of a header, use start offset parameter.
	 * @param searchBytes
	 *            The bytes to search for in the payload.
	 * @param startOffset
	 *            0-based start offset. Should be zero if the payload start at the beginning of the given array, or a
	 *            positive number that points to the payload start in the given array.
	 * @param endOffset
	 *            0-based end offset, exclusive. The total length of the payload is (end - start).
	 * @return 0-based offset of the search, or -1 if not found.
	 */
	public static int indexOfByteInBytes(byte[] data, byte searchByte, int startOffset, int endOffset)
	{
		// Verify that the given parameters make sense
		if (data == null || data.length < endOffset || (endOffset - startOffset) < 1)
			return -1;

		//
		// Search
		//
		int lastPossibleParamPos = endOffset - 1;
		boolean matched = false;
		for (int curOffset = startOffset; curOffset <= lastPossibleParamPos && !matched; curOffset++)
		{
			if (data[curOffset] == searchByte)
				return curOffset;
		}

		return -1;
	}

	/**
	 * Isolate a number in byte array, and return its value.
	 * 
	 * @param data
	 *            Payload bytes, with or without a header. In case of a header, use start offset parameter.
	 * @param startOffset
	 *            0-based start offset. Should be zero if the payload start at the beginning of the given array, or a
	 *            positive number that points to the payload start in the given array.
	 * @param 0-based end offset, exclusive. The total length of the payload is (end - start).
	 * @return Parsed decimal number, or -1 on error.
	 */
	static long bytesToLong(byte[] data, int startOffset, int endOffset)
	{
		// Verify that the given parameters make sense
		if (data == null || data.length < endOffset || endOffset <= startOffset)
			return -1;

		long result = 0;

		for (int i = startOffset; i < endOffset; i++)
		{
			byte curByte = data[i];

			if (curByte == '\r' || curByte == '\n')
				return result;

			if (curByte == ' ')
			{
				// Space before the numbers is allowed, so skip it
				if (result == 0)
					continue;

				// Space after numbers means that this is the end
				return result;
			}

			// Accept only numbers
			if (curByte < '0' || curByte > '9')
				return -1;

			// Next digit
			result = result * 10 + (curByte - '0');
		}

		return result;
	}

	public static String paramAsString(byte[] text, int length, byte[] paramName, int startOffset)
	{
		int paramPos = indexOfBytesInBytes(text, paramName, startOffset, text.length);
		if (paramPos < 0)
			return null;

		int valuePos = paramPos + paramName.length;
		if (valuePos >= length)
			return null;

		boolean withSurr = text[valuePos] == '"';
		if (withSurr)
			valuePos++;

		//
		// Position of the last included char
		//
		int endPos;
		if (withSurr)
		{
			endPos = indexOfBytesInBytes(text, DOUBLE_QUOTE_BYTES, valuePos, length);
			if (endPos < 0)
				return null;
		} else
		{
			endPos = indexOfBytesInBytes(text, NEWLINE_BYTES, valuePos, length);
			if (endPos < 0)
			{
				endPos = indexOfBytesInBytes(text, SLASHN_BYTES, valuePos, length);
				if (endPos < 0)
				{
					endPos = length;
				}
			}
		}

		return new String(text, valuePos, endPos - valuePos);
	}

	/**
	 * Removes all URLs from "a href", and maybe also from "img src".
	 * 
	 * @param removeImages
	 *            If to remove also the "img src" URLs.
	 * @return HTML without the URLS that were found in quotes, leaving the quotes empty.
	 */
	public static byte[] htmlWithoutUrls(byte[] sourceHtml, int startOffset, int endOffset, boolean removeImages)
	{
		// Sanity check
		if (sourceHtml == null || endOffset > sourceHtml.length || startOffset >= endOffset || endOffset < 0)
			return null;

		LinkedList<Integer> removeOffsets = new LinkedList<Integer>();
		LinkedList<Integer> removeLengths = new LinkedList<Integer>();

		int removeLen = 0;
		byte[][] urlParamBytes = removeImages ? URL_PARAM_BYTES_LINKS_AND_IMAGES : URL_PARAM_BYTES_LINKS;

		//
		// Remember all src="
		//
		int offset = startOffset;
		while (true)
		{
			// Look for the two strings
			Couple<Integer> match = searchByteSeq(sourceHtml, urlParamBytes, offset, endOffset);
			if (match == null)
				break;

			offset = match.value1 + match.value2;

			// Check what we have before
			if (match.value1 > 0)
			{
				byte before = sourceHtml[match.value1 - 1];
				// We accept several characters before the HTML parameter
				if (before != '\n' && before != ' ' && before != '"' && before != '\'')
					continue;
			}

			// Check what we have after
			if (offset >= endOffset)
				break;

			byte after = sourceHtml[offset];
			// We accept several characters before the HTML parameter
			if (after != '\'' && after != '"')
				continue;

			// Start removing after the quote sign
			offset++;

			int quoteOffset = indexOfByteInBytes(sourceHtml, after, offset, endOffset);
			if (quoteOffset < offset)
				continue;

			removeOffsets.add(offset);
			int len = quoteOffset - offset;
			removeLen += len;
			removeLengths.add(len);
			offset = quoteOffset + 2;
		}

		//
		// Build the final result
		//
		byte[] result = new byte[endOffset - startOffset - removeLen];
		Iterator<Integer> itRemoveOffsets = removeOffsets.iterator();
		Iterator<Integer> itLength = removeLengths.iterator();

		int srcStartOffset = startOffset;
		int srcEndOffset = itRemoveOffsets.hasNext() ? itRemoveOffsets.next() : endOffset;
		int destOffset = 0;

		while (true)
		{
			// Copy before the remove-block
			int destLen = srcEndOffset - srcStartOffset;
			System.arraycopy(sourceHtml, srcStartOffset, result, destOffset, destLen);
			// Check if this was the end of the entire HTML
			if (!itLength.hasNext())
				break;
			// Skip the remove-block
			srcStartOffset = srcEndOffset + itLength.next();
			srcEndOffset = itRemoveOffsets.hasNext() ? itRemoveOffsets.next() : endOffset;
			destOffset += destLen;
		}

		return result;
	}

	private static Couple<Integer> searchByteSeq(byte[] buffer, byte[][] terms, int startOffset, int endOffset)
	{
		int minOffset = -1;
		int matchLen = 0;
		for (byte[] curTerm : terms)
		{
			int offset = indexOfBytesInBytes(buffer, curTerm, startOffset, endOffset);
			if (offset > 0 && (minOffset < 0 || offset < minOffset))
			{
				matchLen = curTerm.length;
				minOffset = offset;
			}
		}

		if (minOffset < 0)
			return null;

		return new Couple<Integer>(minOffset, matchLen);
	}
}
