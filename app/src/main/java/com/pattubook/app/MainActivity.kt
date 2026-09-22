package com.pattubook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.home.HomeScreen
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.viewmodel.PeopleViewModel

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
        val viewModelFactory = PeopleViewModel.Factory(repository)

        setContent {
            PattubookTheme {
                val peopleViewModel: PeopleViewModel = viewModel(factory = viewModelFactory)
                val uiState by peopleViewModel.uiState.collectAsState()

                HomeScreen(
                    uiState = uiState,
                    onPersonClick = { /* Will navigate to PersonDetail in future step */ },
                    onAddPersonClick = { /* Will navigate to AddPerson in future step */ },
                    onGiveMoneyClick = { /* Will navigate to GiveMoney in future step */ },
                    onRecordReturnClick = { /* Will navigate to RecordReturn in future step */ }
                )
            }
        }
    }
}
