package mpl.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mpl.utils.Env;
import mpl.utils.io.FileHelper;

public class SettingsFile {

	private File settingsFile;
	private ProjectManager project;
	private JSONObject jsonSettings = new JSONObject();
	
	public SettingsFile(String projectPath, ProjectManager project) {
		settingsFile = new File(projectPath + File.separator + "settings.json");
		
		// Read settings file
		try {
			JSONParser parser = new JSONParser();
			jsonSettings = (JSONObject)parser.parse(FileHelper.readFile(settingsFile));
		} catch (ParseException e) {
			System.err.println("An error occurred while reading settings file!");
			e.printStackTrace();
			System.exit(-1);
		}
		
		this.project = project;
	}
	
	public void saveSettings(){
		// Write settings to the settings file
		FileHelper.writeToFile(settingsFile, jsonSettings.toJSONString());
	}
	
	public void readSettings(){
		if(!settingsFile.exists())
			// Create settings file if it doesn't existing
			createSettings();
		else {
			// Remove delete files from settings
			removeDeletedFiles((JSONArray)jsonSettings.get("packages"));
	
			// Iterate over packages
			JSONArray jsonPackages = (JSONArray)jsonSettings.get("packages");
			
			for(Object packageObj : jsonPackages){
				JSONObject jsonPackage = (JSONObject)packageObj;
				
				// Find the package
				Package pkg = project.findPackage((String)jsonPackage.get("name"));
				
				// Iterate over sources
				for(Object sourceObj : (JSONArray)jsonPackage.get("files")){
					JSONObject jsonSourceFile = (JSONObject)sourceObj;
					
					String srcName = (String)jsonSourceFile.get("name");
					String srcOldSha1 = (String)jsonSourceFile.get("sha1");
					Boolean srcContainsMain = (Boolean)jsonSourceFile.get("containsMainFnc");
					
					// Find the source
					Source src = pkg.findSource(srcName);
					
					// Set modified flag
					src.modified = !FileHelper.getSHA1String(src.sourceFile).equals(srcOldSha1);
					
					// If source is unmodified, but object file is deleted,
					// then set it as modified in order to create new object file
					if(!src.modified) {
						File objFile = new File(project.binPath, src.fileNameWithoutExtension + "." + Env.getElfExtension());
						if(!objFile.exists()) {
							src.modified = true;
						}
					}
					
					// Set contains main function flag
					if(srcContainsMain != null) {
						src.containsMainFunction = srcContainsMain;
					}
					
				//	System.out.println(src.fileName + ", modified: " + src.modified);
				}
			}
			
			// Add newly created files to the settings
			addNewFiles(project.rootPackage);
		}
	}
	
