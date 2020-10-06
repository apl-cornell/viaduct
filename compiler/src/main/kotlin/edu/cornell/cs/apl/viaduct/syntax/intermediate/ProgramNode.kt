package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * The intermediate level representation of a program.
 *
 * Instances are created by [elaborated].
 */
class ProgramNode
private constructor(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
        this(declarations.toPersistentList(), sourceLocation)

    val functions: Iterable<FunctionDeclarationNode> =
        declarations.filterIsInstance<FunctionDeclarationNode>()

    val functionMap: Map<FunctionName, FunctionDeclarationNode> =
        functions.map { function -> Pair(function.name.value, function) }.toMap()

    override val children: Iterable<TopLevelDeclarationNode>
        get() = declarations

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode(
            declarations.map { it.toSurfaceNode() },
            sourceLocation
        )

    override fun copy(children: List<Node>): ProgramNode =
        ProgramNode(children.map { it as TopLevelDeclarationNode }, sourceLocation)

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"

    override fun printMetadata(metadata: Map<Node, PrettyPrintable>): Document =
        declarations
            .map { it.printMetadata(metadata) }
            .concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
}
