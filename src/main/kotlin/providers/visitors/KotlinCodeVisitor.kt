package providers.visitors

import exceptions.NotInitializedOptionException
import information.FunctionInfo
import information.Symbol
import information.TypeList
import information.VariableInfo
import ir.*
import ir.control_flow.Break
import ir.control_flow.Continue
import ir.control_flow.If
import ir.control_flow.When
import ir.functions.*
import ir.loops.*
import ir.operators.*
import ir.operators.OperatorKind.*
import ir.types.Type
import ir.types.TypeNothing
import ir.types.TypeUnit
import ir.variables.LocalVariable
import ir.variables.VariableBase
import ir.variables.VariableDeclaration
import ir.variables.VariableDeclarationBlock
import utils.PrintingUtils
import utils.ProductionParams
import java.util.stream.Collectors

class KotlinCodeVisitor: Visitor<String> {
    private fun attributes(s: Symbol): String {
        val attrs = StringBuilder()
        if (s.isPrivate()) {
            attrs.append("private ")
        }
        if (s.isProtected()) {
            attrs.append("protected ")
        }
        if (s.isInternal()) {
            attrs.append("internal ")
        }
        if (s is VariableInfo && s.isConst() && !s.isLocal()) {
            attrs.append("const ")
        }
        if (s is FunctionInfo) {
            if (s.isAbstract()) {
                attrs.append("abstract ")
            }
            if (!s.isFinal()) {
                attrs.append("open ")
            }
            if (s.isSynchronized()) {
                attrs.append("synchronized ")
            }
        }
        return attrs.toString()
    }

    private fun operatorToKotlinCode(operatorKind: OperatorKind) =  when(operatorKind) {
            COMPOUND_ADD -> "+="
            COMPOUND_SUB -> "-="
            COMPOUND_MUL -> "*="
            COMPOUND_DIV -> "/="
            COMPOUND_MOD -> "%="
            ASSIGN -> "="
            OR -> "||"
            BIT_OR -> "or"
            BIT_XOR -> "xor"
            AND -> "&&"
            BIT_AND -> "and"
            EQ -> "=="
            NE -> "!="
            GT -> ">"
            LT -> "<"
            GE -> ">="
            LE -> "<="
            SHR -> "shr"
            SHL -> "shl"
            USHR -> "ushr"
            ADD, STRADD, UNARY_PLUS -> "+"
            SUB, UNARY_MINUS -> "-"
            MUL -> "*"
            DIV -> "/"
            MOD -> "%"
            NOT -> "!"
            PRE_DEC, POST_DEC -> "--"
            PRE_INC, POST_INC -> "++"
            CAST -> throw IllegalArgumentException("Can't convert cast operator to code this way")
    }

    private fun expressionToKotlinCode(operator: Operator, part: IRNode, order: Operator.Order): String {
        var result: String
        try {
            if ((order == Operator.Order.LEFT && (part as Operator).priority < operator.priority)
                || (order == Operator.Order.RIGHT && (part as Operator).priority <= operator.priority)) {
                result = "(${part.accept(this)})"
            } else {
                result = part.accept(this)
            }
        } catch (ex: Exception) {
            result = part.accept(this)
        }
        return result
    }

    private fun addComplexityInfo(node: IRNode) = if (ProductionParams.printComplexity?.value()
                    ?: throw NotInitializedOptionException("printComplexity"))
                    "/* ${node.complexity()} */"
                    else ""

    override fun visit(node: ArgumentDeclaration): String {
        val vi = node.variableInfo
        return "${attributes(vi)}${vi.name}: ${vi.type.accept(this)}"
    }





    override fun visit(node: BinaryOperator): String {
        val left = node.getChild(Operator.Order.LEFT.ordinal)
        val right = node.getChild(Operator.Order.RIGHT.ordinal)
        return "${expressionToKotlinCode(node, left, Operator.Order.LEFT)} ${operatorToKotlinCode(node.opKind)} ${expressionToKotlinCode(node, right, Operator.Order.RIGHT)}"
    }

