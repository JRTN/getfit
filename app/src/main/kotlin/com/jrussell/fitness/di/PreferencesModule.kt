package com.jrussell.fitness.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jrussell.fitness.core.data.di.IoDispatcher
import com.jrussell.fitness.core.data.preferences.DefaultDateRangeRepository
import com.jrussell.fitness.core.data.preferences.DefaultUserPreferencesRepository
import com.jrussell.fitness.core.datastore.RangeSelectionDataSource
import com.jrussell.fitness.core.datastore.UserPreferencesDataSource
import com.jrussell.fitness.core.datastore.createUserPreferencesDataStore
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.domain.range.DateRangeRepository
import com.jrussell.fitness.core.model.MassUnit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Binds UI preferences and remembered date ranges: one DataStore file per install, wrapped by the
 * repositories features use.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    abstract fun bindUserPreferencesRepository(
        implementation: DefaultUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    abstract fun bindDateRangeRepository(implementation: DefaultDateRangeRepository): DateRangeRepository

    companion object {
        private const val FILE_NAME: String = "user_preferences"

        /** Countries that customarily weigh people in pounds; everyone else gets kilograms. */
        private val POUND_COUNTRIES: Set<String> = setOf("US", "LR", "MM")

        /** The weight unit a fresh install shows, from the device locale (the user can change it). */
        fun customaryMassUnit(locale: Locale): MassUnit {
            return if (locale.country in POUND_COUNTRIES) MassUnit.POUND else MassUnit.KILOGRAM
        }

        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> {
            return createUserPreferencesDataStore(scope = CoroutineScope(ioDispatcher + SupervisorJob())) {
                context.preferencesDataStoreFile(FILE_NAME)
            }
        }

        @Provides
        @Singleton
        fun provideUserPreferencesDataSource(dataStore: DataStore<Preferences>): UserPreferencesDataSource {
            return UserPreferencesDataSource(dataStore, defaultMassUnit = customaryMassUnit(Locale.getDefault()))
        }

        @Provides
        @Singleton
        fun provideRangeSelectionDataSource(dataStore: DataStore<Preferences>): RangeSelectionDataSource {
            return RangeSelectionDataSource(dataStore)
        }
    }
}
