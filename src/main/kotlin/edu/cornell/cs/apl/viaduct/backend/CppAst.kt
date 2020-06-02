package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.surface.keyword

sealed class CppAst : PrettyPrintable
typealias CppIdentifier = String

// references
sealed class CppReference : CppAst()

data class CppVariable(val name: CppIdentifier) : CppReference() {
    override val asDocument: Document
        get() = Document(name)
}

data class CppArrayIndex(val array: CppReference, val index: CppExpression) : CppReference() {
    override val asDocument: Document
        get() = array.asDocument + "[" + index.asDocument + "]"
}

// calls
sealed class CppCall : CppAst()

data class CppFunctionCall(
    val funcName: CppIdentifier,
    val arguments: List<CppExpression>
) : CppCall() {
    override val asDocument: Document
        get() {
            val docArguments: List<Document> = arguments.map { it.asDocument }
            return Document(funcName) + "(" + docArguments.concatenated(Document(",")) + ")"
        }
}

data class CppMethodCall(
    val receiver: CppExpression,
    val funcName: CppIdentifier,
    val arguments: List<CppExpression>
) : CppCall() {
    override val asDocument: Document
        get() {
            val docArguments: List<Document> = arguments.map { it.asDocument }
            return receiver.asDocument + "." + Document(funcName) + "(" +
                    docArguments.concatenated(Document(",")) + ")"
        }
}

// types
sealed class CppType : CppAst()

data class CppTypeName(val name: String) : CppType() {
    override val asDocument: Document
        get() = Document(name)
}

data class CppPointerType(val type: CppType) : CppType() {
    override val asDocument: Document
        get() = type.asDocument + "*"
}

data class CppReferenceType(val type: CppType) : CppType() {
    override val asDocument: Document
        get() = type.asDocument + "&"
}

// statements
sealed class CppStatement : CppAst()
sealed class CppSimpleStatement : CppStatement()

data class CppVariableDecl(val type: CppType, val name: CppIdentifier) : CppSimpleStatement() {
    override val asDocument: Document
        get() = type.asDocument * name
}

data class CppArrayDecl(
    val type: CppType,
    val name: CppIdentifier,
    val length: CppExpression
) : CppSimpleStatement() {
    override val asDocument: Document
        get() = type.asDocument * name * length.asDocument
}

data class CppVariableDeclAndAssignment(
    val type: CppType,
    val name: CppIdentifier,
    val rhs: CppExpression
) : CppSimpleStatement() {
    override val asDocument: Document
        get() = type.asDocument * name * "=" * rhs.asDocument
}

data class CppAssignment(
    val rval: CppReference,
    val rhs: CppExpression
) : CppSimpleStatement() {
    override val asDocument: Document
        get() = rval.asDocument * "=" * rhs.asDocument
}

data class CppCallStatement(val call: CppCall) : CppSimpleStatement() {
    override val asDocument: Document
        get() = call.asDocument
}

data class CppBlock(val statements: List<CppStatement>) : CppStatement() {
    override val asDocument: Document
        get() {
            val statements: List<Document> = statements.map {
                if (it is CppSimpleStatement) (it.asDocument + ";") else it.asDocument
            }

            val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
            return Document("{") +
                (Document.forcedLineBreak + body).nested() +
                Document.forcedLineBreak + "}"
        }
}

data class CppIf(
    val guard: CppExpression,
    val thenBranch: CppBlock,
    val elseBranch: CppBlock
) : CppStatement() {
    override val asDocument: Document
        get() = (keyword("if") * "(" + guard + ")") * thenBranch * keyword("else") * elseBranch
}

data class CppWhileLoop(
    val guard: CppExpression,
    val body: CppBlock
) : CppStatement() {
    override val asDocument: Document
        get() = (keyword("while") * "(" + guard + ")") * body
}

// expressions

sealed class CppExpression : CppAst()

enum class CppBinaryOperator : PrettyPrintable {
    PLUS { override val asDocument get() = Document("+") },
    MINUS { override val asDocument get() = Document("-") },
    TIMES { override val asDocument get() = Document("*") },
    DIVIDE { override val asDocument get() = Document("/") },
    GT { override val asDocument get() = Document(">") },
    LT { override val asDocument get() = Document("<") },
    EQ { override val asDocument get() = Document("==") },
    AND { override val asDocument get() = Document("&&") },
    OR { override val asDocument get() = Document("||") },
    XOR { override val asDocument get() = Document("^") }
}

data class CppIntLiteral(val value: Int) : CppExpression() {
    override val asDocument: Document
        get() = Document(value.toString())
}

data class CppReferenceRead(val reference: CppReference) : CppExpression() {
    override val asDocument: Document
        get() = reference.asDocument
}

data class CppCallExpression(val call: CppCall) : CppExpression() {
    override val asDocument: Document
        get() = call.asDocument
}

data class CppBinopApplication(
    val operator: CppBinaryOperator,
    val lhs: CppExpression,
    val rhs: CppExpression
) : CppExpression() {
    override val asDocument: Document
        get() = Document("(") +
                (lhs.asDocument * operator.asDocument * rhs.asDocument) +
                Document(")")
}

data class CppMux(
    val guard: CppExpression,
    val thenBranch: CppExpression,
    val elseBranch: CppExpression
) : CppExpression() {
    override val asDocument: Document
        get() = Document("(") +
                (guard.asDocument * "?" * thenBranch.asDocument * ":" * elseBranch.asDocument) +
                Document(")")
}
