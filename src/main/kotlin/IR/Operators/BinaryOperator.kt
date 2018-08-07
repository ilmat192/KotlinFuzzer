package IR.Operators

import IR.IRNode
import IR.Types.Type
import Visitors.Visitor

class BinaryOperator(
        opKind: OperatorKind,
        resultType: Type?,
        leftOperand: IRNode,
        rightOperand: IRNode
): Operator(opKind, resultType) {
    init {
        if (!opKind.isBinary()) throw Exception("Illegal operator kind: ${opKind.name}")
        addChild(leftOperand)
        addChild(rightOperand)
    }

    override fun complexity(): Long {
        return getChild(Order.LEFT.ordinal).complexity() + getChild(Order.RIGHT.ordinal).complexity() + 1
    }

    override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
}