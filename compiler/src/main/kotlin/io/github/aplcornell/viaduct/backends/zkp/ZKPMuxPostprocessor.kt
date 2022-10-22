package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.passes.MuxPostprocessor
import io.github.aplcornell.viaduct.passes.ProgramPostprocessor
import io.github.aplcornell.viaduct.selection.ProtocolAssignment

fun zkpMuxPostprocessor(
    protocolAssignment: ProtocolAssignment,
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ZKP }, protocolAssignment)
