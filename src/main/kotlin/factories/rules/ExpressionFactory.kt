package factories.rules

import exceptions.NotInitializedOptionException
import factories.SafeFactory
import factories.rules.operators.BitwiseOperatorFactory
import factories.utils.IRNodeBuilder
import factories.utils.ProductionLimiter
import information.TypeList
import ir.IRNode
import ir.operators.OperatorKind
import ir.types.Type
import utils.ProductionParams

open class ExpressionFactory(
        complexityLimit: Long,
        operatorLimit: Int,
        ownerClass: Type?,
        resultType: Type,
        exceptionSafe: Boolean,
        noconsts: Boolean
): SafeFactory<IRNode>() {
    private val rule = Rule<IRNode>("expression")

    init {
        val builder = IRNodeBuilder().
                setComplexityLimit(complexityLimit).
                setOperatorLimit(operatorLimit).
                setOwnerClass(ownerClass).
                setResultType(resultType).
                setExceptionSafe(exceptionSafe).
                setNoConsts(noconsts).
                setForLoop(false)

        if (!noconsts){
            rule.add("literal", builder.getLiteralFactory())
            rule.add("constant", builder.setIsConstant(true).setIsInitialized(true).getVariableFactory())
        }
        rule.add("variable", builder.setIsConstant(false).setIsInitialized(true).getVariableFactory())
        if (operatorLimit > 0 && complexityLimit > 0){
            rule.add("cast", builder.getCastOperatorFactory(), 0.1)
            if (resultType == TypeList.BOOLEAN) {
                rule.add("logic", builder.getLogicOperatorFactory())
                rule.add("range", builder.getRangeOperatorFactory())
            } else {
                rule.add("bitwise", BitwiseOperatorFactory(complexityLimit, operatorLimit, ownerClass, resultType, exceptionSafe, noconsts))
                rule.add("arithmetic", builder.getArithmeticOperatorFactory())
            }
            if (resultType == TypeList.STRING) rule.add("str_plus", builder.setOperatorKind(OperatorKind.STRADD).getBinaryOperatorFactory())
            rule.add("function", builder.getFunctionCallFactory(), ProductionParams.functionCallsPercent?.value() ?: 0.01)

        }
        if (!(ProductionParams.disableArrays?.value()
                ?: throw NotInitializedOptionException("disableArrays"))
                && !exceptionSafe) {
            //rule.add("array_creation", builder.getArrayCreationFactory()) //TODO: add array creation without declaring variable (just add to class list?)
            rule.add("array_element", builder.getArrayElementFactory())
        }
    }

    override fun sproduce(): IRNode {
        ProductionLimiter.limitProduction()
        return rule.produce()
    }
}