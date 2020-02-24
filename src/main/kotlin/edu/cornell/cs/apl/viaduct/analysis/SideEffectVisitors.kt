package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.ExpressionReducer
import edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors.StatementReducer

/** An expression visitor that produces side effects but no result. */
internal interface EffectfulExpressionVisitor : ExpressionReducer<Unit> {
    override val initial: Unit get() = Unit

    override val combine: (Unit, Unit) -> Unit
        get() = ignore
}

/** A statement visitor that produces side effects but no result. */
internal interface EffectfulStatementVisitor : EffectfulExpressionVisitor, StatementReducer<Unit> {
    override val initial: Unit get() = Unit

    override val combine: (Unit, Unit) -> Unit
        get() = ignore
}

private val ignore: (Unit, Unit) -> Unit = { _, _ -> Unit }
