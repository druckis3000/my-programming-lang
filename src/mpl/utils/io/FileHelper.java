package mpl.utils.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	
	public static byte[] getSHA1(File file) {
		try {
			MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
			InputStream fis = new FileInputStream(file);
			int n = 0;
			byte[] buffer = new byte[8192];
			while(n != -1) {
				n = fis.read(buffer);
				if(n > 0) {
					sha1Digest.update(buffer, 0, n);
				}
			}
			fis.close();
			
			return sha1Digest.digest();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String getSHA1String(File file) {
		byte[] sha1 = getSHA1(file);
		String sha1Str = "";
		if(sha1 != null) {
			for(byte b : sha1) {
				sha1Str += String.format("%02x", Math.abs(b));
			}
		}
		return sha1Str;
	}
}