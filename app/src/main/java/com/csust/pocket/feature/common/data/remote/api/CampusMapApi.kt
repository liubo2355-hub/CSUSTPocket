package com.csust.pocket.feature.common.data.remote.api

import com.csust.pocket.feature.common.data.remote.dto.CampusMapGeoJson
import retrofit2.http.GET

/**
 * 掌上长理 Go 服务端「校园地图」接口。
 *
 * BaseUrl：`instancePlanet`（见 [com.csust.pocket.utils.RetrofitUtils]）。
 */
interface CampusMapApi {

    /**
     * 获取两个校区所有建筑的 GeoJSON。返回体结构见 [CampusMapGeoJson]。
     *
     * 线上约 124 个 Feature（~55 KB），可整包请求；App 侧做本地 JSON 缓存。
     */
    @GET("config/campus-map")
    suspend fun getCampusMap(): CampusMapGeoJson
}
