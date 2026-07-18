package com.creamaker.changli_planet_app.feature.common.ui

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.base.ComposeActivity
import com.creamaker.changli_planet_app.core.designsystem.HyperIconButton
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.hyperTap
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.common.data.local.entity.Grade
import com.creamaker.changli_planet_app.feature.common.data.local.mmkv.ScoreCache
import com.creamaker.changli_planet_app.feature.common.ui.compose.colorForGrade
import com.creamaker.changli_planet_app.feature.common.ui.compose.gradeValueOrNull
import com.creamaker.changli_planet_app.feature.common.ui.export.ScoreExportUtil

/** 成绩多选导出页：按学期分组勾选课程，导出所选为 CSV。 */
class ScoreExportActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent { ScoreExportScreen(onBack = ::finish) }
    }
}

private fun keyOf(g: Grade): String = "${g.item}${g.id}${g.name}"

@Composable
private fun ScoreExportScreen(onBack: () -> Unit) {
    val colors = AppTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val grades = remember { ScoreCache.getGrades().orEmpty() }
    val groups = remember(grades) {
        grades.groupBy { it.item }.toSortedMap(compareByDescending { it }).map { it.key to it.value }
    }
    val allKeys = remember(grades) { grades.map { keyOf(it) }.toSet() }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val allSelected = allKeys.isNotEmpty() && selected.size == allKeys.size

    Column(Modifier.fillMaxSize().background(colors.bgPrimaryColor).statusBarsPadding()) {
        HyperSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PortalBackButton(onClick = onBack)
                Text("选择课程", Modifier.weight(1f).padding(start = 12.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor)
                if (grades.isNotEmpty()) {
                    Text(
                        if (allSelected) "取消全选" else "全选",
                        Modifier.hyperTap { selected = if (allSelected) emptySet() else allKeys }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        color = colors.functionalTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (grades.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无成绩数据，请先在成绩查询刷新", color = colors.secondaryTextColor, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { (semester, list) ->
                item(key = "header_$semester") {
                    Text(
                        "$semester · ${list.size} 门",
                        fontSize = 13.sp, color = colors.secondaryTextColor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(list, key = { keyOf(it) }) { g ->
                    val k = keyOf(g)
                    val checked = selected.contains(k)
                    CourseSelectRow(g, checked) {
                        selected = if (checked) selected - k else selected + k
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        HyperSurface(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .hyperTap(enabled = selected.isNotEmpty()) {
                        val chosen = grades.filter { keyOf(it) in selected }
                        val ok = ScoreExportUtil.exportAndShare(context, chosen, "成绩单_所选${chosen.size}门")
                        if (!ok) Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (selected.isEmpty()) "请选择课程" else "导出所选 (${selected.size})",
                    color = if (selected.isEmpty()) colors.secondaryTextColor else colors.functionalTextColor,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CourseSelectRow(g: Grade, checked: Boolean, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    HyperSurface(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().hyperTap(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = colors.functionalTextColor)
            )
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(g.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${g.item} · ${g.score} 学分", fontSize = 11.sp, color = colors.secondaryTextColor, maxLines = 1)
            }
            val gv = gradeValueOrNull(g.grade)
            Text(
                g.grade, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = if (gv != null) colorForGrade(gv) else colors.primaryTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
