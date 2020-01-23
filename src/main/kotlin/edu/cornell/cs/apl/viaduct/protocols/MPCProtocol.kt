package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.syntax.Protocol

/**
 * A secure multiparty computation protocol.
 *
 * There are many kinds of MPC protocols, and each protocol provides a slightly different
 * security guarantee. There should be a subclass for every security condition.
 */
abstract class MPCProtocol : Protocol
