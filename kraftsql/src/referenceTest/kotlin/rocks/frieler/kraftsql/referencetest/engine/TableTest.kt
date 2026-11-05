package rocks.frieler.kraftsql.referencetest.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Column
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Table
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.engine.collect
import rocks.frieler.kraftsql.engine.create
import rocks.frieler.kraftsql.engine.drop
import rocks.frieler.kraftsql.engine.insertInto
import rocks.frieler.kraftsql.referencetest.ReferenceTestConfiguration

class TableTest {
    @Test
    fun `Engine can create and drop a Table`() {
        val table = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))

        table.create()
        table.drop()
    }

    @Test
    fun `Engine can insert data returns the number of inserted rows`() {
        val table = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))
            .also { it.create() }
        val things = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
            DataRow("id" to 2, "value" to "bar"),
        )

        try {
            val insertedRows = things.insertInto(table)

            insertedRows shouldBe things.items.count()

        } finally {
            table.drop()
        }
    }

    @Test
    fun `Engine can collect data from a Table`() {
        val table = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))
            .also { it.create() }
        val things = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
            DataRow("id" to 2, "value" to "bar"),
        )
            .also { it.insertInto(table) }

        try {
            val thingsFromTable = table.collect()

            thingsFromTable shouldBe things.items

        } finally {
            table.drop()
        }
    }

    @Test
    fun `Engine inserts NULL into nullable columns missing in the inserted Data`() {
        val actualTable = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))
            .also { it.create() }
        val targetTable = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
            ))
        val things = ConstantData(
            DataRow("id" to 1),
        )

        try {
            things.insertInto(targetTable)
            val thingsFromTable = actualTable.collect()

            thingsFromTable shouldBe listOf(
                DataRow("id" to 1, "value" to null),
            )

        } finally {
            actualTable.drop()
        }
    }

    @Test
    fun `Engine collects an empty Table as empty result`() {
        val table = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "empty_things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))
            .also { it.create() }

        try {
            val things: List<DataRow> = table.collect()

            things shouldBe beEmpty<DataRow>()

        } finally {
            table.drop()
        }
    }

    @Test
    fun `Engine fails to collect a Table that does not exist`() {
        val table = Table<DataRow>(
            ReferenceTestConfiguration.database,
            ReferenceTestConfiguration.namespace,
            "things",
            listOf(
                Column("id", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))

        shouldThrow<RuntimeException> {
            table.collect()
        }
    }
}
