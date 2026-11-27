package rocks.frieler.kraftsql.data

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Type.Companion.makeNullable
import kotlin.reflect.typeOf

class TypeTest {
    @Test
    fun `KraftSQL supports non-nullable Int`() {
        Type.INTEGER.nullable shouldBe false
        Type.get(typeOf<Int>()) shouldBe Type.INTEGER
        Type.get(Int::class, false) shouldBe Type.INTEGER
        Type.INTEGER.makeNullable() shouldBe Type.NULLABLE_INTEGER
    }

    @Test
    fun `non-nullable Int accepts Ints, but not null`() {
        Type.INTEGER.accepts(1) shouldBe true
        Type.INTEGER.accepts(null) shouldBe false
        Type.INTEGER.accepts(Unit) shouldBe false
    }

    @Test
    fun `KraftSQL supports nullable Int`() {
        Type.NULLABLE_INTEGER.nullable shouldBe true
        Type.get(typeOf<Int?>()) shouldBe Type.NULLABLE_INTEGER
        Type.get(Int::class, true) shouldBe Type.NULLABLE_INTEGER
        Type.NULLABLE_INTEGER.makeNullable() shouldBe Type.NULLABLE_INTEGER
    }

    @Test
    fun `nullable Int accepts Ints and null`() {
        Type.NULLABLE_INTEGER.accepts(1) shouldBe true
        Type.NULLABLE_INTEGER.accepts(null) shouldBe true
        Type.NULLABLE_INTEGER.accepts(Unit) shouldBe false
    }

    @Test
    fun `KraftSQL supports non-nullable String`() {
        Type.STRING.nullable shouldBe false
        Type.get(typeOf<String>()) shouldBe Type.STRING
        Type.get(String::class, false) shouldBe Type.STRING
        Type.STRING.makeNullable() shouldBe Type.NULLABLE_STRING
    }

    @Test
    fun `non-nullable String accepts Strings, but not null`() {
        Type.STRING.accepts("KraftSQL") shouldBe true
        Type.STRING.accepts(null) shouldBe false
        Type.STRING.accepts(Unit) shouldBe false
    }

    @Test
    fun `KraftSQL supports nullable String`() {
        Type.NULLABLE_STRING.nullable shouldBe true
        Type.get(typeOf<String?>()) shouldBe Type.NULLABLE_STRING
        Type.get(String::class, true) shouldBe Type.NULLABLE_STRING
        Type.NULLABLE_STRING.makeNullable() shouldBe Type.NULLABLE_STRING
    }

    @Test
    fun `nullable String accepts Strings and null`() {
        Type.NULLABLE_STRING.accepts("KraftSQL") shouldBe true
        Type.NULLABLE_STRING.accepts(null) shouldBe true
        Type.NULLABLE_STRING.accepts(Unit) shouldBe false
    }

    @Test
    fun `KraftSQL supports non-nullable Boolean`() {
        Type.BOOLEAN.nullable shouldBe false
        Type.get(typeOf<Boolean>()) shouldBe Type.BOOLEAN
        Type.get(Boolean::class, false) shouldBe Type.BOOLEAN
        Type.BOOLEAN.makeNullable() shouldBe Type.NULLABLE_BOOLEAN
    }

    @Test
    fun `non-nullable Boolean accepts Booleans, but not null`() {
        Type.BOOLEAN.accepts(true) shouldBe true
        Type.BOOLEAN.accepts(false) shouldBe true
        Type.BOOLEAN.accepts(null) shouldBe false
        Type.BOOLEAN.accepts(Unit) shouldBe false
    }

    @Test
    fun `KraftSQL supports nullable Boolean`() {
        Type.NULLABLE_BOOLEAN.nullable shouldBe true
        Type.get(typeOf<Boolean?>()) shouldBe Type.NULLABLE_BOOLEAN
        Type.get(Boolean::class, true) shouldBe Type.NULLABLE_BOOLEAN
        Type.NULLABLE_BOOLEAN.makeNullable() shouldBe Type.NULLABLE_BOOLEAN
    }

    @Test
    fun `nullable Boolean accepts Booleans and null`() {
        Type.NULLABLE_BOOLEAN.accepts(true) shouldBe true
        Type.NULLABLE_BOOLEAN.accepts(false) shouldBe true
        Type.NULLABLE_BOOLEAN.accepts(null) shouldBe true
        Type.NULLABLE_BOOLEAN.accepts(Unit) shouldBe false
    }

    @Test
    fun `Boolean types do not accept other representations of truth values like numbers or Strings`() {
        for (type in listOf(Type.BOOLEAN, Type.NULLABLE_BOOLEAN)) {
            for (value in listOf(1, 0, "true", "false", "TRUE", "1", 'Y')) {
                type.accepts(value) shouldBe false
            }
        }
    }
}
