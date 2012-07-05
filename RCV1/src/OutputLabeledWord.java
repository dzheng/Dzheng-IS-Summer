import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class OutputLabeledWord {
	public static void main(String[] args) {
		outputLabelPropagated();
	}
	
	public static void outputLabelPropagated() {
		File file = new File(Config.path, Config.labelPropagatedFile);
		File opFile = new File(Config.path, Config.outputWordLabelFile);

		BufferedReader br;
		BufferedWriter bw;

		try {
			br = new BufferedReader(new FileReader(file));
			bw = new BufferedWriter(new FileWriter(opFile));

			String line;
			
			String[] values;
			line = br.readLine();
			int temp1, temp2;
			String label;
			while (line != null) {
				values = line.split("\t");
				
				//doc's label is not needed, only word's label
				if(values[0].startsWith(Config.docPreffix)) {
					line = br.readLine();
					continue;
				}
				
				temp1 = values[3].indexOf(' ');
				label = values[3].substring(0, temp1);
				if(label.equals(Config.dummyLabel)) { // find the third whitespace index instead
					temp2 = values[3].indexOf(' ', temp1 + 1);
					label = values[3].substring(temp2 + 1, values[3].indexOf(' ', temp2 + 1));
				}
				bw.write(values[0] + "\t" + label);
				bw.newLine();
				
				line = br.readLine();
			}
			
			br.close();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
