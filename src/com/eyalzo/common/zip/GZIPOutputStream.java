/**
 * Copyright 2014 Eyal Zohar. All rights reserved.
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
package com.eyalzo.common.zip;

/**
 * @author Eyal Zohar
 * 
 */
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * This class implements a stream filter for writing compressed data in the GZIP file format. Changed by Eyal because
 * the original class only compresses and it does not provide services for writing bytes that were already compressed.
 * <p>
 * How to use: when created, the header is already written. Since we also need to write a trailer at the end, it is
 * important to call {@link #writeTrailer(int, int) at the end.
 * 
 * @author David Connelly
 * @author Eyal Zohar
 */
public class GZIPOutputStream
{
	private final OutputStream	out;
	/**
	 * GZIP header magic number.
	 */
	private final static int	GZIP_MAGIC	= 0x8b1f;
	/**
	 * The GZIP header according to specification.
	 */
	private final static byte[]	header		=
											{ (byte) GZIP_MAGIC, // Magic number (short)
			(byte) (GZIP_MAGIC >> 8), // Magic number (short)
			Deflater.DEFLATED, // Compression method (CM)
			0, // Flags (FLG)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Extra flags (XFLG)
			0								// Operating system (OS)
											};

	/**
	 * Creates a new output stream with the specified buffer size.
	 * 
	 * @param out
	 *            the output stream
	 * @exception IOException
	 *                If an I/O error has occurred.
	 */
	public GZIPOutputStream(OutputStream out) throws IOException
	{
		this.out = out;
		writeHeader();
	}

	/**
	 * Writes array of bytes to the compressed output stream. This method will block until all the bytes are written.
	 * 
	 * @param buf
	 *            the data to be written
	 * @param off
	 *            the start offset of the data
	 * @param len
	 *            the length of the data
	 * @param uncomprLen
	 *            Optional - see {@link #addUncompressedBytes(int)} as an alternative. The length in bytes of the
	 *            uncompressed data that is being represented by this compressed block. It is an important information
	 *            for the final trailer, that contains the count of the original bytes.
	 * @exception IOException
	 *                If an I/O error has occurred.
	 */
	public synchronized void write(byte[] buf, int off, int len, int uncomprLen) throws IOException
	{
		out.write(buf, off, len);
	}

	private void writeHeader() throws IOException
	{
		out.write(header);
	}

	/**
	 * Writes GZIP trailer to the output stream.
	 */
	public void writeTrailer(int crc, int uncompressedLength) throws IOException
	{
		// CRC-32 of the uncompressed data
		writeInt(crc);
		// Number of uncompressed bytes
		writeInt(uncompressedLength);
	}

	/**
	 * Writes integer in Intel byte order to the output stream.
	 */
	private void writeInt(int i) throws IOException
	{
		writeShort(i & 0xffff);
		writeShort((i >> 16) & 0xffff);
	}

	/**
	 * Writes short integer in Intel byte order to the output stream.
	 */
	private void writeShort(int s) throws IOException
	{
		out.write(s & 0xff);
		out.write((s >> 8) & 0xff);
	}
}
