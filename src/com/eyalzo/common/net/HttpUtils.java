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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.eyalzo.common.misc.Couple;

/**
 * @author Eyal Zohar
 */
public class HttpUtils
{
	private static byte[]		buffer							= new byte[100000];
	public static final int		DEFAULT_CONNECT_TIMEOUT_MILLIS	= 10000;
	public static final int		DEFAULT_READ_TIMEOUT_MILLIS		= 5000;
	public final static String	DEFAULT_USER_AGENT				= "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1";
	private final static int	HEATMAP_CODES[][]				= { { 0, 0, 255 }, { 0, 1, 255 }, { 0, 2, 255 },
			{ 0, 4, 255 }, { 0, 5, 255 }, { 0, 7, 255 }, { 0, 9, 255 }, { 0, 11, 255 }, { 0, 13, 255 }, { 0, 15, 255 },
			{ 0, 18, 253 }, { 0, 21, 251 }, { 0, 24, 250 }, { 0, 27, 248 }, { 0, 30, 245 }, { 0, 34, 243 },
			{ 0, 37, 240 }, { 0, 41, 237 }, { 0, 45, 234 }, { 0, 49, 230 }, { 0, 53, 226 }, { 0, 57, 222 },
			{ 0, 62, 218 }, { 0, 67, 214 }, { 0, 71, 209 }, { 0, 76, 204 }, { 0, 82, 199 }, { 0, 87, 193 },
			{ 0, 93, 188 }, { 0, 98, 182 }, { 0, 104, 175 }, { 0, 110, 169 }, { 0, 116, 162 }, { 7, 123, 155 },
			{ 21, 129, 148 }, { 34, 136, 141 }, { 47, 142, 133 }, { 60, 149, 125 }, { 71, 157, 117 }, { 83, 164, 109 },
			{ 93, 171, 100 }, { 104, 179, 91 }, { 113, 187, 92 }, { 123, 195, 73 }, { 132, 203, 63 }, { 140, 211, 53 },
			{ 148, 220, 43 }, { 156, 228, 33 }, { 163, 237, 22 }, { 170, 246, 11 }, { 176, 255, 0 }, { 183, 248, 0 },
			{ 188, 241, 0 }, { 194, 234, 0 }, { 199, 227, 0 }, { 204, 220, 0 }, { 209, 214, 0 }, { 213, 207, 0 },
			{ 217, 200, 0 }, { 221, 194, 0 }, { 224, 188, 0 }, { 227, 181, 0 }, { 230, 175, 0 }, { 233, 169, 0 },
			{ 236, 163, 0 }, { 238, 157, 0 }, { 240, 151, 0 }, { 243, 145, 0 }, { 244, 140, 0 }, { 246, 134, 0 },
			{ 248, 129, 0 }, { 249, 123, 0 }, { 250, 118, 0 }, { 251, 112, 0 }, { 252, 107, 0 }, { 253, 102, 0 },
			{ 254, 97, 0 }, { 255, 92, 0 }, { 255, 87, 0 }, { 255, 82, 0 }, { 255, 78, 0 }, { 255, 73, 0 },
			{ 255, 68, 0 }, { 255, 64, 0 }, { 255, 59, 0 }, { 255, 55, 0 }, { 255, 51, 0 }, { 255, 47, 0 },
			{ 255, 43, 0 }, { 255, 39, 0 }, { 255, 35, 0 }, { 255, 31, 0 }, { 255, 27, 0 }, { 255, 23, 0 },
			{ 255, 20, 0 }, { 255, 16, 0 }, { 255, 13, 0 }, { 255, 10, 0 }, { 255, 8, 0 }, { 255, 3, 0 } };
	private final static int	HEATMAP_CODES2[][]				= { { 0, 0, 255 }, { 0, 127, 255 }, { 0, 127, 127 },
			{ 127, 127, 0 }, { 255, 127, 0 }, { 255, 0, 0 }	};

	/**
	 * Send URL as command and/or download the content to nowhere. Useful for sending commands when the response is
	 * unimportant or to load a web server.
	 * 
	 * @param urlString
	 *            Given full URL string.
	 * @param log
	 *            Optional logger.
	 * @return True if URL is legal and content was downloaded successfully.
	 */
	public static boolean downloadUrlToNull(String urlString, boolean allowCompression, Logger log)
	{
		return downloadUrlToFile(urlString, null, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS, log);
	}

