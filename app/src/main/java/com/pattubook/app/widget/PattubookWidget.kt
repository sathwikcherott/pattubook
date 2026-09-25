package com.pattubook.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pattubook.app.data.local.PattubookDatabase
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.data.repository.PattubookRepository
import com.pattubook.app.ui.util.CurrencyFormatter
import kotlinx.coroutines.flow.first

class PattubookWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PattubookWidget()
}

class PattubookWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = PattubookDatabase.getDatabase(context.applicationContext)
        val repository = PattubookRepository(
            personDao = database.personDao(),
            ledgerEntryDao = database.ledgerEntryDao(),
            database = database,
        )

        val people = repository.observeAllPeople().first()
        val balancesMap = repository.observeAllPersonBalances().first()

        val activePeopleWithBalances = people.filter { !it.isDeleted }

        provideContent {
            GlanceWidgetContent(
                context = context,
                people = activePeopleWithBalances,
                balancesMap = balancesMap,
            )
        }
    }

    @Composable
    private fun GlanceWidgetContent(
        context: Context,
        people: List<Person>,
        balancesMap: Map<Long, Long>,
    ) {
        val darkBackground = Color(0xFF0B0D10)
        val accentGreen = Color(0xFF20DC8E)
        val textPrimary = Color(0xFFF1F5F9)
        val textSecondary = Color(0xFF94A3B8)
        val surfaceVariant = Color(0xFF1E232A)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(14.dp)
        ) {
            // Widget Title Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pattubook",
                    style = TextStyle(
                        color = ColorProvider(accentGreen),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "Lending Ledger",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (people.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(surfaceVariant)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active people or balances",
                        style = TextStyle(
                            color = ColorProvider(textSecondary),
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(people) { person ->
                        val balance = balancesMap[person.id] ?: 0L
                        val deeplinkIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("pattubook://person_detail/${person.id}")
                        ).apply {
                            setPackage(context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable(actionStartActivity(deeplinkIntent)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = person.name,
                                style = TextStyle(
                                    color = ColorProvider(textPrimary),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Text(
                                text = CurrencyFormatter.formatPaiseToRupees(balance),
                                style = TextStyle(
                                    color = ColorProvider(if (balance > 0) accentGreen else textPrimary),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
