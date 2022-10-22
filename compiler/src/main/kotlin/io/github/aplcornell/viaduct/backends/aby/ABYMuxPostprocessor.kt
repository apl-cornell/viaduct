package io.github.aplcornell.viaduct.backends.aby

import io.github.aplcornell.viaduct.passes.MuxPostprocessor
import io.github.aplcornell.viaduct.passes.ProgramPostprocessor
import io.github.aplcornell.viaduct.selection.ProtocolAssignment

fun abyMuxPostprocessor(
    protocolAssignment: ProtocolAssignment
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ABY }, protocolAssignment)
