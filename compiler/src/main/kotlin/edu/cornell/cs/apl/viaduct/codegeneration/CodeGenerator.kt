package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.asClassName

abstract class CodeGenerator {
    companion object {
        val test: String
            get() {
                val aby = de.tu_darmstadt.cs.encrypto.aby.Aby::class.asClassName()
                val createNewShare = aby.member("createNewName")
                return FileSpec.builder("", "Test")
                    .addFunction(
                        FunSpec.builder("test")
                            .returns(de.tu_darmstadt.cs.encrypto.aby.Share::class)
                            .addStatement("%M()", createNewShare)
                            .build()
                    )
                    .build()
                    .toString()
            }
    }
}
