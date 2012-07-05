import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class CompareBase {
	private static File path = new File("C:\\MSE\\IS\\RCV1");

	private static String outputWordLabelFile = "labledWord.txt";
	private static String trainingFile = "RCV1.small_train.txt";
	private static String testFile = "RCV1.small_test.txt";

	private static String[] labels = { "CCAT", "ECAT", "GCAT", "MCAT" };
	// contains all the words (features)
	private static HashSet<String> words = new HashSet<String>();

	// the splitted docs with the words in order
	private static ArrayList<ArrayList<String>> docWords = new ArrayList<ArrayList<String>>();
	
	// the labels for each doc
	private static ArrayList<String> docLabels = new ArrayList<String>();

	// Beta values for the words for training //label + corresponding beta value
	private static HashMap<String, HashMap<String, Double>> betas = new HashMap<String, HashMap<String, Double>>();
	
	// for each class, an arraylist for the docs. The index matches the index in
	// the test file.
	private static HashMap<String, ArrayList<Double>> labeledDocs = new HashMap<String, ArrayList<Double>>();

	//the true label for each docs 
	private static HashMap<String, ArrayList<Boolean>> trueDocLabels = new HashMap<String, ArrayList<Boolean>>();
	
	private static String biasWord = "__BIAS__";
	private static int iterationTime = 20;
	private static double initLambda = 0.5;

	public static void main(String[] args) {
		initWords();
		//System.out.println(words.size());
		initDocsAndLabels();
		calculateBetaForWords();
		outputTestFileLabel();
		calculateF1Score();
		
//		for (Entry<String, ArrayList<Double>> entry : labeledDocs.entrySet()) {
//			System.out.println("Label is:" + entry.getKey());
//			for (Double value : entry.getValue()) {
//				System.out.println("    " + value);
//			}
//		}
	}

	private static void initWords() {
		File file = new File(path, outputWordLabelFile);

		BufferedReader br;

		try {
			br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			String[] values;

			while (line != null) {
				values = line.split("\t");
				// put each word into the corresponding bucket
				words.add(values[0]);
				line = br.readLine();
			}

			br.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		words.add(biasWord);
	}

	private static void initDocsAndLabels() {
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
						outputWords.add(biasWord); //add the bias word
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

	public static void calculateBetaForWords() {
		for (String label : labels) { // for each label
			HashMap<String, Double> wordAndWeight = new HashMap<String, Double>();

			for (int iteration = 1; iteration <= iterationTime; iteration++) { // iterative calculation
														// - SGD
				double lambda = initLambda / (iteration * iteration);
				double weightedWord = 0;
				double probability = 0;
				
				for (int i = 0; i < docWords.size(); i++) { // for each example

					double labeled = docLabels.get(i).equals(label)? 1.0 : 0.0;
					
					// calculate sum of xij * betaj
					for (String word : docWords.get(i)) {
						if (wordAndWeight.containsKey(word)) {
							weightedWord += wordAndWeight.get(word);
						}
					}
					
					// calculate the probability
					probability = 1 / (1 + Math.exp(-weightedWord));

					// update the weights
					for (String word : docWords.get(i)) {
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
			trueDocLabels.put(label, new ArrayList<Boolean>());
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
				String[] wordsInFile = values[1].split("\\s");

				// init the temp value for different classes
				double[] xtimesw = new double[labels.length];
				for (int i = 0; i < xtimesw.length; i++) {
					// add bias value first
					xtimesw[i] = betas.get(labels[i]).get(biasWord);
				}

				for (int i = 0; i < wordsInFile.length; i++) { // each word
					word = wordsInFile[i].replaceAll("\\W", "").toLowerCase();

					if (word.equals("")) {
						continue;
					}
					// calculate the mark for that doc based on different labels
					for (int j = 0; j < labels.length; j++) {
						if (words.contains(word)) { // the// word// is// in// the// feature// set
							xtimesw[j] += betas.get(labels[j]).get(word);
						}
					}
				}

				// add the value to the result
				for (int i = 0; i < labels.length; i++) {
					labeledDocs.get(labels[i]).add(xtimesw[i]);
					trueDocLabels.get(labels[i]).add(values[0].contains(labels[i])? true: false);
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
		for(String label: labels) {
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


}
