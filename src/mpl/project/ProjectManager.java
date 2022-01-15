package mpl.project;

import java.io.File;
import java.util.ArrayList;

public class ProjectManager {
	public static final int ERROR_NON_EXISTING_DIRECTORY = 1;
	public static final int ERROR_NON_DIRECTORY = 2;
	
	// Project parameters
	public File srcPath;
	public File binPath;
	public SettingsFile settings;
	public String executableName;
	
	// Packages & sources
	protected Package rootPackage;
	protected ArrayList<Source> allSourceFiles = new ArrayList<Source>();
	
	public ProjectManager() {
	}
	
	/** Opens all project source files
	 * 
	 * @return Error code if there's any, otherwise 0 is returned */
	public int openProject(File inputFile){
		File directory = inputFile;
		
		// Check if project directory exists
		if(!directory.exists()){
			// Doesn't exist, print error
			System.err.println("Directory '" + directory.getAbsolutePath() +"' is not existing!");
			return ERROR_NON_EXISTING_DIRECTORY;
		}
		
		// Check if project path is directory
		if(!directory.isDirectory()){
			// If not, print an error
			System.err.println("Project path is not a directory!");
			return ERROR_NON_DIRECTORY;
		}
		
		// Set src and bin paths
		srcPath = new File(directory.getAbsolutePath() + File.separator + "src");
		binPath = new File(directory.getAbsolutePath() + File.separator + "bin");
		
		// Read settings
		settings = new SettingsFile(directory.getAbsolutePath(), this);
		
		// Set executable name
		executableName = directory.getName();
		
		// Create src and bin directories if they don't exist
		if(!srcPath.exists())
			srcPath.mkdir();
		
		if(!binPath.exists())
			binPath.mkdir();
		
		// Create root package
		rootPackage = new Package(this);
		
		// Open all source files from the root package and it's sub packages
		rootPackage.openPackageSourceFiles(srcPath, null, true);
		
		// Find all source files
		findAllSourceFiles(rootPackage);
		
		// Read files settings
		//settings.readSettings();
		
		return 0;
	}
	
	/** Finds package by specified name */
	public Package findPackage(String fullName){
		// Check if it's a root package
		if(fullName.equals(""))
			return rootPackage;
		
		if(fullName.contains("/")){
			String[] splitPackages = fullName.split("/");
			
			int i = 1;
			Package nextPackage = rootPackage.findPackage(splitPackages[i]);
			while(nextPackage != null){
				rootPackage = nextPackage;
				
				i++;
				if(i == splitPackages.length)
					break;
				
				nextPackage = rootPackage.findPackage(splitPackages[i]);
			}
			
			if(nextPackage == null && i < splitPackages.length)
				return null;
		}else{
			// Package in root src folder
			Package childPackage = rootPackage.findPackage(fullName);
			return childPackage;
		}
		
		return null;
	}
	
	/** Finds all source files inside @param Package and sub packages,
	 * and puts them into the list (allSourceFiles) */
	private void findAllSourceFiles(Package p){
		for(Source src : p.sourceFiles)
			allSourceFiles.add(src);
		
		// Find in sub-packages
		for(Package child : p.childPackages)
			findAllSourceFiles(child);
	}
	
	public ArrayList<Source> getSourceFiles(){
		return allSourceFiles;
	}
	
	public Package getRootPackage() {
		return rootPackage;
	}

	public String getBinaryPath() {
		return binPath.getAbsolutePath();
	}
	
	public String getExecutableName() {
		return executableName;
	}
	
	public void printProjectTree(){
		printSubpackages(rootPackage, "");
	}
	
	private void printSubpackages(Package p, String tabs){
		System.out.println(tabs + p.getFullName());
		
		for(Source f : p.sourceFiles){
			System.out.println(tabs + "\t" + f.fileName);
		}
		
		for(Package pkg : p.childPackages){
			printSubpackages(pkg, tabs + "\t");
		}
	}
}