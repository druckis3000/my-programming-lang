package mpl.compiler.asm;

import mpl.syntactic.parts.PBooleanLiteral;
import mpl.syntactic.parts.PCharLiteral;
import mpl.syntactic.parts.PDataType;
import mpl.syntactic.parts.PFunctionCall;
import mpl.syntactic.parts.PFunctionParameter;
import mpl.syntactic.parts.PGlobalVariable;
import mpl.syntactic.parts.PHexLiteral;
import mpl.syntactic.parts.PIntegerLiteral;
import mpl.syntactic.parts.PLocalVariable;
import mpl.syntactic.parts.PNullLiteral;
import mpl.syntactic.parts.PProgramPart;
import mpl.syntactic.parts.PStringLiteral;
import mpl.syntactic.parts.PVarAccessor;
import mpl.syntactic.parts.PVariable;

public class AsmCommon {
	
	private static AsmProgram program = null;
	
	public static void initialize(AsmProgram program){
		AsmCommon.program = program;
	}
	
	public static String movToRegister(AsmRegister reg, String value){
		return "mov " + reg.getName() + ", " + value;
	}
	
	public static String movToRegister(AsmRegister reg, String address, PDataType dataType){
		return getMovInstruction(dataType) + " " + reg.getName() + ", " + dataType.getSizeDirective() + " " + address;
	}
	
	public static String movToRegister(AsmRegister reg, PVarAccessor var){
		return var.movToRegister(reg, program);
	}
	
	public static String movToRegister(AsmRegister reg, AsmRegister regOp){
		return "mov " + reg.getName() + ", " + regOp.getName();
	}
	
	public static String leaToRegister(AsmRegister reg, String address){
		return "lea " + reg.getName() + ", " + address;
	}
	
	public static String movToRegisterPtr(AsmRegister reg, String value){
		return "mov [" + reg.getName() + "], " + value;
	}
	

	public static String movToAddress(String address, String sizeDirective, String data){
		return "mov " + sizeDirective + " " + address + ", " + data;
	}
	
	public static String movToAddress(String address, String sizeDirective, AsmRegister register){
		return movToAddress(address, sizeDirective, register.getNameBySizeDirective(sizeDirective));
	}
	
	
	public static String getVarAddress(PVariable var){
		if(var instanceof PLocalVariable){
			int ebpOffsetWithoutMinus = Math.abs(((PLocalVariable)var).ebpOffset);
			return "[" + AsmRegister.EBP.getName() + " - " + ebpOffsetWithoutMinus + "]";
		}else if(var instanceof PFunctionParameter){
			return "[" + AsmRegister.EBP.getName() + " + " + ((PFunctionParameter)var).ebpOffset + "]";
		}else if(var instanceof PGlobalVariable){
			return "[" + ((PGlobalVariable)var).nameInAssembly + "]";
		}
		return "";
	}
	
	public static String addStringToRoData(PStringLiteral string){
		return string.roDataName + ": db " + string.getAssemblyValue();
	}
	
	/* TODO: There's another solution for this. Replace data type of data parameter
	 * with POperand and then, just return data.getDataType.getSizeInBytes(),
	 * except it wouldn't work with pvaraccessor, or it would, idk lol. */
	public static long findSizeInBytes(PProgramPart data){
		if(data instanceof PIntegerLiteral){
			return ((PIntegerLiteral)data).getDataType().getSizeInBytes();
		}else if(data instanceof PHexLiteral){
			return ((PHexLiteral)data).getDataType().getSizeInBytes();
		}else if(data instanceof PStringLiteral){
			return PDataType.STRING.getSizeInBytes();
		}else if(data instanceof PCharLiteral){
			return PDataType.CHAR.getSizeInBytes();
		}else if(data instanceof PNullLiteral){
			return PDataType.NULL.getSizeInBytes();
		}else if(data instanceof PBooleanLiteral){
			return PDataType.BOOL.getSizeInBytes();
		}else if(data instanceof PLocalVariable){
			return ((PLocalVariable)data).getSizeInBytes();
		}else if(data instanceof PFunctionParameter){
			return ((PFunctionParameter)data).getSizeInBytes();
		}else if(data instanceof PGlobalVariable){
			return ((PGlobalVariable)data).getSizeInBytes();
		}else if(data instanceof PFunctionCall){
			return ((PFunctionCall)data).targetFunction.returnType.getSizeInBytes();
		}else if(data instanceof PVarAccessor){
			return ((PVarAccessor)data).target.getSizeInBytes(); //.getDataTypeUnmodified().getSizeInBytes();
		}
		
		return 0;
	}
	
	/* @return mov instruction depending on data type */
	public static String getMovInstruction(PDataType dt){
		// All pointers are 4-bytes, thus we return plain
		// "mov" instruction
		if(dt.isPointer())
			return "mov";
		
		// Short is only 2-bytes, we need to sign extend
		// register, so we return "movsx" instruction
		if(dt.plain() == PDataType.SHORT)
			return "movsx";
		
		// Char and bool is only one byte, we need to sign extend
		// register, so we return "movsx" instruction
		if(dt.plain() == PDataType.CHAR || dt.plain() == PDataType.BOOL)
			return "movsx";	
		
		// Other data types are 4-bytes, so we return
		// plain "mov" instruction
		return "mov";
	}
}
