package io.github.apl_cornell.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import io.github.apl_cornell.viaduct.passes.ProgramPostprocessor
import io.github.apl_cornell.viaduct.selection.ProtocolAssignment

fun abyMuxPostprocessor(
    protocolAssignment: ProtocolAssignment
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ABY }, protocolAssignment)
