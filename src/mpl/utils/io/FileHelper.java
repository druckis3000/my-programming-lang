package mpl.utils.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper {

	public static void writeToFile(String filePath, String content){
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filePath));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(writer != null)
					writer.close();
			} catch (IOException e) { }
		}
	}

	public static void writeToFile(File filePath, String content){
		writeToFile(filePath.getAbsolutePath(), content);
	}
	
	public static String readFile(String filePath){
		String content = "";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			String line;
			while((line = reader.readLine()) != null)
				content += line + System.lineSeparator();
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			try{
				if(reader != null)
					reader.close();
			}catch(IOException e){ }
		}
		
		return content;
	}
	
	public static String readFile(File file){
		return readFile(file.getAbsolutePath());
	}
}