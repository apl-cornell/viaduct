package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlin.reflect.KClass

/** Returns the [KClass] object for values of this type. */
val ValueType.valueClass: KClass<out Value>
    get() = this.defaultValue::class
