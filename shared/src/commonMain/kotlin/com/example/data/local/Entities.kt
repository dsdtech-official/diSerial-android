package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "connection_profiles")
data class ConnectionProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetName: String,
    val baudRate: Int,
    val dataBits: Int,
    val stopBits: String,
    val parityName: String,
    val dtrEnabled: Boolean,
    val rtsEnabled: Boolean,
    val sortOrder: Int = 0,
)

@Entity(tableName = "macro_commands")
data class MacroCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val groupName: String,
    val payload: String,
    val txFormatName: String,
    val lineEndingName: String,
    val checksumModeName: String,
    val checksumEndianName: String,
    val checksumSkipBytes: Int,
    val colorTagName: String,
    val sortOrder: Int = 0,
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payload: String,
    val txFormatName: String,
    val lineEndingName: String,
    val checksumModeName: String,
    val checksumEndianName: String,
    val useCount: Int,
    val lastUsedAtMs: Long,
    val sentHex: String? = null,
)

@Entity(
    tableName = "session_logs",
    indices = [Index(value = ["startedAtMs", "id"])],
)
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val deviceSummary: String,
    val configSummary: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val frameCount: Int,
    val totalBytes: Long,
)

@Entity(
    tableName = "session_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampLabel: String,
    val directionName: String,
    val rawBytes: ByteArray,
    val asciiText: String,
    val seqIndex: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionLogEntryEntity) return false
        return id == other.id && sessionId == other.sessionId && seqIndex == other.seqIndex
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + seqIndex
        return result
    }
}

data class SessionLogWithEntries(
    @Embedded val session: SessionLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val entries: List<SessionLogEntryEntity>,
)

@Entity(tableName = "plot_configurations", indices = [Index(value = ["name"], unique = true)])
data class PlotConfigurationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rulesJson: String,
)
