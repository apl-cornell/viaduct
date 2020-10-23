package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.backend.MuxPostprocessor
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

fun ZKPMuxPostprocessor(
    protocolAssignment: (FunctionName, Variable) -> Protocol
) : ProgramPostprocessor = MuxPostprocessor({ p -> p is ZKP }, protocolAssignment)

