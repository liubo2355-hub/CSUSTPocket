package com.creamaker.changli_planet_app.feature.common.viewModel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.creamaker.changli_planet_app.core.mvi.MviViewModel
import com.creamaker.changli_planet_app.feature.common.contract.ElectronicContract
import com.creamaker.changli_planet_app.feature.common.data.repository.ElectricityRepository
import com.creamaker.changli_planet_app.overview.data.local.OverviewLocalCache
import com.example.csustdataget.CampusCard.CampusCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ElectronicViewModel : MviViewModel<ElectronicContract.Intent, ElectronicContract.State>() {
    private val repository by lazy { ElectricityRepository() }
    private var queryJob: Job? = null
    private var loadRoomsJob: Job? = null
    override fun initialState(): ElectronicContract.State = ElectronicContract.State()

    override fun processIntent(intent: ElectronicContract.Intent) {
        when (intent) {
            is ElectronicContract.Intent.SelectSchool -> {
                updateState { copy(address = intent.address) }
            }

            is ElectronicContract.Intent.SelectDorm -> {
                updateState { copy(buildId = intent.buildId) }
            }

            is ElectronicContract.Intent.QueryElectricity -> {
                repository.saveBinding(intent.address, intent.buildId, intent.nod)
                updateState {
                    copy(address = intent.address, buildId = intent.buildId)
                }
                // 用户主动刷新：取消可能卡住的在途任务，保证每次点击都真正重查
                queryJob?.cancel()
                queryElectricity(forceRefresh = true)
            }

            ElectronicContract.Intent.AutoRefresh -> {
                if (repository.hasBinding()) queryElectricity(forceRefresh = true)
            }

            is ElectronicContract.Intent.Init -> {
                updateState {
                    copy(address = intent.address, buildId = intent.buildId)
                }
                if (intent.address != "选择校区" && intent.buildId != "选择宿舍楼" && intent.nod.isNotEmpty()) {
                    queryElectricity(forceRefresh = false)
                }
            }

            is ElectronicContract.Intent.LoadRooms -> {
                loadRoomsJob?.cancel()
                loadRoomsJob = viewModelScope.launch(Dispatchers.IO) {
                    updateState { copy(isLoadingRooms = true, availableRooms = emptyList()) }
                    try {
                        val rooms = CampusCardHelper.getRooms(intent.address, intent.buildId)
                            .map { it.name }
                            .sortedWith(compareBy({ it.length }, { it }))
                        if (!isActive) return@launch
                        updateState { copy(isLoadingRooms = false, availableRooms = rooms) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (!isActive) return@launch
                        updateState { copy(isLoadingRooms = false, availableRooms = emptyList()) }
                    }
                }
            }
        }
    }

    private fun queryElectricity(forceRefresh: Boolean) {
        if (queryJob?.isActive == true) return
        updateState { copy(isLoading = true) }
        queryJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = if (forceRefresh) {
                    repository.query(force = true)
                } else {
                    repository.refreshIfNeeded()
                }

                // 若本任务已被新的刷新取消，丢弃迟到结果，避免覆盖最新数据
                if (!isActive) return@launch

                if (result == null) {
                    updateState { copy(isLoading = false, elec = "无数据", isElec = true).withUsageHistory() }
                } else {
                    Log.d("ElectronicViewModel", result.rawValue)
                    updateState {
                        copy(isLoading = false, elec = result.rawValue, isElec = true).withUsageHistory()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isActive) return@launch
                updateState { copy(isLoading = false, elec = "查询失败", isElec = true).withUsageHistory() }
            }
        }
    }

    private fun ElectronicContract.State.withUsageHistory() = copy(
        realtimeHistory = OverviewLocalCache.getRealtimeElectricityHistory(),
        usage7Days = OverviewLocalCache.getDailyUsage(7),
        usage3Months = OverviewLocalCache.getDailyUsage(90)
    )
}
