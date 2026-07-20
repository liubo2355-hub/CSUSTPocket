package com.csust.pocket.feature.common.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csust.pocket.R
import com.csust.pocket.feature.common.ui.adapter.model.Exam
import java.text.SimpleDateFormat
import java.util.Locale

class ExamArrangementAdapter(var examData: MutableList<Exam>) : RecyclerView.Adapter<ExamArrangementAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val examTitle: TextView = view.findViewById(R.id.exam_title)
        val examStatus: TextView = view.findViewById(R.id.exam_status)
        val examTime: TextView = view.findViewById(R.id.exam_time)
        val examPlace: TextView = view.findViewById(R.id.exam_place)
        val examRoom: TextView = view.findViewById(R.id.exam_room)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.exam_arrangement_item, parent, false)
        return ViewHolder(view)
    }

    fun updateData(newExamArrange: MutableList<Exam>) {
        examData = newExamArrange
    }

    override fun getItemCount() = examData.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exam = examData[position]
        holder.examTitle.text = exam.name
        holder.examPlace.text = exam.place
        holder.examRoom.text = exam.room
        holder.examTime.text = exam.time

        // 已考完的考试：标「已结束」并整卡置灰（用 alpha 而非改文字色，避免换肤框架覆盖）
        val finished = isExamFinished(exam.time)
        holder.examStatus.visibility = if (finished) View.VISIBLE else View.GONE
        holder.itemView.alpha = if (finished) 0.45f else 1.0f
    }

    /** time 形如 "2024-11-19 14:05~15:15"；取结束时刻与当前比较。无法解析则视为未结束。 */
    private fun isExamFinished(time: String): Boolean {
        return try {
            val parts = time.trim().split(" ", limit = 2)
            if (parts.size < 2) return false
            val date = parts[0].trim()
            val range = parts[1].trim()
            val endClock = if (range.contains("~")) range.substringAfter("~").trim() else range
            val end = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).parse("$date $endClock") ?: return false
            end.time < System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }
}
