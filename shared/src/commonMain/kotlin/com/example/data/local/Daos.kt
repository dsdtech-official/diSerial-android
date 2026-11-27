package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM connection_profiles ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ConnectionProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ConnectionProfileEntity): Long

    @Query("DELETE FROM connection_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macro_commands ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<MacroCommandEntity>>

    @Query("SELECT * FROM macro_commands ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<MacroCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(macro: MacroCommandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(macros: List<MacroCommandEntity>)

    @Query("DELETE FROM macro_commands WHERE id = :id")
    suspend fun deleteById(id: Long)
}

private const val COMMAND_HISTORY_CAPACITY = 100

@Dao
interface HistoryDao {
    @Query("SELECT * FROM command_history ORDER BY lastUsedAtMs DESC, id DESC LIMIT $COMMAND_HISTORY_CAPACITY")
    fun observeRecent(): Flow<List<CommandHistoryEntity>>

    @Query(
        """
        SELECT * FROM command_history
        WHERE payload = :payload
          AND txFormatName = :txFormatName
          AND lineEndingName = :lineEndingName
          AND checksumModeName = :checksumModeName
          AND checksumEndianName = :checksumEndianName
          AND sentHex = :sentHex
        LIMIT 1
        """,
    )
    suspend fun findMatching(
        payload: String,
        txFormatName: String,
        lineEndingName: String,
        checksumModeName: String,
        checksumEndianName: String,
        sentHex: String,
    ): CommandHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CommandHistoryEntity): Long

    @Update
    suspend fun update(item: CommandHistoryEntity)

    @Query(
        """
        DELETE FROM command_history
        WHERE id NOT IN (
            SELECT id FROM command_history ORDER BY lastUsedAtMs DESC, id DESC LIMIT $COMMAND_HISTORY_CAPACITY
        )
        """,
    )
    suspend fun pruneOldEntries()

    @Transaction
    suspend fun record(item: CommandHistoryEntity) {
        val existing = findMatching(
            payload = item.payload,
            txFormatName = item.txFormatName,
            lineEndingName = item.lineEndingName,
            checksumModeName = item.checksumModeName,
            checksumEndianName = item.checksumEndianName,
            sentHex = requireNotNull(item.sentHex),
        )
        if (existing == null) {
            insert(item)
            pruneOldEntries()
        } else {
            update(existing.copy(useCount = existing.useCount + 1, lastUsedAtMs = item.lastUsedAtMs))
        }
    }

    @Query("DELETE FROM command_history")
    suspend fun clearAll()
}

@Dao
interface SessionLogDao {
    @Query(
        """
        SELECT * FROM session_logs
        WHERE startedAtMs < :untilExclusive AND endedAtMs >= :fromInclusive
          AND startedAtMs <= :beforeStart AND (startedAtMs < :beforeStart OR id < :beforeId)
        ORDER BY startedAtMs DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun loadPage(
        fromInclusive: Long,
        untilExclusive: Long,
        beforeStart: Long,
        beforeId: Long,
        limit: Int,
    ): List<SessionLogEntity>

    @Transaction
    @Query("SELECT * FROM session_logs WHERE id = :id")
    suspend fun loadWithEntries(id: Long): SessionLogWithEntries?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<SessionLogEntryEntity>)

    @Transaction
    suspend fun saveSessionWithEntries(
        session: SessionLogEntity,
        entries: List<SessionLogEntryEntity>,
    ): Long {
        val sessionId = insertSession(session)
        if (entries.isNotEmpty()) {
            insertEntries(entries.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    @Query("DELETE FROM session_logs WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}

@Dao
interface PlotConfigurationDao {
    @Query("SELECT * FROM plot_configurations ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<PlotConfigurationEntity>>

    @Insert
    suspend fun insert(item: PlotConfigurationEntity): Long

    @Update
    suspend fun update(item: PlotConfigurationEntity): Int

    @Query("DELETE FROM plot_configurations WHERE id = :id")
    suspend fun delete(id: Long)
}
