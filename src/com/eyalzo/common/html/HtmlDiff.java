package com.eyalzo.common.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Perform diff between two HTMLs after they were parsed. The goal is to sync the parts in a way that helps to answer a
 * simple question per text part of the original: who is the equivalent text on the other HTML, if such text exists.
 * 
 * @author Eyal Zohar
 */
public class HtmlDiff
{
	/**
	 * Safe sync points where an exact match was found between the original and the other HTML. Number of items equals
	 * to the number of parts in the original, and the values are the offsets that need to be added to each part that
	 * has a match, in order to access the matching part.
	 */
	public ArrayList<Integer>			syncOffsets;
	/**
	 * Offsets of text parts that are not safe because the text differs. These items are typically based on less
	 * accurate estimations, using the sync points.
	 */
	public HashMap<Integer, Integer>	textOffsets;

	/**
	 * @param html1
	 *            The basis for the comparison. The result's size is exactly the number of parts in this parsed HTML.
	 * @param html2
	 *            The other parsed HTML to try to match to this.
	 * @param minAnchorTextLen
	 *            The minimal length of a text or HTML tag to be considered as an anchor.
	 * @param maxOffset
	 *            The maximal forward offset in number of parts that we will consider for anchor.
	 * @return List of offsets to anchors in the other list of parts, containing number of parts for strong anchors and
	 *         nulls for empty, short text/HTML or text that do not fully match. Number of elements in the returned list
	 *         would always be exactly the number of parts in this parsed HTML.
	 */
	public HtmlDiff(ParsedHtml html1, ParsedHtml html2, int minAnchorTextLen, int maxOffset)
	{
		syncOffsets = new ArrayList<Integer>(html1.parts.size());
		textOffsets = new HashMap<Integer, Integer>();

		// Index of the other, taking offset into account already
		int oIndex = 0;
		// Index of the last time something was completely wrong, typically HTML mismatch
		int lastBreakIndex = -1;
		// Remember the previous strong offset, so we can realize if a new offset was found
		int lastStrongOffset = -1;
		// Consider beginning as a strong sync, for HTMLs that start with title after "<html><head><title>"
		boolean isStrongSync = true;
		// Parts of the other, for code readability
		LinkedList<HtmlPart> oParts = html2.parts;

		for (int index = 0; index < html1.parts.size(); index++)
		{
			// If the other is shorter or exhausted, then there is nothing to say about the match
			if (oIndex >= oParts.size())
			{
				syncOffsets.add(null);
				continue;
			}

			HtmlPart curPart = html1.parts.get(index);

			// Do not use empty text or short text/HTML as an anchor
			if (curPart.type == HtmlPartType.TEXT_EMPTY || curPart.text.length() < minAnchorTextLen)
			{
				// If still in sync because of former strong anchor, then try to continue
				if (isStrongSync)
				{
					HtmlPart oCurPart = oParts.get(oIndex);
					if (curPart.equals(oCurPart))
					{
						lastStrongOffset = oIndex - index;
						syncOffsets.add(lastStrongOffset);
						oIndex++;
						continue;
					}

					// If this is a text on both sides, we can assume a weak sync
					if (curPart.type == HtmlPartType.TEXT_REAL && oCurPart.type == HtmlPartType.TEXT_REAL)
						textOffsets.put(index, oIndex - index);
				}

				// TODO consider setting the lastBreakIndex here, if short HTML mismatch

				isStrongSync = false;
				syncOffsets.add(null);
				continue;
			}

			// We now have a part with significant text or HTML tag that we want to match

			// Look at the other list from the current index until a max offset or end
			int oMaxIndex = Math.min(oIndex + maxOffset, oParts.size() - 1);
			boolean match = false;
			// Get the other part
			for (int i = oIndex; i <= oMaxIndex; i++)
			{
				HtmlPart oCurPart = oParts.get(i);
				// On exact match
				if (curPart.equals(oCurPart))
				{
					syncOffsets.add(i - index);
					isStrongSync = true;
					// Next time, will start search at the next part
					oIndex = i + 1;
					match = true;
					break;
				}
			}

			if (match)
			{
				// Try to fix backward some weak matching texts

				System.out.println("resync " + (index + 1));
				// Do this routine only if the new offset equals the last strong one
				if (lastStrongOffset == syncOffsets.get(syncOffsets.size() - 1))
				{
					for (int i = lastBreakIndex + 1; i < index; i++)
					{
						if (html1.parts.get(i).type == HtmlPartType.TEXT_REAL
								&& html2.parts.get(i + lastStrongOffset).type == HtmlPartType.TEXT_REAL)
						{
							System.out.println("fix " + (i + 1));
							textOffsets.put(i, lastStrongOffset);
						}
					}
				} else
				{
					// This is a new offset
					lastStrongOffset = syncOffsets.get(syncOffsets.size() - 1);
				}
				// Do not try to fix beyond this point
				lastBreakIndex = index;
				continue;
			}

			// If nothing match then do not move the other index

			// Check if we can mark as weak sync for the last time, until a strong sync is found again
			if (isStrongSync && curPart.type == HtmlPartType.TEXT_REAL)
			{
				HtmlPart oCurPart = oParts.get(oIndex);
				// If this is a text on both sides, we can assume a weak sync
				if (oCurPart.type == HtmlPartType.TEXT_REAL)
				{
					System.out.println("weak " + (index + 1));
					textOffsets.put(index, oIndex - index);
				}
			}
			// Significant break?
			if (curPart.type == HtmlPartType.HTML_ELEMENT)
			{
				lastBreakIndex = index;
				System.out.println("break " + (index + 1));
			}
			// This point is not a strong sync anymore
			syncOffsets.add(null);
			isStrongSync = false;
		}
	}

