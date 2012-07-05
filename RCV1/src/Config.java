import java.io.File;


public class Config {
	public static final String[] labels = { "CCAT", "ECAT", "GCAT", "MCAT" };
	
	public static final File path = new File("C:\\MSE\\IS\\RCV1");

	public static final String trainingFile = "RCV1.small_train.txt";
	public static final String selectedDocFile = "immediate.txt";
	public static final String outputFile = "output.txt";
	public static final String outputSeedFile = "seed.txt";
	public static final String labelPropagatedFile = "label_prop_output.txt";
	public static final String outputWordLabelFile = "labledWord.txt";
	public static final String testFile = "RCV1.small_test.txt";
	
	public static final String docPreffix = "MyTestDoc";
	public static final String dummyLabel = "__DUMMY__";
	
	public static final String biasWord = "__BIAS__";
}
