package rocks.frieler.kraftsql.referencetest

/**
 * Configuration of the reference test suite, read from the environment.
 *
 * Test invocations can choose a database or namespace of their own, e.g. with a suffix, so they do not interfere with
 * each other, and the tests can use fixed table names.
 */
object ReferenceTestConfiguration {
    const val DATABASE_ENV_VAR = "KRAFTSQL_REFERENCE_TEST_DATABASE"

    /**
     * The [database][rocks.frieler.kraftsql.data.Table.database] for all
     * [Table][rocks.frieler.kraftsql.data.Table]s of the reference tests.
     *
     * Configured by the environment variable [DATABASE_ENV_VAR], defaulting to `null`, which means to use the default
     * database.
     */
    val database: String?
        get() = System.getenv(DATABASE_ENV_VAR)

    const val NAMESPACE_ENV_VAR = "KRAFTSQL_REFERENCE_TEST_NAMESPACE"
    const val DEFAULT_NAMESPACE = "test"

    /**
     * The [namespace][rocks.frieler.kraftsql.data.Table.namespace] for all
     * [Table][rocks.frieler.kraftsql.data.Table]s of the reference tests.
     *
     * Configured by the environment variable [NAMESPACE_ENV_VAR], defaulting to [DEFAULT_NAMESPACE].
     *
     * The namespace is not created by the reference tests. For [Engine][rocks.frieler.kraftsql.engine.Engine]s that
     * need it to exist, the connector's test setup must provide it and clean it up.
     */
    val namespace: String
        get() = System.getenv(NAMESPACE_ENV_VAR) ?: "test"
}