	/**
	 * Compare a given main HTML to several others, and return the indexes of all text parts, along with indexes of
	 * matching text parts in the other HTMLs. When text is completely identical, it returns {@link Integer#MAX_VALUE}
	 * instead of the index of the other, to save string compare time.
	 * 
	 * @param mainHtml
	 *            The Base HTML that is being compared with all the others.
	 * @param otherHtmls
	 *            The other parsed HTMLs to try to match to this.
	 * @param minAnchorTextLen
	 *            The minimal length of a text or HTML tag to be considered as an anchor.
	 * @param maxOffset
	 *            The maximal forward offset in number of parts that we will consider for anchor.
	 * @return List of text indexes (0-based) in the given HTML. Each text part (index) has a secondary list of matching
	 *         indexes in the other HTMLs. When text is completely identical, it returns {@link Integer#MAX_VALUE}
	 *         instead of the index of the other, to save string compare time. The secondary list has exactly the same
	 *         number of items as the number of other HTMLs, ordered by the same order as the given list.
	 */
	public static LinkedHashMap<Integer, LinkedList<Integer>> compareGetTextIndexes(ParsedHtml mainHtml,
			Collection<ParsedHtml> otherHtmls, int minAnchorTextLen, int maxOffset)
	{
		// Prepare the result
		LinkedHashMap<Integer, LinkedList<Integer>> result = new LinkedHashMap<Integer, LinkedList<Integer>>();

		// Compare the main HTML to all others
		HashMap<ParsedHtml, HtmlDiff> othersPartsOffsets = new HashMap<ParsedHtml, HtmlDiff>();
		// Compare with each of the other HTMLs
		for (ParsedHtml curParsedHtml : otherHtmls)
		{
			// Get current offsets
			HtmlDiff curPartsOffsets = new HtmlDiff(mainHtml, curParsedHtml, minAnchorTextLen, maxOffset);
			othersPartsOffsets.put(curParsedHtml, curPartsOffsets);
		}

		//
		// Look for others' matches per text
		//

		// 0-based index of the current part
		int partIndex = -1;
		for (HtmlPart curPart : mainHtml.parts)
		{
			partIndex++;
			if (curPart.type != HtmlPartType.TEXT_REAL)
				continue;

			// The others
			LinkedList<Integer> othersParts = new LinkedList<Integer>();
			// Handle each of the others' current part, if there is a match
			for (ParsedHtml curOtherHtml : otherHtmls)
			{
				// All offsets between this and the current other
				HtmlDiff curPartsOffsets = othersPartsOffsets.get(curOtherHtml);
				// See if there is a clear offset between this text and the current-other's text
				Integer strongOffset = curPartsOffsets.syncOffsets.get(partIndex);
				if (strongOffset != null)
				{
					// Identical text marked as max value
					othersParts.add(Integer.MAX_VALUE);
					continue;
				}
				// See if there is a weak sync, meaning that the text may be different (or short and identical)
				Integer weakOffset = curPartsOffsets.textOffsets.get(partIndex);
				// Add to the list of matching (if) parts per this current part
				othersParts.add(weakOffset == null ? null : partIndex + weakOffset);
			}

			// Add the list of all matching per this current text
			result.put(partIndex, othersParts);
		}

		return result;
	}

}
