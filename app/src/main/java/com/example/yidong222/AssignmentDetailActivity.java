package com.example.yidong222;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.yidong222.data.DataManager;
import com.example.yidong222.data.db.AppDatabase;
import com.example.yidong222.data.db.entity.AssignmentEntity;
import com.example.yidong222.models.Assignment;
import com.example.yidong222.models.AssignmentDto;
import com.google.android.material.button.MaterialButton;

public class AssignmentDetailActivity extends AppCompatActivity {
    private static final String TAG = "AssignmentDetailActivity";

    private Assignment assignment;
    private int position;

    private TextView tvTitle;
    private TextView tvCourse;
    private TextView tvDeadline;
    private TextView tvDescription;
    private CheckBox cbCompleted;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_detail);

        // 获取传递的作业对象
        assignment = (Assignment) getIntent().getSerializableExtra("assignment");
        position = getIntent().getIntExtra("position", -1);

        if (assignment == null) {
            Log.e(TAG, "没有接收到作业数据，关闭页面");
            Toast.makeText(this, "加载作业数据失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        initViews();

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("作业详情");

        // 加载作业数据
        loadAssignmentData();

        // 设置按钮点击事件
        setButtonListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvAssignmentTitle);
        tvCourse = findViewById(R.id.tvAssignmentCourse);
        tvDeadline = findViewById(R.id.tvAssignmentDeadline);
        tvDescription = findViewById(R.id.tvAssignmentDescription);
        cbCompleted = findViewById(R.id.cbAssignmentCompleted);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadAssignmentData() {
        tvTitle.setText(assignment.getTitle());
        tvCourse.setText(assignment.getCourseName());
        tvDeadline.setText(assignment.getDeadline());
        tvDescription.setText(assignment.getDescription());
        cbCompleted.setChecked(assignment.isCompleted());
    }

    private void setButtonListeners() {
        btnEdit.setOnClickListener(v -> {
            // 打开编辑页面或对话框
            Toast.makeText(this, "编辑功能待实现", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> {
            deleteAssignment();
        });

        cbCompleted.setOnClickListener(v -> {
            boolean isChecked = cbCompleted.isChecked();
            updateAssignmentStatus(isChecked);
        });
    }

    private void updateAssignmentStatus(boolean isCompleted) {
        // 更新作业状态
        assignment.setCompleted(isCompleted);

        // 判断是否为临时ID
        if (assignment.isTempId()) {
            Log.d(TAG, "更新临时作业状态：" + assignment.getId());

            try {
                // 更新本地数据库
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // 检查记录是否存在
                AssignmentEntity existingEntity = db.assignmentDao().getAssignmentById(assignment.getId());

                if (existingEntity != null) {
                    // 更新状态
                    existingEntity.status = assignment.getStatus();

                    // 更新记录
                    db.assignmentDao().update(existingEntity);
                    Log.d(TAG, "成功更新本地数据库中的临时作业状态，ID: " + assignment.getId() +
                            ", 新状态: " + existingEntity.status);

                    Toast.makeText(this,
                            isCompleted ? "作业已标记为完成" : "作业已标记为未完成",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "本地数据库中未找到临时作业，ID: " + assignment.getId());

                    // 创建新记录
                    AssignmentEntity entity = new AssignmentEntity();
                    entity.id = assignment.getId();
                    entity.courseId = assignment.getCourseId();
                    entity.title = assignment.getTitle();
                    entity.description = assignment.getDescription();
                    entity.dueDate = assignment.getDeadline();
                    entity.status = assignment.getStatus();

                    // 插入记录
                    db.assignmentDao().insert(entity);
                    Log.d(TAG, "临时作业不存在，已创建新记录并更新状态，ID: " + assignment.getId());

                    Toast.makeText(this,
                            isCompleted ? "作业已标记为完成" : "作业已标记为未完成",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "更新本地数据库中的临时作业状态失败", e);
                Toast.makeText(this, "更新作业状态失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // 真实ID发送到服务器更新
            updateAssignmentStatusOnServer();
        }
    }

    private void updateAssignmentStatusOnServer() {
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 创建DTO对象
        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setId(assignment.getId());
        assignmentDto.setCourseId(assignment.getCourseId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setDescription(assignment.getDescription());
        assignmentDto.setDeadline(assignment.getDeadline());
        assignmentDto.setCourseName(assignment.getCourseName());
        assignmentDto.setCompleted(assignment.isCompleted());

        // 记录请求参数
        Log.d(TAG, "发送更新作业状态请求，ID: " + assignment.getId() +
                ", 新状态: " + assignment.getStatus());

        // 发送请求
        DataManager.updateAssignment(assignment.getId(), assignmentDto, new DataManager.DetailCallback<Assignment>() {
            @Override
            public void onSuccess(Assignment updatedAssignment) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 更新本地对象
                assignment = updatedAssignment;

                // 显示成功消息
                Toast.makeText(AssignmentDetailActivity.this,
                        assignment.isCompleted() ? "作业已标记为完成" : "作业已标记为未完成",
                        Toast.LENGTH_SHORT).show();

                // 同步到本地数据库
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                    // 检查记录是否存在
                    AssignmentEntity existingEntity = db.assignmentDao().getAssignmentById(updatedAssignment.getId());

                    // 创建或更新作业实体
                    AssignmentEntity entity = new AssignmentEntity();
                    entity.id = updatedAssignment.getId();
                    entity.courseId = updatedAssignment.getCourseId();
                    entity.title = updatedAssignment.getTitle();
                    entity.description = updatedAssignment.getDescription();
                    entity.dueDate = updatedAssignment.getDeadline();
                    entity.status = updatedAssignment.getStatus();

                    // 插入或更新
                    db.assignmentDao().insertOrUpdate(entity);
                    Log.d(TAG, "服务器更新状态成功后，同步更新本地数据库，ID: " + updatedAssignment.getId() +
                            ", 操作: " + (existingEntity == null ? "插入新记录" : "更新已有记录"));
                } catch (Exception e) {
                    Log.e(TAG, "服务器更新状态成功后，同步更新本地数据库失败", e);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 显示错误消息
                Toast.makeText(AssignmentDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "更新作业状态失败: " + errorMessage);

                // 尝试更新本地数据库
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                    // 检查记录是否存在
                    AssignmentEntity existingEntity = db.assignmentDao().getAssignmentById(assignment.getId());

                    // 创建或更新作业实体
                    AssignmentEntity entity = new AssignmentEntity();
                    entity.id = assignment.getId();
                    entity.courseId = assignment.getCourseId();
                    entity.title = assignment.getTitle();
                    entity.description = assignment.getDescription();
                    entity.dueDate = assignment.getDeadline();
                    entity.status = assignment.getStatus();

                    // 插入或更新
                    db.assignmentDao().insertOrUpdate(entity);
                    Log.d(TAG, "服务器更新状态失败，但成功更新本地数据库，ID: " + assignment.getId() +
                            ", 操作: " + (existingEntity == null ? "插入新记录" : "更新已有记录"));

                    Toast.makeText(AssignmentDetailActivity.this, "已保存到本地", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "服务器更新状态失败，更新本地数据库也失败", e);
                }
            }
        });
    }

    private void deleteAssignment() {
        // 判断是否为临时ID
        if (assignment.isTempId()) {
            Log.d(TAG, "删除临时作业：" + assignment.getId());

            try {
                // 直接从本地数据库删除
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                // 首先尝试通过ID直接删除
                int rowsDeleted = db.assignmentDao().deleteById(assignment.getId());
                if (rowsDeleted > 0) {
                    Log.d(TAG, "成功通过ID从本地数据库删除作业，ID: " + assignment.getId() + ", 删除行数: " + rowsDeleted);
                } else {
                    // 如果通过ID删除失败，尝试通过实体删除
                    AssignmentEntity assignmentEntity = db.assignmentDao().getAssignmentById(assignment.getId());
                    if (assignmentEntity != null) {
                        db.assignmentDao().delete(assignmentEntity);
                        Log.d(TAG, "成功通过实体从本地数据库删除临时作业，ID: " + assignment.getId());
                    } else {
                        Log.d(TAG, "本地数据库中未找到临时作业，ID: " + assignment.getId());
                    }
                }

                // 返回结果
                Intent resultIntent = new Intent();
                resultIntent.putExtra("position", position);
                resultIntent.putExtra("action", "delete");
                setResult(RESULT_OK, resultIntent);

                // 关闭页面
                Toast.makeText(this, "作业已删除", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Log.e(TAG, "从本地数据库删除临时作业失败", e);
                Toast.makeText(this, "删除作业失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // 真实ID发送到服务器删除
            deleteAssignmentFromServer();
        }
    }

    private void deleteAssignmentFromServer() {
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 记录请求信息
        Log.d(TAG, "发送删除作业请求，ID: " + assignment.getId());

        // 发送请求
        DataManager.deleteAssignment(assignment.getId(), new DataManager.DetailCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 从本地数据库删除
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    int rowsDeleted = db.assignmentDao().deleteById(assignment.getId());
                    Log.d(TAG, "服务器删除成功后，从本地数据库删除作业，ID: " + assignment.getId() + ", 删除行数: " + rowsDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "服务器删除成功后，从本地数据库删除作业失败", e);
                }

                // 返回结果
                Intent resultIntent = new Intent();
                resultIntent.putExtra("position", position);
                resultIntent.putExtra("action", "delete");
                setResult(RESULT_OK, resultIntent);

                // 关闭页面
                Toast.makeText(AssignmentDetailActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 显示错误消息
                Toast.makeText(AssignmentDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "删除作业失败: " + errorMessage);

                // 尝试从本地数据库删除
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    int rowsDeleted = db.assignmentDao().deleteById(assignment.getId());

                    if (rowsDeleted > 0) {
                        Log.d(TAG, "服务器删除失败，但成功从本地数据库删除作业，ID: " + assignment.getId() + ", 删除行数: " + rowsDeleted);

                        // 返回结果
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("position", position);
                        resultIntent.putExtra("action", "delete");
                        setResult(RESULT_OK, resultIntent);

                        // 关闭页面
                        Toast.makeText(AssignmentDetailActivity.this, "已从本地删除", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.d(TAG, "服务器删除失败，本地数据库中也没有找到作业，ID: " + assignment.getId());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "服务器删除失败，从本地数据库删除也失败", e);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 返回结果
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updatedAssignment", assignment);
        resultIntent.putExtra("position", position);
        setResult(RESULT_OK, resultIntent);

        super.onBackPressed();
    }
}