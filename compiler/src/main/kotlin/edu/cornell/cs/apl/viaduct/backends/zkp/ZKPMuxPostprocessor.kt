package edu.cornell.cs.apl.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.selection.ProtocolAssignment

fun zkpMuxPostprocessor(
    protocolAssignment: ProtocolAssignment,
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ZKP }, protocolAssignment)
