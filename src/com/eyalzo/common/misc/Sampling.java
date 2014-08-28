/**
 * Copyright 2011 Eyal Zohar. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY EYAL ZOHAR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL EYAL ZOHAR OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Eyal Zohar.
 */
package com.eyalzo.common.misc;

/**
 * @author Eyal Zohar
 */
public class Sampling
{
	/**
	 * @param itemIndex
	 *            1-based index of the current item.
	 * @param sampleSize
	 *            Number of samples to collect.
	 * @return Zero if no need to collect this item, or a positive index in the
	 *         range (1..sampleSize) if need to collect.
	 */
	public static int DeterministicReservoir(int itemIndex, int sampleSize)
	{
		// Sanity
		if (sampleSize <= 0 || itemIndex < 1)
			return 0;

		if (itemIndex <= sampleSize)
			return itemIndex;

		int quotient = (itemIndex - 1) / sampleSize;
		int quotientRemainder = (itemIndex - 1 - quotient * sampleSize) % (quotient + 1);
		if (quotientRemainder != 0)
			return 0;

		return ((itemIndex - 1) % sampleSize) + 1;
	}
}
