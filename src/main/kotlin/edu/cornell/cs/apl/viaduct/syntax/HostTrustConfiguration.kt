package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.security.Label

/** A map that associates each host with its authority label. */
typealias HostTrustConfiguration = Map<Host, Label>
