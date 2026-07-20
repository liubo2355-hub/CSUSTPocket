package com.csust.pocket.feature.common.contract

import com.csust.pocket.core.mvi.MviIntent
import com.csust.pocket.core.mvi.MviSideEffect
import com.csust.pocket.core.mvi.MviViewState
import com.csust.pocket.feature.common.ui.adapter.model.Exam

interface ExamArrangementContract {
    sealed class Intent : MviIntent {
        data class LoadExamArrangement(val termTime: String) : Intent()
    }

    data class State(
        val exams: List<Exam> = emptyList(),
        val isLoading: Boolean = false
    ) : MviViewState

    sealed class Effect : MviSideEffect {
        data class ShowToast(val message: String) : Effect()
        data class ShowErrorDialog(val message: String) : Effect()
    }
}
