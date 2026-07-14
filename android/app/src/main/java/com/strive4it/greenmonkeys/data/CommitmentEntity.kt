package com.strive4it.greenmonkeys.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strive4it.greenmonkeys.logic.CommitmentKind
import com.strive4it.greenmonkeys.logic.commitmentDisplayText
import com.strive4it.greenmonkeys.logic.patternKey
import com.strive4it.greenmonkeys.logic.patternLabel
import java.util.UUID

/** Mirrors iOS `Commitment`. */
@Entity(
    tableName = "commitments",
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
data class CommitmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val kindRaw: String = CommitmentKind.CUSTOM.rawValue,
    /** The specifics: "6" for maxDrinks, "23:00" for leaveBy, free text for custom. */
    val detail: String = "",
    /** Set during the morning-after verdict. null until judged. */
    val wasBroken: Boolean? = null,
) {
    val kind: CommitmentKind get() = CommitmentKind.fromRaw(kindRaw)
    val patternKey: String get() = patternKey(kind, detail)
    val patternLabel: String get() = patternLabel(kind, detail)
    val displayText: String get() = commitmentDisplayText(kind, detail)
}
