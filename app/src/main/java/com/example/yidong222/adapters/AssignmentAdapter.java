package com.example.yidong222.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yidong222.R;
import com.example.yidong222.models.Assignment;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private List<Assignment> assignmentList;
    private AssignmentItemClickListener listener;

    public AssignmentAdapter(List<Assignment> assignmentList, AssignmentItemClickListener listener) {
        this.assignmentList = assignmentList;
        this.listener = listener;
    }

    public AssignmentAdapter() {
        this.assignmentList = new ArrayList<>();
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignmentList = assignments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment_management, parent,
                false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        Assignment assignment = assignmentList.get(position);
        holder.tvTitle.setText(assignment.getTitle());
        holder.tvCourse.setText(assignment.getCourseName());
        holder.tvDeadline.setText("截止日期: " + assignment.getDeadline());
        holder.cbCompleted.setChecked(assignment.isCompleted());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(holder.getAdapterPosition());
            }
        });

        holder.btnMenu.setOnClickListener(v -> {
            showPopupMenu(holder.btnMenu, holder.getAdapterPosition());
        });

        holder.cbCompleted.setOnClickListener(v -> {
            boolean isChecked = holder.cbCompleted.isChecked();
            if (listener != null) {
                listener.onMenuItemClick(holder.getAdapterPosition(), isChecked ? "complete" : "incomplete");
            }
        });
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.inflate(R.menu.menu_assignment_item);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (listener != null) {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit) {
                    listener.onMenuItemClick(position, "edit");
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onMenuItemClick(position, "delete");
                    return true;
                } else if (itemId == R.id.action_complete) {
                    listener.onMenuItemClick(position, "complete");
                    return true;
                }
            }
            return false;
        });

        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return assignmentList.size();
    }

    public static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCourse, tvDeadline;
        ImageButton btnMenu;
        CheckBox cbCompleted;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvCourse = itemView.findViewById(R.id.tvAssignmentCourse);
            tvDeadline = itemView.findViewById(R.id.tvAssignmentDeadline);
            btnMenu = itemView.findViewById(R.id.btnAssignmentMenu);
            cbCompleted = itemView.findViewById(R.id.cbAssignmentCompleted);
        }
    }

    public interface AssignmentItemClickListener {
        void onItemClick(int position);

        void onMenuItemClick(int position, String action);
    }
}