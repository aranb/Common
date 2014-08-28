/**
 * Copyright 2012 Eyal Zohar. All rights reserved.
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
package com.eyalzo.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reading files made of a header line and separate textual data lines.
 */
public abstract class FilesReader
{
	/**
	 * Header line to be written as the first line in every file, and is also checked when reading.
	 */
	protected final String	headerLine;
	/**
	 * Absolute or relative file name.
	 */
	protected String		fileName;
	/**
	 * Time stamp of the last version read from disk.
	 */
	private long			fileTimeStamp;
	/**
	 * True only if the file exists and is not empty.
	 */
	protected boolean		valid;

	/**
	 * Constructs a new instance, reading the list of ranges from the file given in the path
	 * 
	 * @param fileName
	 *            Relative or absolute path.
	 * @param Header
	 *            line to be searched in the file's first line, as a preliminary test before the other lines are read.
	 */
	public FilesReader(String fileName, String headerLine)
	{
		// Replace newlines with spaces and remove surrounding spaces.
		this.headerLine = headerLine.trim().replace('\n', '_');
		this.fileName = fileName;
	}

	/**
	 * Read the file into the data structure, assuming that it is already sorted.
	 */
	protected void initializeList()
	{
		BufferedReader in = null;
		int lineSerial = 0;
		try
		{
			in = new BufferedReader(new FileReader(fileName));
			readFileHeader(in); // Reads the file header

			String currentReadLine; // Read the remaining file
			while ((currentReadLine = in.readLine()) != null)
			{
				lineSerial++;
				
				// Skip comments
				if (currentReadLine.startsWith("#") || currentReadLine.startsWith("'") || currentReadLine.startsWith("/"))
					continue;
				
				processFileLine(lineSerial, currentReadLine);
			}

			File file = new File(fileName);
			fileTimeStamp = file.lastModified();
			onInitializeListSuccess();
		} catch (FileNotFoundException f)
		{
			valid = false;
		} catch (Exception e)
		{
			e.printStackTrace();
			valid = false;
		} finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				} catch (IOException e)
				{
					valid = false;
				}
			}
		}
	}

	/**
	 * Read the configuration file again
	 * 
	 * @return True if managed to read the file, false otherwise
	 */
	public boolean reinitialize()
	{
		try
		{
			initializeList();
			return true;
		} catch (Exception e)
		{
			valid = false;
			return false;
		}
	}

	/**
	 * Override this if you need to take an action after the list has been initialized successfully.
	 */
	protected void onInitializeListSuccess()
	{
	}

	/**
	 * Reads the file header lines according to the predetermined format.
	 */
	protected void readFileHeader(BufferedReader in) throws IOException
	{
		String readHeader = in.readLine();
		if (!readHeader.equals(this.headerLine))
			throw new IOException("Bad header format. Read '" + readHeader + "' <> '" + this.headerLine + "'");
	}

	private boolean hasAnUpdatedFile()
	{
		File file = new File(fileName);
		long timeStamp = file.lastModified();
		return (timeStamp != fileTimeStamp);
	}

	public boolean updateListsIfNeeded()
	{
		if (hasAnUpdatedFile())
		{
			initializeList();
			return true;
		}
		return false;
	}

	/**
	 * @return Usage description of the file.
	 */
	protected abstract String getTypeStr();

	/**
	 * All sub-classes must implement that method that processes the data, one line at a time.
	 * 
	 * @param serial
	 *            1-based serial number of line, mostly for clear log messages when needed.
	 * @param data
	 *            Data itself, without any line terminators or null-characters.
	 * @return True if more lines should be read, or false if wish to terminate reading.
	 */
	protected abstract boolean processFileLine(int serial, String data);

	/**
	 * @return Simple header for files that can later be read by this class. File's type is the same as returned by
	 *         {@link #getTypeStr()}.
	 */
	public String generateHeader()
	{
		return headerLine + "\n";
	}

	public boolean getValid()
	{
		return valid;
	}
}
