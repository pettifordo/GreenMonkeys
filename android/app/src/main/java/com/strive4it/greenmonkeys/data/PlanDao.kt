package com.strive4it.greenmonkeys.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Transaction
    @Query("SELECT * FROM plans ORDER BY sessionStart ASC")
    fun observePlans(): Flow<List<PlanWithDetails>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    fun observePlan(planId: String): Flow<PlanWithDetails?>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getPlan(planId: String): PlanWithDetails?

    @Transaction
    @Query("SELECT * FROM plans ORDER BY sessionStart ASC")
    suspend fun getAllPlans(): List<PlanWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: SessionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitments(commitments: List<CommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: CommitmentEntity)

    @Insert
    suspend fun insertVideo(video: SessionVideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)

    /**
     * Insert-only by design: a verdict is never overwritten (hard rule 3).
     * ABORT throws if one already exists for the plan (unique planId index).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVerdict(verdict: VerdictEntity)

    /** Cascades to commitments/videos/verdict. Only reachable behind the two-stage confirmation. */
    @Delete
    suspend fun deletePlan(plan: SessionPlanEntity)

    @Query("DELETE FROM commitments WHERE planId = :planId")
    suspend fun deleteCommitmentsForPlan(planId: String)
}
