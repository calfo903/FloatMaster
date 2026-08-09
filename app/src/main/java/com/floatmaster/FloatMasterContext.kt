package com.floatmaster

import android.content.Context

/** WHY: Application context is process-scoped and cannot leak an Activity; retained only for legacy composable helpers that lack a Context parameter. */
object FloatMasterContext {
    @Volatile private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun get(): Context = applicationContext ?: error("FloatMasterContext is not initialized")
}

val context: Context
    get() = FloatMasterContext.get()
