package rocks.frieler.kraftsql.h2.engine

import java.sql.Connection
import java.sql.DriverManager
import java.util.ServiceLoader

/**
 * Interface to provide connection settings for the [H2Engine].
 *
 * Register an implementation for the [ServiceLoader] to define settings. If no implementation is registered,
 * [H2ConnectionSettingsFromEnv] will be used.
 */
interface H2EngineConnectionSettings {
    /**
     * The database to create and use as default.
     */
    val database: String

    /**
     * A schema to create automatically on startup and use as default.
     */
    val autoCreateDefaultSchema: String?
}

internal fun makeConnectionFromSettings(): Connection {
    val settings = ServiceLoader.load(H2EngineConnectionSettings::class.java).iterator().run {
        if (hasNext())
            next().also {
                check(!hasNext()) { "Multiple H2EngineConnectionSettings implementations registered; there must be one at most." }
            }
        else H2ConnectionSettingsFromEnv
    }

    val connectionString = StringBuilder("jdbc:h2:mem")
    connectionString.append(":${settings.database}")
    connectionString.append(";DATABASE_TO_UPPER=FALSE")
    if (settings.autoCreateDefaultSchema != null) {
        connectionString.append(";INIT=CREATE SCHEMA IF NOT EXISTS ${settings.autoCreateDefaultSchema}\\; SET SCHEMA ${settings.autoCreateDefaultSchema}")
    }

    return DriverManager.getConnection(connectionString.toString())
}
