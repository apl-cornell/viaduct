package edu.cornell.cs.apl.viaduct.cli

import com.github.ajalt.clikt.core.CliktCommand
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.lowering.LoweringPass
import edu.cornell.cs.apl.viaduct.lowering.PartialEvaluator
import edu.cornell.cs.apl.viaduct.lowering.optimize
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.elaborated
import java.io.File

class Evaluate : CliktCommand(help = "Partially evaluate compiled program.") {
    private val input: File? by inputProgram()

    override fun run() {
        val program =
            input.sourceFile()
                .parse(DefaultCombinedBackend.protocolParsers)
                .elaborated()

        val flowchart = LoweringPass.lower(program.main.body).optimize()
        println("original")
        println(flowchart.toDocument().print())

        val residualFlowchart = PartialEvaluator.evaluate(flowchart).optimize()
        println("\nresidual")
        println(residualFlowchart.toDocument().print())
    }
}
