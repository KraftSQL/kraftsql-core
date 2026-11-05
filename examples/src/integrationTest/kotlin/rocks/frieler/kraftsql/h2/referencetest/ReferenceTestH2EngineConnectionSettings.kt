package rocks.frieler.kraftsql.h2.referencetest

import rocks.frieler.kraftsql.h2.engine.H2EngineConnectionSettings
import rocks.frieler.kraftsql.referencetest.ReferenceTestConfiguration

class ReferenceTestH2EngineConnectionSettings : H2EngineConnectionSettings {
    override val database = ReferenceTestConfiguration.database ?: "kraftsql"
    override val autoCreateDefaultSchema = ReferenceTestConfiguration.namespace
}
