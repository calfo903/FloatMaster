package com.floatmaster.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floatmaster.data.SessionRepository
import com.floatmaster.service.FloatingWindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * WHY: WorkManager periodic check restores windows after OEM kill — survives task killer.
 */
class SessionRestoreWorker @Inject constructor(
    @ApplicationContext private val ctx: Context,
    params: WorkerParameters,
    private val sessionRepo: SessionRepository,
    private val manager: FloatingWindowManager
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val saved = sessionRepo.restore()
        if (saved.isNotEmpty() && manager.allWindows().isEmpty()) {
            saved.take(8).forEach { manager.create(it.type, it.title, it.url, geometry = it.geometry) }
        }
        return Result.success()
    }
}
