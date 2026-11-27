package com.example.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DISERIAL_DATABASE_NAME = "serialscope.db"

@Database(
    entities = [
        ConnectionProfileEntity::class,
        MacroCommandEntity::class,
        CommandHistoryEntity::class,
        SessionLogEntity::class,
        SessionLogEntryEntity::class,
        PlotConfigurationEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun macroDao(): MacroDao
    abstract fun historyDao(): HistoryDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun plotConfigurationDao(): PlotConfigurationDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

object AppDatabaseFactory {
    private val historyFrameMigration = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE command_history ADD COLUMN sentHex TEXT")
        }
    }

    private val defaultDtrMigration = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            // User-saved presets have sortOrder 100, even when their name matches a built-in.
            connection.execSQL(
                """
                UPDATE connection_profiles SET dtrEnabled = 0
                WHERE dtrEnabled = 1 AND (
                    (sortOrder = 1 AND presetName = '标准调试 115200') OR
                    (sortOrder = 3 AND presetName = '工业 PLC 19200 8E1')
                )
                """.trimIndent(),
            )
        }
    }

    private val logPagingMigration = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_session_logs_startedAtMs_id ON session_logs (startedAtMs, id)")
        }
    }

    private val plotConfigurationsMigration = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS plot_configurations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    rulesJson TEXT NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plot_configurations_name ON plot_configurations (name)")
        }
    }

    fun build(
        builder: RoomDatabase.Builder<AppDatabase>,
        useBundledDriver: Boolean = true,
    ): AppDatabase {
        if (useBundledDriver) {
            builder.setDriver(BundledSQLiteDriver())
        }
        return builder
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(historyFrameMigration, defaultDtrMigration, logPagingMigration, plotConfigurationsMigration)
            .addCallback(SeedOnCreateCallback)
            .build()
    }

    internal object SeedOnCreateCallback : RoomDatabase.Callback() {
        override fun onCreate(connection: SQLiteConnection) {
            super.onCreate(connection)
            connection.execSQL(
                """
                INSERT INTO connection_profiles (presetName, baudRate, dataBits, stopBits, parityName, dtrEnabled, rtsEnabled, sortOrder) VALUES
                ('Standard Debug 115200', 115200, 8, '1', 'NONE', 0, 0, 1),
                ('Modbus RTU 9600', 9600, 8, '1', 'NONE', 0, 0, 2),
                ('Industrial PLC 19200 8E1', 19200, 8, '1', 'EVEN', 0, 1, 3),
                ('High-Speed Log 921600', 921600, 8, '1', 'NONE', 0, 0, 4)
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO macro_commands (title, groupName, payload, txFormatName, lineEndingName, checksumModeName, checksumEndianName, checksumSkipBytes, colorTagName, sortOrder) VALUES
                ('Read 4 Holding Registers', 'Modbus RTU Examples', '01 03 00 00 00 04 44 09', 'HEX', 'NONE', 'NONE', 'LITTLE_ENDIAN', 0, 'COBALT', 1),
                ('Read Firmware Version', 'AT Examples', '41 54 2B 43 47 4D 52 0D 0A', 'HEX', 'NONE', 'NONE', 'LITTLE_ENDIAN', 0, 'VIOLET', 2)
                """.trimIndent(),
            )
        }
    }
}
