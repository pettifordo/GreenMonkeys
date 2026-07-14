package com.strive4it.greenmonkeys.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strive4it.greenmonkeys.logic.VideoKind
import java.time.Instant
import java.util.UUID

/** Mirrors iOS `SessionVideo`. Videos never leave the device (hard rule 2). */
@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = SessionPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("planId")],
)
data class SessionVideoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val kind: String = VideoKind.PLAN.rawValue,
    /** File name inside the app-private videos directory. */
    val fileName: String = "",
    val recordedAt: Instant = Instant.now(),
)
