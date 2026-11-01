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
}
