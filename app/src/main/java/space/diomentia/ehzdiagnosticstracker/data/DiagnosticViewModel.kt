package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapConcat
import javax.inject.Inject

val LocalDiagnosticViewModel =
    staticCompositionLocalOf<DiagnosticViewModel> { throw error("no composition model") }

@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context
) : ViewModel() {
    private val areaId: String = savedStateHandle["areaId"] ?: throw error("no area id provided")

    val diagnosticArea = ReportDatabase.getInstance(context).diagnosticAreaDao().getAreaById(areaId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val diagnosticObject = diagnosticArea.flatMapConcat {
        ReportDatabase.getInstance(context).diagnosticAreaDao()
            .getObjectById(it.diagnosticArea.diagnosticObject)
    }
    val points = ReportDatabase.getInstance(context).diagnosticAreaDao().getAreaPoints(areaId)
}