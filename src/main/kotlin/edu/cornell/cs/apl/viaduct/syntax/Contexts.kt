package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * A mapping from a name to the piece of data associated with it along with the source location
 * where the name was first declared.
 */
private typealias NameMap<Name, Data> = PersistentMap<Name, Pair<Data, SourceLocation>>

/**
 * Provides information about [Name]s in scope.
 *
 * Only provides information about names declared at statement level.
 * See [ProgramContextProvider] for [Name]s declared at the top level.
 *
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param LoopData Context information attached to each [JumpLabel].
 */
interface StatementContextProvider<out TemporaryData, out ObjectData, out LoopData> {
    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getTemporaryData")
    operator fun get(name: TemporaryNode): TemporaryData

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getObjectData")
    operator fun get(name: ObjectVariableNode): ObjectData

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getLoopData")
    operator fun get(name: JumpLabelNode): LoopData
}

/**
 * Provides information about [Name]s that are declared at the program top level.
 *
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
interface ProgramContextProvider<out HostData, out ProtocolData> {
    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getHostData")
    operator fun get(name: HostNode): HostData

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getProtocolData")
    operator fun get(name: ProtocolNode): ProtocolData
}

/** A [StatementContextProvider] that supports persistent updates. */
interface PersistentStatementContextProvider<TemporaryData, ObjectData, LoopData> :
    StatementContextProvider<TemporaryData, ObjectData, LoopData> {
    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putTemporaryData")
    fun put(name: TemporaryNode, data: TemporaryData):
        PersistentStatementContextProvider<TemporaryData, ObjectData, LoopData>

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putObjectData")
    fun put(name: ObjectVariableNode, data: ObjectData):
        PersistentStatementContextProvider<TemporaryData, ObjectData, LoopData>

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putLoopData")
    fun put(name: JumpLabelNode, data: LoopData):
        PersistentStatementContextProvider<TemporaryData, ObjectData, LoopData>
}

/** An implementation of [PersistentStatementContextProvider]. */
class StatementContext<TemporaryData, ObjectData, LoopData>
private constructor(
    private val temporaries: NameMap<Temporary, TemporaryData>,
    private val objects: NameMap<ObjectVariable, ObjectData>,
    private val loops: NameMap<JumpLabel, LoopData>
) : PersistentStatementContextProvider<TemporaryData, ObjectData, LoopData> {
    /** Constructs the empty context. */
    constructor() : this(persistentMapOf(), persistentMapOf(), persistentMapOf())

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getTemporaryData")
    override operator fun get(name: TemporaryNode): TemporaryData {
        return temporaries[name.value]?.first ?: throw UndefinedNameError(name)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getObjectData")
    override operator fun get(name: ObjectVariableNode): ObjectData {
        return objects[name.value]?.first ?: throw UndefinedNameError(name)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getLoopData")
    override operator fun get(name: JumpLabelNode): LoopData {
        return loops[name.value]?.first ?: throw UndefinedNameError(name)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putTemporaryData")
    override fun put(name: TemporaryNode, data: TemporaryData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, temporaries)
        return copy(temporaries = temporaries.put(name.value, Pair(data, name.sourceLocation)))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putObjectData")
    override fun put(name: ObjectVariableNode, data: ObjectData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, objects)
        return copy(objects = objects.put(name.value, Pair(data, name.sourceLocation)))
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("putLoopData")
    override fun put(name: JumpLabelNode, data: LoopData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, loops)
        return copy(loops = loops.put(name.value, Pair(data, name.sourceLocation)))
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        temporaries: NameMap<Temporary, TemporaryData> = this.temporaries,
        objects: NameMap<ObjectVariable, ObjectData> = this.objects,
        loops: NameMap<JumpLabel, LoopData> = this.loops
    ): StatementContext<TemporaryData, ObjectData, LoopData> {
        return StatementContext(temporaries, objects, loops)
    }
}

/** A [ProgramContextProvider] that supports updates. */
class ProgramContext<HostData, ProtocolData>
private constructor(
    private val hosts: NameMap<Host, HostData>,
    private val protocols: NameMap<Protocol, ProtocolData>
) : ProgramContextProvider<HostData, ProtocolData> {
    /** Constructs the empty context. */
    constructor() : this(persistentMapOf(), persistentMapOf())

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getHostData")
    override operator fun get(name: HostNode): HostData {
        return hosts[name.value]?.first ?: throw UndefinedNameError(name)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getProtocolData")
    override operator fun get(name: ProtocolNode): ProtocolData {
        return protocols[name.value]?.first ?: throw UndefinedNameError(name)
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putHostData")
    fun put(name: HostNode, data: HostData): ProgramContext<HostData, ProtocolData> {
        assertNotDeclared(name, hosts)
        return copy(hosts = hosts.put(name.value, Pair(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putProtocolData")
    fun put(
        name: ProtocolNode,
        data: ProtocolData
    ): ProgramContext<HostData, ProtocolData> {
        assertNotDeclared(name, protocols)
        return copy(protocols = protocols.put(name.value, Pair(data, name.sourceLocation)))
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        hosts: NameMap<Host, HostData> = this.hosts,
        protocols: NameMap<Protocol, ProtocolData> = this.protocols
    ): ProgramContext<HostData, ProtocolData> {
        return ProgramContext(hosts, protocols)
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