	/**
	 * @param urlString
	 *            Given full URL string.
	 * @param fileName
	 *            Optional file name. Can be empty or null if wish to download to null.
	 * @param log
	 *            Optional logger.
	 * @return True if URL is legal and content was downloaded successfully and saved to file (if file name was given).
	 */
	public static boolean downloadUrlToFile(String urlString, String fileName, Logger log)
	{
		return downloadUrlToFile(urlString, fileName, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS, log);
	}

	/**
	 * @param urlString
	 *            Given full URL string.
	 * @param fileName
	 *            Optional file name. Can be empty or null if wish to download to null.
	 * @param connectTimeoutMillis
	 *            Connect timeout in milliseconds.
	 * @param readTimeoutMillis
	 *            Read timeout in milliseconds.
	 * @param log
	 *            Optional logger.
	 * @return True if URL is legal and content was downloaded successfully and saved to file (if file name was given).
	 */
	public static boolean downloadUrlToFile(String urlString, String fileName, int connectTimeoutMillis,
			int readTimeoutMillis, Logger log)
	{
		return downloadUrlToFile(urlString, fileName, connectTimeoutMillis, readTimeoutMillis, false, log);
	}

	/**
	 * @param urlString
	 *            Given full URL string.
	 * @param fileName
	 *            Optional file name. Can be empty or null if wish to download to null.
	 * @param connectTimeoutMillis
	 *            Connect timeout in milliseconds. If not positive then uses default
	 *            {@link #DEFAULT_CONNECT_TIMEOUT_MILLIS}.
	 * @param readTimeoutMillis
	 *            Read timeout in milliseconds. If not positive then uses default {@link #DEFAULT_READ_TIMEOUT_MILLIS}.
	 * @param log
	 *            Optional logger.
	 * @return True if URL is legal and content was downloaded successfully and saved to file (if file name was given).
	 */
	public static boolean downloadUrlToFile(String urlString, String fileName, int connectTimeoutMillis,
			int readTimeoutMillis, boolean allowCompression, Logger log)
	{
		//
		// Build the full URL
		//
		URL url;
		try
		{
			url = new URL(urlString);
		} catch (MalformedURLException e)
		{
			if (log != null)
				log.log(Level.WARNING, "Malformed URL exception: " + e);
			return false;
		}

		if (log != null)
			log.log(Level.FINER, "Sending " + url);

		// Default timeouts
		if (connectTimeoutMillis <= 0)
			connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
		if (readTimeoutMillis <= 0)
			readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

		//
		// Connect only, meaning 3-way handshake
		//
		HttpURLConnection connection = sendHttpRequest(url, connectTimeoutMillis, readTimeoutMillis, allowCompression,
				log);
		if (connection == null)
		{
			if (log != null)
				log.log(Level.FINE, "Failed to connect " + url.getHost());
			return false;
		}

		// long lastModifiedUrl = connection.getLastModified();
		// System.out.println(String.format("Last modified URL: %,d",
		// lastModifiedUrl));

		//
		// Prepare the file
		//
		BufferedOutputStream out = null;
		if (fileName != null && !fileName.isEmpty())
		{
			try
			{
				out = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			} catch (FileNotFoundException e)
			{
				if (log != null)
					log.log(Level.FINE, "Failed to create file " + fileName);

				return false;
			}

			if (log != null)
				log.log(Level.FINER, "Writing " + fileName);
		}

		//
		// Prepare input stream
		//
		InputStream is = null;
		try
		{
			// Only after this call the GET request is sent
			is = connection.getInputStream();
		} catch (IOException e)
		{
			if (log != null)
				log.log(Level.FINE, "Failed to get input stream " + url + "\n" + e);
			try
			{
				if (out != null)
					out.close();
			} catch (Exception e1)
			{
			}
			return false;
		}

		//
		// Read loop
		//
		int readBytes = 0;
		try
		{
			while ((readBytes = is.read(buffer)) != -1)
			{
				if (out != null)
					out.write(buffer, 0, readBytes);
			}
		} catch (IOException e)
		{
			if (log != null)
				log.log(Level.FINE, "Failed to read from " + url + "\n" + e);
			try
			{
				if (out != null)
					out.close();
			} catch (Exception e1)
			{
			}
			return false;
		}

		return true;
	}

