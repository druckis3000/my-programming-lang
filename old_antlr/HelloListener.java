// Generated from Hello.g4 by ANTLR 4.6

    package spl.parser.syntactic;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link HelloParser}.
 */
public interface HelloListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link HelloParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(HelloParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(HelloParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDataType(HelloParser.DataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDataType(HelloParser.DataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#numericType}.
	 * @param ctx the parse tree
	 */
	void enterNumericType(HelloParser.NumericTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#numericType}.
	 * @param ctx the parse tree
	 */
	void exitNumericType(HelloParser.NumericTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void enterImportStatement(HelloParser.ImportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void exitImportStatement(HelloParser.ImportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionDeclStatement}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclStatement(HelloParser.FunctionDeclStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionDeclStatement}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclStatement(HelloParser.FunctionDeclStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDef(HelloParser.FunctionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDef(HelloParser.FunctionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDecl(HelloParser.FunctionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDecl(HelloParser.FunctionDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionParam}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParam(HelloParser.FunctionParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionParam}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParam(HelloParser.FunctionParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#structFunction}.
	 * @param ctx the parse tree
	 */
	void enterStructFunction(HelloParser.StructFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#structFunction}.
	 * @param ctx the parse tree
	 */
	void exitStructFunction(HelloParser.StructFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#globalStatement}.
	 * @param ctx the parse tree
	 */
	void enterGlobalStatement(HelloParser.GlobalStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#globalStatement}.
	 * @param ctx the parse tree
	 */
	void exitGlobalStatement(HelloParser.GlobalStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#localStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalStatement(HelloParser.LocalStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#localStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalStatement(HelloParser.LocalStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionCallStatement}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallStatement(HelloParser.FunctionCallStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionCallStatement}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallStatement(HelloParser.FunctionCallStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(HelloParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(HelloParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#fncCallStructMember}.
	 * @param ctx the parse tree
	 */
	void enterFncCallStructMember(HelloParser.FncCallStructMemberContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#fncCallStructMember}.
	 * @param ctx the parse tree
	 */
	void exitFncCallStructMember(HelloParser.FncCallStructMemberContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#functionArgument}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgument(HelloParser.FunctionArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#functionArgument}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgument(HelloParser.FunctionArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(HelloParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(HelloParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#elseStatement}.
	 * @param ctx the parse tree
	 */
	void enterElseStatement(HelloParser.ElseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#elseStatement}.
	 * @param ctx the parse tree
	 */
	void exitElseStatement(HelloParser.ElseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#elseIfStatement}.
	 * @param ctx the parse tree
	 */
	void enterElseIfStatement(HelloParser.ElseIfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#elseIfStatement}.
	 * @param ctx the parse tree
	 */
	void exitElseIfStatement(HelloParser.ElseIfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(HelloParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(HelloParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoWhileStatement(HelloParser.DoWhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoWhileStatement(HelloParser.DoWhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(HelloParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(HelloParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#forEachStatement}.
	 * @param ctx the parse tree
	 */
	void enterForEachStatement(HelloParser.ForEachStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#forEachStatement}.
	 * @param ctx the parse tree
	 */
	void exitForEachStatement(HelloParser.ForEachStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStatement(HelloParser.BreakStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStatement(HelloParser.BreakStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(HelloParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(HelloParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(HelloParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(HelloParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#unaryStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnaryStatement(HelloParser.UnaryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#unaryStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnaryStatement(HelloParser.UnaryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterDeleteStatement(HelloParser.DeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitDeleteStatement(HelloParser.DeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(HelloParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(HelloParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#varDefinitionStatement}.
	 * @param ctx the parse tree
	 */
	void enterVarDefinitionStatement(HelloParser.VarDefinitionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#varDefinitionStatement}.
	 * @param ctx the parse tree
	 */
	void exitVarDefinitionStatement(HelloParser.VarDefinitionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#varDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterVarDeclarationStatement(HelloParser.VarDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#varDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitVarDeclarationStatement(HelloParser.VarDeclarationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#structDefinitionStatement}.
	 * @param ctx the parse tree
	 */
	void enterStructDefinitionStatement(HelloParser.StructDefinitionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#structDefinitionStatement}.
	 * @param ctx the parse tree
	 */
	void exitStructDefinitionStatement(HelloParser.StructDefinitionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentStatement(HelloParser.AssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentStatement(HelloParser.AssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#pointerAssignmentStatement}.
	 * @param ctx the parse tree
	 */
	void enterPointerAssignmentStatement(HelloParser.PointerAssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#pointerAssignmentStatement}.
	 * @param ctx the parse tree
	 */
	void exitPointerAssignmentStatement(HelloParser.PointerAssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#structTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterStructTypeDeclaration(HelloParser.StructTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#structTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitStructTypeDeclaration(HelloParser.StructTypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void enterStructDefinition(HelloParser.StructDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void exitStructDefinition(HelloParser.StructDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#varDefinition}.
	 * @param ctx the parse tree
	 */
	void enterVarDefinition(HelloParser.VarDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#varDefinition}.
	 * @param ctx the parse tree
	 */
	void exitVarDefinition(HelloParser.VarDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#varDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVarDeclaration(HelloParser.VarDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#varDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVarDeclaration(HelloParser.VarDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#pointer}.
	 * @param ctx the parse tree
	 */
	void enterPointer(HelloParser.PointerContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#pointer}.
	 * @param ctx the parse tree
	 */
	void exitPointer(HelloParser.PointerContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#reference}.
	 * @param ctx the parse tree
	 */
	void enterReference(HelloParser.ReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#reference}.
	 * @param ctx the parse tree
	 */
	void exitReference(HelloParser.ReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(HelloParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(HelloParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#pointerAssignment}.
	 * @param ctx the parse tree
	 */
	void enterPointerAssignment(HelloParser.PointerAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#pointerAssignment}.
	 * @param ctx the parse tree
	 */
	void exitPointerAssignment(HelloParser.PointerAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#dims}.
	 * @param ctx the parse tree
	 */
	void enterDims(HelloParser.DimsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#dims}.
	 * @param ctx the parse tree
	 */
	void exitDims(HelloParser.DimsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#dimsSlice}.
	 * @param ctx the parse tree
	 */
	void enterDimsSlice(HelloParser.DimsSliceContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#dimsSlice}.
	 * @param ctx the parse tree
	 */
	void exitDimsSlice(HelloParser.DimsSliceContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#dimsSize}.
	 * @param ctx the parse tree
	 */
	void enterDimsSize(HelloParser.DimsSizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#dimsSize}.
	 * @param ctx the parse tree
	 */
	void exitDimsSize(HelloParser.DimsSizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#braceGroup}.
	 * @param ctx the parse tree
	 */
	void enterBraceGroup(HelloParser.BraceGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#braceGroup}.
	 * @param ctx the parse tree
	 */
	void exitBraceGroup(HelloParser.BraceGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#braceElement}.
	 * @param ctx the parse tree
	 */
	void enterBraceElement(HelloParser.BraceElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#braceElement}.
	 * @param ctx the parse tree
	 */
	void exitBraceElement(HelloParser.BraceElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#structMemberAccess}.
	 * @param ctx the parse tree
	 */
	void enterStructMemberAccess(HelloParser.StructMemberAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#structMemberAccess}.
	 * @param ctx the parse tree
	 */
	void exitStructMemberAccess(HelloParser.StructMemberAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(HelloParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(HelloParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#parenthesisExpr}.
	 * @param ctx the parse tree
	 */
	void enterParenthesisExpr(HelloParser.ParenthesisExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#parenthesisExpr}.
	 * @param ctx the parse tree
	 */
	void exitParenthesisExpr(HelloParser.ParenthesisExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#typeCast}.
	 * @param ctx the parse tree
	 */
	void enterTypeCast(HelloParser.TypeCastContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#typeCast}.
	 * @param ctx the parse tree
	 */
	void exitTypeCast(HelloParser.TypeCastContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void enterArrayAccess(HelloParser.ArrayAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#arrayAccess}.
	 * @param ctx the parse tree
	 */
	void exitArrayAccess(HelloParser.ArrayAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#sliceExpression}.
	 * @param ctx the parse tree
	 */
	void enterSliceExpression(HelloParser.SliceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#sliceExpression}.
	 * @param ctx the parse tree
	 */
	void exitSliceExpression(HelloParser.SliceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#conditionExp}.
	 * @param ctx the parse tree
	 */
	void enterConditionExp(HelloParser.ConditionExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#conditionExp}.
	 * @param ctx the parse tree
	 */
	void exitConditionExp(HelloParser.ConditionExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(HelloParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(HelloParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(HelloParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(HelloParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(HelloParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(HelloParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(HelloParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(HelloParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#preIncrementDecrement}.
	 * @param ctx the parse tree
	 */
	void enterPreIncrementDecrement(HelloParser.PreIncrementDecrementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#preIncrementDecrement}.
	 * @param ctx the parse tree
	 */
	void exitPreIncrementDecrement(HelloParser.PreIncrementDecrementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#postIncrementDecrement}.
	 * @param ctx the parse tree
	 */
	void enterPostIncrementDecrement(HelloParser.PostIncrementDecrementContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#postIncrementDecrement}.
	 * @param ctx the parse tree
	 */
	void exitPostIncrementDecrement(HelloParser.PostIncrementDecrementContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#unaryNot}.
	 * @param ctx the parse tree
	 */
	void enterUnaryNot(HelloParser.UnaryNotContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#unaryNot}.
	 * @param ctx the parse tree
	 */
	void exitUnaryNot(HelloParser.UnaryNotContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#newOperand}.
	 * @param ctx the parse tree
	 */
	void enterNewOperand(HelloParser.NewOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#newOperand}.
	 * @param ctx the parse tree
	 */
	void exitNewOperand(HelloParser.NewOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#packageIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterPackageIdentifier(HelloParser.PackageIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#packageIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitPackageIdentifier(HelloParser.PackageIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link HelloParser#operand}.
	 * @param ctx the parse tree
	 */
	void enterOperand(HelloParser.OperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link HelloParser#operand}.
	 * @param ctx the parse tree
	 */
	void exitOperand(HelloParser.OperandContext ctx);
}