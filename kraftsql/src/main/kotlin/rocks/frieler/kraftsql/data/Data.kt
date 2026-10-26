package rocks.frieler.kraftsql.data

/**
 * Base interface for all representations of structured, tabular data.
 *
 * @param T the Kotlin type of the [Data]'s rows
 */
interface Data<T : Any> {
    val schema: List<Column>
}