	/**
	 * @return First line of textual response.
	 */
	public static String readUrlText(String urlString, int connectTimeoutMillis, int readTimeoutMillis, Logger log)
	{
		//
		// Build the full URL
		//
		URL url;
		try
		{
			url = new URL(urlString);
		} catch (MalformedURLException e)
		{
			if (log != null)
				log.log(Level.WARNING, "Malformed URL exception: " + e);
			return null;
		}

		if (log != null)
			log.log(Level.FINER, "Sending " + url);

		//
		// Connect and send the request
		//
		HttpURLConnection connection = sendHttpRequest(url, connectTimeoutMillis, readTimeoutMillis, log);
		if (connection == null)
		{
			if (log != null)
				log.log(Level.FINE, "Failed to connect " + url.getHost());
			return null;
		}

		//
		// Open input stream and read
		//
		BufferedReader reader;
		try
		{
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			reader = new BufferedReader(in);
		} catch (Exception e)
		{
			if (e instanceof java.net.SocketTimeoutException)
			{
				if (log != null)
					log.log(Level.INFO, "Command " + urlString + ": Read timeout after " + readTimeoutMillis + " mSec");
			} else
			{
				if (log != null)
					log.log(Level.INFO, "Command " + urlString + ": Failed to get input stream:\n" + e);
			}
			return null;
		}

		//
		// Read the line
		//
		String line;
		try
		{
			line = reader.readLine();
		} catch (IOException e)
		{
			return null;
		}

		//
		// Close the input
		//
		try
		{
			reader.close();
		} catch (IOException e)
		{
		}

		return line;
	}

	/**
	 * @return All the lines of a textual response.
	 */
	public static LinkedList<String> readUrlTextLines(String urlString, Logger log)
	{
		return readUrlTextLines(urlString, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS, log);
	}

	/**
	 * @return All the lines of a textual response.
	 */
	public static LinkedList<String> readUrlTextLines(String urlString, int connectTimeoutMillis,
			int readTimeoutMillis, Logger log)
	{
		//
		// Build the full URL
		//
		URL url;
		try
		{
			url = new URL(urlString);
		} catch (MalformedURLException e)
		{
			if (log != null)
				log.log(Level.WARNING, "Malformed URL exception: " + e);
			return null;
		}

		if (log != null)
			log.log(Level.FINER, "Sending " + url);

		//
		// Connect and send the request
		//
		HttpURLConnection connection = sendHttpRequest(url, connectTimeoutMillis, readTimeoutMillis, log);
		if (connection == null)
		{
			if (log != null)
				log.log(Level.FINE, "Failed to connect " + url.getHost());
			return null;
		}

		//
		// Open input stream and read
		//
		BufferedReader reader;
		LinkedList<String> result;
		try
		{
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			reader = new BufferedReader(in);
			result = new LinkedList<String>();
		} catch (Exception e)
		{
			if (e instanceof java.net.SocketTimeoutException)
			{
				if (log != null)
					log.log(Level.INFO, "Command " + urlString + ": Read timeout after " + readTimeoutMillis + " mSec");
			} else
			{
				if (log != null)
					log.log(Level.INFO, "Command " + urlString + ": Failed to get input stream:\n" + e);
			}
			return null;
		}

		//
		// Read the lines
		//
		while (true)
		{
			String line;
			try
			{
				line = reader.readLine();
			} catch (IOException e)
			{
				break;
			}
			if (line == null)
				break;
			result.add(line);
		}

		//
		// Close the input
		//
		try
		{
			reader.close();
		} catch (IOException e)
		{
		}

		return result;
	}

	/**
	 * Send HTTP GET request.
	 * <p>
	 * Opens an HTTP connection with timeouts both for connect and for read and sends a request.
	 * 
	 * @param urlFile
	 *            The file part of the URL.
	 * @param readTimeoutMillis
	 *            Read-timeout to set, in milli-seconds.
	 * @return Null if failed, or the connection in order to be able to read from it.
	 */
	private static HttpURLConnection sendHttpRequest(URL url, int connectTimeoutMillis, int readTimeoutMillis,
			Logger log)
	{
		return sendHttpRequest(url, connectTimeoutMillis, readTimeoutMillis, false, log);
	}