    override fun visit(node: Block): String {
        val code = StringBuilder()
        for (i in node.children) {
            val s = i.accept(this)
            if (!s.isEmpty()) {
                val level = node.level
                if (i is Block) {
                    code.append(s)
                } else {
                    code.append(PrintingUtils.align(level)).append(s)
                }
                code.append(addComplexityInfo(i))
                code.append("\n")
            }
        }
        return code.toString()
    }

    override fun visit(node: Break) = "break"

    override fun visit(node: CastOperator): String {
        return expressionToKotlinCode(node, node.getChild(0), Operator.Order.LEFT) +
                " as ${node.getResultType()?.accept(this) ?: throw IllegalArgumentException("Can't cast to target without a type")}"
    }

    override fun visit(node: Continue) = "continue"

    override fun visit(node: CounterInitializer): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: CounterManipulator): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: Declaration) = node.getChild(0).accept(this)

    override fun visit(node: DoWhile): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: For): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: FunctionCall): String {
        val funVal = node.functionInfo
        val nameAndArgs = if (node.owner != null) "${funVal.name}(${
            node.children.stream().
                    skip(if (funVal.isConstructor() || funVal.isStatic()) 0 else 1).
                    map { it?.accept(this) ?: throw IllegalArgumentException("Function argument can't be null") }.
                    collect(Collectors.joining(", "))
        })"
        else "${funVal.name}(${
                node.children.stream().
                map { it?.accept(this) ?: throw IllegalArgumentException("Function argument can't be null") }.
                collect(Collectors.joining(", "))
        })"


        var prefix = ""
        if (funVal.owner != null) {
            if (funVal.isStatic()) {
                if (node.owner == funVal.owner) {
                    prefix = "${funVal.owner!!.getName()}."
                }
            } else {
                val obj = node.getChild(0)
                val objectString = obj.accept(this)
                if (objectString != "this") {
                    if (obj is VariableBase || obj is FunctionCall || obj is Literal) {
                        prefix = "$objectString."
                    } else {
                        prefix = "($objectString)."
                    }
                }
            }
        }
        return prefix + nameAndArgs
    }

    override fun visit(node: FunctionDeclaration): String {
        val args = node.children.
                stream().
                map { it.accept(this) }.
                collect(Collectors.joining(", "))
        val functionInfo = node.functionInfo
        if (functionInfo.owner == null) throw IllegalArgumentException("Function declaration owner can't be null")
        return if (functionInfo.owner.isInterface()) "" else "abstract" + attributes(functionInfo) +
                "fun " + functionInfo.name + "(" + args + "): " + functionInfo.type.accept(this)
    }

    override fun visit(node: FunctionDeclarationBlock): String {
        val code = StringBuilder()
        for (i in node.children) {
            code.append(PrintingUtils.align(node.level)).
                    append(i.accept(this)).
                    append(addComplexityInfo(i)).
                    append("\n")
        }
        return code.toString()
    }

    override fun visit(node: FunctionDefinition): String {
        val args = node.children.stream().
                skip(2).
                map { it.accept(this) }.
                collect(Collectors.joining(", "))
        val body = node.getChild(0)
        val ret = node.getChild(1)
        val functionInfo = node.functionInfo
        return StringBuilder().
                append(attributes(functionInfo)).
                append("fun ").
                append(functionInfo.name).
                append("(").
                append(args).
                append("): ").
                append(functionInfo.type.accept(this)).
                append(" {\n").
                append(body.accept(this)).
                append(if (ret !is NothingNode) PrintingUtils.align(node.level + 1) + ret.accept(this) + "\n" else "").
                append(PrintingUtils.align(node.level)).
                append("}\n").toString()
    }

    override fun visit(node: FunctionDefinitionBlock): String {
        val code = StringBuilder()
        for (i in node.children) {
            code.append("\n").
                    append(PrintingUtils.align(node.level)).
                    append(i.accept(this)).
                    append(addComplexityInfo(i)).
                    append("\n")
        }
        return code.toString()
    }

    override fun visit(node: If): String {
        val thenBlock = PrintingUtils.align(node.level) + "{\n" +
                node.getChild(If.IfPart.THEN.ordinal).accept(this) +
                PrintingUtils.align(node.level) + "}"

        var elseBlock: String? = null
        if (node.getChild(If.IfPart.ELSE.ordinal) !is NothingNode) {
            elseBlock = "{\n" +
                    (node.getChild(If.IfPart.ELSE.ordinal).accept(this)) +
                    PrintingUtils.align(node.level) + "}"
        }

        return "if (" + node.getChild(If.IfPart.CONDITION.ordinal).accept(this) + ")\n" + thenBlock +
                (if (elseBlock != null) "\n" + PrintingUtils.align(node.level) + "else " + elseBlock else "")
    }

    override fun visit(node: Initialization): String {      //TODO: what should i do with val/var?
        val vi = node.variableInfo
        val init = node.getChild(0)
        return attributes(vi) +
                (if (vi.isConst()) "val " else "var ") +
                vi.name +
                ": " +
                vi.type.accept(this) +
                " = " +
                (if (vi.type.isBuiltIn() && vi.type != TypeList.BOOLEAN) "(" else "") +
                (init.accept(this)) +
                (if (vi.type.isBuiltIn() && vi.type != TypeList.BOOLEAN) ").to${vi.type}()" else "")
    }

    override fun visit(node: Literal): String {
        val resType = node.getResultType()
        val value = node.value
        return when (resType) {
            TypeList.LONG -> value.toString() + "L"
            TypeList.FLOAT -> value.toString() + "F"
            TypeList.DOUBLE -> value.toString()
            TypeList.CHAR -> if (value as Char == '\\') "\'\\\\\'" else "\'$value\'"
            TypeList.SHORT -> "($value).toShort()"
            TypeList.BYTE ->"($value).toByte()"
            TypeList.STRING -> "\"${value.toString().replace("\n", "\\n")}\""
            else -> value.toString()
        }
    }

    override fun visit(node: LocalVariable) = node.variableInfo.name

    override fun visit(node: LoopingCondition): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: MainFunction): String {
        val body = node.getChild(0)
        return StringBuilder().
                append("fun main(args: Array<String>) {\n").
                append(body.accept(this)).
                append("} ").
                append(addComplexityInfo(body)).
                append("\n").toString()
    }

    override fun visit(node: NothingNode) = ""

    override fun visit(node: Return) = "return ${node.retExpr.accept(this)}"

    override fun visit(node: Statement) = node.getChild(0).accept(this)

    override fun visit(node: Type) = node.typename

    override fun visit(node: UnaryOperator): String {
        val expr = node.getChild(0)
        return if (node.isPrefix()) {
            operatorToKotlinCode(node.opKind) +
                    (if (expr is Operator) " " else "") +
                    expressionToKotlinCode(node, expr, Operator.Order.LEFT)
        } else {
            expressionToKotlinCode(node, expr, Operator.Order.RIGHT) +
                    (if (expr is Operator) " " else "") +
                    operatorToKotlinCode(node.opKind)
        }
    }

    override fun visit(node: VariableDeclaration): String {
        val vi = node.variableInfo
        return attributes(vi) + (if (!vi.isLocal()) "lateinit " else "") + "var " + vi.name + ": " + vi.type.accept(this)   //is correct?
    }

    override fun visit(node: VariableDeclarationBlock): String {
        val code = StringBuilder()
        for (i in node.children) {
            code.append(PrintingUtils.align(node.level)).
                    append(i.accept(this)).
                    append(addComplexityInfo(i)).
                    append("\n")
        }
        return code.toString()
    }

    override fun visit(node: When): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visit(node: While): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}