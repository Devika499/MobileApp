package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.LoginRequest
import com.community.swaphub.data.model.RegisterRequest
import com.community.swaphub.data.model.User
import com.community.swaphub.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        println("DEBUG: AuthViewModel init - checking auth status")
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            println("DEBUG: AuthViewModel - checkAuthStatus called")
            authRepository.getCurrentUser().collect { user ->
                println("DEBUG: AuthViewModel - getCurrentUser returned: ${user?.id ?: "null"}")
                _currentUser.value = user
                // Also update UI state if we have a user
                if (user != null && _uiState.value !is AuthUiState.Success) {
                    _uiState.value = AuthUiState.Success(user)
                }
            }
        }
    }

    fun refreshCurrentUserIfNeeded() {
        viewModelScope.launch {
            println("DEBUG: AuthViewModel - refreshCurrentUserIfNeeded called")
            authRepository.refreshCurrentUserIfNeeded()
            // Force re-check auth status after refresh
            checkAuthStatus()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            println("DEBUG: AuthViewModel - login called for: $email")
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                println("DEBUG: AuthViewModel - login successful")
                // Force refresh the current user after login
                refreshCurrentUserIfNeeded()
                _uiState.value = AuthUiState.Success(_currentUser.value)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                println("DEBUG: AuthViewModel - login failed: $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        location: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            println("DEBUG: AuthViewModel - register called for: $email")
            val result = authRepository.register(name, email, password, location)
            if (result.isSuccess) {
                println("DEBUG: AuthViewModel - register successful")
                // Force refresh the current user after registration
                refreshCurrentUserIfNeeded()
                _uiState.value = AuthUiState.Success(_currentUser.value)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed"
                println("DEBUG: AuthViewModel - register failed: $errorMsg")
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            println("DEBUG: AuthViewModel - logout called")
            authRepository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
            println("DEBUG: AuthViewModel - logout completed, current user is now null")
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    // Add this method to manually set current user (for debugging)
    fun setCurrentUserForDebug(user: User) {
        println("DEBUG: AuthViewModel - manually setting current user: ${user.id}")
        _currentUser.value = user
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}