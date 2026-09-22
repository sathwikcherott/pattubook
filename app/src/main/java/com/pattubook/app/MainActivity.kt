package com.pattubook.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.components.NavDestination
import com.pattubook.app.ui.detail.PersonDetailScreen
import com.pattubook.app.ui.home.HomeScreen
import com.pattubook.app.ui.more.MoreScreen
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.viewmodel.PeopleViewModel
import com.pattubook.app.ui.viewmodel.PersonDetailViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PattubookDatabase.getDatabase(applicationContext)
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database
        )
        val peopleViewModelFactory = PeopleViewModel.Factory(repository)

        setContent {
            PattubookTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                val context = LocalContext.current
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
                            onGiveMoneyConfirm = { personId, amountPaise, note ->
                                peopleViewModel.addMoneyGiven(personId, amountPaise, note)
                            },
                            onRecordReturnConfirm = { personId, amountPaise, note ->
                                peopleViewModel.addMoneyGivenBack(personId, amountPaise, note)
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
                            onGiveMoneyConfirm = { amountPaise, note ->
                                detailViewModel.addMoneyGiven(amountPaise, note = note)
                            },
                            onRecordReturnConfirm = { amountPaise, note ->
                                detailViewModel.addMoneyGivenBack(amountPaise, note = note)
                            },
                            onUndoClick = {
                                detailViewModel.undoLastLedgerEntry()
                            }
                        )
                    }

                    composable("more") {
                        MoreScreen(
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
                            }
                        )
                    }
                }
            }
        }
    }
}
