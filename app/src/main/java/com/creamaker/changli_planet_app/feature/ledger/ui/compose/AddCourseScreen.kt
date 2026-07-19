package com.creamaker.changli_planet_app.feature.ledger.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.timetable.ui.compose.TIMETABLE_TOTAL_SECTIONS

data class AddCourseTimeSlotDraft(
    val weeks: Set<Int>,
    val dayOfWeek: Int,
    val startSection: Int,
    val sectionSpan: Int,
)

data class AddCourseDraft(
    val courseName: String,
    val teacher: String,
    val room: String,
    val credit: String,
    val note: String,
    val colorValue: Long,
    val timeSlots: List<AddCourseTimeSlotDraft>,
)

private val coursePalette = listOf(
    Color(0xFF20B7A6), Color(0xFF3399E6), Color(0xFFFFB02E), Color(0xFFFF607D),
    Color(0xFF7E74E8), Color(0xFF41B96F), Color(0xFFF05A5A), Color(0xFF5F87A8),
)
private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun AddCourseScreen(
    initialWeek: Int,
    initialDay: Int,
    initialStartSection: Int,
    initialSpan: Int,
    courseSuggestions: List<String>,
    onBack: () -> Unit,
    onSave: (AddCourseDraft) -> Unit,
) {
    val colors = AppTheme.colors
    var courseName by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var slots by remember {
        mutableStateOf(
            listOf(
                AddCourseTimeSlotDraft(
                    weeks = (1..20).toSet(),
                    dayOfWeek = initialDay,
                    startSection = initialStartSection,
                    sectionSpan = initialSpan,
                )
            )
        )
    }
    var weekDialogIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgPrimaryColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = colors.primaryTextColor)
                }
                Text("添加课程", modifier = Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryTextColor)
                TextButton(onClick = {
                    onSave(AddCourseDraft(courseName, teacher, room, credit, note, coursePalette[colorIndex].value.toLong(), slots))
                }) {
                    Text("保存", fontSize = 17.sp, color = colors.primaryTextColor)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp),
            ) {
                FormTextField(Icons.Filled.Edit, Color(0xFF20B7A6), courseName, { courseName = it }, "课程名称")
                if (courseSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 58.dp, top = 6.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        courseSuggestions.forEach { suggestion ->
                            Surface(
                                modifier = Modifier.clickable { courseName = suggestion },
                                shape = RoundedCornerShape(22.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.secondaryTextColor.copy(alpha = 0.55f)),
                            ) {
                                Text(suggestion, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.primaryTextColor)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { colorIndex = (colorIndex + 1) % coursePalette.size }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp).background(coursePalette[colorIndex], RoundedCornerShape(7.dp)))
                    Spacer(Modifier.width(20.dp))
                    Text("点此更改颜色", color = coursePalette[colorIndex], fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        coursePalette.forEachIndexed { index, color ->
                            Box(Modifier.size(if (index == colorIndex) 12.dp else 8.dp).background(color, CircleShape))
                        }
                    }
                }
                FormTextField(Icons.Filled.Edit, Color(0xFF3399E6), credit, { credit = it }, "学分（可不填）", KeyboardType.Decimal)

                slots.forEachIndexed { index, slot ->
                    TimeSlotCard(
                        index = index,
                        slot = slot,
                        canDelete = slots.size > 1,
                        onDelete = { slots = slots.toMutableList().also { it.removeAt(index) } },
                        onWeeksClick = { weekDialogIndex = index },
                        onChange = { changed -> slots = slots.toMutableList().also { it[index] = changed } },
                    )
                }

                FormTextField(Icons.Filled.Person, Color(0xFF238AD1), teacher, { teacher = it }, "授课老师（可不填）")
                FormTextField(Icons.Filled.LocationOn, Color(0xFFF05050), room, { room = it }, "上课地点（可不填）")
                FormTextField(Icons.Filled.Edit, Color(0xFFFFC928), note, { note = it }, "备注（可不填）")
            }
        }

        FloatingActionButton(
            onClick = {
                val previous = slots.last()
                val nextStart = (previous.startSection + previous.sectionSpan).coerceAtMost(TIMETABLE_TOTAL_SECTIONS)
                slots = slots + previous.copy(startSection = nextStart, sectionSpan = minOf(2, 11 - nextStart))
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(60.dp),
            containerColor = colors.commonColor.copy(alpha = 0.18f),
            contentColor = colors.primaryTextColor,
            shape = RoundedCornerShape(18.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "增加时间段", modifier = Modifier.size(30.dp)) }
    }

    if (weekDialogIndex in slots.indices) {
        WeekSelectionDialog(
            selected = slots[weekDialogIndex].weeks,
            onDismiss = { weekDialogIndex = -1 },
            onConfirm = { weeks ->
                slots = slots.toMutableList().also { it[weekDialogIndex] = it[weekDialogIndex].copy(weeks = weeks) }
                weekDialogIndex = -1
            },
        )
    }
}

