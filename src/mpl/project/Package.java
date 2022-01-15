package mpl.project;

import java.io.File;
import java.util.ArrayList;

public class Package {
	public static final int ERROR_NON_EXISTING_DIRECTORY = 1;
	
	protected ProjectManager projectManager;
	protected File directoryPath;
	protected String packageName = "";
	public Package parentPackage = null;
	public ArrayList<Source> sourceFiles = new ArrayList<Source>();
	public ArrayList<Package> childPackages = new ArrayList<Package>();
	
	public Package(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	
	/** Opens source files from package and it's sub packages
	 * 
	 *  @return 0 if success, otherwise error code is returned */
	public int openPackageSourceFiles(File directory, Package parent, boolean isRootPackage){
		// Check if project directory exists
		if(!directory.exists()){
			// If not, print an error
			System.err.println("Package '" + directory.getAbsolutePath() +"' does not exist!");
			return ERROR_NON_EXISTING_DIRECTORY;
		}
		
		// Set package directory path
		directoryPath = directory;
		
		// Set parent package
		parentPackage = parent;
		
		// Set package name
		//packageName = parent == null ? "" : (parent.packageName + "/");
		if(isRootPackage)
			packageName = "";
		else
			packageName = directoryPath.getAbsolutePath().substring(directoryPath.getAbsolutePath().lastIndexOf(File.separator)+1,
																	directoryPath.getAbsolutePath().length());
		
		// Find sources and packages inside current directory (package) (@param File directory)
		for(File f : directory.listFiles()){
			if(f.isDirectory()){
				// It's a sub-package, open source files from it
				Package pkg = new Package(projectManager);
				pkg.openPackageSourceFiles(f, this, false);
				
				// Add it to the child packages list
				childPackages.add(pkg);
			}else{
				// It's not a directory, check if that's a source file
				if(f.getName().endsWith(".mpl")){
					// If so, open it and add it to the source files list
					Source src = new Source();
					src.open(f, this);
					sourceFiles.add(src);
				}
			}
		}
		
		return 0;
	}
	
	public Package findPackage(String name){
		for(Package p : childPackages){
			if(p.packageName.equals(name)){
				return p;
			}
		}
		
		return null;
	}
	
	public Source findSource(String name){
		for(Source src : sourceFiles){
			if(src.fileNameWithoutExtension.equals(name)){
				return src;
			}
		}
		
		return null;
	}
	
	public File getDirectory() {
		return directoryPath;
	}
	
	public String getName() {
		return packageName;
	}
	
	public String getFullName(){
		String fullName = packageName;
		
		Package p = parentPackage;
		while(p != null){
			fullName = p.packageName + "/" + fullName;
			p = p.parentPackage;
		}
		
		return fullName;
	}
	
	@Override
	public String toString(){
		return "package '" + getFullName() + "', #sources: " + sourceFiles.size();
	}
}