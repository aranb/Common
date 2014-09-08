package com.eyalzo.common.misc;

import static org.junit.Assert.*;

import org.junit.Test;

public class SamplingTest
{
	@Test
	public void testDeterministicReservoir()
	{
		int sampleSize = 5;
		int factor = sampleSize;

		for (int f = 1; f <= factor; f++)
		{
			for (int i = 1; i <= sampleSize; i++)
			{
				int itemIndex = (f - 1) * sampleSize + i;
				System.out.print(String.format("%d=%d ", itemIndex,
						Sampling.DeterministicReservoir(itemIndex, sampleSize)));
			}
			System.out.println();
		}

		int result = Sampling.DeterministicReservoir(6, sampleSize);
		assertEquals(1, result);
		result = Sampling.DeterministicReservoir(7, sampleSize);
		assertEquals(0, result);
	}
}
