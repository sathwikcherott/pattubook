package com.pattubook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.detail.PersonDetailScreen
import com.pattubook.app.ui.home.HomeScreen
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
                var selectedPersonId by remember { mutableStateOf<Long?>(null) }

                val currentPersonId = selectedPersonId
                if (currentPersonId == null) {
                    val peopleViewModel: PeopleViewModel = viewModel(factory = peopleViewModelFactory)
                    val uiState by peopleViewModel.uiState.collectAsState()

                    HomeScreen(
                        uiState = uiState,
                        onPersonClick = { personId ->
                            selectedPersonId = personId
                        },
                        onAddPersonConfirm = { name ->
                            peopleViewModel.addPerson(name)
                        },
                        onGiveMoneyConfirm = { personId, amountPaise, note ->
                            peopleViewModel.addMoneyGiven(personId, amountPaise, note)
                        },
                        onRecordReturnConfirm = { personId, amountPaise, note ->
                            peopleViewModel.addMoneyGivenBack(personId, amountPaise, note)
                        }
                    )
                } else {
                    val detailViewModelFactory = remember(currentPersonId) {
                        PersonDetailViewModel.Factory(currentPersonId, repository)
                    }
                    val detailViewModel: PersonDetailViewModel = viewModel(
                        key = "person_detail_$currentPersonId",
                        factory = detailViewModelFactory
                    )
                    val detailUiState by detailViewModel.uiState.collectAsState()

                    PersonDetailScreen(
                        uiState = detailUiState,
                        onBackClick = {
                            selectedPersonId = null
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
            }
        }
    }
}
