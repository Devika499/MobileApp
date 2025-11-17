package com.community.swaphub.data.repository

import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.model.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {

    fun getNotifications(): Flow<Result<List<Notification>>> = flow {
        try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception(response.message() ?: "Failed to fetch notifications")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getUnreadCount(): Flow<Result<Long>> = flow {
        try {
            val response = apiService.getUnreadNotificationCount()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Failed to fetch unread count")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun markAsRead(id: String): Result<Notification> {
        return try {
            val response = apiService.markNotificationAsRead(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to mark as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
