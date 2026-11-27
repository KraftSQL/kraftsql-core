package rocks.frieler.kraftsql.h2.engine

import rocks.frieler.kraftsql.data.Type
import java.sql.JDBCType

/**
 * The native H2 [JDBCType] this KraftSQL [Type] gets mapped to.
 *
 * - [Int][rocks.frieler.kraftsql.data.Type.INTEGER] to `INTEGER`
 * - [String][rocks.frieler.kraftsql.data.Type.STRING] to `VARCHAR` (without a length limit)
 * - [Boolean][rocks.frieler.kraftsql.data.Type.BOOLEAN] to `BOOLEAN`
 */
val Type.native get() = when (this) {
    Type.INTEGER, Type.NULLABLE_INTEGER -> JDBCType.INTEGER
    Type.STRING, Type.NULLABLE_STRING -> JDBCType.VARCHAR
    Type.BOOLEAN, Type.NULLABLE_BOOLEAN -> JDBCType.BOOLEAN
    else -> throw IllegalStateException("Unsupported type $this")
}
