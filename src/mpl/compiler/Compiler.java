package mpl.compiler;

import java.io.File;
import java.io.IOException;

import mpl.analysis.SemanticAnalyser;
import mpl.compiler.asm.AssemblyCodeBuilder;
import mpl.parser.ParserException;
import mpl.parser.syntactic.ASTCreator;
import mpl.parser.syntactic.parts.PProgram;
import mpl.utils.ProcessEx;
import mpl.utils.io.FileHelper;

public class Compiler {
	private ASTCreator astCreator = null;
	private SemanticAnalyser semAnalyzer = null;
	private AssemblyCodeBuilder asmBuilder = new AssemblyCodeBuilder();
	
	public Compiler(){
	}
	
	public void compile(CompilerOptions options){
		try {
			// Read the source code
			System.out.println("Reading source code...");
			astCreator = new ASTCreator(options.inputFile, null);
			
			// Check if tokens can form an allowable statements
			System.out.println("Creating abstract syntax tree...");
			PProgram program = astCreator.createAstTree();
			
			// Check if statements are correct
			System.out.println("Validating program...");
			semAnalyzer = new SemanticAnalyser(program);
			semAnalyzer.analyze();
			
			// Create assembly code
			System.out.println("Building assembly code...");
			AssemblyCodeBuilder.ADD_COMMENTS_BEFORE_ASSEMBLY_CODE = true;
			asmBuilder.createAssemblyCode(program, options);
			
			int status = 0;
			
			String nasmFormat = "-felf" + (options.outputArch == CompilerOptions.COMPILE_ARCH_64 ? "64" : "32");
			String gccFormat = "-m" + (options.outputArch == CompilerOptions.COMPILE_ARCH_64 ? "64" : "32");
			
			if(options.outputFormat == CompilerOptions.COMPILE_FORMAT_ALL){
				// Save assembly code
				FileHelper.writeToFile(options.outputFile + "/output.asm", asmBuilder.getAssemblyCode());
				
				// Compile assembly code
				ProcessEx asm = new ProcessEx("nasm " + nasmFormat + " " + options.outputFile + "/output.asm" + " -o " + options.outputFile + "/output.o");
				status = asm.start();
				if(status != 0)
					System.exit(1);
				
				// Link compiled assembly code
				ProcessEx gcc = new ProcessEx("gcc " + gccFormat + " -g " + options.outputFile + "/output.o" + " -o " + options.outputFile + "/output");
				status = gcc.start();
				if(status != 0)
					System.exit(1);
			}else{
				if(options.outputFormat == CompilerOptions.COMPILE_FORMAT_ASM){
					FileHelper.writeToFile(options.outputFile, asmBuilder.getAssemblyCode());
				}else{
					// Save assembly code
					File tmpAsm = File.createTempFile("splCompile", "asm");
					FileHelper.writeToFile(tmpAsm.getAbsolutePath(), asmBuilder.getAssemblyCode());
					
					if(options.outputFormat == CompilerOptions.COMPILE_FORMAT_ELF){
						// Compile assembly code
						ProcessEx asm = new ProcessEx("nasm " + nasmFormat + " " + tmpAsm.getAbsolutePath() + " -o " + options.outputFile);
						status = asm.start();
						if(status != 0)
							System.exit(1);
					}else{
						if(options.outputFormat == CompilerOptions.COMPILE_FORMAT_LIB){
							// Compile assembly code
							File tmpElf = File.createTempFile("splCompile", "o");
							ProcessEx asm = new ProcessEx("nasm " + nasmFormat + " " + tmpAsm.getAbsolutePath() + " -o " + tmpElf.getAbsolutePath());
							status = asm.start();
							if(status != 0)
								System.exit(1);
							
							// Compile object file to shared library
							ProcessEx shared = new ProcessEx("gcc " + gccFormat + " -shared " + tmpElf.getAbsolutePath() + " -o " + options.outputFile);
							status = shared.start();
							if(status != 0)
								System.exit(1);
						}else{
							// Compile assembly code
							File tmpElf = File.createTempFile("splCompile", "o");
							ProcessEx asm = new ProcessEx("nasm " + nasmFormat + " " + tmpAsm.getAbsolutePath() + " -o " + tmpElf.getAbsolutePath());
							status = asm.start();
							if(status != 0)
								System.exit(1);
							
							// Link compiled assembly code
							ProcessEx gcc = new ProcessEx("gcc " + gccFormat + " -g " + tmpElf.getAbsolutePath() + " -o " + options.outputFile);
							status = gcc.start();
							if(status != 0)
								System.exit(1);
						}
					}
				}
			}
			
			// Run the program
			if(options.outputFormat != CompilerOptions.COMPILE_FORMAT_LIB)
				new ProcessEx("sh -c ./output", "/home/daslee/Desktop").start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}
}