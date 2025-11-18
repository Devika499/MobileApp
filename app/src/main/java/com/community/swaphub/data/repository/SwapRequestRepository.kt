package com.community.swaphub.data.repository

import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.model.CreateSwapRequest
import com.community.swaphub.data.model.SwapRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SwapRequestRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository   // ✅ added
) {
    suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserIdSync()
    }


    suspend fun createSwapRequest(requestedItemId: String, requesterId: String): Result<SwapRequest> {
        return try {
            if (requestedItemId.isBlank() || requesterId.isBlank()) {
                return Result.failure(Exception("Item ID and Requester ID cannot be empty"))
            }

            val request = CreateSwapRequest(
                requestedItemId = requestedItemId,
                requesterId = requesterId
            )

            val response = apiService.createSwapRequest(request)
            if (response.isSuccessful && response.body() != null) {
                val swapRequest = response.body()!!
                Result.success(swapRequest)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorBody ?: "Failed to create swap request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMySwapRequests(): Flow<Result<List<SwapRequest>>> = flow {
        try {
            val response = apiService.getMySwapRequests()
            if (response.isSuccessful && response.body() != null) {
                val requests = response.body()!!
                emit(Result.success(requests))
            } else {
                emit(Result.failure(Exception(response.message() ?: "Failed to fetch swap requests")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getMyRequestedItems(): Flow<Result<List<SwapRequest>>> = flow {
        try {
            val response = apiService.getMyRequestedItems()
            if (response.isSuccessful && response.body() != null) {
                val requests = response.body()!!
                emit(Result.success(requests))
            } else {
                emit(Result.failure(Exception(response.message() ?: "Failed to fetch your requested items")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun acceptSwapRequest(id: String): Result<SwapRequest> {
        return try {
            val response = apiService.acceptSwapRequest(id)
            if (response.isSuccessful && response.body() != null) {
                val swapRequest = response.body()!!
                Result.success(swapRequest)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to accept swap request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineSwapRequest(id: String): Result<SwapRequest> {
        return Result.failure(Exception("Decline endpoint not available in backend"))
    }

    suspend fun completeSwapRequest(id: String): Result<SwapRequest> {
        return try {
            val response = apiService.completeSwapRequest(id)
            if (response.isSuccessful && response.body() != null) {
                val swapRequest = response.body()!!
                Result.success(swapRequest)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to complete swap request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
