package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Maintains information about [Name]s in scope.
 *
 * Only maintains information about names declared at statement level.
 * See [ProgramContext] for [Name]s declared at the top level.
 *
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param LoopData Context information attached to each [JumpLabel].
 */
internal class StatementContext<TemporaryData, ObjectData, LoopData>
private constructor(
    // Below, [Located] tracks the source location where the [Name] was declared.
    private val temporaries: PersistentMap<Temporary, Located<TemporaryData>>,
    private val objects: PersistentMap<ObjectVariable, Located<ObjectData>>,
    private val loops: PersistentMap<JumpLabel, Located<LoopData>>
) {
    /** Constructs the empty context. */
    constructor() : this(persistentMapOf(), persistentMapOf(), persistentMapOf())

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getTemporaryData")
    fun get(name: TemporaryNode): TemporaryData {
        return temporaries[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getObjectData")
    fun get(name: ObjectVariableNode): ObjectData {
        return objects[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getLoopData")
    fun get(name: JumpLabelNode): LoopData {
        return loops[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putTemporaryData")
    fun put(name: TemporaryNode, data: TemporaryData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, temporaries)
        return copy(temporaries = temporaries.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putObjectData")
    fun put(name: ObjectVariableNode, data: ObjectData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, objects)
        return copy(objects = objects.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putLoopData")
    fun put(name: JumpLabelNode, data: LoopData):
        StatementContext<TemporaryData, ObjectData, LoopData> {
        assertNotDeclared(name, loops)
        return copy(loops = loops.put(name.value, Located(data, name.sourceLocation)))
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        temporaries: PersistentMap<Temporary, Located<TemporaryData>> = this.temporaries,
        objects: PersistentMap<ObjectVariable, Located<ObjectData>> = this.objects,
        loops: PersistentMap<JumpLabel, Located<LoopData>> = this.loops
    ): StatementContext<TemporaryData, ObjectData, LoopData> {
        return StatementContext(temporaries, objects, loops)
    }
}

/**
 * Maintains information about [Name]s that are declared at the program top level.
 *
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
internal class ProgramContext<HostData, ProtocolData>
private constructor(
    // Below, [Located] tracks the source location where the [Name] was declared.
    private val hosts: PersistentMap<Host, Located<HostData>>,
    private val protocols: PersistentMap<Protocol, Located<ProtocolData>>
) {
    /** Constructs the empty context. */
    constructor() : this(persistentMapOf(), persistentMapOf())

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getHostData")
    fun get(name: HostNode): HostData {
        return hosts[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    @JvmName("getProtocolData")
    fun get(name: ProtocolNode): ProtocolData {
        return protocols[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putHostData")
    fun put(name: HostNode, data: HostData): ProgramContext<HostData, ProtocolData> {
        assertNotDeclared(name, hosts)
        return copy(hosts = hosts.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    @JvmName("putProtocolData")
    fun put(name: ProtocolNode, data: ProtocolData): ProgramContext<HostData, ProtocolData> {
        assertNotDeclared(name, protocols)
        return copy(protocols = protocols.put(name.value, Located(data, name.sourceLocation)))
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        hosts: PersistentMap<Host, Located<HostData>> = this.hosts,
        protocols: PersistentMap<Protocol, Located<ProtocolData>> = this.protocols
    ): ProgramContext<HostData, ProtocolData> {
        return ProgramContext(hosts, protocols)
    }
}

/**
 * Assert that a [Name] does not have a prior declaration.
 *
 * @throws NameClashError
 */
private fun <N : Name> assertNotDeclared(name: Located<N>, declarations: Map<N, Located<*>>) {
    val previousDeclaration = declarations[name.value]?.sourceLocation
    if (previousDeclaration != null) {
        throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
    }
}