	/**
	 * Send HTTP GET request.
	 * <p>
	 * Opens an HTTP connection with timeouts both for connect and for read and sends a request.
	 * 
	 * @param urlFile
	 *            The file part of the URL.
	 * @param readTimeoutMillis
	 *            Read-timeout to set, in milli-seconds.
	 * @param compressionAllowed
	 *            If true it send an additional header that allows the response to be compressed with gzip or deflate.
	 * @return Null if failed, or the connection in order to be able to read from it.
	 */
	public static HttpURLConnection sendHttpRequest(URL url, int connectTimeoutMillis, int readTimeoutMillis,
			boolean compressionAllowed, Logger log)
	{
		return sendHttpRequest(url, connectTimeoutMillis, readTimeoutMillis,
				compressionAllowed ? "gzip,deflate" : null, log);
	}

	/**
	 * Send HTTP GET request.
	 * <p>
	 * Opens an HTTP connection with timeouts both for connect and for read and sends a request.
	 * 
	 * @param urlFile
	 *            The file part of the URL.
	 * @param readTimeoutMillis
	 *            Read-timeout to set, in milli-seconds.
	 * @param acceptEncodingField
	 *            If not empty/null it send an additional header that allows the response to be compressed with the
	 *            specified formats. For example "gzip,deflate".
	 * @return Null if failed, or the connection in order to be able to read from it.
	 */
	public static HttpURLConnection sendHttpRequest(URL url, int connectTimeoutMillis, int readTimeoutMillis,
			String acceptEncodingField, Logger log)
	{
		//
		// Prepare the connection
		//
		HttpURLConnection connection;
		try
		{
			connection = (HttpURLConnection) url.openConnection();
		} catch (IOException e)
		{
			if (log != null)
				log.log(Level.WARNING, "Failed to prepare connection URL " + url + ":\n" + e);
			return null;
		}

		connection.setConnectTimeout(connectTimeoutMillis);
		connection.setReadTimeout(readTimeoutMillis);
		connection.setUseCaches(false);
		connection.addRequestProperty("Connection", "close");
		connection.addRequestProperty("User-Agent", DEFAULT_USER_AGENT);

		// If asked to use compression
		if (acceptEncodingField != null && !acceptEncodingField.isEmpty())
			connection.setRequestProperty("Accept-Encoding", acceptEncodingField);

		//
		// Connect, meaning 3-way handshake
		//
		try
		{
			connection.connect();
		} catch (SocketTimeoutException e1)
		{
			if (log != null)
				log.log(Level.WARNING, "Failed to connect. Timeout after " + connectTimeoutMillis + " mSec. URL " + url);
			return null;
		} catch (IOException e)
		{
			if (log != null)
				log.log(Level.WARNING, "Failed to connect. URL " + url + ":\n{1}" + e);
			return null;
		}

		return connection;
	}

	/**
	 * Search for a parameter in text, written as key=value or key="value", and return its value.
	 * 
	 * @param text
	 *            Text to scan.
	 * @param paramName
	 *            Name of parameter, case sensitive.
	 * @param defaultResult
	 *            Default result to return on error.
	 * @return Value if found and parsed to valid number, or default if any kind of problem occurred.
	 */
	public static long paramAsLong(String text, String paramName, long defaultResult)
	{
		String value = paramAsString(text, paramName);
		if (value == null)
			return defaultResult;

		long result;
		try
		{
			result = Long.parseLong(value);
		} catch (NumberFormatException e)
		{
			return defaultResult;
		}

		return result;
	}

	/**
	 * Search for a parameter in text, written as key=value or key="value", and return its value.
	 * 
	 * @param text
	 *            Text to scan.
	 * @param paramName
	 *            Name of parameter, case sensitive.
	 * @return Value if found, or null if any kind of problem occurred.
	 */
	public static String paramAsString(String text, String paramName)
	{
		int paramPos = text.indexOf(paramName + "=");
		if (paramPos < 0)
			return null;

		int valuePos = paramPos + paramName.length() + 1;
		if (valuePos >= text.length())
			return null;

		boolean withSurr = text.charAt(valuePos) == '"';
		if (withSurr)
			valuePos++;

		//
		// Position of the last included char
		//
		int endPos;
		if (withSurr)
		{
			endPos = text.indexOf('"', valuePos);
			if (endPos < 0)
				return null;
		} else
		{
			endPos = text.indexOf(' ', valuePos);
			if (endPos < 0)
			{
				endPos = text.length();
			}
		}

		return text.substring(valuePos, endPos);
	}

	/**
	 * @param hostname
	 *            Host name.
	 * @return Domain of the host name, or null if there is any problem with the host name. If the last part starts with
	 *         a number, it assumes that this is an IP, and returns the entire hostname as domain.
	 */
	public static String getDomainFromHostname(String hostname)
	{
		return getDomainFromHostname(hostname, true);
	}

