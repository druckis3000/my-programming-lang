package mpl.parser.syntactic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import mpl.parser.syntactic.HelloParser.ArrayAccessContext;
import mpl.parser.syntactic.HelloParser.AssignmentContext;
import mpl.parser.syntactic.HelloParser.BraceElementContext;
import mpl.parser.syntactic.HelloParser.BraceGroupContext;
import mpl.parser.syntactic.HelloParser.ConditionExpContext;
import mpl.parser.syntactic.HelloParser.DeleteStatementContext;
import mpl.parser.syntactic.HelloParser.ExpressionContext;
import mpl.parser.syntactic.HelloParser.ForEachStatementContext;
import mpl.parser.syntactic.HelloParser.ForStatementContext;
import mpl.parser.syntactic.HelloParser.FunctionDeclContext;
import mpl.parser.syntactic.HelloParser.FunctionDeclStatementContext;
import mpl.parser.syntactic.HelloParser.GlobalStatementContext;
import mpl.parser.syntactic.HelloParser.ImportStatementContext;
import mpl.parser.syntactic.HelloParser.LocalStatementContext;
import mpl.parser.syntactic.HelloParser.OperandContext;
import mpl.parser.syntactic.HelloParser.PointerAssignmentStatementContext;
import mpl.parser.syntactic.HelloParser.PointerContext;
import mpl.parser.syntactic.HelloParser.PostIncrementDecrementContext;
import mpl.parser.syntactic.HelloParser.PreIncrementDecrementContext;
import mpl.parser.syntactic.HelloParser.ReferenceContext;
import mpl.parser.syntactic.HelloParser.StructDefinitionContext;
import mpl.parser.syntactic.HelloParser.StructDefinitionStatementContext;
import mpl.parser.syntactic.HelloParser.StructMemberAccessContext;
import mpl.parser.syntactic.HelloParser.StructTypeDeclarationContext;
import mpl.parser.syntactic.HelloParser.TypeCastContext;
import mpl.parser.syntactic.HelloParser.UnaryExpressionContext;
import mpl.parser.syntactic.HelloParser.UnaryNotContext;
import mpl.parser.syntactic.HelloParser.UnaryStatementContext;
import mpl.parser.syntactic.HelloParser.VarDeclarationContext;
import mpl.parser.syntactic.HelloParser.VarDefinitionContext;
import mpl.parser.syntactic.parts.PArrayInitializer;
import mpl.parser.syntactic.parts.PAssignmentStatement;
import mpl.parser.syntactic.parts.PBody;
import mpl.parser.syntactic.parts.PBooleanLiteral;
import mpl.parser.syntactic.parts.PBraceGroup;
import mpl.parser.syntactic.parts.PBreakStatement;
import mpl.parser.syntactic.parts.PCharLiteral;
import mpl.parser.syntactic.parts.PCondition;
import mpl.parser.syntactic.parts.PConditionExp;
import mpl.parser.syntactic.parts.PContinueStatement;
import mpl.parser.syntactic.parts.PDataType;
import mpl.parser.syntactic.parts.PDeleteStatement;
import mpl.parser.syntactic.parts.PDoStatement;
import mpl.parser.syntactic.parts.PElseIfStatement;
import mpl.parser.syntactic.parts.PElseStatement;
import mpl.parser.syntactic.parts.PExpression;
import mpl.parser.syntactic.parts.PForEachStatement;
import mpl.parser.syntactic.parts.PForStatement;
import mpl.parser.syntactic.parts.PFunction;
import mpl.parser.syntactic.parts.PFunctionCall;
import mpl.parser.syntactic.parts.PFunctionParameter;
import mpl.parser.syntactic.parts.PFunctionParams;
import mpl.parser.syntactic.parts.PGlobalVariable;
import mpl.parser.syntactic.parts.PHexLiteral;
import mpl.parser.syntactic.parts.PIfStatement;
import mpl.parser.syntactic.parts.PIntegerLiteral;
import mpl.parser.syntactic.parts.PLocalVariable;
import mpl.parser.syntactic.parts.PNewOperand;
import mpl.parser.syntactic.parts.PNullLiteral;
import mpl.parser.syntactic.parts.POperand;
import mpl.parser.syntactic.parts.POperator;
import mpl.parser.syntactic.parts.PPointerAssignment;
import mpl.parser.syntactic.parts.PSliceExp;
import mpl.parser.syntactic.parts.POperator.POperatorType;
import mpl.project.ProjectManager;
import mpl.project.Source;
import mpl.parser.syntactic.parts.PProgram;
import mpl.parser.syntactic.parts.PProgramPart;
import mpl.parser.syntactic.parts.PReturnStatement;
import mpl.parser.syntactic.parts.PStringLiteral;
import mpl.parser.syntactic.parts.PStructInitializer;
import mpl.parser.syntactic.parts.PStructType;
import mpl.parser.syntactic.parts.PTypeCast;
import mpl.parser.syntactic.parts.PUnaryExpression;
import mpl.parser.syntactic.parts.PVarAccessor;
import mpl.parser.syntactic.parts.PVariable;
import mpl.parser.syntactic.parts.PWhileStatement;

public class ASTCreator {
	private ProjectManager projectManager;
	private String sourceFile = "";
	private HelloLexer lexer;
	private PProgram program;

