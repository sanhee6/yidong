package com.example.yidong222.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yidong222.R;
import com.example.yidong222.models.Exam;

import java.util.ArrayList;
import java.util.List;

public class CourseDetailExamAdapter extends RecyclerView.Adapter<CourseDetailExamAdapter.ViewHolder> {

    private List<Exam> exams;
    private OnItemClickListener listener;

    public CourseDetailExamAdapter() {
        this.exams = new ArrayList<>();
    }

    public void setExams(List<Exam> exams) {
        this.exams = exams;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exam exam = exams.get(position);
        holder.tvTitle.setText(exam.getTitle());
        holder.tvDate.setText(exam.getDate() + " " + exam.getTime());
        holder.tvLocation.setText(exam.getLocation());
        holder.tvType.setText(exam.getStatus());

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(exam);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLocation, tvType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvExamTitle);
            tvDate = itemView.findViewById(R.id.tvExamDate);
            tvLocation = itemView.findViewById(R.id.tvExamLocation);
            tvType = itemView.findViewById(R.id.tvExamType);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Exam exam);
    }
}