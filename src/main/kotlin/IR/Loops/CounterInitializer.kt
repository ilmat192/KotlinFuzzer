package IR.Loops

import IR.IRNode
import IR.Initialization
import Information.VariableInfo
import Visitors.Visitor

class CounterInitializer(varInfo: VariableInfo, initExpr: IRNode): Initialization(varInfo, initExpr){
    override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
}