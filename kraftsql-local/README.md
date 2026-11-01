# KraftSqlLocalEngine

Provides an Engine implementation that works locally in the JVM.

The [KraftSqlLocalEngine](src/main/kotlin/rocks/frieler/kraftsql/local/engine/KraftSqlLocalEngine.kt) is primarily meant as a dummy Engine for (unit) testing business logic, when the real Engine is not available or too expensive to use.
It is not optimized and not intended to handle "Big Data".

## Integration testing against the reference test suite

The `KraftSqlLocalEngine` is already used to run the [reference tests](../kraftsql/src/referenceTest) of **Kraft**SQL.
Also, it does not add any features beyond **Kraft**SQL's specification.
Hence, it needs no integration test here to check it against the reference test suite, as other connectors would.
