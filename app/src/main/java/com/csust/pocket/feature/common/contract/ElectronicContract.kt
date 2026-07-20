package com.csust.pocket.feature.common.contract

import com.csust.pocket.core.mvi.MviIntent
import com.csust.pocket.core.mvi.MviSideEffect
import com.csust.pocket.core.mvi.MviViewState
import com.csust.pocket.overview.data.local.OverviewLocalCache

interface ElectronicContract {
    sealed class Intent : MviIntent {
        data class SelectSchool(val address: String) : Intent()
        data class SelectDorm(val buildId: String) : Intent()
        data class QueryElectricity(val address: String, val buildId: String, val nod: String) :
            Intent()

        data object AutoRefresh : Intent()

        data class Init(val address: String, val buildId: String, val nod: String) : Intent()

        data class LoadRooms(val address: String, val buildId: String) : Intent()
    }

    data class State(
        val address: String = "选择校区",
        val buildId: String = "选择宿舍楼",
        val elec: String = "",
        val isElec: Boolean = false,
        val isLoading: Boolean = false,
        val isLoadingRooms: Boolean = false,
        val availableRooms: List<String> = emptyList(),
        val realtimeHistory: List<OverviewLocalCache.ElectricityHistoryEntry> = emptyList(),
        val usage7Days: List<OverviewLocalCache.ElectricityUsageEntry> = emptyList(),
        val usage3Months: List<OverviewLocalCache.ElectricityUsageEntry> = emptyList()
    ) : MviViewState

    sealed class Effect : MviSideEffect {
        data class UpdateWidget(val appWidgetIds: IntArray) :
            Effect() // Optional: logic to update widget might be in Activity, but effect can trigger it

        data class ShowToast(val message: String) : Effect()
    }
}
