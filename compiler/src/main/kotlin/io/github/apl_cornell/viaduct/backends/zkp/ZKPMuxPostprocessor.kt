package io.github.apl_cornell.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import io.github.apl_cornell.viaduct.passes.ProgramPostprocessor
import io.github.apl_cornell.viaduct.selection.ProtocolAssignment

fun zkpMuxPostprocessor(
    protocolAssignment: ProtocolAssignment,
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ZKP }, protocolAssignment)
