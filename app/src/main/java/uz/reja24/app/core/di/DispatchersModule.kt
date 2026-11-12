package uz.reja24.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Og'ir ishlar (ma'lumot bazasi, fayl) uchun oqim. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Hisob-kitoblar (ball hisoblash va h.k.) uchun oqim. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Coroutine oqimlarini Hilt orqali beramiz.
 * Testlarda ularni oson almashtirish mumkin bo'ladi.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Ekran yopilsa ham tugashi kerak bo'lgan ishlar uchun (budilnik, baholash). */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}

/** Butun ilova yashash davridagi coroutine doirasi. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
