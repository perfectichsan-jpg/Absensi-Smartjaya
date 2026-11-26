package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.signOut
import com.example.data.model.UiState
import com.example.ui.components.ProfileEditorDialog
import com.example.ui.viewmodel.AbsenViewModel

const val ROUTE_HOME = "home"
const val ROUTE_HISTORY = "history"
const val ROUTE_LEAVE = "leave"
const val ROUTE_PROFILE = "profile"
const val ROUTE_SUPERVISOR = "supervisor"

private data class NavTabItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

@Composable
fun MainScaffold(viewModel: AbsenViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val logsState by viewModel.attendanceLogsState.collectAsStateWithLifecycle()
  val leavesState by viewModel.leaveRequestsState.collectAsStateWithLifecycle()
  val adminAttendanceState by viewModel.adminAttendanceState.collectAsStateWithLifecycle()
  val adminLeaveState by viewModel.adminLeaveState.collectAsStateWithLifecycle()
  val currentTimeMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
  val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  var showProfileEditor by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearFeedback()
  }

  val tabs =
    remember(viewModel.isAdmin) {
      buildList {
        add(NavTabItem(ROUTE_HOME, "Beranda", Icons.Filled.Home, Icons.Outlined.Home))
        add(NavTabItem(ROUTE_HISTORY, "Riwayat", Icons.Filled.History, Icons.Outlined.History))
        add(NavTabItem(ROUTE_LEAVE, "Izin", Icons.Filled.EventNote, Icons.Outlined.EventNote))
        if (viewModel.isAdmin) {
          add(
            NavTabItem(
              ROUTE_SUPERVISOR,
              "Pembimbing",
              Icons.Filled.SupervisorAccount,
              Icons.Outlined.SupervisorAccount,
            )
          )
        }
        add(NavTabItem(ROUTE_PROFILE, "Profil", Icons.Filled.Badge, Icons.Outlined.Badge))
      }
    }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

  val navigateToTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar {
            tabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail {
            tabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_tab_${tab.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ROUTE_HOME,
          modifier = Modifier.weight(1f),
        ) {
          composable(ROUTE_HOME) {
            HomeScreen(
              profileState = profileState,
              logsState = logsState,
              leavesState = leavesState,
              currentTimeMillis = currentTimeMillis,
              displayNameFallback = viewModel.currentUserDisplayName,
              isSubmitting = isSubmitting,
              onOpenProfileEditor = { showProfileEditor = true },
              onCheckIn = { mode, location, note ->
                val profile = (profileState as? UiState.Success)?.data
                viewModel.checkIn(profile, mode, location, note)
              },
              onCheckOut = { logId, note -> viewModel.checkOut(logId, note) },
              onUpdateActivities = { logId, note -> viewModel.updateDailyActivities(logId, note) },
            )
          }

          composable(ROUTE_HISTORY) {
            HistoryScreen(
              logsState = logsState,
              isSubmitting = isSubmitting,
              onUpdateActivities = { logId, note, onSuccess ->
                viewModel.updateDailyActivities(logId, note, onSuccess)
              },
            )
          }

          composable(ROUTE_LEAVE) {
            LeaveScreen(
              leavesState = leavesState,
              isSubmitting = isSubmitting,
              onSubmitLeave = { type, start, end, reason, onSuccess ->
                val profile = (profileState as? UiState.Success)?.data
                viewModel.submitLeaveRequest(profile, type, start, end, reason, onSuccess)
              },
              onCancelLeave = { requestId -> viewModel.cancelLeaveRequest(requestId) },
            )
          }

          if (viewModel.isAdmin) {
            composable(ROUTE_SUPERVISOR) {
              SupervisorScreen(
                adminAttendanceState = adminAttendanceState,
                adminLeaveState = adminLeaveState,
                isSubmitting = isSubmitting,
                onReviewLeave = { requestId, status, note, onSuccess ->
                  viewModel.reviewLeaveRequest(requestId, status, note, onSuccess)
                },
              )
            }
          }

          composable(ROUTE_PROFILE) {
            ProfileScreen(
              profileState = profileState,
              userEmail = viewModel.currentUserEmail,
              userDisplayName = viewModel.currentUserDisplayName,
              isAdmin = viewModel.isAdmin,
              onEditProfile = { showProfileEditor = true },
              onSignOut = {
                signOut(credentialManager = credentialManager, scope = scope)
              },
            )
          }
        }
      }
    }
  }

  if (showProfileEditor) {
    val currentProfile = (profileState as? UiState.Success)?.data
    ProfileEditorDialog(
      initialProfile = currentProfile,
      defaultName = viewModel.currentUserDisplayName,
      isSubmitting = isSubmitting,
      onDismiss = { showProfileEditor = false },
      onSave = { name, division, university, mentor, company, workMode, shiftStart, shiftEnd ->
        viewModel.saveProfile(
          fullName = name,
          division = division,
          university = university,
          mentorName = mentor,
          companyName = company,
          workModeDefault = workMode,
          shiftStart = shiftStart,
          shiftEnd = shiftEnd,
          onSuccess = { showProfileEditor = false },
        )
      },
    )
  }
}
