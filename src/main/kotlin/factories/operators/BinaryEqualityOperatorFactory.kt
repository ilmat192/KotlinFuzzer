package factories.operators

import information.TypeList
import ir.operators.OperatorKind
import ir.types.Type
import utils.PseudoRandom

class BinaryEqualityOperatorFactory(
        opKind: OperatorKind,
        complexityLimit: Long,
        operatorLimit: Int,
        ownerClass: Type?,
        exceptionSafe: Boolean,
        noconsts: Boolean
) : BinaryOperatorFactory(opKind, complexityLimit, operatorLimit, ownerClass, TypeList.BOOLEAN, exceptionSafe, noconsts) {

    override fun isApplicable(resultType: Type) = resultType == TypeList.BOOLEAN

    override fun generateTypes() = Pair(TypeList.BOOLEAN, TypeList.BOOLEAN)
}

