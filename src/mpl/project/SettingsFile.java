package mpl.project;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mpl.utils.io.FileHelper;

public class SettingsFile {

	private File file;
	private ProjectManager project;
	private JSONObject settings = new JSONObject();
	
	public SettingsFile(String projectPath, ProjectManager project) {
		file = new File(projectPath + File.separator + "settings.json");
		this.project = project;
	}
	
	public void saveSettings(){
		// Write settings to the settings file
		FileHelper.writeToFile(file, settings.toJSONString());
	}

	public void readSettings(){
		// Create settings file if it's not existing
		if(!file.exists())
			createSettings();
		else {
			// Read settings from the settings file
			JSONParser parser = new JSONParser();
			try {
				JSONObject obj = (JSONObject)parser.parse(FileHelper.readFile(file));
				JSONArray jsonPackages = (JSONArray)obj.get("packages");
				
				for(Object packageObj : jsonPackages){
					JSONObject jsonPackage = (JSONObject)packageObj;
					
					// Find the package
					System.out.println("looking for package: " + (String)jsonPackage.get("name"));
					Package pkg = project.findPackage((String)jsonPackage.get("name"));
					
					// Iterate over sources
					for(Object sourceObj : (JSONArray)jsonPackage.get("files")){
						JSONObject jsonSource = (JSONObject)sourceObj;
						
						// Find the source
						Source src = pkg.findSource((String)jsonSource.get("name"));
						
						// Set modified bool
						File f = new File(pkg.directoryPath + File.separator + src.fileName);
						src.modified = f.lastModified() != (long)jsonSource.get("modified");
						
						System.out.println(src.fileName + ", modified: " + src.modified);
					}
				}
			} catch (ParseException e) {
				System.err.println("An error occurred while reading settings file!");
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createSettings(){
		// Create settings file
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Create initial settings
		JSONArray packagesArray = new JSONArray();
		iteratePackages(project.rootPackage, packagesArray);
		settings.put("packages", packagesArray);
	}
	
	@SuppressWarnings("unchecked")
	private void iteratePackages(Package pkg, JSONArray jsonPackages){
		// Create package object
		JSONObject jsonPkg = new JSONObject();
		jsonPkg.put("name", pkg.getFullName());
		
		// Create files array
		JSONArray filesArray = new JSONArray();
		pkg.sourceFiles.forEach(source -> {
			File f = new File(source.pkg.directoryPath.getAbsolutePath() + File.separator + source.fileName);
			long lastModified = f.lastModified();
			
			JSONObject sourceObj = new JSONObject();
			sourceObj.put("name", source.fileNameWithoutExtension);
			sourceObj.put("modified", lastModified);
			
			filesArray.add(sourceObj);
		});
		
		// Add files array to the package object
		jsonPkg.put("files", filesArray);
		
		// Add package to the settings
		jsonPackages.add(jsonPkg);
		
		// Iterate sub-packages
		for(Package p : pkg.childPackages)
			iteratePackages(p, jsonPackages);
	}
}