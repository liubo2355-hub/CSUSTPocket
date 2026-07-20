package com.csust.pocket.feature.common.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.csust.pocket.databinding.ScoreItemCourseBinding
import com.csust.pocket.feature.common.ui.adapter.model.CourseScore
import com.csust.pocket.feature.common.ui.adapter.vh.CourseViewHolder

class CourseAdapter(
    private val context: Context,
    private val onCourseClick: (CourseScore) -> Unit
) : ListAdapter<CourseScore, CourseViewHolder>(CourseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ScoreItemCourseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CourseViewHolder(binding, context, onCourseClick)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class CourseDiffCallback : DiffUtil.ItemCallback<CourseScore>() {
    override fun areItemsTheSame(oldItem: CourseScore, newItem: CourseScore): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: CourseScore, newItem: CourseScore): Boolean {
        return oldItem == newItem
    }
}