@Composable
private fun FormTextField(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 7.dp),
        placeholder = { Text(hint, color = colors.greyTextColor) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = iconColor) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor.copy(alpha = 0.55f),
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = colors.bgCardColor.copy(alpha = 0.45f),
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = colors.primaryTextColor,
            unfocusedTextColor = colors.primaryTextColor,
        ),
    )
}

@Composable
private fun TimeSlotCard(
    index: Int,
    slot: AddCourseTimeSlotDraft,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onWeeksClick: () -> Unit,
    onChange: (AddCourseTimeSlotDraft) -> Unit,
) {
    val colors = AppTheme.colors
    var dayMenu by remember { mutableStateOf(false) }
    var startMenu by remember { mutableStateOf(false) }
    var endMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).background(colors.bgCardColor.copy(alpha = 0.38f)).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (index == 0) "时间段" else "时间段 ${index + 1}", modifier = Modifier.weight(1f), color = colors.primaryTextColor, fontSize = 15.sp)
            if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "删除时间段", tint = colors.secondaryTextColor) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onWeeksClick).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.DateRange, null, tint = Color(0xFF20B7A6))
            Spacer(Modifier.width(20.dp))
            Text(formatWeeks(slot.weeks), color = colors.primaryTextColor, fontSize = 17.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = Color(0xFFFFA91F))
            Spacer(Modifier.width(20.dp))
            Box {
                Text(weekdayNames[slot.dayOfWeek - 1], modifier = Modifier.clickable { dayMenu = true }.padding(vertical = 8.dp), color = colors.primaryTextColor)
                DropdownMenu(dayMenu, { dayMenu = false }) {
                    weekdayNames.forEachIndexed { i, text -> DropdownMenuItem({ Text(text) }, { onChange(slot.copy(dayOfWeek = i + 1)); dayMenu = false }) }
                }
            }
            Spacer(Modifier.width(22.dp))
            Box {
                Text("第${slot.startSection}节", modifier = Modifier.clickable { startMenu = true }.padding(vertical = 8.dp), color = colors.primaryTextColor)
                DropdownMenu(startMenu, { startMenu = false }) {
                    (1..TIMETABLE_TOTAL_SECTIONS).forEach { section -> DropdownMenuItem({ Text("第${section}节") }, {
                        val end = (slot.startSection + slot.sectionSpan - 1).coerceAtLeast(section)
                        onChange(slot.copy(startSection = section, sectionSpan = (end - section + 1).coerceAtLeast(1))); startMenu = false
                    }) }
                }
            }
            Text(" - ", color = colors.secondaryTextColor)
            Box {
                val endSection = (slot.startSection + slot.sectionSpan - 1).coerceAtMost(TIMETABLE_TOTAL_SECTIONS)
                Text("第${endSection}节", modifier = Modifier.clickable { endMenu = true }.padding(vertical = 8.dp), color = colors.primaryTextColor)
                DropdownMenu(endMenu, { endMenu = false }) {
                    (slot.startSection..TIMETABLE_TOTAL_SECTIONS).forEach { section -> DropdownMenuItem({ Text("第${section}节") }, {
                        onChange(slot.copy(sectionSpan = section - slot.startSection + 1)); endMenu = false
                    }) }
                }
            }
        }
    }
}

@Composable
private fun WeekSelectionDialog(selected: Set<Int>, onDismiss: () -> Unit, onConfirm: (Set<Int>) -> Unit) {
    var chosen by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择上课周次") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).height(360.dp)) {
                (1..20).chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { week ->
                            Row(
                                modifier = Modifier.weight(1f).clickable { chosen = if (week in chosen) chosen - week else chosen + week },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(week in chosen, { checked -> chosen = if (checked) chosen + week else chosen - week })
                                Text("$week")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (chosen.isNotEmpty()) onConfirm(chosen) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatWeeks(weeks: Set<Int>): String {
    if (weeks.isEmpty()) return "请选择周次"
    val sorted = weeks.sorted()
    return if (sorted == (sorted.first()..sorted.last()).toList()) "第${sorted.first()} - ${sorted.last()}周" else "第${sorted.joinToString(",")}周"
}
