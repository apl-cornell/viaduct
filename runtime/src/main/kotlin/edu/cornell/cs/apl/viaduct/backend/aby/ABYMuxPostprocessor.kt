package edu.cornell.cs.apl.viaduct.backend.aby

import edu.cornell.cs.apl.viaduct.backends.aby.ABY
import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

fun ABYMuxPostprocessor(
    protocolAssignment: (FunctionName, Variable) -> Protocol
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ABY }, protocolAssignment)
