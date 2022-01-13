package mpl.utils.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SourceReader {
	
	public static String read(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String total = "";
		String line;
		while((line = reader.readLine()) != null){
			total += line + '\n';
		}
		reader.close();
		return total;
	}
}