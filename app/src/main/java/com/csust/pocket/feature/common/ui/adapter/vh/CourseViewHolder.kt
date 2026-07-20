package com.csust.pocket.feature.common.ui.adapter.vh

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.csust.pocket.R
import com.csust.pocket.databinding.ScoreItemCourseBinding
import com.csust.pocket.feature.common.ui.adapter.model.CourseScore
import com.csust.pocket.feature.common.ui.compose.colorForGrade
import com.csust.pocket.feature.common.ui.compose.gradeValueOrNull
import com.csust.pocket.skin.views.SkinTextView

class CourseViewHolder(
    private val binding: ScoreItemCourseBinding,
    private val context: Context,
    private val onCourseClick: (CourseScore) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("DefaultLocale")
    fun bind(courseScore: CourseScore) {
        binding.apply {
            tvCourseName.text = courseScore.name
            tvScore.text = courseScore.scoreText
            tvCredit.text = String.format("学分: %.1f", courseScore.credit)
            getCredit.text = String.format("绩点: %.1f", courseScore.earnedCredit)
            tvType.text = courseScore.courseType

            // tv_score 经换肤工厂被替换为 SkinTextView，其 onAttachedToWindow 会在 bind 之后
            // 再次 applySkin，按 color_text_highlight 覆盖普通 setTextColor。
            // 数字成绩：关掉该 View 换肤，改用成绩动态色（红→黄→绿）；
            // 非数字成绩（无法识别）：交还换肤框架，保持高亮色，避免复用视图串色。
            val skinScore = tvScore as? SkinTextView
            val gradeValue = gradeValueOrNull(courseScore.scoreText)
            if (gradeValue != null) {
                skinScore?.isSkinable = false
                tvScore.setTextColor(colorForGrade(gradeValue).toArgb())
            } else if (skinScore != null) {
                skinScore.isSkinable = true
                skinScore.applySkin()
            } else {
                tvScore.setTextColor(ContextCompat.getColor(context, R.color.color_text_highlight))
            }
        }

        binding.scoreItemLayout.setOnClickListener {
            onCourseClick(courseScore)
        }
    }
}
