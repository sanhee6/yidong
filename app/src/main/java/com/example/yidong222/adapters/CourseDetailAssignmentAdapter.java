package com.example.yidong222.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yidong222.R;
import com.example.yidong222.models.Assignment;

import java.util.ArrayList;
import java.util.List;

public class CourseDetailAssignmentAdapter extends RecyclerView.Adapter<CourseDetailAssignmentAdapter.ViewHolder> {

    private List<Assignment> assignments;
    private OnItemClickListener listener;

    public CourseDetailAssignmentAdapter() {
        this.assignments = new ArrayList<>();
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = assignments.get(position);
        holder.tvTitle.setText(assignment.getTitle());
        holder.tvDate.setText(assignment.getDeadline());
        holder.tvStatus.setText(assignment.getStatus());

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(assignment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvDate = itemView.findViewById(R.id.tvAssignmentDate);
            tvStatus = itemView.findViewById(R.id.tvAssignmentStatus);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Assignment assignment);
    }
}