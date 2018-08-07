package IR.Loops

import IR.IRNode
import IR.Statement
import Visitors.Visitor

/*
 * Note: Can be theoretically subclassed from Operator and have an          //???
 * operatorPriority field. Therefore, it can used later as a part
 * of some expression.
 */

class CounterManipulator(manipulator: Statement): IRNode(manipulator.getResultType()){
    init {
        addChild(manipulator)
    }

    override fun complexity() = getChild(0).complexity()

    override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
}