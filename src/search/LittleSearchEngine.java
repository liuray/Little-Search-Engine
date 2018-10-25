package search;

import java.io.*;
import java.util.*;

class Occurrence {
	String document;
	int frequency;

	public Occurrence(String doc, int freq) {
		document = doc;
		frequency = freq;
	}

	public String toString() {
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of
 * documents in which it occurs, with frequency of occurrence in each document.
 * Once the index is built, the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine {

	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and
	 * the associated value is an array list of all occurrences of the keyword
	 * in documents. The array list is maintained in descending order of
	 * occurrence frequencies.
	 */
	HashMap<String, ArrayList<Occurrence>> keywordsIndex;

	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String, String> noiseWords;

	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String, ArrayList<Occurrence>>(1000, 2.0f);
		noiseWords = new HashMap<String, String>(100, 2.0f);
	}

	/**
	 * This method indexes all keywords found in all the input documents. When
	 * this method is done, the keywordsIndex hash table will be filled with all
	 * keywords, each of which is associated with an array list of Occurrence
	 * objects, arranged in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile
	 *            Name of file that has a list of all the document file names,
	 *            one name per line
	 * @param noiseWordsFile
	 *            Name of file that has a list of noise words, one noise word
	 *            per line
	 * @throws FileNotFoundException
	 *             If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile)
			throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.put(word, word);
		}

		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String, Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}

	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of
	 * keyword occurrences in the document. Uses the getKeyWord method to
	 * separate keywords from other words.
	 * 
	 * @param docFile
	 *            Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated
	 *         with an Occurrence object
	 * @throws FileNotFoundException
	 *             If the document file is not found on disk
	 */
	public HashMap<String, Occurrence> loadKeyWords(String docFile)
			throws FileNotFoundException {
		if (docFile == null) {
			return null;
		}
		HashMap<String, Occurrence> keywordsMap = new HashMap<String, Occurrence>();
		Scanner sc = new Scanner(new File(docFile));
		while (sc.hasNext()) {
			String keyword = getKeyWord(sc.next());
			if (keyword == null) {
				continue;
			} else {
				if (!keywordsMap.containsKey(keyword)) {
					Occurrence keywordOccurrence = new Occurrence(docFile, 1);
					keywordsMap.put(keyword, keywordOccurrence);
				} else {
					keywordsMap.get(keyword).frequency++;
				}
			}
		}
		return keywordsMap;
	}

	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document must
	 * be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash
	 * table. This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws
	 *            Keywords hash table for a document
	 */
	public void mergeKeyWords(HashMap<String, Occurrence> kws) {
		if (kws == null) {
			return;
		}
		for (Map.Entry<String, Occurrence> entry : kws.entrySet()) {
			String keyword = entry.getKey();
			if (keywordsIndex.get(keyword) == null) {
				ArrayList<Occurrence> occList = new ArrayList<Occurrence>();
				occList.add(entry.getValue());
				keywordsIndex.put(keyword, occList);
			} else {
				ArrayList<Occurrence> occs = keywordsIndex.get(keyword);
				occs.add(entry.getValue());
				insertLastOccurrence(occs);
			}
		}
	}

	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped
	 * of any TRAILING punctuation, consists only of alphabetic letters, and is
	 * not a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word
	 *            Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) {
		if (word == null) {
			return null;
		}
		String lowerCaseWord = word.toLowerCase();
		final String PUNCTUATION = ".,?:;!";
		final String SYMBOL = "abcdefghijklmnopqrstuvwxyz";
		char[] wordChar = lowerCaseWord.toCharArray();
		boolean symbolMatched = false;
		int i = wordChar.length;
		for (int j = wordChar.length - 1; j >= 0; j--) {
			if (SYMBOL.indexOf(wordChar[j]) >= 0) {
				symbolMatched = true;
				continue;
			} else if (PUNCTUATION.indexOf(wordChar[j]) >= 0) {
				if (!symbolMatched) {
					i = j;
					continue;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		String keyword = lowerCaseWord.substring(0, i);
		if (noiseWords.get(keyword) != null) {
			return null;
		}
		return keyword;
	}

	/**
	 * Inserts the last occurrence in the parameter list in the correct position
	 * in the same list, based on ordering occurrences on descending
	 * frequencies. The elements 0..n-2 in the list are already in the correct
	 * order. Insertion of the last element (the one at index n-1) is done by
	 * first finding the correct spot using binary search, then inserting at
	 * that spot.
	 * 
	 * @param occs
	 *            List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the
	 *         binary search process, null if the size of the input list is 1.
	 *         This returned array list is only used to test your code - it is
	 *         not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		if (occs == null) {
			return null;
		}
		ArrayList<Integer> midPointIndex = new ArrayList<Integer>();
		int length = occs.size();
		int lastFrequency = occs.get(length - 1).frequency;
		if (length == 1) {
			return null;
		} else {
			int low = 0;
			int high = length - 2;
			int midFrequency;
			while (low <= high) {
				int midPoint = (low + high) / 2;
				midFrequency = occs.get(midPoint).frequency;
				if (lastFrequency < midFrequency) {
					midPointIndex.add(midPoint);
					low = midPoint + 1;
				} else if (lastFrequency > midFrequency) {
					midPointIndex.add(midPoint);
					high = midPoint - 1;
				} else {
					midPointIndex.add(midPoint);
					occs.add(midPoint, occs.get(occs.size() - 1));
					occs.remove(occs.size() - 1);
					return midPointIndex;
				}
			}
			occs.add(low, occs.get(occs.size() - 1));
			occs.remove(occs.size() - 1);
		}
		return midPointIndex;
	}

	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or
	 * kw2 occurs in that document. Result set is arranged in descending order
	 * of occurrence frequencies. (Note that a matching document will only
	 * appear once in the result.) Ties in frequency values are broken in favor
	 * of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and
	 * kw2 is in doc2 also with the same frequency f1, then doc1 will appear
	 * before doc2 in the result. The result set is limited to 5 entries. If
	 * there are no matching documents, the result is null.
	 * 
	 * @param kw1
	 *            First keyword
	 * @param kw1
	 *            Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs,
	 *         arranged in descending order of frequencies. The result size is
	 *         limited to 5 documents. If there are no matching documents, the
	 *         result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		ArrayList<String> top5List = new ArrayList<String>();
		ArrayList<Occurrence> kw1Occs = keywordsIndex.get(kw1);
		ArrayList<Occurrence> kw2Occs = keywordsIndex.get(kw2);
		if (kw1Occs == null && kw2Occs == null) {
			return null;
		}
		if (kw1Occs == null || kw2Occs == null) {
			if (kw1Occs != null) {
				int i = 0;
				while (top5List.size() < 5) {
					top5List.add(kw1Occs.get(i).document);
					i++;
				}
				return top5List;
			}
			if (kw2Occs != null) {
				int i = 0;
				while (top5List.size() < 5) {
					top5List.add(kw2Occs.get(i).document);
					i++;
				}
				return top5List;
			}
		}
		ListIterator<Occurrence> firstIter = kw1Occs.listIterator();
		ListIterator<Occurrence> secondIter = kw2Occs.listIterator();
		while (firstIter.hasNext() || secondIter.hasNext()) {
			if (top5List.size() == 5) {
				break;
			} else {
				if (!firstIter.hasNext()) {
					while (top5List.size() < 5 && secondIter.hasNext()) {
						top5List.add(secondIter.next().document);
					}
				} else if (!secondIter.hasNext()) {
					while (top5List.size() < 5 && firstIter.hasNext()) {
						top5List.add(firstIter.next().document);
					}
				} else {
					Occurrence firstOcc = firstIter.next();
					Occurrence secondOcc = secondIter.next();
					if (firstOcc.frequency >= secondOcc.frequency) {
						if (!top5List.contains(firstOcc.document)) {
							top5List.add(firstOcc.document);
						}
						secondIter.previous();
					} else {
						if (!top5List.contains(secondOcc.document)) {
							top5List.add(secondOcc.document);
						}
						firstIter.previous();

					}
				}
			}
		}
		return top5List;
	}
}
