package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.analysis.AnalysisProvider
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.SourceLocation
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
        override val sourceLocation: SourceLocation,
    ) : Node(), List<TopLevelDeclarationNode> by declarations {
        constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
            this(declarations.toPersistentList(), sourceLocation)

        // TODO: Should be moved to analysis.Declarations
        val hostDeclarations: Iterable<HostDeclarationNode>
            get() = declarations.filterIsInstance<HostDeclarationNode>()

        // TODO: Should be provided by HostTrustConfiguration
        val hosts: Set<Host>
            get() = hostDeclarations.map { it.name.value }.toSet()

        // TODO: Should be moved to analysis.Declarations
        val functions: Iterable<FunctionDeclarationNode>
            get() = declarations.filterIsInstance<FunctionDeclarationNode>()

        val functionMap: Map<FunctionName, FunctionDeclarationNode>
            get() = functions.associateBy { function -> function.name.value }

        val analyses: AnalysisProvider<Node, ProgramNode> = AnalysisProvider(this)

        override val children: Iterable<TopLevelDeclarationNode>
            get() = declarations

        override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.ProgramNode =
            io.github.aplcornell.viaduct.syntax.surface.ProgramNode(
                declarations.map { it.toSurfaceNode(metadata) },
                sourceLocation,
            )

        override fun copy(children: List<Node>): ProgramNode = ProgramNode(children.map { it as TopLevelDeclarationNode }, sourceLocation)

        override fun toString(): String = "Program (" + sourceLocation.sourcePath + ")"
    }
