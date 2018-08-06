package IR

import kotlin.math.max

class When(level: Long, children: List<IRNode>, val caseBlockIndex: Int): IRNode(children.get(caseBlockIndex).getResultType()) {

    init{                                                       //TODO: how to add typecheck to cases?  I think, just add as a statement...
        this.level = level
        addChildren(children)
    }

    override fun complexity(): Long {
        val whenExpr = getChild(0)
        var compl = whenExpr?.complexity() ?: 0
        for (i in caseBlockIndex until children.size){
            compl += getChild(i)?.complexity() ?: 0
        }
        return compl
    }

    override fun countDepth() = max(level, super.countDepth())
}