package rocks.frieler.kraftsql.h2.engine

/**
 * [H2EngineConnectionSettings] derived from environment variables:
 *
 * - [database]: from 'KRAFTSQL_H2_DATABASE', or [DEFAULT_DATABASE_NAME] if not set
 * - [autoCreateDefaultSchema]: from 'KRAFTSQL_H2_AUTO_CREATE_DEFAULT_SCHEMA', or `null` if not set
 */
object H2ConnectionSettingsFromEnv : H2EngineConnectionSettings {
    const val DEFAULT_DATABASE_NAME = "kraftsql"

    override val database: String
        get() = System.getenv("KRAFTSQL_H2_DATABASE") ?: DEFAULT_DATABASE_NAME
    override val autoCreateDefaultSchema: String?
        get() = System.getenv("KRAFTSQL_H2_AUTO_CREATE_DEFAULT_SCHEMA")
}
