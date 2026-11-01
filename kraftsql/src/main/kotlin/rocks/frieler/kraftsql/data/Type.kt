package rocks.frieler.kraftsql.data

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A data type in KraftSQL's abstraction of data.
 *
 * KraftSQL allows a set Kotlin types for which the individual connectors implement the mapping to native types in their
 * SQL engine.
 */
interface Type {
    /**
     * Indicates, whether this [Type] can hold `null` values.
     */
    val nullable: Boolean get() = toString().endsWith("?")

    /**
     * Checks, whether the given value is of this [Type].
     *
     * @param value the value to check
     * @return whether the value is of this [Type] or not
     */
    fun accepts(value: Any?): Boolean

    override fun toString(): String

    companion object {
        // TODO: Implement allowed types.

        /**
         * A non-nullable 32bit Integer.
         */
        val INTEGER = object : Type {
            override fun accepts(value: Any?) = value is Int
            override fun toString() = "Int"
        }

        /**
         * A nullable 32bit Integer.
         */
        val NULLABLE_INTEGER = object : Type {
            override fun accepts(value: Any?) = value is Int?
            override fun toString() = "Int?"
        }

        /**
         * A non-nullable character [String] of arbitrary length.
         *
         * A constant String containing characters which the SQL engine does not support, e.g. the NUL character, makes
         * the [Engine][rocks.frieler.kraftsql.engine.Engine] fail with a [RuntimeException].
         *
         * Connectors map it to the native character string type of their SQL engine, e.g. `VARCHAR`.
         */
        val STRING = object : Type {
            override fun accepts(value: Any?) = value is String
            override fun toString() = "String"
        }

        /**
         * A nullable [String] of characters.
         *
         * Like [STRING], but additionally allows `null`, which is distinct from the empty string.
         */
        val NULLABLE_STRING = object : Type {
            override fun accepts(value: Any?) = value is String?
            override fun toString() = "String?"
        }

        /**
         * Returns the [Type] corresponding to the given [KType].
         *
         * @param kType a Kotlin type
         * @return the corresponding KraftSQL [Type]
         */
        fun get(kType: KType) = when (kType) {
            typeOf<Int>() -> INTEGER
            typeOf<Int?>() -> NULLABLE_INTEGER
            typeOf<String>() -> STRING
            typeOf<String?>() -> NULLABLE_STRING
            else -> throw IllegalStateException("Unsupported type $kType")
        }

        /**
         * Returns the [Type] corresponding to the given [KClass] and nullability modifier.
         *
         * @param kClass a Kotlin class
         * @param nullable whether `null` is allowed or not
         * @return the corresponding KraftSQL [Type]
         */
        fun get(kClass: KClass<*>, nullable: Boolean = false) = when (kClass) {
            Int::class -> if (nullable) NULLABLE_INTEGER else INTEGER
            String::class -> if (nullable) NULLABLE_STRING else STRING
            else -> throw IllegalStateException("Unsupported type $kClass")
        }

        /**
         * Obtains the nullable variant of this [Type].
         *
         * If this [Type] is already nullable, it is returned as is.
         *
         * @return the nullable variant of this [Type]
         */
        fun Type.makeNullable(): Type = when {
            this.nullable -> this
            this == INTEGER -> NULLABLE_INTEGER
            this == STRING -> NULLABLE_STRING
            else -> throw NotImplementedError("Nullable variant of $this is not supported.")
        }
    }
}
