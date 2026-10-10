package com.hybridmode.data.di

import android.content.Context
import androidx.room.Room
import com.hybridmode.common.time.Clock
import com.hybridmode.common.time.SystemClock
import com.hybridmode.data.BuildConfig
import com.hybridmode.data.connectivity.AndroidConnectivityRepository
import com.hybridmode.data.local.db.AppDatabase
import com.hybridmode.data.local.db.AppMigrations
import com.hybridmode.data.local.db.dao.UploadDao
import com.hybridmode.data.local.db.dao.UserDao
import com.hybridmode.data.remote.api.AuthApi
import com.hybridmode.data.remote.api.UploadApi
import com.hybridmode.data.remote.mock.MockAuthInterceptor
import com.hybridmode.data.remote.network.KotlinxSerializationConverterFactory
import com.hybridmode.data.repository.AuthRepositoryImpl
import com.hybridmode.data.repository.ProfileRepositoryImpl
import com.hybridmode.data.repository.UploadRepositoryImpl
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.connectivity.ConnectivityRepository
import com.hybridmode.domain.profile.ProfileRepository
import com.hybridmode.domain.reporting.FieldReportRepository
import com.hybridmode.domain.shared.DomainEventPublisher
import com.hybridmode.domain.shared.LoggingDomainEventPublisher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindFieldReportRepository(impl: UploadRepositoryImpl): FieldReportRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(
        impl: AndroidConnectivityRepository,
    ): ConnectivityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock

    @Provides
    @Singleton
    fun provideDomainEventPublisher(): DomainEventPublisher = LoggingDomainEventPublisher()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "hybrid_mode.db")
            .addMigrations(*AppMigrations.ALL)
            // v1 had no exported schema; wipe only that pre-release starting version.
            .fallbackToDestructiveMigrationFrom(1)
            .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideUploadDao(database: AppDatabase): UploadDao = database.uploadDao()

    @Provides
    @Singleton
    fun provideOkHttp(
        mockAuthInterceptor: MockAuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .apply {
                if (BuildConfig.USE_MOCK_BACKEND) {
                    addInterceptor(mockAuthInterceptor)
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(KotlinxSerializationConverterFactory.create(json))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUploadApi(retrofit: Retrofit): UploadApi = retrofit.create(UploadApi::class.java)
}
