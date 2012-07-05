import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class RCV1 {
	// private static String testFile = "RCV1.very_small.txt";
	// private static String dataFile = "RCV1.very_small.txt";

	// word occurrence-number pair for each document
	private static ArrayList<HashMap<String, Double>> formattedDocs = new ArrayList<HashMap<String, Double>>();
	// label for each document
	private static ArrayList<String> docLabels = new ArrayList<String>();
	// word docCount pair
	// will be used to calculate idf, so use double
	private static HashMap<String, Double> docCountContainingTheWord = new HashMap<String, Double>();

	private static int docNumberPerClass = 10;
	private static int labelCount = 4;
	
	public static void main(String[] args) {
//		 labelDocPairToFile();
//		 formatEachDoc();
//		 calculateTF();
//		 calculateIDF();
//		 calculateTFIDF();
//		 outputDocWordTFIDF();
//		 outputSeedFile();
		//outputLabelPropagated();
	}

	// create an intermediate file with label-content pair per line
	public static void labelDocPairToFile() {
		// create the file
		// createFileInPath (outputFile);

		File trainFile = new File(Config.path, Config.trainingFile);
		File outFile = new File(Config.path, Config.selectedDocFile);

		BufferedReader br;
		BufferedWriter bw;

		try {
			br = new BufferedReader(new FileReader(trainFile));
			bw = new BufferedWriter(new FileWriter(outFile));

			String line;
			line = br.readLine();

			int ccat = 0;
			int ecat = 0;
			int gcat = 0;
			int mcat = 0;

			String[] values;
			// random pickup.
			// just one label for each doc.
			while (line != null
					&& (ccat < docNumberPerClass || ecat < docNumberPerClass
							|| gcat < docNumberPerClass || mcat < docNumberPerClass)) {
				values = line.split("\t");
				// values[0] is the labels
				// values[1] is the content
				if (values[0].contains("CCAT") && ccat < docNumberPerClass) {
					bw.write("CCAT" + "\t" + values[1]);
					bw.newLine();
					ccat++;
				}
				if (values[0].contains("ECAT") && ecat < docNumberPerClass) {
					bw.write("ECAT" + "\t" + values[1]);
					bw.newLine();
					ecat++;
				}
				if (values[0].contains("GCAT") && gcat < docNumberPerClass) {
					bw.write("GCAT" + "\t" + values[1]);
					bw.newLine();
					gcat++;
				}
				if (values[0].contains("MCAT") && mcat < docNumberPerClass) {
					bw.write("MCAT" + "\t" + values[1]);
					bw.newLine();
					mcat++;
				}

				line = br.readLine();
			}

			br.close();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void formatEachDoc() {
		File file = new File(Config.path, Config.selectedDocFile);
		BufferedReader br;
		String[] values;
		try {
			br = new BufferedReader(new FileReader(file));

			String line;
			line = br.readLine();

			while (line != null) {
				values = line.split("\t");
				docLabels.add(values[0]);
				formattedDocs.add(docToWordCount(values[1]));
				line = br.readLine();
			}

			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static HashMap<String, Double> docToWordCount(String doc) {
		HashMap<String, Double> wordCount = new HashMap<String, Double>();

		String value;
		String[] values = doc.split("\\s");

		//int count = 0;
		for (int i = 0; i < values.length; i++) {
			value = values[i].replaceAll("\\W", "").toLowerCase();

			if (value.equals("")) {
				continue;
			}
			//count++;
			// System.out.println(value);
			if (wordCount.containsKey(value)) {// the word appeared before
				wordCount.put(value, (wordCount.get(value) + 1));

			} else {// first time the word appears
				wordCount.put(value, 1.0);

				// add to the global doc count containing the word
				if (docCountContainingTheWord.containsKey(value)) {// the
																	// word
																	// appeared
																	// before
					docCountContainingTheWord.put(value,
							(docCountContainingTheWord.get(value) + 1));

				} else {// first time the word appears
					docCountContainingTheWord.put(value, 1.0);
				}
			}

		}

		// Clculate the wordcount and devide the wordOccurenceTime to get
		// the frequency. No longer required...
//		for (Entry<String, Double> entry : wordCount.entrySet()) {
//			String key = entry.getKey();
//			double wordOccurence = entry.getValue();
//			entry.setValue(wordOccurence / count);
//		}

		return wordCount;
	}

	public static void calculateTF() {
		HashMap<String, Double> wordCountPerDoc;
		double calc;
		
		Iterator<HashMap<String, Double>> iter = formattedDocs.iterator();
		while (iter.hasNext()) { // for each document
			wordCountPerDoc = iter.next();
			//sum of the square of the frequency for each word
			double total = 0;
				
			// for each word in the document
			for (Entry<String, Double> entry : wordCountPerDoc.entrySet()) {
				double value = entry.getValue();
				calc = Math.log(value + 1);
				entry.setValue(calc);
				total += (calc * calc);
			}
			
			double sqrtTotal = Math.sqrt(total);
			
			for (Entry<String, Double> entry : wordCountPerDoc.entrySet()) {
				entry.setValue(entry.getValue() / sqrtTotal);
			}
		}
	}
	
	// change the docCountContainingTheWord to idf value for each word
	public static void calculateIDF() {
		double calc;
		double docCount = docNumberPerClass * labelCount;

		for (Entry<String, Double> entry : docCountContainingTheWord.entrySet()) {
			String key = entry.getKey();
			double value = entry.getValue();

			calc = Math.log(docCount / value);
			docCountContainingTheWord.put(key, calc);
		}

	}

	public static void calculateTFIDF() {
		HashMap<String, Double> wordCountPerDoc;
		double calc;

		Iterator<HashMap<String, Double>> iter = formattedDocs.iterator();
		while (iter.hasNext()) { // for each document
			wordCountPerDoc = iter.next();

			// for each word in the document
			for (Entry<String, Double> entry : wordCountPerDoc.entrySet()) {
				String key = entry.getKey();
				calc = entry.getValue() * docCountContainingTheWord.get(key);
				
				System.out.println("TF:" + entry.getValue());
				System.out.println("IDF:" + docCountContainingTheWord.get(key));
				System.out.println(calc);
				
				entry.setValue(calc);
			}
		}
	}

	public static void outputDocWordTFIDF() {
		File file = new File(Config.path, Config.outputFile);

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(file));

			HashMap<String, Double> wordCountPerDoc;
			for (int i = 0; i < formattedDocs.size(); i++) {
				wordCountPerDoc = formattedDocs.get(i);

				// for each word in the document
				for (Entry<String, Double> entry : wordCountPerDoc.entrySet()) {
					String key = entry.getKey();
					double value = entry.getValue();
					// bw.write(docLabels.get(i) + "\t" + i + "\t" + key + "\t"
					// + value);
					// use the format in junto
					bw.write(Config.docPreffix + i + "\t" + key + "\t" + value);
					bw.newLine();
				}
			}

			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void outputSeedFile() {
		File file = new File(Config.path, Config.outputSeedFile);

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(file));

			for (int i = 0; i < docLabels.size(); i++) {
				bw.write(Config.docPreffix + i + "\t" + docLabels.get(i) + "\t" + "1.0");
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void createFileInPath(String file) {
		File outputFile = new File(Config.path, file);
		if (outputFile.exists()) {
			if (!outputFile.delete()) {
				System.out.println("The file:" + outputFile.getAbsolutePath()
						+ " cannot be deleted. Exiting...");
				System.exit(0);
			}
		}
		try {
			outputFile.createNewFile();
		} catch (IOException e) {
			System.out.println("The file:" + outputFile.getAbsolutePath()
					+ " cannot be created. Exitng...");
			System.exit(0);
		}
	}
	
	
}
