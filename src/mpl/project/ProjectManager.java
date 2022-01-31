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
	protected ArrayList<Source> modifiedSourceFiles = new ArrayList<Source>();
	
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
		
		// Create settings
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
		getAllSourceFiles(rootPackage);
		
		// Read project settings
		settings.readSettings();
		
		return 0;
	}
	
	/**
	 * Update and save project settings
	 */
	public void close() {
		settings.updateSettings();
		settings.saveSettings();
	}
	
	public void debug() {
		
	}
	
	/**
	 * Find package by it's full name in currently opened project
	 * 
	 * @param fullName - full name of the package, e.g.: main/subpackage/memory
	 * 
	 * @return {@link mpl.project.Package} object is returned if package was found, null if not found
	 */
	public Package findPackage(String fullName){
		// Check if it's a root package
		if(fullName.equals(""))
			return rootPackage;
		
		if(fullName.contains("/")){
			// Get list/of/sub-packages
			String[] splitPackages = fullName.split("/");
			
			// Iterate over sub-packages and look for specified package name
			Package nextPackage = rootPackage;
			for(int i=0; i < splitPackages.length && nextPackage != null; i++) {
				nextPackage = nextPackage.findPackage(splitPackages[i]);
			}
			
			return nextPackage;
		}else{
			// Package in root folder
			Package childPackage = rootPackage.findPackage(fullName);
			return childPackage;
		}
	}
	
	/**
	 * Finds all source files in currently opened project,
	 * and puts them into the array list
	 */
	private void getAllSourceFiles(Package p){
		// Get sources from specified package
		for(Source src : p.sourceFiles)
			allSourceFiles.add(src);
		
		// Get sources from sub-packages
		for(Package child : p.childPackages)
			getAllSourceFiles(child);
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
	
	/**
	 * Prints packages and sources tree
	 */
	public void printProjectTree(){
		printPackageTree(rootPackage, "");
	}
	
	/**
	 * Prints specified package and it's sub-packages tree, including sources
	 * 
	 * @param p - package to print
	 * @param tabs - number of tabs to add
	 */
	private void printPackageTree(Package p, String tabs){
		// Print package full name
		if(!p.isRootPackage)
			System.out.println(tabs + p.getFullName());
		
		// Print package source files
		for(Source f : p.sourceFiles){
			System.out.println(tabs + " - " + f.fileName);
		}
		
		// Print sub-package trees
		for(Package pkg : p.childPackages){
			printPackageTree(pkg, tabs + "\t");
		}
	}
}