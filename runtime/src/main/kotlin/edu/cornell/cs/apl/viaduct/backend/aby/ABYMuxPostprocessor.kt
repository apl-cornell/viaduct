package edu.cornell.cs.apl.viaduct.backend.aby

import edu.cornell.cs.apl.viaduct.backends.aby.ABY
import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.selection.ProtocolAssignment

fun abyMuxPostprocessor(
    protocolAssignment: ProtocolAssignment
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ABY }, protocolAssignment)
