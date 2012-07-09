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
	
	//the true label for each docs 
	private static HashMap<String, ArrayList<Boolean>> trueDocLabels = new HashMap<String, ArrayList<Boolean>>();

	private static int iterationTime = 20;
	private static double initLambda = 0.5;

	public static void main(String[] args) {
		initLabelAndWords();
		initDocsWordsAndLabels();
		constructDataset();
		calculateBetaForWords();

		outputTestFileLabel();
		calculateF1Score();
		// for(Entry<String, ArrayList<Double>> entry: labeledDocs.entrySet()) {
		// System.out.println("Lable is:" + entry.getKey());
		// for(Double value: entry.getValue()) {
		// System.out.println("    " + value);
		// }
		// }

//		System.out.println(betas.size());
//		for (Entry<String, HashMap<String, Double>> entry : betas.entrySet()) {
//			System.out.println(entry.getKey());
//			HashMap<String, Double> value = entry.getValue();
//			int top20 = 20;
//			for (Entry<String, Double> wordsValue : value.entrySet()) {
//				System.out.println(wordsValue.getKey() + " "
//						+ wordsValue.getValue());
//				if (--top20 == 0) {
//					System.out.println("bias" + " " + value.get(biasWord));
//					break;
//				}
//			}
//		}
		
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
		for (int i = 0; i < Config.labels.length; i++) {
			labelAndWords.put(Config.labels[i], new HashSet<String>());
		}

		// read the word label pair
		File file = new File(Config.path, Config.outputWordLabelFile);

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
		File trainFile = new File(Config.path, Config.trainingFile);
		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(trainFile));

			String line;
			line = br.readLine();

			String[] values;

			while (line != null) {
				values = line.split("\t");
				// values[0] is the labels

				for (int k = 0; k < Config.labels.length; k++) {
					if (values[0].contains(Config.labels[k])) {

						// update the doc label
						// duplicate the doc if there are two labels for each
						// doc
						docLabels.add(Config.labels[k]);

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
		for (String label : Config.labels) {
			dataSet.put(label, new ArrayList<XandY>());
		}

		ArrayList<String> words;
		// foreach label, go through the docs(words[]) and init the XandY object
		// for each doc
		for (String label : Config.labels) {
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
				xandy.words.add(Config.biasWord);
				dataSet.get(label).add(xandy);
			}

		}

	}

	// logistic regression
	public static void calculateBetaForWords() {
		for (String label : Config.labels) { // for each label
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
//					for (String word : data.words) {
//						if (wordAndWeight.containsKey(word)) {
//							wordAndWeight.put(word, wordAndWeight.get(word)
//									+ lambda * (labeled - probability));
//						} else {
//							wordAndWeight.put(word, 0.0);
//						}
//					}
					
					// updated again with word updated check
					HashSet<String> wordUpdated = new HashSet<String>();
					
					for (String word : data.words) {
						if(wordUpdated.contains(word)) {
							continue;
						}
						
						if (wordAndWeight.containsKey(word)) {
							wordAndWeight.put(word, wordAndWeight.get(word)
									+ lambda * (labeled - probability));
						} else {
							wordAndWeight.put(word, lambda * (labeled - probability));
						}
						
						wordUpdated.add(word);
					}
				}
			}
			// output the result
			betas.put(label, wordAndWeight);
		}
	}

	public static void outputTestFileLabel() {
		// init the labeledDocs
		for (String label : Config.labels) {
			labeledDocs.put(label, new ArrayList<Double>());
			trueDocLabels.put(label, new ArrayList<Boolean>());
		}

		File trainFile = new File(Config.path, Config.testFile);
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
				double[] xtimesw = new double[Config.labels.length];
				for (int i = 0; i < xtimesw.length; i++) {
					// add bias value first
					xtimesw[i] = betas.get(Config.labels[i]).get(Config.biasWord);
				}

				for (int i = 0; i < words.length; i++) { // each word
					word = words[i].replaceAll("\\W", "").toLowerCase();

					if (word.equals("")) {
						continue;
					}
					// calculate the mark for that doc based on different labels
					for (int j = 0; j < Config.labels.length; j++) {
						if (labelAndWords.get(Config.labels[j]).contains(word)) { // the
																			// word
																			// is
																			// in
																			// the
																			// feature
																			// set
							xtimesw[j] += betas.get(Config.labels[j]).get(word);
						}
					}
				}

				// add the value to the result
				for (int i = 0; i < Config.labels.length; i++) {
					labeledDocs.get(Config.labels[i]).add(xtimesw[i]);
					trueDocLabels.get(Config.labels[i]).add(values[0].contains(Config.labels[i])? true: false);
				}

				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void calculateF1Score() {
		for(String label: Config.labels) {
			System.out.println("label is:" + label);
			
			ArrayList<Double> docsResult = labeledDocs.get(label);
			ArrayList<Boolean> trueDocsResult = trueDocLabels.get(label);
			
			int correctPositive = 0;
			int totalLabeledPositive = 0;
			int totalTruePositive = 0;
			
			double probability;
			for(int i = 0; i < docsResult.size(); i++)  {
				probability = 1 / (1 + Math.exp(-docsResult.get(i)));
				
				if(probability > 0.5) {
					totalLabeledPositive++;
					if(trueDocsResult.get(i)) {
						correctPositive++;
					}
				}
				
				if(trueDocsResult.get(i)) {
					totalTruePositive++;
				}
			}
			
			double precision = (double)correctPositive / (double)totalLabeledPositive;
			double recall = (double)correctPositive / (double) totalTruePositive;
			
			double f1score = 2.0 * precision * recall / (precision + recall);
			System.out.println("correctPositive:" + correctPositive);
			System.out.println("totalLabeledPositive:" + totalLabeledPositive);
			System.out.println("totalTruePositive:" + totalTruePositive);
			System.out.println("precision:" + precision);
			System.out.println("recall:" + recall);
			System.out.println(f1score);
			//probability = 1 / (1 + Math.exp(-weightedWord))
		}
	}

	public static class XandY {
		// true if the doc is of the same label as the target label
		public boolean isSameLabel;
		// all the words in the doc which are of the label
		public ArrayList<String> words = new ArrayList<String>();
	}
}
