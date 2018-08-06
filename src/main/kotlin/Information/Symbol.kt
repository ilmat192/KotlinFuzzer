package Information

import IR.Types.ClassType
import IR.Types.Type

open class Symbol(val name: String, val owner: ClassType?, val type: Type, val flags: Int) {
    //protected constructor(name: String) : this(name, null, null, NONE)
    protected constructor(other: Symbol): this(other.name, other.owner, other.type, other.flags)

    companion object {
        val NONE = 0x00
        val PRIVATE = 0x01
        val DEFAULT = 0x02
        val PROTECTED = 0x04
        val PUBLIC = 0x08
        val ACCESS_ATTRS_MASK = PRIVATE + PROTECTED + DEFAULT + PUBLIC
        val STATIC = 0x10
        val OPEN = 0x20
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || other !is Symbol) return false

        return try{
            val s = other as Symbol
            owner == s.owner && name == s.name
        } catch (e: Exception) {
            false
        }
    }

    override fun hashCode() = name.hashCode()

    open fun isStatic() = (flags and STATIC) > 0

    fun isOpen() = (flags and OPEN) > 0

    fun isPrivate() = flags and PRIVATE > 0

    protected open fun copy() = Symbol(this)

    open fun deepCopy() = Symbol(this)
}