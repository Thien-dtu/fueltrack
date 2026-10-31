package com.example.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the process-wide coroutine scope used for work that must outlive a single
 * screen, such as firing maintenance notifications after a data write.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    /**
     * One scope for the whole process.
     *
     * Individual call sites used to create their own `CoroutineScope(Dispatchers.IO)`
     * per invocation, which leaks a job for every odometer update and swallows any
     * exception raised inside it. [SupervisorJob] keeps one failure from cancelling
     * unrelated work.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
