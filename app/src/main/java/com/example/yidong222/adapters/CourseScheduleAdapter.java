package com.example.yidong222.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yidong222.R;
import com.example.yidong222.models.CourseSchedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CourseScheduleAdapter extends RecyclerView.Adapter<CourseScheduleAdapter.ViewHolder> {
    private final Context context;
    private final List<CourseSchedule> courseList;
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private final SimpleDateFormat dateFormat;

    // 多选模式相关
    private boolean multiSelectMode = false;
    private final Set<Integer> selectedItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(CourseSchedule course, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(CourseSchedule course, int position);
    }

    public CourseScheduleAdapter(Context context, List<CourseSchedule> courseList) {
        this.context = context;
        this.courseList = courseList;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseSchedule course = courseList.get(position);

        holder.tvCourseName.setText(course.getCourseName());
        holder.tvTeacherName.setText(course.getTeacherName());
        holder.tvClassTime.setText(course.getClassTime());
        holder.tvClassroom.setText(course.getClassroom());

        if (course.getCreatedAt() != null) {
            holder.tvCreatedAt.setText(dateFormat.format(course.getCreatedAt()));
        }

        if (course.getUpdatedAt() != null) {
            holder.tvUpdatedAt.setText(dateFormat.format(course.getUpdatedAt()));
        }

        // 多选模式UI处理
        if (multiSelectMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.checkBox.setChecked(selectedItems.contains(position));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectMode) {
                toggleSelection(position);
            } else if (onItemClickListener != null) {
                onItemClickListener.onItemClick(course, position);
            }
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(position));

        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(course, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    // 设置多选模式
    public void setMultiSelectMode(boolean multiSelectMode) {
        this.multiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    // 获取多选模式状态
    public boolean isMultiSelectMode() {
        return multiSelectMode;
    }

    // 切换选中状态
    public void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyItemChanged(position);
    }

    // 获取所有选中的项目
    public List<CourseSchedule> getSelectedItems() {
        List<CourseSchedule> selected = new ArrayList<>();
        for (Integer position : selectedItems) {
            if (position >= 0 && position < courseList.size()) {
                selected.add(courseList.get(position));
            }
        }
        return selected;
    }

    // 清除选择
    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // 全选
    public void selectAll() {
        selectedItems.clear();
        for (int i = 0; i < courseList.size(); i++) {
            selectedItems.add(i);
        }
        notifyDataSetChanged();
    }

    // 获取选中的数量
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName;
        TextView tvTeacherName;
        TextView tvClassTime;
        TextView tvClassroom;
        TextView tvCreatedAt;
        TextView tvUpdatedAt;
        ImageButton btnDelete;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name);
            tvClassTime = itemView.findViewById(R.id.tv_class_time);
            tvClassroom = itemView.findViewById(R.id.tv_classroom);
            tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
            tvUpdatedAt = itemView.findViewById(R.id.tv_updated_at);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}