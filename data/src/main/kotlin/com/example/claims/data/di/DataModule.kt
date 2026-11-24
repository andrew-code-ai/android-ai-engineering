package com.example.claims.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.claims.data.ClaimRepositoryImpl
import com.example.claims.data.PreferencesRepositoryImpl
import com.example.claims.data.local.ClaimDao
import com.example.claims.data.local.ClaimDatabase
import com.example.claims.data.remote.ClaimsRemoteDataSource
import com.example.claims.data.remote.FakeClaimsRemoteDataSource
import com.example.claims.domain.ClaimRepository
import com.example.claims.domain.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds abstract fun claimRepository(impl: ClaimRepositoryImpl): ClaimRepository
    @Binds abstract fun preferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds abstract fun remote(impl: FakeClaimsRemoteDataSource): ClaimsRemoteDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object DataProviders {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): ClaimDatabase =
        Room.databaseBuilder(context, ClaimDatabase::class.java, "claims.db").build()

    @Provides
    fun claimDao(db: ClaimDatabase): ClaimDao = db.claimDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_prefs") }

    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()
}
