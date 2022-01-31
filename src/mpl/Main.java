package mpl;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import mpl.compiler.Compiler;
import mpl.compiler.CompilerOptions;
import mpl.project.ProjectManager;
import mpl.utils.ProcessEx;

public class Main {
	private static Options options = new Options();
	private static HelpFormatter formatter = new HelpFormatter();
	//private static Compiler compiler = new Compiler();
	
	static {
		Option help = new Option("h", "help", false, "print this message");
		Option source = new Option("s", "source", true, "specify input");
		Option format = new Option("f", "format", true, "output format");
		Option arch = new Option("a", "arch", true, "output architecture");
		Option output = new Option("o", "output", true, "place output into");
		
		source.setArgName("file");
		format.setArgName("format");
		arch.setArgName("arch");
		output.setArgName("file");

		options.addOption(help);
		options.addOption(source);
		options.addOption(format);
		options.addOption(arch);
		options.addOption(output);
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 0){
			formatter.printHelp("compiler", options);
			return;
		}
		
		try {
			CommandLineParser cliParser = new DefaultParser();
			CommandLine cli = cliParser.parse(options, args);
			
			CompilerOptions cmpOptions = new CompilerOptions();
			
			if(cli.hasOption("s")){
				cmpOptions.inputFile = new File(cli.getOptionValue("s"));
			}else{
				System.err.println("Input file not specified");
				System.exit(-1);
			}
			
			if(cli.hasOption("f")){
				String value = cli.getOptionValue("f");
				switch(value){
				case "bin": cmpOptions.outputFormat = CompilerOptions.COMPILE_FORMAT_BIN; break;
				case "elf": cmpOptions.outputFormat = CompilerOptions.COMPILE_FORMAT_ELF; break;
				case "asm": cmpOptions.outputFormat = CompilerOptions.COMPILE_FORMAT_ASM; break;
				case "all": cmpOptions.outputFormat = CompilerOptions.COMPILE_FORMAT_ALL; break;
				case "lib": cmpOptions.outputFormat = CompilerOptions.COMPILE_FORMAT_LIB; break;
				default:
					System.err.println("Unknown output format '" + value + "'");
					System.exit(-1);
				}
			}
			
			if(cli.hasOption("o")){
				cmpOptions.outputFile = new File(cli.getOptionValue("o"));
			}else{
				cmpOptions.outputFile = new File(getOutputFile(cmpOptions.inputFile, cmpOptions.outputFormat));
			}
			
			if(cli.hasOption("a")){
				String value = cli.getOptionValue("a");
				switch(value){
				case "32": cmpOptions.outputArch = CompilerOptions.COMPILE_ARCH_32; break;
				case "64": cmpOptions.outputArch = CompilerOptions.COMPILE_ARCH_64; break;
				default:
					System.err.println("Unknown output architecture '" + value + "'");
					System.exit(-1);
				}
			}

			// Open project
			ProjectManager manager = new ProjectManager();
			manager.openProject(cmpOptions.inputFile);
			
			// Compile project
			Compiler compiler = new Compiler(cmpOptions, manager);
			compiler.compileAll();
			
			// Close project
			manager.close();
			
			// Run executable
			System.out.println("");
			new ProcessEx("sh -c ./" + manager.executableName, manager.binPath.getAbsolutePath()).start();
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}
	
	private static String getOutputFile(File inputFile, int outputFormat){
		if(outputFormat == CompilerOptions.COMPILE_FORMAT_ALL){
			return inputFile.getParent();
		}
		return getNameWithoutExtension(inputFile) + getFileExtension(outputFormat);
	}
	
	private static String getNameWithoutExtension(File f){
		String name = f.getName();
		
		if(name.lastIndexOf(".") > 0)
			name = name.substring(0, name.lastIndexOf("."));
		
		return name;
	}
	
	private static String getFileExtension(int format){
		switch(format){
		case CompilerOptions.COMPILE_FORMAT_BIN:
			return "";
		case CompilerOptions.COMPILE_FORMAT_ELF:
			return ".o";
		case CompilerOptions.COMPILE_FORMAT_ASM:
			return ".asm";
		case CompilerOptions.COMPILE_FORMAT_LIB:
			return ".so";
		}
		
		return "";
	}
}