	/**
	 * @param hostname
	 *            Host name. Can contain a port number.
	 * @param allowNumeric
	 *            False if host name that ends with a digit is not considered as domain.
	 * @return Domain of the host name, or null if there is any problem with the host name. If the last part starts with
	 *         a number, it assumes that this is an IP, and returns the entire host name as domain only if it is allowed
	 *         according to the given parameters.
	 */
	public static String getDomainFromHostname(String hostname, boolean allowNumeric)
	{
		if (hostname == null)
			return null;

		String split[] = hostname.split("\\.");
		// Minimum is two parts
		if (split.length < 2)
			return null;

		//
		// Check for top-level-domains
		//

		// Remove port number if specified
		String lastPart = split[split.length - 1].split("\\:")[0];
		String beforeLastPart = split[split.length - 2];

		// Two-parts only
		if (split.length == 2)
			return beforeLastPart + "." + lastPart;

		// Top-level with no option for three-parts
		if (lastPart.equals("com") || lastPart.equals("net") || lastPart.equals("info") || lastPart.equals("biz")
				|| lastPart.equals("name") || lastPart.equals("org") || lastPart.equals("pro")
				|| lastPart.equals("gov") || lastPart.equals("edu") || lastPart.equals("tv") || lastPart.equals("pl"))
			return beforeLastPart + "." + lastPart;

		// Countries with optional two-parts
		if (lastPart.equals("ru") || lastPart.equals("fr") || lastPart.equals("cn") || lastPart.equals("br")
				|| lastPart.equals("hu") || lastPart.equals("it") || lastPart.equals("es") || lastPart.equals("at")
				|| lastPart.equals("pk") || lastPart.equals("ca") || lastPart.equals("za") || lastPart.equals("be")
				|| lastPart.equals("pt") || lastPart.equals("ch") || lastPart.equals("ro") || lastPart.equals("se")
				|| lastPart.equals("de") || lastPart.equals("ie") || lastPart.equals("nl") || lastPart.equals("my")
				|| lastPart.equals("no") || lastPart.equals("dk"))
		{
			if (!beforeLastPart.equals("co") && !beforeLastPart.equals("com") && !beforeLastPart.equals("gov")
					&& !beforeLastPart.equals("edu") && !beforeLastPart.equals("net"))
				return beforeLastPart + "." + lastPart;
		}

		// Numeric IP? does not have to be accurate, so it is a quick check
		if (Character.isDigit(lastPart.charAt(0)))
			return allowNumeric ? hostname : null;

		return split[split.length - 3] + "." + split[split.length - 2] + "." + lastPart;
	}

	public static String getColourHeatmapTemp(long value, long minVal, long maxVal)
	{
		if (value > maxVal || value < minVal || minVal >= maxVal)
			return "";

		int index = (int) ((HEATMAP_CODES2.length - 1) * ((double) (value - minVal)) / (maxVal - minVal));
		int rgb[] = HEATMAP_CODES2[index];
		return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
	}

	public static String getHtmlWithoutUrls(String sourceHtml)
	{
		StringBuffer buffer = new StringBuffer();

		int curOffset = 0;
		while (true)
		{
			int imgOffset = sourceHtml.indexOf(" src=\"", curOffset);
			int linkOffset = sourceHtml.indexOf(" href=\"", curOffset);
			int useOffset;
			if (imgOffset < 0)
			{
				if (linkOffset < 0)
					break;
				useOffset = linkOffset + 7;
			} else if (linkOffset < 0)
			{
				useOffset = imgOffset + 6;
			} else if (imgOffset < linkOffset)
			{
				useOffset = imgOffset + 6;
			} else
			{
				useOffset = linkOffset + 7;
			}

			String url = getUrl(sourceHtml, useOffset);
			if (url == null)
				break;
			// Add everything until the url itself, with the opening quotes
			buffer.append(sourceHtml.substring(curOffset, useOffset));
			// Update offset to point to where the closing quotes are
			curOffset = useOffset + url.length();
			continue;
		}
		// Closing block
		buffer.append(sourceHtml.substring(curOffset, sourceHtml.length()));
		return buffer.toString();
	}

	private static String getUrl(String sourceHtml, int i)
	{
		int endIndex = sourceHtml.indexOf("\"", i);
		if (endIndex < 0)
			return null;
		return sourceHtml.substring(i, endIndex);
	}
}
