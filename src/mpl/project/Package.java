package mpl.project;

import java.io.File;
import java.util.ArrayList;

/**
 * Package class contains a list of source files in it's directory and
 * a list of it's children packages.
 * In order to create new Package object, one must specify {@link mpl.project.ProjectManager}
 * to the constructor and then call {@link mpl.project.Package#openPackageSourceFiles(File, Package, boolean)}
 */
public class Package {
	public static final int ERROR_NON_EXISTING_DIRECTORY = 1;
	
	protected ProjectManager projectManager;
	protected File directoryPath;
	protected String packageName = "";
	protected boolean isRootPackage = false;
	public Package parentPackage = null;
	public ArrayList<Source> sourceFiles = new ArrayList<Source>();
	public ArrayList<Package> childPackages = new ArrayList<Package>();
	
	public Package(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	
	/**
	 * Opens source files from this package and also opens all of it's sub-packages
	 * 
	 * @return 0 if success, otherwise error code is returned
	 */
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
		
		// Set root package flag
		this.isRootPackage = isRootPackage;
		
		// Set package name
		if(isRootPackage)
			packageName = "";
		else
			packageName = directoryPath.getAbsolutePath().substring(directoryPath.getAbsolutePath().lastIndexOf(File.separator)+1,
																	directoryPath.getAbsolutePath().length());
		
		// Find sources and packages inside current directory (package)
		for(File f : directory.listFiles()){
			if(f.isDirectory()){
				// Found sub-package, open source files from that sub-package
				Package subPackage = new Package(projectManager);
				subPackage.openPackageSourceFiles(f, this, false);
				
				// Add sub-package to the child packages list
				childPackages.add(subPackage);
			}else{
				// It's not a directory, check if that's a source file
				if(f.getName().endsWith(".mpl")){
					// Open source file and add it to the sources list
					Source src = new Source();
					src.open(f, this);
					sourceFiles.add(src);
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Finds sub-package in this package
	 * 
	 * @param name - name of sub-package to look for
	 * @return {@link Package} object of specified sub-package.
	 * Null is returned if specified sub-package is not found in this package.
	 */
	public Package findPackage(String name){
		for(Package p : childPackages){
			if(p.packageName.equals(name)){
				return p;
			}
		}
		
		return null;
	}
	
	/**
	 * @param name - name of source file without extension
	 * @return {@link Source} object of specified source file, from this package.
	 * Null is returned if specified source file is not found in this package.
	 */
	public Source findSource(String name){
		for(Source src : sourceFiles){
			if(src.fileNameWithoutExtension.equals(name)){
				return src;
			}
		}
		
		return null;
	}
	
	/**
	 * @return File with absolute directory path to this package
	 */
	public File getDirectory() {
		return directoryPath;
	}
	
	/**
	 * @return name of package, not including/full/name
	 */
	public String getName() {
		return packageName;
	}
	
	/**
	 * @return full/name/of/package
	 */
	public String getFullName(){
		String fullName = packageName;
		
		Package p = parentPackage;
		while(p != null && !p.isRootPackage){
			fullName = p.packageName + "/" + fullName;
			p = p.parentPackage;
		}
		
		return fullName;
	}
	
	@Override
	public String toString(){
		return "package: " + getFullName() + "; sources: " + sourceFiles.size();
	}
}