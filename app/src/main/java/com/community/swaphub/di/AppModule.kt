package com.community.swaphub.di

import android.content.Context
import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.api.AuthInterceptor
import com.community.swaphub.data.local.AppDatabase
import com.community.swaphub.data.repository.*
import com.community.swaphub.util.PreferencesManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.flow.first


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Base URL - Update this with your Spring Boot backend URL
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // For Android Emulator
    // For real device, use your computer's IP: "http://192.168.x.x:8080/"
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(preferencesManager: PreferencesManager): AuthInterceptor {
        return AuthInterceptor {
            kotlinx.coroutines.runBlocking {
                return@runBlocking preferencesManager.authToken.first()
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        database: AppDatabase,
        preferencesManager: PreferencesManager
    ): AuthRepository {
        return AuthRepository(apiService, database.userDao(), preferencesManager)
    }

    @Provides
    @Singleton
    fun provideItemRepository(
        apiService: ApiService,
        database: AppDatabase,
        @ApplicationContext context: Context
    ): ItemRepository {
        return ItemRepository(
            apiService = apiService,
            itemDao = database.itemDao(),
            context = context
        )
    }



    @Provides
    @Singleton
    fun provideSwapRequestRepository(apiService: ApiService): SwapRequestRepository {
        return SwapRequestRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        apiService: ApiService,
        database: AppDatabase
    ): ChatRepository {
        return ChatRepository(apiService, database.chatDao())
    }
    
    @Provides
    @Singleton
    fun providePointsRepository(apiService: ApiService): PointsRepository {
        return PointsRepository(apiService)
    }
}

