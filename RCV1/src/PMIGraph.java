import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class PMIGraph {

	private static HashMap<String, HashSet<String>> labelAndWords = new HashMap<String, HashSet<String>>();
	
	// use integer as probability = count/docCount
	// use only for relative comparison. The docCount is a constant, no impact on the comparison
	private static HashMap<String, HashMap<String, Integer>> labelAndFeatureAndProbability = new HashMap<String, HashMap<String, Integer>>();
	
	private static HashMap<String, Integer> labelAndItsTotalDocWordCount = new HashMap<String, Integer>();
	
	private static HashMap<String, HashMap<String, Integer>> labelAndVocabularyCount = new HashMap<String, HashMap<String, Integer>>();
	
	//whether it is a positive or a negative feature for the label
	private static HashMap<String, HashMap<String, Boolean>> labelAndFeatureFlag = new HashMap<String, HashMap<String, Boolean>>();
	
	// the splitted docs with the words in order
	private static ArrayList<ArrayList<String>> docWords = new ArrayList<ArrayList<String>>();
	
	//each doc contains which features
	private static ArrayList<HashSet<String>> docFeatures = new ArrayList<HashSet<String>>();
	
	// the labels for each doc
	private static ArrayList<String> docLabels = new ArrayList<String>();

	private static HashMap<FeaturePair, Double> PMIforFeaturePair = new HashMap<FeaturePair, Double>();
	
	private static HashMap<FeaturePair, Boolean> SignforFeaturePair = new HashMap<FeaturePair, Boolean>();
	
	private static Multimap<Double, Boolean> PMIandSigns = ArrayListMultimap.create();
	
	private static TreeMap<Double, Double> PMIandCumulativeRate = new TreeMap<Double, Double>();
	
	public static void main(String[] args) {
		initLabelAndWords();
		
		initDocsWordsAndLabels();
		EachLabelTotalWordCountAndVocabularyCount();
		CalculateFeatureFlag();
		CalculateFeatureProbability();
		CalculatePMI();
		CalculatePairSign();
		
		ConstructPMIandSignPair();
		ConstructPMIandCumulativeRate();
		//System.out.println(PMIandSigns);
		
		System.out.println(PMIforFeaturePair.size());
		System.out.println(SignforFeaturePair.size());
		System.out.println(PMIandSigns.size());
		System.out.println(PMIandCumulativeRate.size());
		
		//System.out.println(Runtime.getRuntime().totalMemory());
		//PMIforFeaturePair.size());
	}

	public static void initLabelAndWords() {
		// initialize the hashmap obj
		for (int i = 0; i < Config.labels.length; i++) {
			labelAndWords.put(Config.labels[i], new HashSet<String>());
			labelAndFeatureAndProbability.put(Config.labels[i], new HashMap<String, Integer>());
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
				labelAndFeatureAndProbability.get(values[1]).put(values[0], 0);
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
						HashSet<String> outputFeatures = new HashSet<String>();
						
						for (int i = 0; i < words.length; i++) {
							word = words[i].replaceAll("\\W", "").toLowerCase();

							if (word.equals("")) {
								continue;
							}

							outputWords.add(word);
							
							//if the word is a feature, add the word to the feature set of the doc
							for(HashSet<String> featurePerLabel :labelAndWords.values()) {
								if(featurePerLabel.contains(word)) {
									outputFeatures.add(word);
									break; //assume no feature appears in two labels
								}
							}
						}
						docWords.add(outputWords);
						docFeatures.add(outputFeatures);
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
	
	public static void EachLabelTotalWordCountAndVocabularyCount () {
		for (int i = 0; i < Config.labels.length; i++) {
			labelAndItsTotalDocWordCount.put(Config.labels[i], 0);
			labelAndVocabularyCount.put(Config.labels[i], new HashMap<String,Integer>());
		}
		
		//for each document
		for(int i = 0; i < docLabels.size(); i++) {
			//update the total word count for each label
			int tmp = labelAndItsTotalDocWordCount.get(docLabels.get(i));
			tmp += docWords.get(i).size();
			labelAndItsTotalDocWordCount.put(docLabels.get(i), tmp);
			
			//for each word in each doc
			for(String word: docWords.get(i)) {
				if(labelAndVocabularyCount.get(docLabels.get(i)).containsKey(word)) { //the word already exists
					int wordCount = labelAndVocabularyCount.get(docLabels.get(i)).get(word);
					wordCount++;
					labelAndVocabularyCount.get(docLabels.get(i)).put(word, wordCount);
				}
				else { // the word does not exist yet
					labelAndVocabularyCount.get(docLabels.get(i)).put(word, 1);
				}
			}
		}
	}

	public static void CalculateFeatureFlag(){
		for (int i = 0; i < Config.labels.length; i++) {
			labelAndFeatureFlag.put(Config.labels[i], new HashMap<String,Boolean>());
			
			//average occurrence
			double avgOccur = (double) labelAndItsTotalDocWordCount.get(Config.labels[i]) 
					/ (double) labelAndVocabularyCount.get(Config.labels[i]).size();
			
			//foreach feature in the label
			for(String feature : labelAndWords.get(Config.labels[i])) {
				double wordCount = (double) labelAndVocabularyCount.get(Config.labels[i]).get(feature);
				if(wordCount > avgOccur) {
					labelAndFeatureFlag.get(Config.labels[i]).put(feature, true);
				}
				else {
					labelAndFeatureFlag.get(Config.labels[i]).put(feature, false);
				}
			}
		}
	}
	
	//How many times the feature appears in each doc divided by total doc count
	public static void CalculateFeatureProbability() {
		for(HashSet<String> featuresInDoc : docFeatures) { //foreach doc

			for(String featureInDoc : featuresInDoc) {//foreach feature in the doc
				
				for(HashMap<String, Integer> featuresPerLabel :labelAndFeatureAndProbability.values()) {//may appear in different label feature sets
					if(featuresPerLabel.containsKey(featureInDoc)) {
						int tmp = featuresPerLabel.get(featureInDoc);
						tmp++;
						featuresPerLabel.put(featureInDoc, tmp);
						break; //assume no feature appears in two labels
					}
				}
			}
		}
	}
	
	public static void CalculatePMI() {
		//init feature pair
		for(int i = 0; i < Config.labels.length - 1; i++) {
			for(int j = (i + 1); j < Config.labels.length; j++ ){
				for(String feature1 : labelAndWords.get(Config.labels[i])) {
					for(String feature2 : labelAndWords.get(Config.labels[j])) {
						
						FeaturePair newPair = new FeaturePair();
						newPair.feature1 = feature1;
						newPair.feature2 = feature2;
						
						PMIforFeaturePair.put(newPair, 0.0);
					}
				}
					
			}
		}
		
		//foreach doc
		for(HashSet<String> features : docFeatures) {
			//for each feature pair in one doc
			for(String feature1 : features) {
				for(String feature2: features) {
					FeaturePair pair = new FeaturePair();
					pair.feature1 = feature1;
					pair.feature2 = feature2;
					
					if(PMIforFeaturePair.containsKey(pair)) { //then add the value by one
						double tmp = PMIforFeaturePair.get(pair);
						tmp++;
						PMIforFeaturePair.put(pair, tmp);
					}
				}
			}
		}
		
		// increase the current value by multiplying the doc count
		for(Entry<FeaturePair, Double> entry : PMIforFeaturePair.entrySet()) {
			PMIforFeaturePair.put(entry.getKey(), entry.getValue() * docFeatures.size());
		}
		
		//for each pair, calculate PMI = log(current value / (p(x) * p(y)))
		for(Entry<FeaturePair, Double> entry : PMIforFeaturePair.entrySet()) {
			FeaturePair pair = entry.getKey();
			double pFeature1 = 0.0;
			double pFeature2 = 0.0;
			
			for(HashMap<String, Integer> featuresPerLabel :labelAndFeatureAndProbability.values()) {//may appear in different label feature sets
				if(featuresPerLabel.containsKey(pair.feature1)) {
					pFeature1 = (double) featuresPerLabel.get(pair.feature1);
				}
				if(featuresPerLabel.containsKey(pair.feature2)) {
					pFeature2 = (double) featuresPerLabel.get(pair.feature2);
				}
			}

			double tmp = 0.0;
			//FIXME: may not contained in the training doc.
			if(pFeature1 != 0.0 && pFeature2 != 0.0) {
				tmp = entry.getValue() / (pFeature1 * pFeature2);
			}
			else {
				System.out.println("not happened?");
			}
			
			//Log the number
			PMIforFeaturePair.put(pair, Math.log(tmp));
		}
	}

	//If both positive, then the pair positive
	//If either negative, then the pair negative
	//If both negative, then the pair negative
	public static void CalculatePairSign(){
		//init feature pair
		for(int i = 0; i < Config.labels.length - 1; i++) {
			for(int j = (i + 1); j < Config.labels.length; j++ ){
				for(String feature1 : labelAndWords.get(Config.labels[i])) {
					for(String feature2 : labelAndWords.get(Config.labels[j])) {
						
						FeaturePair newPair = new FeaturePair();
						newPair.feature1 = feature1;
						newPair.feature2 = feature2;
						
						if(labelAndFeatureFlag.get(Config.labels[i]).get(feature1) && labelAndFeatureFlag.get(Config.labels[j]).get(feature2)) {
							SignforFeaturePair.put(newPair, true);
						}
						else if(!labelAndFeatureFlag.get(Config.labels[i]).get(feature1) && !labelAndFeatureFlag.get(Config.labels[j]).get(feature2)) {
							SignforFeaturePair.put(newPair, false);
						}
						else {
							SignforFeaturePair.put(newPair, false);
						}
					}
				}
					
			}
		}
	}
	
	public static void ConstructPMIandSignPair() {
		for(Entry<FeaturePair, Double> entry : PMIforFeaturePair.entrySet()) {
			PMIandSigns.put(entry.getValue(), SignforFeaturePair.get(entry.getKey()));
		}
	}
	
	public static void ConstructPMIandCumulativeRate() {
		int trueValue = 0;
		int falseValue = 0;
		
		Double previousKey = new Double(-0.1);
		
		BufferedWriter bw;
		File outFile = new File(Config.path, Config.PMIandRateFile);
		boolean skip = true;
		try {
			bw = new BufferedWriter(new FileWriter(outFile));
			for(Double key : new TreeSet<Double>(PMIandSigns.keySet())){
				if(skip) {
					skip = false;
					System.out.println("size:" + PMIandSigns.get(key).size());
					
					int tmpTruValue = 0;
					int tempFalseValue = 0;
					for(Boolean value: PMIandSigns.get(key)) {
						if(value == null) {
							continue;
						}
						if(value == true) {
							tmpTruValue++;
						}
						else {
							tempFalseValue++;
						}
					}
					double tmp = (double)tmpTruValue / (double)(tmpTruValue + tempFalseValue);
					System.out.println(tmp);
					continue;
				}
				
				//just an assertion
				if(previousKey > key) {
					System.out.println("not in a good shape");
				}
				previousKey = key;
				
				for(Boolean value: PMIandSigns.get(key)) {
					if(value == null) {
						continue;
					}
					if(value == true) {
						trueValue++;
					}
					else {
						falseValue++;
					}
				}
				double tmp = (double)trueValue / (double)(trueValue + falseValue);
				PMIandCumulativeRate.put(key, tmp);
				bw.write(key + " ," + tmp + " ," + PMIandSigns.get(key).size());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public static class FeaturePair {
		public String feature1;
		public String feature2;
		
		//poor version. No class check
		@Override
		public boolean equals(Object obj) {
			FeaturePair comparison = (FeaturePair) obj;
			return (comparison.feature1.equals(feature1) && comparison.feature2.equals(feature2)) ;
					//|| (comparison.feature1.equals(feature2) && comparison.feature2.equals(feature1));
		}
		
		@Override
		public int hashCode() {
			return feature1.hashCode() + feature2.hashCode();
		}
	}
}


