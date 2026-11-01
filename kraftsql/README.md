# KraftSQL model and API

This module provides **Kraft**SQL's model and API.

## Reference test suite

The module alo provides a [reference test suite](src/referenceTest) for connectors to test their Engine implementation against the spec of **Kraft**SQL.
It does not test every detail, but is a useful smoke-test.

For local development in this project, the reference tests are configured to run against **Kraft**SQL's `KraftSqlLocalEngine`.
This allows to run tests directly and individually without a JUnit suite.
