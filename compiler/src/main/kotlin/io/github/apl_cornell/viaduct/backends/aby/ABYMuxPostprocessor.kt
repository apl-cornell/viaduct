package io.github.apl_cornell.viaduct.backends.aby

import io.github.apl_cornell.viaduct.passes.MuxPostprocessor
import io.github.apl_cornell.viaduct.passes.ProgramPostprocessor
import io.github.apl_cornell.viaduct.selection.ProtocolAssignment

fun abyMuxPostprocessor(
    protocolAssignment: ProtocolAssignment
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ABY }, protocolAssignment)
