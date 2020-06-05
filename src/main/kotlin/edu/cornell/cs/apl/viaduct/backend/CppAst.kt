package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.styled
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.surface.keyword
import edu.cornell.cs.apl.viaduct.syntax.types.ValueTypeStyle

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
            return Document(funcName) + "(" + docArguments.concatenated(Document(", ")) + ")"
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
                docArguments.concatenated(Document(", ")) + ")"
        }
}

// types
sealed class CppType : CppAst()

data class CppTypeName(val name: String) : CppType() {
    override val asDocument: Document
        get() = Document(name).styled(ValueTypeStyle)
}

data class CppPointerType(val type: CppType) : CppType() {
    override val asDocument: Document
        get() = type.asDocument + "*"
}

data class CppReferenceType(val type: CppType) : CppType() {
    override val asDocument: Document
        get() = type.asDocument + "&"
}

// expressions

sealed class CppExpression : CppAst()

enum class CppUnaryOperator : PrettyPrintable {
    NEGATION {
        override val asDocument get() = Document("-")
    },
    NOT {
        override val asDocument get() = Document("!")
    },
}

enum class CppBinaryOperator : PrettyPrintable {
    ADD {
        override val asDocument get() = Document("+")
    },
    SUBTRACT {
        override val asDocument get() = Document("-")
    },
    MULTIPLY {
        override val asDocument get() = Document("*")
    },
    DIVIDE {
        override val asDocument get() = Document("/")
    },
    GREATER_THAN {
        override val asDocument get() = Document(">")
    },
    LESS_THAN {
        override val asDocument get() = Document("<")
    },
    LT_EQUALS {
        override val asDocument get() = Document("<")
    },
    EQUALS {
        override val asDocument get() = Document("==")
    },
    AND {
        override val asDocument get() = Document("&&")
    },
    OR {
        override val asDocument get() = Document("||")
    },
    NOT {
        override val asDocument get() = Document("||")
    },
    XOR {
        override val asDocument get() = Document("^")
    }
}

data class CppIntLiteral(val value: Int) : CppExpression() {
    override val asDocument: Document
        get() = Document(value.toString())
}

data class CppReferenceRead(val reference: CppReference) : CppExpression() {
    override val asDocument: Document
        get() = reference.asDocument
}

data class CppCallExpr(val call: CppCall) : CppExpression() {
    override val asDocument: Document
        get() = call.asDocument
}

data class CppUnaryOpExpr(
    val operator: CppUnaryOperator,
    val expr: CppExpression
) : CppExpression() {
    override val asDocument: Document
        get() = Document("(") + operator.asDocument + expr.asDocument + Document(")")
}

data class CppBinaryOpExpr(
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

data class CppDeref(
    val expr: CppExpression
) : CppExpression() {
    override val asDocument: Document
        get() = Document("(*") + expr.asDocument + Document(")")
}

// statements
sealed class CppStatement : CppAst()
sealed class CppSimpleStatement : CppStatement()

data class CppVariableDecl(
    val type: CppType,
    val name: CppIdentifier,
    val arguments: List<CppExpression> = listOf()
) : CppSimpleStatement() {
    override val asDocument: Document
        get() {
            return if (arguments.isEmpty()) {
                type.asDocument * name
            } else {
                val docArguments: List<Document> = arguments.map { it.asDocument }
                type.asDocument * name + "(" + (docArguments.concatenated(Document(", "))) + ")"
            }
        }
}

data class CppArrayDecl(
    val type: CppType,
    val name: CppIdentifier,
    val length: CppExpression
) : CppSimpleStatement() {
    override val asDocument: Document
        get() = CppPointerType(type).asDocument * Document(name) *
                Document("=") * Document("new") * type.asDocument +
                (Document("[") + length.asDocument + Document("]"))
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

data class CppCallStmt(val call: CppCall) : CppSimpleStatement() {
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
        get() {
            return if (elseBranch.statements.isNotEmpty()) {
                (keyword("if") * "(" + guard + ")") * thenBranch * keyword("else") * elseBranch
            } else {
                (keyword("if") * "(" + guard + ")") * thenBranch
            }
        }
}

data class CppWhileLoop(
    val guard: CppExpression,
    val body: CppBlock
) : CppStatement() {
    override val asDocument: Document
        get() = (keyword("while") * "(" + guard + ")") * body
}

object CppBreak : CppSimpleStatement() {
    override val asDocument: Document
        get() = keyword("break")
}

// top-level decls
sealed class CppTopLevelDeclaration : CppAst()

data class CppFunctionDecl(
    val type: CppType,
    val name: CppIdentifier,
    val arguments: List<CppVariableDecl>,
    val body: CppBlock
) : CppTopLevelDeclaration() {
    override val asDocument: Document
        get() {
            val docArguments: List<Document> = arguments.map { it.asDocument }
            return type.asDocument *
                    (Document(name) + "(" + docArguments.concatenated(Document(", ")) + ")") *
                    body.asDocument
        }
}

data class CppDefineMacro(
    val name: CppIdentifier,
    val value: CppExpression
) : CppTopLevelDeclaration() {
    override val asDocument: Document
        get() = Document("#define") * name * value.asDocument
}

// program
data class CppProgram(
    val declarations: List<CppTopLevelDeclaration>
) : CppAst() {
    override val asDocument: Document
        get() =
            (declarations.map { it.asDocument })
                .concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
}
