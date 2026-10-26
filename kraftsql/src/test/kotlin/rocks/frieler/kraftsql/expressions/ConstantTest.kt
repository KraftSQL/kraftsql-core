package rocks.frieler.kraftsql.expressions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Type

class ConstantTest {
    @Test
    fun `Constant of a non-null value has the non-nullable Type`() {
        val constant = Constant<Int>(123)

        constant.value shouldBe 123
        constant.type shouldBe Type.INTEGER
    }

    @Test
    fun `Constant of a nullable Kotlin type has the nullable type, even for a non-null value`() {
        Constant<Int?>(0).type shouldBe Type.NULLABLE_INTEGER
    }

    @Test
    fun `Constant can be NULL with an explicitly nullable type`() {
        val nullAsInt = Constant<Int?>(null)

        nullAsInt.type shouldBe Type.NULLABLE_INTEGER
        nullAsInt.value shouldBe null
    }

    @Test
    fun `Constant rejects null without explicit type argument`() {
        shouldThrow<IllegalArgumentException> {
            Constant(null)
        }
    }

    @Test
    fun `Constant rejects a value of an unsupported Kotlin type`() {
        shouldThrow<IllegalStateException> {
            Constant<Unit>(Unit)
        }
    }

    @Test
    fun `Constant rejects a value that does not comply with its type`() {
        shouldThrow<IllegalArgumentException> {
            Constant(123, Type.STRING)
        }
    }

    @Test
    fun `Constant equals another Constant with an equal value of the same type`() {
        Constant<Int>(123) shouldBe Constant<Int>(123)
    }

    @Test
    fun `Constant does not equal a Constant with a different value`() {
        Constant<Int>(1) shouldNotBe Constant<Int>(2)
    }

    @Test
    fun `Constant does not equal a Constant with an equal value of a different type`() {
        Constant<Int>(1) shouldNotBe Constant<Int?>(1)
    }

    @Test
    fun `Constant does not equal something that is not a Constant`() {
        Constant<Int>(1) shouldNotBe Any()
    }

    @Test
    fun `Equal Constants have the same hashCode`() {
        Constant<Int>(123).hashCode() shouldBe Constant<Int>(123).hashCode()
    }

    @Test
    fun `Constant's toString() returns the String value of its value`() {
        Constant<Int>(123).toString() shouldBe "123"
        Constant<String>("KraftSQL").toString() shouldBe "KraftSQL"
        Constant<Int?>(null).toString() shouldBe "null"
    }
}
