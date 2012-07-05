import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class Step2 {

	private static File path = new File("C:\\MSE\\IS\\RCV1");

	private static String trainingFile = "RCV1.small_train.txt";
	private static String outputWordLabelFile = "labledWord.txt";
	private static String testFile = "RCV1.very_small_test.txt";

	private static String[] labels = { "CCAT", "ECAT", "GCAT", "MCAT" };
	private static HashMap<String, HashSet<String>> labelAndWords = new HashMap<String, HashSet<String>>();
	// the splitted docs with the words in order
	private static ArrayList<ArrayList<String>> docWords = new ArrayList<ArrayList<String>>();
	// the labels for each doc
	private static ArrayList<String> docLabels = new ArrayList<String>();
	// feature-label pair (XandY) for each doc and the labels
	private static HashMap<String, ArrayList<XandY>> dataSet = new HashMap<String, ArrayList<XandY>>();
	// Beta values for the words for training //label + corresponding beta value
	private static HashMap<String, HashMap<String, Double>> betas = new HashMap<String, HashMap<String, Double>>();
	// for each class, an arraylist for the docs. The index matches the index in
	// the test file.
	private static HashMap<String, ArrayList<Double>> labeledDocs = new HashMap<String, ArrayList<Double>>();

	private static String biasWord = "__BIAS__";
	private static int iterationTime = 20;
	private static double initLambda = 0.5;

	public static void main(String[] args) {
		initLabelAndWords();
		initDocsWordsAndLabels();
		constructDataset();
		calculateBetaForWords();
		// outputTestFileLabel();

		// for(Entry<String, ArrayList<Double>> entry: labeledDocs.entrySet()) {
		// System.out.println("Lable is:" + entry.getKey());
		// for(Double value: entry.getValue()) {
		// System.out.println("    " + value);
		// }
		// }

		System.out.println(betas.size());
		for (Entry<String, HashMap<String, Double>> entry : betas.entrySet()) {
			System.out.println(entry.getKey());
			HashMap<String, Double> value = entry.getValue();
			int top20 = 20;
			for (Entry<String, Double> wordsValue : value.entrySet()) {
				System.out.println(wordsValue.getKey() + " "
						+ wordsValue.getValue());
				if (--top20 == 0) {
					System.out.println("bias" + " " + value.get(biasWord));
					break;
				}
			}
		}
		// System.out.println(dataSet.size());
		// for (Entry<String, ArrayList<XandY>> entry : dataSet.entrySet()) {
		// String key = entry.getKey();
		// ArrayList<XandY> value = entry.getValue();
		// System.out.println("");
		// System.out.println(key);
		// for(int i = 0; i < 10; i++) {
		// System.out.println(value.get(i).isSameLabel);
		// for(String word: value.get(i).words) {
		// System.out.print(word + " ");
		// }
		// System.out.println("");
		// }
		// }
	}

	public static void initLabelAndWords() {
		// initialize the hashmap obj
		for (int i = 0; i < labels.length; i++) {
			labelAndWords.put(labels[i], new HashSet<String>());
		}

		// read the word label pair
		File file = new File(path, outputWordLabelFile);

		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			String[] values;

			while (line != null) {
				values = line.split("\t");
				// put each word into the corresponding bucket
				labelAndWords.get(values[1]).add(values[0]);
				line = br.readLine();
			}

			br.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void initDocsWordsAndLabels() {
		// read the training file, get the doc-label pair
		File trainFile = new File(path, trainingFile);
		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(trainFile));

			String line;
			line = br.readLine();

			String[] values;

			while (line != null) {
				values = line.split("\t");
				// values[0] is the labels

				for (int k = 0; k < labels.length; k++) {
					if (values[0].contains(labels[k])) {

						// update the doc label
						// duplicate the doc if there are two labels for each
						// doc
						docLabels.add(labels[k]);

						// values[1] is the content
						// update the docWords
						String word;
						String[] words = values[1].split("\\s");
						ArrayList<String> outputWords = new ArrayList<String>();

						for (int i = 0; i < words.length; i++) {
							word = words[i].replaceAll("\\W", "").toLowerCase();

							if (word.equals("")) {
								continue;
							}

							outputWords.add(word);
						}
						docWords.add(outputWords);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void constructDataset() {
		// init the dataSet with labels as keys
		for (String label : labels) {
			dataSet.put(label, new ArrayList<XandY>());
		}

		ArrayList<String> words;
		// foreach label, go through the docs(words[]) and init the XandY object
		// for each doc
		for (String label : labels) {
			// for each doc
			for (int i = 0; i < docWords.size(); i++) {
				words = docWords.get(i);
				XandY xandy = new XandY();

				if (docLabels.get(i).equals(label)) {
					xandy.isSameLabel = true;
				} else {
					xandy.isSameLabel = false;
				}

				HashSet<String> labeledWords = labelAndWords.get(label);
				// for each word
				for (String word : words) {
					if (labeledWords.contains(word)) {
						xandy.words.add(word);
					}
				}

				// add the bias word (x0) for logistic regression
				xandy.words.add(biasWord);
				dataSet.get(label).add(xandy);
			}

		}

	}

	// logistic regression
	public static void calculateBetaForWords() {
		for (String label : labels) { // for each label
			HashMap<String, Double> wordAndWeight = new HashMap<String, Double>();

			for (int i = 1; i <= iterationTime; i++) { // iterative calculation
														// - SGD
				double lambda = initLambda / (i * i);
				double weightedWord = 0;
				double probability = 0;
				for (XandY data : dataSet.get(label)) { // for each example

					double labeled = data.isSameLabel ? 1.0 : 0.0;
					// calculate sum of xij * betaj
					for (String word : data.words) {
						if (wordAndWeight.containsKey(word)) {
							weightedWord += wordAndWeight.get(word);
						}
					}
					// calculate the probability
					probability = 1 / (1 + Math.exp(-weightedWord));

					// update the weights
					for (String word : data.words) {
						if (wordAndWeight.containsKey(word)) {
							wordAndWeight.put(word, wordAndWeight.get(word)
									+ lambda * (labeled - probability));
						} else {
							wordAndWeight.put(word, 0.0);
						}
					}
				}
			}
			// output the result
			betas.put(label, wordAndWeight);
		}
	}

	public static void outputTestFileLabel() {
		// init the labeledDocs
		for (String label : labels) {
			labeledDocs.put(label, new ArrayList<Double>());
		}

		File trainFile = new File(path, testFile);
		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(trainFile));

			String line;
			line = br.readLine();

			String[] values;

			while (line != null) { // each document
				values = line.split("\t");

				// values[1] is the content
				String word;
				String[] words = values[1].split("\\s");

				// init the temp value for different classes
				double[] xtimesw = new double[labels.length];
				for (int i = 0; i < xtimesw.length; i++) {
					// add bias value first
					xtimesw[i] = betas.get(labels[i]).get(biasWord);
				}

				for (int i = 0; i < words.length; i++) { // each word
					word = words[i].replaceAll("\\W", "").toLowerCase();

					if (word.equals("")) {
						continue;
					}
					// calculate the mark for that doc based on different labels
					for (int j = 0; j < labels.length; j++) {
						if (labelAndWords.get(labels[j]).contains(word)) { // the
																			// word
																			// is
																			// in
																			// the
																			// feature
																			// set
							xtimesw[j] += betas.get(labels[j]).get(word);
						}
					}
				}

				// add the value to the result
				for (int i = 0; i < labels.length; i++) {
					labeledDocs.get(labels[i]).add(xtimesw[i]);
				}

				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static class XandY {
		// true if the doc is of the same label as the target label
		public boolean isSameLabel;
		// all the words in the doc which are of the label
		public ArrayList<String> words = new ArrayList<String>();
	}
}
