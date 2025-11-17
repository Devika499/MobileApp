package com.community.swaphub.data.repository

import com.community.swaphub.data.api.ApiService
import com.community.swaphub.data.local.dao.UserDao
import com.community.swaphub.data.model.AuthResponse
import com.community.swaphub.data.model.LoginRequest
import com.community.swaphub.data.model.User
import com.community.swaphub.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) {

    // ------------------ REGISTER ------------------
    suspend fun register(
        name: String, email: String, password: String, location: String?
    ): Result<AuthResponse> {
        return try {
            val user = User(
                id = "",
                name = name,
                email = email,
                password = password,
                location = location
            )

            val response = apiService.register(user)

            if (response.isSuccessful && response.body() != null) {
                val savedUser = response.body()!!
                userDao.insertUser(savedUser)
                preferencesManager.saveUserId(savedUser.id)
                preferencesManager.saveUserEmail(savedUser.email)

                login(email, password)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------ LOGIN ------------------
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                authResponse.userId?.let { userId ->
                    preferencesManager.saveUserId(userId)
                    preferencesManager.saveUserEmail(email)

                    val userResponse = apiService.getUser(userId)
                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        userDao.insertUser(userResponse.body()!!)
                    }
                }

                authResponse.token?.let { preferencesManager.saveAuthToken(it) }

                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------ LOGOUT ------------------
    suspend fun logout() {
        preferencesManager.clearAuth()
        userDao.clearAll()
    }

    // ------------------ CURRENT USER ------------------
    fun getCurrentUser(): Flow<User?> {
        return preferencesManager.userId.flatMapLatest { userId ->
            if (userId != null) userDao.getUserById(userId) else flowOf(null)
        }
    }

    suspend fun refreshCurrentUserIfNeeded() {
        val userId = preferencesManager.userId.first() ?: return

        try {
            val response = apiService.getUser(userId)
            if (response.isSuccessful && response.body() != null) {
                userDao.insertUser(response.body()!!)
            }
        } catch (_: Exception) { }
    }

    // ------------------ UPDATE USER DETAILS ------------------
    suspend fun updateUser(updatedUser: User): Result<User> {
        return try {
            val response = apiService.updateUser(updatedUser.id, updatedUser)

            if (response.isSuccessful && response.body() != null) {
                val saved = response.body()!!
                userDao.insertUser(saved)
                Result.success(saved)
            } else {
                Result.failure(Exception("Failed to update user"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
