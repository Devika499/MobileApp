package com.community.swaphub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.community.swaphub.ui.screens.admin.AdminDashboardScreen
import com.community.swaphub.ui.screens.auth.LoginScreen
import com.community.swaphub.ui.screens.auth.RegisterScreen
import com.community.swaphub.ui.screens.chat.ChatScreen
import com.community.swaphub.ui.screens.chat.ConversationsListScreen
import com.community.swaphub.ui.screens.edititem.EditItemScreen
import com.community.swaphub.ui.screens.home.HomeScreen
import com.community.swaphub.ui.screens.itemdetail.ItemDetailScreen
import com.community.swaphub.ui.screens.post.PostItemScreen
import com.community.swaphub.ui.screens.profile.ProfileScreen
import com.community.swaphub.ui.screens.notifications.NotificationsScreen
import com.community.swaphub.viewmodel.AuthViewModel
import com.community.swaphub.viewmodel.SwapRequestViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: String) = "item_detail/$itemId"
    }
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: String) = "edit_item/$itemId"
    }
    object PostItem : Screen("post_item")
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat/{otherUserId}/{itemId}") {
        fun createRoute(otherUserId: String, itemId: String? = null) =
            "chat/$otherUserId/${itemId ?: ""}"
    }
    object Admin : Screen("admin")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn = currentUser != null

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
    ) {

        // ---------------- AUTH ----------------
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // ---------------- HOME ----------------
        composable(Screen.Home.route) {
            HomeScreen(
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                },
                onPostItemClick = { navController.navigate(Screen.PostItem.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToConversations = { navController.navigate(Screen.Conversations.route) }
            )
        }

        // ---------------- PROFILE ----------------
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onItemClick = { itemId ->
                    // Navigate to edit screen for own items in profile
                    navController.navigate(Screen.EditItem.createRoute(itemId))
                },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
            )
        }

        // ---------------- NOTIFICATIONS ----------------
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // ---------------- CONVERSATIONS LIST ----------------
        composable(Screen.Conversations.route) {
            ConversationsListScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { otherUserId, itemId ->
                    navController.navigate(
                        Screen.Chat.createRoute(otherUserId, itemId)
                    )
                }
            )
        }

        // ---------------- POST ITEM ----------------
        composable(Screen.PostItem.route) {
            PostItemScreen(
                onBackClick = { navController.popBackStack() },
                onPostSuccess = { navController.popBackStack() }
            )
        }

        // ---------------- ITEM DETAIL ----------------
        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val userId = currentUser?.id ?: ""

            // Create SwapRequestViewModel using hiltViewModel() for this specific screen
            val swapRequestViewModel: SwapRequestViewModel = hiltViewModel()

            ItemDetailScreen(
                itemId = itemId,
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onChatClick = { otherUserId ->
                    navController.navigate(
                        Screen.Chat.createRoute(otherUserId, itemId)
                    )
                },
                swapViewModel = swapRequestViewModel
            )
        }

        // ---------------- EDIT ITEM ----------------
        composable(
            route = Screen.EditItem.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""

            EditItemScreen(
                itemId = itemId,
                onBackClick = { navController.popBackStack() },
                onUpdateSuccess = { navController.popBackStack() },
                onDeleteSuccess = { navController.popBackStack() }
            )
        }

        // ---------------- CHAT ----------------
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("otherUserId") {
                    type = androidx.navigation.NavType.StringType
                },
                navArgument("itemId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val itemId = backStackEntry.arguments?.getString("itemId")

            // Clean the itemId - if it's empty, use null
            val cleanItemId = if (itemId.isNullOrEmpty()) null else itemId

            ChatScreen(
                otherUserId = otherUserId,
                itemId = cleanItemId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ---------------- ADMIN ----------------
        composable(Screen.Admin.route) {
            AdminDashboardScreen(onBackClick = { navController.popBackStack() })
        }
    }
}