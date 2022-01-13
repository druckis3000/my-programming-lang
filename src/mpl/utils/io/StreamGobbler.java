package mpl.utils.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
	private InputStream inputStream;
	
	public StreamGobbler(InputStream input) {
		inputStream = input;
	}

	@Override
	public void run(){
		try{
			InputStreamReader streamReader = new InputStreamReader(inputStream);
			BufferedReader reader = new BufferedReader(streamReader);
			String line = null;
			while((line = reader.readLine()) != null){
				System.out.println(line);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}