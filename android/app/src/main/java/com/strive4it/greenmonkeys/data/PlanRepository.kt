package com.strive4it.greenmonkeys.data

import com.strive4it.greenmonkeys.logic.CommitmentRecord
import kotlinx.coroutines.flow.Flow

/** Thin wrapper over the DAO so ViewModels never touch Room types directly. */
class PlanRepository(private val dao: PlanDao) {

    fun observePlans(): Flow<List<PlanWithDetails>> = dao.observePlans()

    fun observePlan(planId: String): Flow<PlanWithDetails?> = dao.observePlan(planId)

    suspend fun getPlan(planId: String): PlanWithDetails? = dao.getPlan(planId)

    suspend fun getAllPlans(): List<PlanWithDetails> = dao.getAllPlans()

    suspend fun savePlan(plan: SessionPlanEntity, commitments: List<CommitmentEntity>) {
        dao.upsertPlan(plan)
        dao.deleteCommitmentsForPlan(plan.id)
        dao.insertCommitments(commitments)
    }

    suspend fun addVideo(video: SessionVideoEntity) = dao.insertVideo(video)

    suspend fun removeVideo(videoId: String) = dao.deleteVideo(videoId)

    /** Judged commitments get their wasBroken flag set during the morning-after flow. */
    suspend fun judgeCommitment(commitment: CommitmentEntity, wasBroken: Boolean) =
        dao.updateCommitment(commitment.copy(wasBroken = wasBroken))

    suspend fun recordVerdict(verdict: VerdictEntity) = dao.insertVerdict(verdict)

    suspend fun deletePlan(plan: SessionPlanEntity) = dao.deletePlan(plan)

    /**
     * Every judged commitment across history, as pure records for
     * PatternService and the "you've promised this before" callbacks.
     */
    suspend fun judgedRecords(): List<CommitmentRecord> =
        getAllPlans()
            .filter { it.verdict != null }
            .flatMap { it.commitments }
            .mapNotNull { commitment ->
                commitment.wasBroken?.let {
                    CommitmentRecord(
                        key = commitment.patternKey,
                        label = commitment.patternLabel,
                        wasBroken = it,
                    )
                }
            }
}
