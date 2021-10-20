package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backends.zkp.ZKP
import edu.cornell.cs.apl.viaduct.passes.MuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

fun zkpMuxPostprocessor(
    protocolAssignment: (FunctionName, Variable) -> Protocol
): ProgramPostprocessor = MuxPostprocessor({ p -> p is ZKP }, protocolAssignment)
