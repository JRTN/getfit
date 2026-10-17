package com.jrussell.fitness.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jrussell.fitness.core.data.preferences.DefaultUserPreferencesRepository
import com.jrussell.fitness.core.datastore.UserPreferencesDataSource
import com.jrussell.fitness.core.datastore.createUserPreferencesDataStore
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds UI preferences: one DataStore file per install, wrapped by the repository features use. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    abstract fun bindUserPreferencesRepository(
        implementation: DefaultUserPreferencesRepository,
    ): UserPreferencesRepository

    companion object {
        private const val FILE_NAME: String = "user_preferences"

        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return createUserPreferencesDataStore { context.preferencesDataStoreFile(FILE_NAME) }
        }

        @Provides
        @Singleton
        fun provideUserPreferencesDataSource(dataStore: DataStore<Preferences>): UserPreferencesDataSource {
            return UserPreferencesDataSource(dataStore)
        }
    }
}
