package io.github.apl_cornell.viaduct.backends.commitment

import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorDispatcher
import io.github.apl_cornell.viaduct.syntax.Protocol

class CommitmentDispatchCodeGenerator(
    private val context: CodeGeneratorContext
) : CodeGeneratorDispatcher() {
    private val commitmentCreatorGenerator = CommitmentCreatorGenerator(context)
    private val commitmentHolderGenerator = CommitmentHolderGenerator(context)

    override fun generatorFor(protocol: Protocol): CodeGenerator {
        require(protocol is Commitment)
        return if (context.host == protocol.cleartextHost)
            commitmentCreatorGenerator
        else
            commitmentHolderGenerator
    }
}
