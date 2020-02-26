package edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

/**
 * Traverses this program with [annotator] and returns the annotations computed for the program.
 *
 * Annotations are per [Protocol]. Annotations on [Variable]s are the return values of
 * [StatementVisitorWithContext.getData] calls.
 */
fun <StatementResult, ProcessResult, TemporaryData, ObjectData, HostData, ProtocolData> ProgramNode.annotate(
    annotator: ProgramAnnotator<StatementResult, ProcessResult, TemporaryData, ObjectData, HostData, ProtocolData>
): Map<Protocol, ProcessResult> =
    this.traverse(annotator)

/**
 * A mapping from names to their annotations along with the source locations where the names were
 * first declared.
 */
private typealias NameMap<Name, Annotation> = PersistentMap<Name, Pair<Annotation, SourceLocation>>

/**
 * A mapping from [Variable]s in a process to their annotations.
 *
 * @param TemporaryAnnotation Annotations attached to [Temporary] variables.
 * @param ObjectAnnotation Annotations attached to [ObjectVariable]s.
 */
class VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation>
private constructor(
    private val temporaries: NameMap<Temporary, TemporaryAnnotation>,
    private val objects: NameMap<ObjectVariable, ObjectAnnotation>
) {
    /** Returns the empty map. */
    constructor() : this(persistentMapOf(), persistentMapOf())

    /**
     * Returns the annotation on [variable].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getTemporaryAnnotation")
    operator fun get(variable: TemporaryNode): TemporaryAnnotation {
        return temporaries[variable.value]?.first ?: throw UndefinedNameError(variable)
    }

    /**
     * Returns the annotation on [variable].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getObjectAnnotation")
    operator fun get(variable: ObjectVariableNode): ObjectAnnotation {
        return objects[variable.value]?.first ?: throw UndefinedNameError(variable)
    }

    /**
     * Returns a new map where [variable] is associated with [annotation].
     *
     * @throws NameClashError
     */
    @JvmName("putTemporaryAnnotation")
    fun put(variable: TemporaryNode, annotation: TemporaryAnnotation):
        VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        assertNotDeclared(
            variable,
            temporaries
        )
        return copy(
            temporaries = temporaries.put(
                variable.value,
                Pair(annotation, variable.sourceLocation)
            )
        )
    }

    /**
     * Returns a new map where [variable] is associated with [annotation].
     *
     * @throws NameClashError
     */
    @JvmName("putObjectAnnotation")
    fun put(variable: ObjectVariableNode, annotation: ObjectAnnotation):
        VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        assertNotDeclared(
            variable,
            objects
        )
        return copy(
            objects = objects.put(
                variable.value,
                Pair(annotation, variable.sourceLocation)
            )
        )
    }

    /**
     * Returns a new map where annotations attached to [Temporary] variables are transformed
     * using [temporaries], and annotations attached to [ObjectVariable]s are transformed using
     * [objects].
     */
    fun <TemporaryAnnotation2, ObjectAnnotation2> map(
        temporaries: (TemporaryAnnotation) -> TemporaryAnnotation2,
        objects: (ObjectAnnotation) -> ObjectAnnotation2
    ): VariableAnnotationMap<TemporaryAnnotation2, ObjectAnnotation2> {
        return VariableAnnotationMap(
            this.temporaries.mapValues { Pair(temporaries(it.value.first), it.value.second) }
                .toPersistentMap(),
            this.objects.mapValues { Pair(objects(it.value.first), it.value.second) }
                .toPersistentMap()
        )
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        temporaries: NameMap<Temporary, TemporaryAnnotation> = this.temporaries,
        objects: NameMap<ObjectVariable, ObjectAnnotation> = this.objects
    ): VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        return VariableAnnotationMap(
            temporaries,
            objects
        )
    }
}

/**
 * A visitor that computes a [VariableAnnotationMap] for each [ProcessDeclarationNode] in a program.
 *
 * @param StatementResult Data returned from each [StatementNode].
 * @param ProcessResult Data returned from each [ProcessDeclarationNode].
 * @param TemporaryData Context information and annotation attached to each [Temporary] declaration.
 * @param ObjectData Context information and annotation attached to each [ObjectVariable] declaration.
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
abstract class ProgramAnnotator<StatementResult, ProcessResult, TemporaryData, ObjectData, HostData, ProtocolData> :
    ProgramVisitorWithContext<StatementResult, ProcessResult?, Map<Protocol, ProcessResult>, HostData, ProtocolData> {
    final override fun leave(node: HostDeclarationNode): ProcessResult? = null

    final override fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<StatementResult, *, *, *, HostData, ProtocolData>
    ): ProcessResult {
        return leaveProcessDeclaration(node) { visitor ->
            val annotator = StatementAnnotator(visitor)
            body(annotator)
            annotator.annotations
        }
    }

    final override fun leave(
        node: ProgramNode,
        declarations: List<ProcessResult?>
    ): Map<Protocol, ProcessResult> {
        val result = mutableMapOf<Protocol, ProcessResult>()
        node.declarations.zip(declarations) { declaration, processResult ->
            if (declaration is ProcessDeclarationNode) {
                result[declaration.protocol.value] = processResult!!
            }
        }
        return result
    }

    abstract fun leaveProcessDeclaration(
        node: ProcessDeclarationNode,
        body: (StatementVisitorWithContext<*, StatementResult, TemporaryData, ObjectData, *, HostData, ProtocolData>)
        -> VariableAnnotationMap<TemporaryData, ObjectData>
    ): ProcessResult
}

/**
 * A wrapper for [visitor] that behaves the same, except it stores computed XXXResult and XXXData
 * into [annotations].
 */
private class StatementAnnotator<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>(
    val visitor: StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
) : StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> by visitor {
    val annotations =
        VariableAnnotationMap<TemporaryData, ObjectData>()

    override fun getData(node: LetNode, value: ExpressionResult): TemporaryData {
        val annotation = visitor.getData(node, value)
        annotations.put(node.temporary, annotation)
        return annotation
    }

    override fun getData(node: DeclarationNode, arguments: List<ExpressionResult>): ObjectData {
        val annotation = visitor.getData(node, arguments)
        annotations.put(node.variable, annotation)
        return annotation
    }

    override fun getData(node: InputNode, data: HostData): TemporaryData {
        val annotation = visitor.getData(node, data)
        annotations.put(node.temporary, annotation)
        return annotation
    }

    override fun getData(node: ReceiveNode, data: ProtocolData): TemporaryData {
        val annotation = visitor.getData(node, data)
        annotations.put(node.temporary, annotation)
        return annotation
    }
}

/**
 * Asserts that a [Name] does not have a prior declaration.
 *
 * @throws NameClashError
 */
private fun <N : Name> assertNotDeclared(name: Located<N>, declarations: NameMap<N, *>) {
    val previousDeclaration = declarations[name.value]?.second
    if (previousDeclaration != null) {
        throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
    }
}