	/**
	 * Updates SHA1 in settings for compiled source files
	 */
	@SuppressWarnings("unchecked")
	public void updateSettings() {
		// Iterate through all source files
		for(Source src : project.allSourceFiles) {
			if(src.compiled) {
				// Find source in settings
				JSONObject jsonSrcPackage = findJsonPackage(src.pkg.getFullName());
				JSONObject jsonSource = findJsonSource(src.fileNameWithoutExtension, jsonSrcPackage);
				
				// Update sha1
				String srcSha1 = FileHelper.getSHA1String(src.sourceFile);
				jsonSource.replace("sha1", srcSha1);
				
				// Update main fnc definition
				if(src.containsMainFunction) {
					if(!jsonSource.containsKey("containsMainFnc")) {
						jsonSource.put("containsMainFnc", Boolean.TRUE);
					}else{
						jsonSource.replace("containsMainFnc", Boolean.TRUE);
					}
				}else{
					if(jsonSource.containsKey("containsMainFnc")) {
						jsonSource.remove("containsMainFnc");
					}
				}
				
			//	System.out.println("Updated settings for source: " + src.fileNameWithoutExtension);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createSettings(){
		// Create settings file
		if(!settingsFile.exists()){
			try {
				settingsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Create initial settings
		JSONArray packagesArray = new JSONArray();
		addAllPackages(project.rootPackage, packagesArray);
		jsonSettings.put("packages", packagesArray);
	}
	
	/**
	 * Recursively adds newly created packages & source files to the settings
	 * 
	 * @param rootPkg - root package to start adding from
	 */
	@SuppressWarnings("unchecked")
	private void addNewFiles(Package rootPkg) {
		JSONObject jsonPackage = findJsonPackage(rootPkg.getFullName());
		
		if(jsonPackage == null) {
			// Package is not in settings,
			// add that package and it's source files (including sub-packages)
			addAllPackages(rootPkg, (JSONArray)jsonSettings.get("packages"));
		}else{
			JSONArray jsonSourcesArray = (JSONArray)jsonPackage.get("files");
			
			for(Source src : rootPkg.sourceFiles) {
				// Check if source files exist in settings
				JSONObject jsonSource = findJsonSource(src.fileNameWithoutExtension, jsonPackage);
				
				if(jsonSource == null) {
					// Source is not in settings, add that source file to the settings
					File f = new File(src.pkg.directoryPath.getAbsolutePath() + File.separator + src.fileName);
					String fSha1 = FileHelper.getSHA1String(f);
					
					jsonSource = new JSONObject();
					jsonSource.put("name", src.fileNameWithoutExtension);
					jsonSource.put("sha1", fSha1);
					
					jsonSourcesArray.add(jsonSource);
					
					// Mark all new source files as modified, so they get compiled
					src.modified = true;
				}
			}
		}
		
		// Iterate through sub-packages
		for(Package subPackage : rootPkg.childPackages) {
			addNewFiles(subPackage);
		}
	}
	
	/**
	 * Removes deleted packages & source files from the settings
	 * 
	 * @param jsonPackagesArray - JSONArray of packages
	 */
	@SuppressWarnings("unchecked")
	private void removeDeletedFiles(JSONArray jsonPackagesArray){
		// To avoid concurrent modification exception
		ArrayList<Object> packagesToRemove = new ArrayList<Object>();
		
		// Find deleted packages
		for(Object jsonPackageObj : jsonPackagesArray) {
			JSONObject jsonPackage = (JSONObject)jsonPackageObj;
			
			// Check if package still exists
			String fullPkgName = (String)jsonPackage.get("name");
			Package pkg = project.findPackage(fullPkgName);
			
			if(pkg != null) {
				// To avoid concurrent modification exception
				ArrayList<Object> sourcesToRemove = new ArrayList<Object>();

				// Find deleted source files
				JSONArray jsonPackageFiles = (JSONArray)jsonPackage.get("files");
				
				for(Object jsonFileObj : jsonPackageFiles) {
					JSONObject jsonSource = (JSONObject)jsonFileObj;
					
					// Check if source file still exists
					String sourceName = (String)jsonSource.get("name");
					if(pkg.findSource(sourceName) == null) {
						sourcesToRemove.add(jsonFileObj);
					}
				}
				
				// Remove deleted sources
				jsonPackageFiles.removeAll(sourcesToRemove);
			}else{
				// Package was deleted
				packagesToRemove.add(jsonPackageObj);
			}
		}
		
		// Remove deleted packages
		jsonPackagesArray.removeAll(packagesToRemove);
	}

	/**
	 * Recursively adds all packages and their sources to the JSONArray.
	 * This function is called only when creating new settings file or when
	 * new package was found in source directory.
	 * 
	 * @param pkg - package to add
	 * @param jsonPackagesArray - JSONArray of packages
	 */
	@SuppressWarnings("unchecked")
	private void addAllPackages(Package pkg, JSONArray jsonPackagesArray){
		// Create package object
		JSONObject jsonPkg = new JSONObject();
		jsonPkg.put("name", pkg.getFullName());
		
		// Create files array
		JSONArray filesArray = new JSONArray();
		pkg.sourceFiles.forEach(source -> {
			File f = new File(source.pkg.directoryPath.getAbsolutePath() + File.separator + source.fileName);
			String fSha1 = FileHelper.getSHA1String(f);
			
			JSONObject sourceObj = new JSONObject();
			sourceObj.put("name", source.fileNameWithoutExtension);
			sourceObj.put("sha1", fSha1);
			
			filesArray.add(sourceObj);
		});
		
		// Add files array to the package object
		jsonPkg.put("files", filesArray);
		
		// Add package to the settings
		jsonPackagesArray.add(jsonPkg);
		
		// Iterate sub-packages
		for(Package p : pkg.childPackages)
			addAllPackages(p, jsonPackagesArray);
	}

	
	private JSONObject findJsonPackage(String fullName) {
		JSONArray jsonPackages = (JSONArray)jsonSettings.get("packages");
		for(Object jsonPackageObj : jsonPackages) {
			JSONObject jsonPackage = (JSONObject)jsonPackageObj;
			String jsonPackageName = (String)jsonPackage.get("name");
			
			if(fullName.equals(jsonPackageName))
				return jsonPackage;
		}
		
		return null;
	}
	
	private JSONObject findJsonSource(String sourceName, JSONObject pkg) {
		JSONArray jsonFiles = (JSONArray)pkg.get("files");
		
		for(Object jsonSourceObj : jsonFiles) {
			JSONObject jsonSource = (JSONObject)jsonSourceObj;
			String jsonSourceName = (String)jsonSource.get("name");
			
			if(jsonSourceName.equals(sourceName))
				return jsonSource;
		}
		
		return null;
	}
}