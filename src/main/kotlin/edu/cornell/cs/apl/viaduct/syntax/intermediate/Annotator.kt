package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Traverses this program with [visitor] and returns annotations computed for the program.
 *
 * Annotations are per [Protocol]. Annotations on [Variable]s are the return values of
 * [StatementVisitorWithContext.getData] calls.
 */
fun <StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, HostData, ProtocolData> ProgramNode.annotate(
    visitor: ProgramVisitorWithVariableContext<StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, HostData, ProtocolData>
): ProgramAnnotationMap<TemporaryData, ObjectData> {
    val annotator = ProgramAnnotator(visitor)
    this.traverse(annotator)
    return annotator.annotations
}

/**
 * A mapping from names to their annotations along with the source locations where the names were
 * first declared.
 */
private typealias NameMap<Name, Annotation> = PersistentMap<Name, Pair<Annotation, SourceLocation>>

/**
 * A mapping from [Variable]s in a statement to their annotations.
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
    fun get(variable: TemporaryNode): TemporaryAnnotation {
        return temporaries[variable.value]?.first ?: throw UndefinedNameError(variable)
    }

    /**
     * Returns the annotation on [variable].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getObjectAnnotation")
    fun get(variable: ObjectVariableNode): ObjectAnnotation {
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
        assertNotDeclared(variable, temporaries)
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
        assertNotDeclared(variable, objects)
        return copy(
            objects = objects.put(
                variable.value,
                Pair(annotation, variable.sourceLocation)
            )
        )
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        temporaries: NameMap<Temporary, TemporaryAnnotation> = this.temporaries,
        objects: NameMap<ObjectVariable, ObjectAnnotation> = this.objects
    ): VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        return VariableAnnotationMap(temporaries, objects)
    }
}

/**
 * A mapping from [Protocol]s to the annotations on their body.
 *
 * @see VariableAnnotationMap
 */
class ProgramAnnotationMap<TemporaryAnnotation, ObjectAnnotation>
private constructor(
    private val statementAnnotations: NameMap<Protocol, VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation>>
) {
    /** Returns the empty map. */
    constructor() : this(persistentMapOf())

    /**
     * Returns the annotations for the body of [protocol].
     *
     * @throws UndefinedNameError
     */
    fun get(protocol: ProtocolNode): VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        return statementAnnotations[protocol.value]?.first ?: throw UndefinedNameError(protocol)
    }

    /**
     * Returns a new map where the body of [protocol] is associated with [annotations].
     *
     * @throws NameClashError
     */
    fun put(
        protocol: ProtocolNode,
        annotations: VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation>
    ): ProgramAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        assertNotDeclared(protocol, statementAnnotations)
        return copy(
            statementAnnotations = statementAnnotations.put(
                protocol.value,
                Pair(annotations, protocol.sourceLocation)
            )
        )
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        statementAnnotations: NameMap<Protocol, VariableAnnotationMap<TemporaryAnnotation, ObjectAnnotation>> = this.statementAnnotations
    ): ProgramAnnotationMap<TemporaryAnnotation, ObjectAnnotation> {
        return ProgramAnnotationMap(statementAnnotations)
    }
}

/**
 * Like [ProgramVisitorWithContext], but fixes [TemporaryData] and [ObjectData].
 *
 * @param StatementResult Data returned from each [StatementNode].
 * @param DeclarationResult Data returned from each [TopLevelDeclarationNode].
 * @param ProgramResult Data returned from the [ProgramNode].
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
abstract class ProgramVisitorWithVariableContext<StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, HostData, ProtocolData> :
    ProgramVisitorWithContext<StatementResult, DeclarationResult, ProgramResult, HostData, ProtocolData> {
    final override fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<StatementResult, *, *, *, HostData, ProtocolData>
    ): DeclarationResult {
        return leaveProcessDeclaration(node) { body(it) }
    }

    abstract fun leaveProcessDeclaration(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<StatementResult, TemporaryData, ObjectData, *, HostData, ProtocolData>
    ): DeclarationResult
}

/**
 * A wrapper for [visitor] that behaves the same, except it stores computed XXXResult and XXXData
 * into [annotations].
 */
private class StatementAnnotator<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>(
    val visitor: StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
) : StatementVisitorWithContext<ExpressionResult, StatementResult, TemporaryData, ObjectData, LoopData, HostData, ProtocolData> by visitor {
    val annotations = VariableAnnotationMap<TemporaryData, ObjectData>()

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
 * A wrapper for [visitor] that behaves the same, except it stores annotations computed for each
 * process body into [annotations].
 */
private class ProgramAnnotator<StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, HostData, ProtocolData>(
    val visitor: ProgramVisitorWithVariableContext<StatementResult, DeclarationResult, ProgramResult, TemporaryData, ObjectData, HostData, ProtocolData>
) : ProgramVisitorWithContext<StatementResult, DeclarationResult, ProgramResult, HostData, ProtocolData> by visitor {
    val annotations = ProgramAnnotationMap<TemporaryData, ObjectData>()

    override fun leave(
        node: ProcessDeclarationNode,
        body: SuspendedTraversal<StatementResult, *, *, *, HostData, ProtocolData>
    ): DeclarationResult {
        return visitor.leaveProcessDeclaration(node) { visitor ->
            val annotator = StatementAnnotator(visitor)
            val bodyResult = body(annotator)
            annotations.put(node.protocol, annotator.annotations)
            bodyResult
        }
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