	public ASTCreator(File file, ProjectManager projectManager) {
		// Create lexer
		try {
			sourceFile = file.getName();
			this.projectManager = projectManager;
			lexer = new HelloLexer(CharStreams.fromFileName(file.getAbsolutePath()));
			// new ANTLRInputStream(new FileReader(file))
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public PProgram createAstTree(){
		// Get our matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// Pass the tokens to the parser
		HelloParser parser = new HelloParser(tokens);
				
		// Set our entry point
		HelloParser.ProgContext ctx = parser.prog();

		// Walk it and attach our listener
		ParseTreeWalker walker = new ParseTreeWalker();
		ProgramListener listener = new ProgramListener();
		walker.walk(listener, ctx);
		
		return program;
	}

	private class ProgramListener extends HelloBaseListener {

		/*
		 * Listeners
		 */

		@Override
		public void enterProg(HelloParser.ProgContext ctx) {
			super.enterProg(ctx);
			program = new PProgram(projectManager);
		}

		// Imports

		@Override
		public void enterImportStatement(ImportStatementContext ctx) {
			super.enterImportStatement(ctx);
			
			// Remove quotes from import statement
			String importPackage = ctx.StringLiteral().getText();
			importPackage = importPackage.substring(1, importPackage.length()-1);
			
			// Create package path
			String packagePath = "";
			if(importPackage.contains("/"))
				packagePath = importPackage.substring(0, importPackage.lastIndexOf("/"));
			
			// Create source name
			String sourceName = "";
			if(importPackage.contains("/"))
				sourceName = importPackage.substring(importPackage.lastIndexOf("/")+1, importPackage.length());
			else
				sourceName = importPackage;
			
			// Find the package
			mpl.project.Package pkg = program.projectManager.findPackage(packagePath);
			if(pkg == null){
				// Package was not found
				System.err.println("Package '" + packagePath + "' is not existing!");
				return;
			}
			
			// Find the source file
			Source src = pkg.findSource(sourceName);
			if(src == null){
				// Source was not found
				System.err.println("Unable to import '" + importPackage + "', file is not existing");
				return;
			}
			
			// Add imported source to the program
			program.importedSources.add(src);
		}
		
		// Struct
		
		@Override
		public void enterStructTypeDeclaration(StructTypeDeclarationContext ctx) {
			super.enterStructTypeDeclaration(ctx);

			PStructType struct = new PStructType(ctx.Identifier().getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			struct.antlrHash = ctx.hashCode();
			
			program.structTypes.add(struct);
		}
		
		// Function

		private PFunction createFunctionDeclaration(FunctionDeclContext ctx){
			PFunction function = new PFunction(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			// Mark this function as declaration
			function.isDeclaration = true;
			// Set function's name
			function.name = ctx.name.getText();
			// If first char of function name is upper case,
			// it means that function is public
			function.isPublic = Character.isUpperCase(function.name.charAt(0));
			
			// Create return type
			if(ctx.dataType() != null){
				function.returnType = PDataType.fromToken(ctx.dataType().start.getText(), ctx.pointer() != null, false, ctx.dims() != null, ctx.dimsSlice() != null);
			}else if(ctx.retType != null){
				function.returnType = PDataType.fromToken("struct", ctx.pointer() != null, false, ctx.dims() != null, ctx.dimsSlice() != null);
			
				// Create return struct type
				PStructType structType = new PStructType(ctx.retType.getText(), sourceFile, ctx.retType.getLine(), ctx.retType.getCharPositionInLine(), function);
				function.returnStruct = structType;
			}
			
			// If that's a struct's function, create struct type
			if(ctx.structFunction() != null){
				PDataType dt = PDataType.STRUCT;
				PFunctionParameter structParam = new PFunctionParameter(dt, "this", sourceFile, ctx.structFunction().start.getLine(), ctx.structFunction().start.getCharPositionInLine(), function);
				structParam.struct = new PStructType(ctx.structFunction().structType.getText(), sourceFile, ctx.structFunction().start.getLine(), ctx.structFunction().start.getCharPositionInLine(), function);
				
				// Assign struct to the 'this' parameter
				function.thisParam = structParam;
			}
			
			function.antlrHash = ctx.hashCode();
			function.body.lineInSourceCode = -1;
			function.body.antlrHash = -1;

			// Create function parameters, if there's any
			if (ctx.functionParam() != null) {
				createFunctionParams(function.params, ctx.functionParam());
			}
			
			return function;
		}
		
		@Override
		public void enterFunctionDeclStatement(FunctionDeclStatementContext ctx) {
			// Create function declaration
			PFunction function = createFunctionDeclaration(ctx.functionDecl());
			
			// Add function to the functions list
			program.functions.add(function);
		}
		
		public void enterFunctionDef(HelloParser.FunctionDefContext ctx) {
			// Create function
			PFunction function = createFunctionDeclaration(ctx.functionDecl());
			// Unmark function as declaration
			function.isDeclaration = false;
			// Prepare body
			function.body.lineInSourceCode = ctx.body().start.getLine();
			function.body.antlrHash = ctx.body().hashCode();
			
			// Add function to the functions list
			program.functions.add(function);
		}

		// Flow control statements

		@Override
		public void enterIfStatement(HelloParser.IfStatementContext ctx) {
			super.enterIfStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			// Create if statement
			PIfStatement ifSt = new PIfStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			ifSt.body.lineInSourceCode = ctx.body().start.getLine();
			ifSt.body.antlrHash = ctx.body().hashCode();
			ifSt.condition = createConditionExp(ctx.conditionExp(), ifSt);

			parent.statements.add(ifSt);

			// Create else-if statements, if there's any
			if (ctx.elseIfStatement().size() > 0) {
				for (HelloParser.ElseIfStatementContext elIfCtx : ctx.elseIfStatement()) {
					PElseIfStatement elIfSt = new PElseIfStatement(sourceFile, elIfCtx.start.getLine(), elIfCtx.start.getCharPositionInLine(), parent);
					elIfSt.body.lineInSourceCode = elIfCtx.body().start.getLine();
					elIfSt.body.antlrHash = elIfCtx.body().hashCode();
					elIfSt.condition = createConditionExp(elIfCtx.conditionExp(), elIfSt);

					parent.statements.add(elIfSt);
				}
			}

			// Create else statement, if there is one
			if (ctx.elseStatement() != null) {
				PElseStatement elseSt = new PElseStatement(sourceFile, ctx.elseStatement().start.getLine(), ctx.elseStatement().start.getCharPositionInLine(), parent);
				elseSt.body.lineInSourceCode = ctx.elseStatement().body().start.getLine();
				elseSt.body.antlrHash = ctx.elseStatement().body().hashCode();
				
				parent.statements.add(elseSt);
			}
		}

		@Override
		public void enterWhileStatement(HelloParser.WhileStatementContext ctx) {
			super.enterWhileStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());

			PWhileStatement whileSt = new PWhileStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			whileSt.body.lineInSourceCode = ctx.body().start.getLine();
			whileSt.body.antlrHash = ctx.body().hashCode();
			whileSt.condition = createConditionExp(ctx.conditionExp(), whileSt);

			parent.statements.add(whileSt);
		}

		@Override
		public void enterDoWhileStatement(HelloParser.DoWhileStatementContext ctx) {
			super.enterDoWhileStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());

			PDoStatement doSt = new PDoStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			doSt.body.lineInSourceCode = ctx.body().start.getLine();
			doSt.body.antlrHash = ctx.body().hashCode();
			doSt.condition = createConditionExp(ctx.conditionExp(), doSt);

			parent.statements.add(doSt);
		}

		@Override
		public void enterForStatement(ForStatementContext ctx) {
			super.enterForStatement(ctx);
			
			PBody parent = findBody(ctx.parent.parent.hashCode());

			// Create for statement
			PForStatement forSt = new PForStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			forSt.body.lineInSourceCode = ctx.body().start.getLine();
			forSt.body.antlrHash = ctx.body().hashCode();
			forSt.condition = createConditionExp(ctx.conditionExp(), forSt.initBody); // forSt.body
			
			// Create for statement initializer
			if(ctx.varDefinition() != null){
				createLocalVarDefinition(ctx.varDefinition(), forSt.initBody);
			}else if(ctx.assignment() != null){
				PAssignmentStatement assignment = createAssignment(ctx.assignment(), forSt.initBody);
				forSt.initBody.statements.add(assignment);
			}
			
			// Create for statement incrementor
			PUnaryExpression unaryExp = createUnaryExpression(ctx.unaryExpression(), forSt.incrementBody);
			forSt.incrementBody.statements.add(unaryExp);

			parent.statements.add(forSt);
		}
		
		@Override
		public void enterForEachStatement(ForEachStatementContext ctx) {
			super.enterForEachStatement(ctx);
			
			PBody parent = findBody(ctx.parent.parent.hashCode());

			// Create for each statement
			PForEachStatement forSt = new PForEachStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			forSt.body.lineInSourceCode = ctx.body().start.getLine();
			forSt.body.antlrHash = ctx.body().hashCode();
			
			// Create for each statement variable
			forSt.iteratingVar = new PLocalVariable(sourceFile, ctx.var.getLine(), ctx.var.getCharPositionInLine(), forSt);
			forSt.iteratingVar.name = ctx.var.getText();
			
			// Create for each statement array
			forSt.array = new PVariable(sourceFile, ctx.array.getLine(), ctx.array.getCharPositionInLine(), forSt);
			forSt.array.name = ctx.array.getText();
			
			parent.statements.add(forSt);
		}
		
		// Plain statements

		@Override
		public void enterStructDefinitionStatement(StructDefinitionStatementContext ctx) {
			super.enterStructDefinitionStatement(ctx);
			
			if(ctx.parent instanceof GlobalStatementContext){
				createGlobalStructDefinition(ctx.structDefinition());
			}else if(ctx.parent instanceof LocalStatementContext){
				createLocalStructDefinition(ctx.structDefinition());
			}
		}
		
		@Override
		public void enterVarDefinitionStatement(HelloParser.VarDefinitionStatementContext ctx) {
			super.enterVarDefinitionStatement(ctx);
			
			if(ctx.parent instanceof GlobalStatementContext){
				createGlobalVarDefinition(ctx.varDefinition());
			}else if(ctx.parent instanceof LocalStatementContext){
				createLocalVarDefinition(ctx.varDefinition(), null);
			}
		}
		
		@Override
		public void enterVarDeclarationStatement(HelloParser.VarDeclarationStatementContext ctx) {
			super.enterVarDeclarationStatement(ctx);
			
			if(ctx.parent instanceof GlobalStatementContext){
				createGlobalVarDeclaration(ctx.varDeclaration());
			}else if(ctx.parent instanceof LocalStatementContext){
				createLocalVarDeclaration(ctx.varDeclaration(), null);
			}else if(ctx.parent instanceof StructTypeDeclarationContext){
				createStructMemberDeclaration(ctx.varDeclaration(), null);
			}
		}

		@Override
		public void enterAssignmentStatement(HelloParser.AssignmentStatementContext ctx) {
			super.enterAssignmentStatement(ctx);
			
			PBody parentBody = findBody(ctx.parent.parent.hashCode());
			parentBody.statements.add(createAssignment(ctx.assignment(), parentBody));
		}
		
		@Override
		public void enterPointerAssignmentStatement(PointerAssignmentStatementContext ctx) {
			super.enterPointerAssignmentStatement(ctx);
			
			// Find parent body
			PBody parentBody = findBody(ctx.parent.parent.hashCode());
			
			// Create pointer assignment statement
			PPointerAssignment ptrAssignment = new PPointerAssignment(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parentBody);
			ptrAssignment.pointerExp = createExpression(ctx.pointerAssignment().ptrExp, ptrAssignment);
			ptrAssignment.exp = createExpression(ctx.pointerAssignment().exp, ptrAssignment);
			
			parentBody.statements.add(ptrAssignment);
		}
		
		@Override
		public void enterFunctionCallStatement(HelloParser.FunctionCallStatementContext ctx) {
			super.enterFunctionCallStatement(ctx);
			
			PBody parent = findBody(ctx.parent.parent.hashCode());
			parent.statements.add(createFunctionCall(ctx.functionCall(), parent));
		}

		@Override
		public void enterBreakStatement(HelloParser.BreakStatementContext ctx) {
			super.enterBreakStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			PBreakStatement breakSt = new PBreakStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			parent.statements.add(breakSt);
		}

		@Override
		public void enterContinueStatement(HelloParser.ContinueStatementContext ctx) {
			super.enterContinueStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			PContinueStatement continueSt = new PContinueStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			parent.statements.add(continueSt);
		}
		
		@Override
		public void enterReturnStatement(HelloParser.ReturnStatementContext ctx) {
			super.enterReturnStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			PReturnStatement returnSt = new PReturnStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
			if(ctx.expression() != null){
				returnSt.returnData = createExpression(ctx.expression(), returnSt);
			}
			
			parent.statements.add(returnSt);
		}
		
		@Override
		public void enterUnaryStatement(UnaryStatementContext ctx) {
			super.enterUnaryStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			// Create unary expression
			PUnaryExpression unaryExp = createUnaryExpression(ctx.unaryExpression(), parent);
			
			// Add unary expression statement to the parent body
			parent.statements.add(unaryExp);
		}
		
		@Override
		public void enterDeleteStatement(DeleteStatementContext ctx) {
			super.enterDeleteStatement(ctx);
			PBody parent = findBody(ctx.parent.parent.hashCode());
			
			// Create delete statement
			PDeleteStatement delete = new PDeleteStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
			// Create var accessor
			PVarAccessor var = createVarAccessor(ctx.Identifier().getSymbol(), null, null, ctx.arrayAccess(), ctx.structMemberAccess(), null, delete);
			
			// Set var
			delete.var = var;
			
			// Add statement to the parent body
			parent.statements.add(delete);
		}
		
		/*
		 * Other functions
		 */

		public void createFunctionParams(PFunctionParams params, HelloParser.FunctionParamContext ctx) {
			boolean isPtr = ctx.varDeclaration().pointer() != null;
			boolean isArray = ctx.varDeclaration().dims() != null;
			boolean isSlice = ctx.varDeclaration().dimsSlice() != null;
			String name = ctx.varDeclaration().id.getText();
			
			// Create data type
			// There's no array data type in function params, 
			// since arrays are passed as references to the functions
			String dataTypeStr = ctx.varDeclaration().dataType() != null ? ctx.varDeclaration().dataType().start.getText() : "struct";
			PDataType dataType = PDataType.fromToken(dataTypeStr, isPtr | isArray, false, false, isSlice);
			
			// Create function param
			PFunctionParameter param = new PFunctionParameter(dataType, name, sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), params);
			
			// Create struct param if needed
			if(dataType.plain() == PDataType.STRUCT){
				param.struct = new PStructType(ctx.varDeclaration().dt.getText(), sourceFile, param.lineInSourceCode, param.columnInSourceCode, program);
				if(ctx.varDeclaration().pkg != null){
					param.struct.pkg = new Source();
					param.struct.pkg.fileNameWithoutExtension = ctx.varDeclaration().pkg.getText();
				}
			}
			
			// Add param to the params list
			params.params.add(param);

			// If there's more parameters to be created, let's do it
			if (ctx.functionParam() != null) {
				createFunctionParams(params, ctx.functionParam());
			}
		}
		
		public void createFunctionArgs(PFunctionCall fCall, HelloParser.FunctionArgumentContext ctx) {
			PExpression arg = createExpression(ctx.expression(), fCall);
			fCall.arguments.add(arg);
			
			// If there's more arguments to be created, let's do it
			if (ctx.functionArgument() != null) {
				createFunctionArgs(fCall, ctx.functionArgument());
			}
		}

		public PFunctionCall createFunctionCall(HelloParser.FunctionCallContext ctx, PProgramPart parent){
			PFunctionCall fCall = new PFunctionCall(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			fCall.functionName = ctx.fncName.getText();
			fCall.isCFunctionCall = fCall.functionName.startsWith("_c_");
			
			// Create function call args
			if(ctx.functionArgument() != null){
				createFunctionArgs(fCall, ctx.functionArgument());
			}
			
			// Create struct function call
			if(ctx.struct != null){
				// TODO: Struct must be passed by reference, so that function
				// can manipulate struct members by accessing 'this' keyword
				PVarAccessor varAccessor = new PVarAccessor(sourceFile, ctx.struct.getLine(), ctx.struct.getCharPositionInLine(), parent);
				
				PVariable var = new PVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
				var.name = ctx.struct.getText();
				
				varAccessor.target = var;
				
				if(ctx.arrayAccess() != null){
					// TODO: Rewrite this, due to slicing 
					varAccessor.accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
					varAccessor.arrayIndex = createExpression(ctx.arrayAccess().expression(), parent);
				}
				
				if(ctx.structMemberAccess().size() > 0){
					PVarAccessor lastAccess = null;
					
					for(StructMemberAccessContext memberCtx : ctx.structMemberAccess()){
						PVariable memberVar = new PVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
						memberVar.name = memberCtx.Identifier().getText();
						
						PVarAccessor memberAccess = new PVarAccessor(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
						memberAccess.target = memberVar;
						
						// Stuff for accessing element of member of array type
						if(memberCtx.arrayAccess() != null){
							// TODO: Rewrite this, due to slicing 
							memberAccess.accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
							memberAccess.arrayIndex = createExpression(memberCtx.arrayAccess().expression(), parent);
						}
						
						if(lastAccess == null){
							memberAccess.parent = varAccessor;
							varAccessor.child = memberAccess;
						}else{
							memberAccess.parent = lastAccess;
							lastAccess.child = memberAccess;
						}
						
						lastAccess = memberAccess;
					}
				}
				
				fCall.struct = varAccessor;
			}
			
			
			return fCall;
		}
		
		/** Condition expression, consisting multiple conditions, including
		 * parentheses condition expressions, for ex.: x > a && (y < 5 || y > 0) */
		public PConditionExp createConditionExp(HelloParser.ConditionExpContext ctx, PProgramPart parent) {
			PConditionExp conditionExp = new PConditionExp(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			addCondition(conditionExp, ctx);
			
			return conditionExp;
		}
		
		private void addCondition(PConditionExp conditionExp, HelloParser.ConditionExpContext ctx){
			if(ctx.op != null){
				// Condition of two expressions that can be anything,
				// parentheses expression, or just a singular condition
				ConditionExpContext lhs = ctx.lhs;
				Token operator = ctx.op;
				ConditionExpContext rhs = ctx.rhs;
				
				addCondition(conditionExp, lhs);
				conditionExp.expression.add(getOperator(operator, conditionExp));
				addCondition(conditionExp, rhs);
			}else{
				// Condition of single expression which can be
				// parentheses expression or just a singular condition
				if(ctx.parenCondition != null){
					// If it's a parentheses expression, create child condition expression
					PConditionExp parenthesesCondition = createConditionExp(ctx.parenCondition, conditionExp);
					conditionExp.expression.add(parenthesesCondition);
				}else{
					if(ctx.condition() != null){
						// Singular condition. Create condition and add it to the expression
						conditionExp.expression.add(createSingularCondition(ctx.condition(), conditionExp));
					}
				}
			}
		}
		
		/** Singular condition for ex.: x > y; a == 5; */
		public PCondition createSingularCondition(HelloParser.ConditionContext ctx, PProgramPart parent){
			PCondition condition = new PCondition(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
			// It can be relational expression (<, <=, >, >=), or equality expression (==, !=)
			if(ctx.relationalExpression() != null){
				condition.operator = getOperator(ctx.relationalExpression().op, condition);
				condition.leftExp = createExpression(ctx.relationalExpression().lhs, condition);
				condition.rightExp = createExpression(ctx.relationalExpression().rhs, condition);
			}else{
				condition.operator = getOperator(ctx.equalityExpression().op, condition);
				condition.leftExp = createExpression(ctx.equalityExpression().lhs, condition);
				condition.rightExp = createExpression(ctx.equalityExpression().rhs, condition);
			}
			
			return condition;
		}

		public PExpression createExpression(HelloParser.ExpressionContext ctx, PProgramPart parent){
			PExpression exp = new PExpression(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			addExpression(exp, ctx);
			
	//		System.out.println("created exp (" + exp.pointerExpression + "): " + exp);
			
			return exp;
		}
		
		private void addExpression(PExpression exp, HelloParser.ExpressionContext ctx){
			if(ctx.op != null){
				// It's an expression
				ExpressionContext lhs = ctx.lhs;
				Token operator = ctx.op;
				ExpressionContext rhs = ctx.rhs;
				
				addExpression(exp, lhs);
				exp.expression.add(getOperator(operator, exp));
				addExpression(exp, rhs);
			}else{
				// Find out type cast
				PTypeCast typeCast = null;
				if(ctx.typeCast() != null)
					typeCast = getTypeCast(ctx.typeCast(), exp);
				
				// There's only one operand
				if(ctx.parenthesisExpr() != null){
					// It's a parenthesis expression
					if(ctx.parenthesisExpr().expression() != null){
						// If it contains expression, add new PExpression
						PExpression parenthesesExp = createExpression(ctx.parenthesisExpr().expression(), exp);
						parenthesesExp.pointerExpression = ctx.pointer() != null;
						parenthesesExp.typeCast = typeCast;
						
						exp.expression.add(parenthesesExp);
					}else{
						// Otherwise it contains assignment, add new PAssignmentStatement
						// TODO: Uncomment this
						//PAssignmentStatement assignment = createAssignment(ctx.parenthesisExpr().assignment(), exp);
						//exp.expression.add(assignment);
					}
				}else{
					if(ctx.operand() != null){
						// It's an operand, add it to the PExpression
						exp.expression.add(getOperand(ctx.operand(), exp));
					}
				}
			}
		}
		
		public PBraceGroup createBraceGroup(BraceGroupContext ctx, PProgramPart parent){
			PBraceGroup group = new PBraceGroup(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			//addBraceElement(group, ctx.element);
			
			// Add all elements contained in the brace group
			for(BraceElementContext element : ctx.braceElement()){
				addBraceElement(group, element);
			}
			
			return group;
		}
		
		public void addBraceElement(PBraceGroup group, BraceElementContext ctx){
			if(ctx.expression() != null){
				group.elements.add(createExpression(ctx.expression(), group));
			}else{
				group.elements.add(createBraceGroup(ctx.braceGroup(), group));
			}
		}
		
		public void createGlobalVarDefinition(VarDefinitionContext ctx){
			boolean isArray = ctx.dimsSize() != null ? true : (ctx.dims() != null);
			boolean isSlice = ctx.dimsSlice() != null;
			boolean isPointer = ctx.pointer() != null;
			
			PGlobalVariable var = new PGlobalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			String dataType = ctx.dataType() != null ? ctx.dataType().start.getText() : "struct";
			var.dataType = PDataType.fromToken(dataType, isPointer, false, isArray, isSlice);
			var.name = ctx.Identifier().getText();
			var.isPublic = Character.isUpperCase(var.name.charAt(0));
			
			if(ctx.braceGroup() != null){
				PVarAccessor varAccess = new PVarAccessor(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
				varAccess.target = var;
				
				PArrayInitializer arrayInit = new PArrayInitializer(sourceFile, ctx.braceGroup().start.getLine(), ctx.braceGroup().start.getCharPositionInLine(), var);
				arrayInit.arrayVar = varAccess;
				arrayInit.group = createBraceGroup(ctx.braceGroup(), arrayInit);
				
				// Set the array size
				var.arraySize = arrayInit.group.size();
				
				// Set variable initial value
				var.initialValue = arrayInit;
			}else{
				// Set variable initial value to the expression
				var.initialValue = createExpression(ctx.expression(), var);
			}

			program.vars.add(var);
		}
		
		public void createGlobalVarDeclaration(VarDeclarationContext ctx){
			boolean isArray = ctx.dimsSize() != null;
			boolean isSlice = ctx.dims() != null;
			boolean isPointer = ctx.pointer() != null;
			
			// Create variable and add it to the program vars
			PGlobalVariable var = new PGlobalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			
			String dataType = ctx.dataType() != null ? ctx.dataType().start.getText() : "struct";
			var.dataType = PDataType.fromToken(dataType, isPointer, false, isArray, isSlice);
			var.name = ctx.id.getText();
			var.isPublic = Character.isUpperCase(var.name.charAt(0));
			
			if(var.dataType.plain() == PDataType.STRUCT){
				// If that's a struct var, create struct type
				var.struct = new PStructType(ctx.Identifier(0).getText(), var.sourceFileName, var.lineInSourceCode, var.columnInSourceCode, program);
			}
			
			// Check if variable is an array
			if(isArray){
				// If variable is an array, set it's array size
				var.arraySize = Integer.parseInt(ctx.dimsSize().IntegerLiteral().getText());
				//showError(var.lineInSourceCode, var.columnInSourceCode, "Array with unspecified size is only allowed in the array initializer");
			}

			program.vars.add(var);
		}
		
		public void createGlobalStructDefinition(StructDefinitionContext ctx){
			// Create struct variable
			PGlobalVariable var = new PGlobalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			var.name = ctx.id.getText();
			var.dataType = PDataType.fromToken("struct", ctx.pointer() != null, false, ctx.dims() != null, false);
			var.struct = new PStructType(ctx.type.getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			var.isPublic = Character.isUpperCase(var.name.charAt(0));
			
			// Add package to the struct type, if there's one
			if(ctx.pkg != null){
				var.struct.pkg = new Source();
				var.struct.pkg.fileNameWithoutExtension = ctx.pkg.getText();
			}
			
			// Create struct initializer
			PStructInitializer structInit = new PStructInitializer(sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), var);
			
			// Create var accessor for initializer
			PVarAccessor varAccessor = new PVarAccessor(sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), structInit);
			varAccessor.target = var;
			
			// Create struct initializer
			structInit.structVar = varAccessor;
			structInit.group = createBraceGroup(ctx.braceGroup(), structInit);
			
			// Set struct initial value
			var.initialValue = structInit;
			
			// Add it to the global vars
			program.vars.add(var);
		}
		
		public void createLocalVarDefinition(VarDefinitionContext ctx, PBody parentBody){
			PBody parent = parentBody == null ? findBody(ctx.parent.parent.parent.hashCode()) : parentBody;
			
			// Get variable parameters
			boolean isArray = ctx.dims() != null ? true : (ctx.dimsSize() != null);
			boolean isSlice = ctx.dimsSlice() != null;
			boolean isPointer = ctx.pointer() != null;
			
			// Create variable
			PLocalVariable var = new PLocalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			String dataType = ctx.dataType() != null ? ctx.dataType().start.getText() : "struct";
			var.dataType = PDataType.fromToken(dataType, isPointer, false, isArray, isSlice);
			var.name = ctx.Identifier().getText();
			
			// Set array size if variable is an array
			if(ctx.dimsSize() != null)
				var.arraySize = Integer.parseInt(ctx.dimsSize().IntegerLiteral().getText());
			
			// Create var accessor for assignment
			PVarAccessor varAccessor = new PVarAccessor(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			varAccessor.target = var;
			
			// Add variable to the parent body
			parent.vars.add(var);

			if(ctx.braceGroup() != null){
				// Create array initializer
				PArrayInitializer arrayInit = new PArrayInitializer(sourceFile, ctx.braceGroup().start.getLine(), ctx.braceGroup().start.getCharPositionInLine(), var);
				arrayInit.arrayVar = varAccessor;
				arrayInit.group = createBraceGroup(ctx.braceGroup(), arrayInit);
				
				// Set the array size
				if(ctx.dimsSize() == null)
					var.arraySize = arrayInit.group.size();
				
				parent.statements.add(arrayInit);
			}else if(ctx.conditionExp() != null){
				// TODO: Implement condition operands
			}else{
				// Create assignment statement and add it to the parent body vars
				PAssignmentStatement assignment = new PAssignmentStatement(var.sourceFileName, var.lineInSourceCode, var.columnInSourceCode, var);
				assignment.variable = varAccessor;
				assignment.checkIfVariableDefined = false;
				assignment.expression = createExpression(ctx.expression(), assignment);
				
				parent.statements.add(assignment);
			}
		}
		
		public void createLocalVarDeclaration(VarDeclarationContext ctx, PBody parentBody){
			PBody parent = parentBody == null ? findBody(ctx.parent.parent.parent.hashCode()) : parentBody;
			
			// Get variable parameters
			boolean isArray = ctx.dimsSize() != null ? true : (ctx.dims() != null);
			boolean isSlice = ctx.dimsSlice() != null;
			boolean isPointer = ctx.pointer() != null;

			// Create variable and add it to the parent body vars
			PLocalVariable var = new PLocalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			String dataType = ctx.dataType() != null ? ctx.dataType().start.getText() : "struct";
			var.dataType = PDataType.fromToken(dataType, isPointer, false, isArray, isSlice);
			var.name = ctx.id.getText();
			
			if(var.dataType.plain() == PDataType.STRUCT){
				// Struct declaration
				var.struct = new PStructType(ctx.dt.getText(), var.sourceFileName, var.lineInSourceCode, var.columnInSourceCode, program);
				
				// Set package for the struct, if there's one
				if(ctx.pkg != null){
					var.struct.pkg = new Source();
					var.struct.pkg.fileNameWithoutExtension = ctx.pkg.getText();
				}
			}
			
			if(isArray){
				if(ctx.dimsSize() != null){
					var.arraySize = Integer.parseInt(ctx.dimsSize().IntegerLiteral().getText());
				}
			}
			
			parent.vars.add(var);
		}
		
		public void createLocalStructDefinition(StructDefinitionContext ctx){
			PBody parent = findBody(ctx.parent.parent.parent.hashCode());
			
			// Create struct variable
			PLocalVariable structVar = new PLocalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			structVar.name = ctx.id.getText();
			structVar.dataType = PDataType.fromToken("struct", ctx.pointer() != null, false, ctx.dims() != null, false);
			structVar.struct = new PStructType(ctx.type.getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), program);
			
			// Add package to the struct type, if there's one
			if(ctx.pkg != null){
				structVar.struct.pkg = new Source();
				structVar.struct.pkg.fileNameWithoutExtension = ctx.pkg.getText();
			}
			
			// Create var accessor for assignment
			PVarAccessor varAccessor = new PVarAccessor(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			varAccessor.target = structVar;
			
			// Check if that's a pointer struct
			if(ctx.pointer() == null){
				// Not a pointer
				
				if(ctx.braceGroup() != null){
					if(structVar.dataType.isArray()){
						// Create array initializer
						PArrayInitializer arrayInit = new PArrayInitializer(sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), parent);
						arrayInit.arrayVar = varAccessor;
						arrayInit.group = createBraceGroup(ctx.braceGroup(), arrayInit);
						
						// Add array initializer statement to parent body
						parent.statements.add(arrayInit);
					}else{
						// Create struct initializer
						PStructInitializer structInit = new PStructInitializer(sourceFile, ctx.braceGroup().start.getLine(), ctx.braceGroup().start.getCharPositionInLine(), parent);
						structInit.structVar = varAccessor;
						structInit.group = createBraceGroup(ctx.braceGroup(), structInit);
						
						// Add struct initializer statement to parent body
						parent.statements.add(structInit);
					}
				}else{
					// Create assignment statement
					PAssignmentStatement assignment = new PAssignmentStatement(structVar.sourceFileName, structVar.lineInSourceCode, structVar.columnInSourceCode, structVar);
					assignment.variable = varAccessor;
					assignment.checkIfVariableDefined = false;
					assignment.expression = createExpression(ctx.exp, assignment);
					
					// Add it to the parent body vars
					parent.statements.add(assignment);
				}
			}else{
				// Pointer struct
				
				if(ctx.newOperand() == null){
					// If it's not a 'new' initializer, 
					// create assignemtn statment for initialization
					PAssignmentStatement assignment = new PAssignmentStatement(structVar.sourceFileName, structVar.lineInSourceCode, structVar.columnInSourceCode, structVar);
					assignment.variable = varAccessor;
					assignment.checkIfVariableDefined = false;
					assignment.expression = createExpression(ctx.exp, assignment);
					
					// Add it to the parent body vars
					parent.statements.add(assignment);
				}else{
					// Create assignment to the struct var
					PAssignmentStatement assignment = new PAssignmentStatement(sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), parent);
					assignment.variable = varAccessor;
					assignment.expressionAssignment = false;
					assignment.checkIfVariableDefined = false;
					
					// Create 'new' operand
					PNewOperand newOp = new PNewOperand(sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), assignment);
					newOp.structType = new PStructType(ctx.newOperand().Identifier().getText(), sourceFile, ctx.id.getLine(), ctx.id.getCharPositionInLine(), newOp);
					
					// Set assignment operand to the new operand
					assignment.operand = newOp;
					
					// Add 'new' assignment statement to the parent body
					parent.statements.add(assignment);
				}
			}
			
			// Add var to the parent body vars
			parent.vars.add(structVar);
		}
		
		public void createStructMemberDeclaration(VarDeclarationContext ctx, PStructType parentBody){
			PStructType structType = parentBody == null ? findStruct(ctx.parent.parent.hashCode()) : parentBody;
			
			boolean isArray = ctx.dimsSize() != null ? true : (ctx.dims() != null);
			boolean isPointer = ctx.pointer() != null;

			// Create variable and add it to the parent body vars
			PLocalVariable var = new PLocalVariable(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), structType);
			
			String dataType = ctx.dataType() != null ? ctx.dataType().start.getText() : "struct";
			var.dataType = PDataType.fromToken(dataType, isPointer, false, isArray, ctx.dims() != null);
			var.name = ctx.id.getText();
			
			if(var.dataType.plain() == PDataType.STRUCT){
				var.struct = new PStructType(ctx.Identifier(0).getText(), var.sourceFileName, var.lineInSourceCode, var.columnInSourceCode, program);
			}
			
			if(isArray){
				if(ctx.dimsSize() != null){
					var.arraySize = Integer.parseInt(ctx.dimsSize().IntegerLiteral().getText());
				}else{
					// Array with unspecified size is only allowed in the array initializer
					//showError(var.lineInSourceCode, var.columnInSourceCode, "Array with unspecified size is only allowed in the array initializer");
				
					// Syntax has changed, now this is a slice of an array
				}
			}
			
			structType.members.add(var);
		}
		
		public PAssignmentStatement createAssignment(AssignmentContext ctx, PProgramPart parent){
			//PBody parent = parentBody == null ? findBody(ctx.parent.parent.parent.hashCode()) : parentBody;
			
			// Create assignment statement
			PAssignmentStatement assignmentStatement = new PAssignmentStatement(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			assignmentStatement.checkIfVariableDefined = true;
			
			// Create result variable
			assignmentStatement.variable = createVarAccessor(ctx.id, ctx.pointer(), null, ctx.arrayAccess(), ctx.structMemberAccess(), null, parent);//varAccessor;

			if(ctx.expression() != null){
				// Create expression
				assignmentStatement.expression = createExpression(ctx.expression(), assignmentStatement);
				assignmentStatement.expressionAssignment = true;
			}else{
				assignmentStatement.operand = getOperand(ctx.operand(), assignmentStatement);
				assignmentStatement.expressionAssignment = false;
			}
			
			return assignmentStatement;
		}
		
		public PUnaryExpression createUnaryExpression(UnaryExpressionContext ctx, PProgramPart parent){
			// Create unary operation
			PUnaryExpression unaryExp = new PUnaryExpression(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
			// Create operator and variable
			if(ctx.preIncrementDecrement() != null){
				PreIncrementDecrementContext preExp = ctx.preIncrementDecrement();
				unaryExp.preOperator = getOperator(preExp.op, unaryExp);
				unaryExp.variable = createVarAccessor(preExp.Identifier().getSymbol(), preExp.pointer(), null, preExp.arrayAccess(), preExp.structMemberAccess(), null, unaryExp);
			}else if(ctx.postIncrementDecrement() != null){
				PostIncrementDecrementContext postExp = ctx.postIncrementDecrement();
				unaryExp.postOperator = getOperator(postExp.op, unaryExp);
				unaryExp.variable = createVarAccessor(postExp.Identifier().getSymbol(), postExp.pointer(), null, postExp.arrayAccess(), postExp.structMemberAccess(), null, unaryExp);
			}else if(ctx.unaryNot() != null){
				UnaryNotContext notExp = ctx.unaryNot();
				unaryExp.preOperator = getOperator(notExp.op, unaryExp);
				unaryExp.variable = createVarAccessor(notExp.Identifier().getSymbol(), notExp.pointer(), null, notExp.arrayAccess(), notExp.structMemberAccess(), null, unaryExp);
			}
			
			return unaryExp;
		}
			
		public PBody findBody(int hashcode) {
			for (PProgramPart part : PProgramPart.parts) {
				if (part instanceof PBody && part.antlrHash == hashcode) {
					return (PBody) part;
				}
			}
			return null;
		}
		
		public PStructType findStruct(int hashcode) {
			for (PProgramPart part : PProgramPart.parts) {
				if (part instanceof PStructType && part.antlrHash == hashcode) {
					return (PStructType) part;
				}
			}
			return null;
		}

		public POperator getOperator(Token token, PProgramPart parent) {
			switch (token.getText()) {
			// Arithmetic
			case "/":
				return new POperator(POperatorType.DIV, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "*":
				return new POperator(POperatorType.MUL, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "+":
				return new POperator(POperatorType.ADD, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "-":
				return new POperator(POperatorType.SUB, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			// Bitwise
			case "&":
				return new POperator(POperatorType.BITWISE_AND, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "|":
				return new POperator(POperatorType.BITWISE_OR, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "^":
				return new POperator(POperatorType.BITWISE_XOR, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "~":
				return new POperator(POperatorType.BITWISE_NOT, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			// Logical
			case "&&":
				return new POperator(POperatorType.LOGICAL_AND, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "||":
				return new POperator(POperatorType.LOGICAL_OR, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			// Relational
			case "<":
				return new POperator(POperatorType.LESSER, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case ">":
				return new POperator(POperatorType.GREATER, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "<=":
				return new POperator(POperatorType.LESSER_EQUAL, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case ">=":
				return new POperator(POperatorType.GREATER_EQUAL, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			// Equality
			case "==":
				return new POperator(POperatorType.EQUALS, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "!=":
				return new POperator(POperatorType.NOT_EQUALS, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			// Unary
			case "++":
				return new POperator(POperatorType.INCREASE, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "--":
				return new POperator(POperatorType.DECREASE, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			case "!":
				return new POperator(POperatorType.NOT, sourceFile, token.getLine(), token.getCharPositionInLine(), parent);
			}
			return null;
		}
		
		public PTypeCast getTypeCast(TypeCastContext ctx, PProgramPart parent){
			PTypeCast typeCast = new PTypeCast(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
			if(ctx.dataType() != null){
				// Cast to primitive data type
				typeCast.primitiveType = PDataType.fromToken(ctx.dataType().getText(), ctx.pointer() != null, false, false, ctx.dimsSlice() != null);
			}else{
				// Cast to struct type
				typeCast.structType = new PStructType(ctx.Identifier().getText(), sourceFile, ctx.Identifier().getSymbol().getLine(), ctx.Identifier().getSymbol().getCharPositionInLine(), typeCast);
				typeCast.pointer = ctx.pointer() != null;
			}
			
			return typeCast;
		}
		
		public POperand getOperand(OperandContext ctx, PProgramPart parent){
			if(ctx.IntegerLiteral() != null){
				PIntegerLiteral integer = new PIntegerLiteral(Integer.parseInt(ctx.IntegerLiteral().getText()), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
				
				// Create type cast
				if(ctx.typeCast() != null){
					integer.typeCast = getTypeCast(ctx.typeCast(), integer);
					integer.verifyTypeCast();
				}
				
				return integer;
			}else if(ctx.HexLiteral() != null){
				PHexLiteral hex = new PHexLiteral(ctx.HexLiteral().getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			
				// Create type cast
				if(ctx.typeCast() != null){
					hex.typeCast = getTypeCast(ctx.typeCast(), hex);
					hex.verifyTypeCast();
				}
				
				return hex;
			}else if(ctx.StringLiteral() != null){
				return new PStringLiteral(ctx.StringLiteral().getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			}else if(ctx.CharLiteral() != null){
				String noQuotesValue = ctx.CharLiteral().getText().substring(1, ctx.CharLiteral().getText().length() - 1);
				char value = getCharValue(noQuotesValue);
				PCharLiteral character = new PCharLiteral(value, sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
				
				// Create type cast
				if(ctx.typeCast() != null){
					character.typeCast = getTypeCast(ctx.typeCast(), character);
					character.verifyTypeCast();
				}
				
				return character;
			}else if(ctx.functionCall() != null){
				PFunctionCall fCall = createFunctionCall(ctx.functionCall(), parent);
				
				// Check if there's a type cast
				if(ctx.typeCast() != null){
					// Type cast function returned value
					fCall.typeCast = getTypeCast(ctx.typeCast(), fCall);
				}
				
				return fCall;
			}else if(ctx.id != null){
				return createVarAccessor(ctx.id, ctx.pointer(), ctx.reference(), ctx.arrayAccess(), ctx.structMemberAccess(), ctx.typeCast(), parent);
			}else if(ctx.newOperand() != null){
				PNewOperand newOp = new PNewOperand(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
				newOp.structType = new PStructType(ctx.newOperand().Identifier().getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), newOp);
				
				return newOp;
			}else if(ctx.NullLiteral() != null){
				return new PNullLiteral(sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			}else if(ctx.BooleanLiteral() != null){
				return new PBooleanLiteral(ctx.BooleanLiteral().getText(), sourceFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), parent);
			}
			
			return null;
		}
	
		private PVarAccessor createVarAccessor(Token id, PointerContext pointer, ReferenceContext reference, ArrayAccessContext arrayAccess, List<StructMemberAccessContext> structMemberAccess, TypeCastContext typeCast, PProgramPart parent){
			// Create variable that's being accessed
			PVariable var = new PVariable(sourceFile, id.getLine(), id.getCharPositionInLine(), parent);
			var.name = id.getText();
			
			// Create var accessor for the variable
			PVarAccessor varAccessor = new PVarAccessor(sourceFile, var.lineInSourceCode, var.columnInSourceCode, parent);
			varAccessor.target = var;
			
			// Set access type if neccessary
			if(pointer != null){
				varAccessor.accessType = PVarAccessor.ACCESS_TYPE_POINTER;
			}else if(reference != null){
				varAccessor.accessType = PVarAccessor.ACCESS_TYPE_REFERENCE;
			}
			
			// Set array access if neccessary
			if(arrayAccess != null){
				if(arrayAccess.sliceExpression() != null){
					varAccessor.accessType |= PVarAccessor.ACCESS_TYPE_SLICE;
					varAccessor.sliceExp = new PSliceExp(sourceFile, arrayAccess.sliceExpression().start.start.getLine(), arrayAccess.sliceExpression().start.start.getCharPositionInLine(), parent);
					varAccessor.sliceExp.start = createExpression(arrayAccess.sliceExpression().start, varAccessor.sliceExp);
					varAccessor.sliceExp.end = createExpression(arrayAccess.sliceExpression().length, varAccessor.sliceExp);
				}else{
					varAccessor.accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
					varAccessor.arrayIndex = createExpression(arrayAccess.expression(), parent);
				}
			}
			
			// Struct member access
			if(structMemberAccess.size() > 0){
				PVarAccessor lastAccess = null;
				
				for(StructMemberAccessContext memberCtx : structMemberAccess){
					PVariable memberVar = new PVariable(sourceFile, memberCtx.Identifier().getSymbol().getLine(), memberCtx.Identifier().getSymbol().getCharPositionInLine(), parent);
					memberVar.name = memberCtx.Identifier().getText();
					
					PVarAccessor memberAccess = new PVarAccessor(sourceFile, memberVar.lineInSourceCode, memberVar.columnInSourceCode, parent);
					memberAccess.target = memberVar;
					
					// Stuff for accessing element of member of array type
					if(memberCtx.arrayAccess() != null){
						if(memberCtx.arrayAccess().sliceExpression() != null){
							memberAccess.accessType |= PVarAccessor.ACCESS_TYPE_SLICE;
							memberAccess.sliceExp = new PSliceExp(sourceFile, memberCtx.arrayAccess().sliceExpression().start.start.getLine(), memberCtx.arrayAccess().sliceExpression().start.start.getCharPositionInLine(), varAccessor);
							memberAccess.sliceExp.start = createExpression(memberCtx.arrayAccess().sliceExpression().start, parent);
							memberAccess.sliceExp.end = createExpression(memberCtx.arrayAccess().sliceExpression().length, parent);
						}else{
							memberAccess.accessType |= PVarAccessor.ACCESS_TYPE_ARRAY;
							memberAccess.arrayIndex = createExpression(memberCtx.arrayAccess().expression(), parent);
						}
						
					}
					
					if(lastAccess == null){
						memberAccess.parent = varAccessor;
						varAccessor.child = memberAccess;
					}else{
						memberAccess.parent = lastAccess;
						lastAccess.child = memberAccess;
					}
					
					lastAccess = memberAccess;
				}
			}
			
			// Check if there's a type cast
			if(typeCast != null){
				// Type cast function returned value
				varAccessor.typeCast = getTypeCast(typeCast, varAccessor);
			}
			
			return varAccessor;
		}
		
		private char getCharValue(String character){
			character = character.replaceAll("\\\\b", "\b");
			character = character.replaceAll("\\\\t", "\t");
			character = character.replaceAll("\\\\n", "\n");
			character = character.replaceAll("\\\\f", "\f");
			character = character.replaceAll("\\\\r", "\r");
			character = character.replaceAll("\\\\\"", "\"");
			character = character.replaceAll("\\\\'", "'");
			character = character.replaceAll("\\\\0", "\0");
			
			return character.charAt(0);
		}
		
		/*public void showError(int line, int column, String error){
			System.err.println("line " + line + ":" + column + " " + error);
		}*/
	}
}
