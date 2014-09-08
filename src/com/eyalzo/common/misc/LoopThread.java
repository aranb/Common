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
package com.eyalzo.common.misc;

import com.eyalzo.common.webgui.DisplayTable;

/**
 * Wakes up every interval and performs an action.
 * 
 * @author Eyal Zohar
 */
public abstract class LoopThread extends Thread
{
	protected long		loopIntervalMillis;
	protected long		nextLoop;
	protected boolean	quit					= false;

	//
	// Statistics
	//
	/**
	 * Total number of loops, includes an incomplete loop that is running now.
	 */
	private long		statLoops;
	/**
	 * Loops that ran code that returned true, just for statistics.
	 */
	private long		statLoopsSuccess;
	/**
	 * Loops that caused an exception, which is surrounded by try/catch here.
	 */
	private long		statLoopsException;
	private String		statExceptionMessage	= "";
	private long		statLastLoopTimeMillis;

	public LoopThread(String name, long loopIntervalMillis)
	{
		super(name);
		this.loopIntervalMillis = loopIntervalMillis;
	}

	/**
	 * override this to run code on initialize, before going to sleep for the first time.
	 * 
	 * @return True on success, for success-loops counter.
	 */
	public abstract boolean runFirstTime();

	/**
	 * override this to run code on every loop after the first sleep.
	 * 
	 * @return True on success, for success-loops counter.
	 */
	public abstract boolean runLoop();

	@Override
	public void run()
	{
		while (true)
		{
			// Loop counter considers also loops that are incomplete
			statLoops++;
			// Next loop should not be subject to how long it took to perform the actions
			nextLoop = System.currentTimeMillis() + loopIntervalMillis;

			//
			// Run code
			//
			long before = System.nanoTime();
			boolean success;
			try
			{
				success = (statLoops == 1) ? runFirstTime() : runLoop();
				if (success)
					statLoopsSuccess++;
			} catch (Exception e)
			{
				statLoopsException++;
				statExceptionMessage = e.getMessage();
			}
			statLastLoopTimeMillis = (System.nanoTime() - before) / 1000000;

			//
			// Sleep
			//
			long sleepMillis = nextLoop - System.currentTimeMillis();
			if (sleepMillis > 10 && !quit)
			{
				try
				{
					Thread.sleep(sleepMillis);
				} catch (InterruptedException e)
				{
				}
			}

			// Check for the quit signal
			if (quit)
				return;
		}
	}

	public DisplayTable webGuiDetails()
	{
		long now = System.currentTimeMillis();

		DisplayTable result = new DisplayTable();

		result.addField("Loops", statLoops,
				"Number of loops started so far, not necessarily completed yet (the last one)");
		result.addField(1, "Successfull loops", statLoopsSuccess,
				"Number of loops that returned \"true\" for statistics");
		result.addField(1, "Exception loops", statLoopsException, statLoopsException == 0 ? "" : "red",
				"Number of loops that crashed with an exception", null);
		result.addField(2, "Exception message", statExceptionMessage,
				"Last exception message, or empty if never happened");
		result.addField("Next loop", String.format("%,d Sec", (this.nextLoop - now) / 1000),
				"When the next loop will begin");
		result.addField("Last loop time", String.format("%,d mSec", statLastLoopTimeMillis),
				"How long it took to complete the last loop");

		return result;
	}

	/**
	 * Mark a flag to quit and send an interrupt. Call {@link Thread#interrupt()} if no need to quit but just to run the
	 * loop now.
	 */
	public void quit()
	{
		this.quit = true;
		this.interrupt();
	}
}
