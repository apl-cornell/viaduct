package edu.cornell.cs.apl.viaduct.imp.ast2

import edu.cornell.cs.apl.viaduct.imp.ast.Operator
import edu.cornell.cs.apl.viaduct.imp.ast.Variable
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue

sealed class ExpressionNode : AstNode()

data class LiteralNode(val value: ImpValue) : ExpressionNode()

data class ReadNode(val variable: Variable) : ExpressionNode()

data class OperatorApplicationNode(val operator: Operator, val arguments: List<ExpressionNode>) :
    ExpressionNode()
