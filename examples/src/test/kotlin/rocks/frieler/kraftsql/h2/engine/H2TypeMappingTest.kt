package rocks.frieler.kraftsql.h2.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Type
import java.sql.JDBCType

class H2TypeMappingTest {
    @Test
    fun `Integer types are mapped to INTEGER`() {
        Type.INTEGER.native shouldBe JDBCType.INTEGER
        Type.NULLABLE_INTEGER.native shouldBe JDBCType.INTEGER
    }

    @Test
    fun `String types are mapped to VARCHAR`() {
        Type.STRING.native shouldBe JDBCType.VARCHAR
        Type.NULLABLE_STRING.native shouldBe JDBCType.VARCHAR
    }

    @Test
    fun `an unknown Type cannot be mapped`() {
        shouldThrow<IllegalStateException> {
            object : Type {
                override fun accepts(value: Any?): Boolean = false
                override fun toString(): String = "_unknown_"
            }.native
        }
    }
}
