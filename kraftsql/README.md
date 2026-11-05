# KraftSQL model and API

This module provides **Kraft**SQL's model and API.

## Reference test suite

The module alo provides a [reference test suite](src/referenceTest) for connectors to test their Engine implementation against the spec of **Kraft**SQL.
It does not test every detail, but is a useful smoke-test.

The reference tests run integratively against a real engine.
They need a database and a namespace as defined by the [`ReferenceTestConfiguration`](src/referenceTest/kotlin/rocks/frieler/kraftsql/referencetest/ReferenceTestConfiguration.kt), which must exist; the tests do not create them:
- The database can be configured by the environment variable `KRAFTSQL_REFERENCE_TEST_DATABASE` and defaults to the engine's default database if not set.
- The namespace can be configured by the `KRAFTSQL_REFERENCE_TEST_NAMESPACE` and defaults to `test` if not set.

To isolate test runs, either the database or the namespace should be unique, e.g. by adding your personal identifier when executing them locally or a run ID in CI pipelines.

For local development in this project, the reference tests are configured to run against **Kraft**SQL's `KraftSqlLocalEngine`.
This allows to run tests directly and individually without a JUnit suite.
