package com.pattubook.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.breakdown.OutstandingBreakdownScreen
import com.pattubook.app.ui.components.AppLockOverlay
import com.pattubook.app.ui.components.NavDestination
import com.pattubook.app.ui.detail.PersonDetailScreen
import com.pattubook.app.ui.home.HomeScreen
import com.pattubook.app.ui.more.MoreScreen
import com.pattubook.app.ui.recyclebin.RecycleBinScreen
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.util.AppLockManager
import com.pattubook.app.ui.viewmodel.PeopleViewModel
import com.pattubook.app.ui.viewmodel.PersonDetailViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PattubookDatabase.getDatabase(applicationContext)
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )
        val peopleViewModelFactory = PeopleViewModel.Factory(repository)

        setContent {
            PattubookTheme {
                val context = LocalContext.current
                val appLockManager = remember { AppLockManager(applicationContext) }

                var isAppLockEnabled by remember { mutableStateOf(appLockManager.isAppLockEnabled()) }
                var isUnlocked by remember {
                    mutableStateOf(!appLockManager.isAppLockEnabled() || appLockManager.isSessionUnlocked)
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            if (appLockManager.isAppLockEnabled()) {
                                appLockManager.isSessionUnlocked = false
                                isUnlocked = false
                            }
                        } else if (event == Lifecycle.Event.ON_RESUME) {
                            if (appLockManager.isAppLockEnabled() && !appLockManager.isSessionUnlocked) {
                                isUnlocked = false
                                appLockManager.authenticate(
                                    activity = this@MainActivity,
                                    title = "Unlock Pattubook",
                                    subtitle = "Authenticate to access your ledger",
                                    onSuccess = { isUnlocked = true },
                                    onError = { /* Stay locked */ }
                                )
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (isAppLockEnabled && !isUnlocked) {
                    AppLockOverlay(
                        onUnlockClick = {
                            appLockManager.authenticate(
                                activity = this@MainActivity,
                                title = "Unlock Pattubook",
                                subtitle = "Authenticate to access your ledger",
                                onSuccess = { isUnlocked = true }
                            )
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                    var lastBackPressTime by remember { mutableLongStateOf(0L) }
                    val isAtHome = currentRoute == "home"

                    BackHandler(enabled = isAtHome) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000L) {
                            (context as? Activity)?.finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, "Swipe again to exit", Toast.LENGTH_SHORT).show()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("home") {
                            val peopleViewModel: PeopleViewModel = viewModel(factory = peopleViewModelFactory)
                            val uiState by peopleViewModel.uiState.collectAsState()

                            HomeScreen(
                                uiState = uiState,
                                onPersonClick = { personId ->
                                    navController.navigate("person_detail/$personId")
                                },
                                onAddPersonConfirm = { name ->
                                    peopleViewModel.addPerson(name)
                                },
                                onGiveMoneyConfirm = { personId, amountPaise, note, timestamp ->
                                    peopleViewModel.addMoneyGiven(personId, amountPaise, note, timestamp)
                                },
                                onRecordReturnConfirm = { personId, amountPaise, note, timestamp ->
                                    peopleViewModel.addMoneyGivenBack(personId, amountPaise, note, timestamp)
                                },
                                onHidePersonConfirm = { person ->
                                    peopleViewModel.setPersonHidden(person.id, true)
                                },
                                onUnhidePersonConfirm = { person ->
                                    peopleViewModel.setPersonHidden(person.id, false)
                                },
                                onDeletePersonConfirm = { person ->
                                    peopleViewModel.movePersonToRecycleBin(person)
                                },
                                onBalanceCardClick = {
                                    navController.navigate("outstanding_breakdown")
                                },
                                onBottomNavSelected = { destination ->
                                    when (destination) {
                                        NavDestination.HOME -> {}
                                        NavDestination.MORE -> {
                                            navController.navigate("more") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        composable("outstanding_breakdown") {
                            val peopleViewModel: PeopleViewModel = viewModel(factory = peopleViewModelFactory)
                            val uiState by peopleViewModel.uiState.collectAsState()

                            OutstandingBreakdownScreen(
                                uiState = uiState,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onPersonClick = { personId ->
                                    navController.navigate("person_detail/$personId")
                                }
                            )
                        }

                        composable(
                            route = "person_detail/{personId}",
                            arguments = listOf(
                                navArgument("personId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
                            val detailViewModelFactory = remember(personId) {
                                PersonDetailViewModel.Factory(personId, repository)
                            }
                            val detailViewModel: PersonDetailViewModel = viewModel(
                                key = "person_detail_$personId",
                                factory = detailViewModelFactory
                            )
                            val detailUiState by detailViewModel.uiState.collectAsState()

                            PersonDetailScreen(
                                uiState = detailUiState,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onGiveMoneyConfirm = { amountPaise, note, timestamp ->
                                    detailViewModel.addMoneyGiven(amountPaise, timestamp = timestamp, note = note)
                                },
                                onRecordReturnConfirm = { amountPaise, note, timestamp ->
                                    detailViewModel.addMoneyGivenBack(amountPaise, timestamp = timestamp, note = note)
                                },
                                onUndoClick = {
                                    detailViewModel.undoLastLedgerEntry()
                                },
                                onRedoClick = {
                                    detailViewModel.redoLastUndoneEntry()
                                },
                                onDeleteTransactionSwipe = { entry ->
                                    detailViewModel.deleteEntry(entry)
                                },
                                onEditTransactionConfirm = { updatedEntry ->
                                    detailViewModel.updateEntry(updatedEntry)
                                },
                                eventFlow = detailViewModel.eventFlow,
                            )
                        }

                        composable("more") {
                            val peopleViewModel: PeopleViewModel = viewModel(factory = peopleViewModelFactory)
                            val coroutineScope = rememberCoroutineScope()

                            MoreScreen(
                                isAppLockEnabled = isAppLockEnabled,
                                onAppLockToggle = { enable ->
                                    if (enable) {
                                        if (!appLockManager.canAuthenticate(context)) {
                                            Toast.makeText(
                                                context,
                                                "Device authentication is not supported or enrolled on this device.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            appLockManager.authenticate(
                                                activity = this@MainActivity,
                                                title = "Enable App Lock",
                                                subtitle = "Authenticate to turn on App Lock",
                                                onSuccess = {
                                                    appLockManager.setAppLockEnabled(true)
                                                    isAppLockEnabled = true
                                                    isUnlocked = true
                                                    Toast.makeText(context, "App Lock enabled", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { msg ->
                                                    Toast.makeText(context, "Failed to enable App Lock: $msg", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        appLockManager.authenticate(
                                            activity = this@MainActivity,
                                            title = "Disable App Lock",
                                            subtitle = "Authenticate to turn off App Lock",
                                            onSuccess = {
                                                appLockManager.setAppLockEnabled(false)
                                                isAppLockEnabled = false
                                                isUnlocked = true
                                                Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { msg ->
                                                Toast.makeText(context, "Failed to disable App Lock: $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                onDestinationSelected = { destination ->
                                    when (destination) {
                                        NavDestination.HOME -> {
                                            navController.navigate("home") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        NavDestination.MORE -> {}
                                    }
                                },
                                onOpenRecycleBinClick = {
                                    navController.navigate("recycle_bin")
                                },
                                onExportBackupToUri = { uri ->
                                    coroutineScope.launch {
                                        val jsonResult = repository.generateBackupJson()
                                        jsonResult.onSuccess { json ->
                                            val exportResult = repository.exportBackupToUri(context, uri, json)
                                            exportResult.onSuccess {
                                                Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                Toast.makeText(context, e.message ?: "Failed to write backup file.", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure { e ->
                                            Toast.makeText(context, e.message ?: "Failed to generate backup.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onImportBackupFromUri = { uri ->
                                    coroutineScope.launch {
                                        val restoreResult = repository.restoreBackupFromUri(context, uri)
                                        restoreResult.onSuccess {
                                            Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, e.message ?: "Failed to restore backup.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onDeleteAllDataConfirm = {
                                    peopleViewModel.deleteAllData()
                                    Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        composable("recycle_bin") {
                            val peopleViewModel: PeopleViewModel = viewModel(factory = peopleViewModelFactory)
                            val uiState by peopleViewModel.uiState.collectAsState()

                            RecycleBinScreen(
                                uiState = uiState,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onRestorePerson = { person ->
                                    peopleViewModel.restorePerson(person)
                                },
                                onPermanentlyDeletePerson = { person ->
                                    peopleViewModel.permanentlyDeletePerson(person)
                                },
                                onEmptyRecycleBin = {
                                    peopleViewModel.emptyRecycleBin()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
