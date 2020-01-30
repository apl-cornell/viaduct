package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Maintains information about [Name]s in scope at a specific point in the abstract syntax tree.
 *
 * @param TemporaryData Context information attached to each [Temporary] declaration.
 * @param ObjectData Context information attached to each [ObjectVariable] declaration.
 * @param LoopData Context information attached to each [JumpLabel].
 * @param HostData Context information attached to each [Host] declaration.
 * @param ProtocolData Context information attached to each [Protocol] declaration.
 */
class Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData>
private constructor(
    // Below, [Located] tracks the source location where the [Name] was declared.
    private val temporaries: PersistentMap<Temporary, Located<TemporaryData>>,
    private val objects: PersistentMap<ObjectVariable, Located<ObjectData>>,
    private val loops: PersistentMap<JumpLabel, Located<LoopData>>,
    private val hosts: PersistentMap<Host, Located<HostData>>,
    private val protocols: PersistentMap<Protocol, Located<ProtocolData>>
) {
    /** Constructs the empty context. */
    constructor() : this(
        persistentMapOf(),
        persistentMapOf(),
        persistentMapOf(),
        persistentMapOf(),
        persistentMapOf()
    )

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    fun getTemporaryData(name: TemporaryNode): TemporaryData {
        return temporaries[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    fun getObjectData(name: ObjectVariableNode): ObjectData {
        return objects[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    fun getLoopData(name: JumpLabelNode): LoopData {
        return loops[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    fun getHostData(name: HostNode): HostData {
        return hosts[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError
     */
    fun getProtocolData(name: ProtocolNode): ProtocolData {
        return protocols[name.value]?.value ?: throw UndefinedNameError(name)
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    fun putTemporaryData(name: TemporaryNode, data: TemporaryData):
        Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        val previousDeclaration = temporaries[name.value]?.sourceLocation
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return copy(temporaries = temporaries.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    fun putObjectData(name: ObjectVariableNode, data: ObjectData):
        Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        val previousDeclaration = objects[name.value]?.sourceLocation
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return copy(objects = objects.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    fun putLoopData(name: JumpLabelNode, data: LoopData):
        Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        val previousDeclaration = loops[name.value]?.sourceLocation
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return copy(loops = loops.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    fun putHostData(name: HostNode, data: HostData):
        Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        val previousDeclaration = hosts[name.value]?.sourceLocation
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return copy(hosts = hosts.put(name.value, Located(data, name.sourceLocation)))
    }

    /**
     * Returns a new context where [name] is associated with [data].
     *
     * @throws NameClashError
     */
    fun putProtocolData(name: ProtocolNode, data: ProtocolData):
        Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        val previousDeclaration = protocols[name.value]?.sourceLocation
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return copy(protocols = protocols.put(name.value, Located(data, name.sourceLocation)))
    }

    /** Creates a copy of this object where some fields are modified. */
    private fun copy(
        temporaries: PersistentMap<Temporary, Located<TemporaryData>> = this.temporaries,
        objects: PersistentMap<ObjectVariable, Located<ObjectData>> = this.objects,
        loops: PersistentMap<JumpLabel, Located<LoopData>> = this.loops,
        hosts: PersistentMap<Host, Located<HostData>> = this.hosts,
        protocols: PersistentMap<Protocol, Located<ProtocolData>> = this.protocols
    ): Context<TemporaryData, ObjectData, LoopData, HostData, ProtocolData> {
        return Context(temporaries, objects, loops, hosts, protocols)
    }
}
