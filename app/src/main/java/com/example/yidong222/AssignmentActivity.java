package com.example.yidong222;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.yidong222.adapters.AssignmentAdapter;
import com.example.yidong222.data.DataManager;
import com.example.yidong222.data.MockDataProvider;
import com.example.yidong222.data.db.AppDatabase;
import com.example.yidong222.data.db.entity.AssignmentEntity;
import com.example.yidong222.models.Assignment;
import com.example.yidong222.models.AssignmentDto;
import com.example.yidong222.models.Course;
import com.example.yidong222.models.CourseDto;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class AssignmentActivity extends AppCompatActivity implements AssignmentAdapter.AssignmentItemClickListener {

    private static final String TAG = "AssignmentActivity";
    private RecyclerView recyclerView;
    private AssignmentAdapter adapter;
    private List<Assignment> assignmentList = new ArrayList<>();
    private FloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private View emptyView;
    private boolean isOfflineMode = false;
    private View progressBar;
    private static final int REQUEST_CODE_ASSIGNMENT_DETAIL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);

        try {
            // 打印数据库文件信息
            File dbFile = getDatabasePath("course_db");
            Log.d(TAG, "数据库文件路径: " + dbFile.getAbsolutePath());
            Log.d(TAG, "数据库文件是否存在: " + dbFile.exists());

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("作业管理");

            recyclerView = findViewById(R.id.recyclerViewAssignments);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            emptyView = findViewById(R.id.tvEmptyAssignments);

            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(this::loadAssignmentsFromServer);

            adapter = new AssignmentAdapter(assignmentList, this);
            recyclerView.setAdapter(adapter);

            fabAdd = findViewById(R.id.fabAddAssignment);
            fabAdd.setOnClickListener(v -> showAddAssignmentDialog());

            progressBar = findViewById(R.id.progressBar);

            // 首先加载课程数据
            loadCourseData();

            // 从服务器加载作业数据
            loadAssignmentsFromServer();
        } catch (Exception e) {
            Log.e(TAG, "初始化失败", e);
            Toast.makeText(this, "应用初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 页面恢复时刷新课程数据
        loadCourseData();

        // 如果处于离线模式，尝试重新加载数据
        if (isOfflineMode) {
            loadAssignmentsFromServer();
        }
    }

    private void loadAssignmentsFromServer() {
        swipeRefreshLayout.setRefreshing(true);
        Log.d(TAG, "开始从服务器加载作业数据");

        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Log.d(TAG, "无网络连接，使用离线模式");
            Toast.makeText(this, "无网络连接，显示本地数据", Toast.LENGTH_SHORT).show();
            isOfflineMode = true;
            loadAssignmentsFromLocalDatabase();
            return;
        }

        // 确保DataManager已初始化
        try {
            DataManager.init(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "DataManager初始化失败", e);
        }

        // 从服务器获取作业列表
        DataManager.getAssignments(new DataManager.DataCallback<Assignment>() {
            @Override
            public void onSuccess(List<Assignment> data) {
                Log.d(TAG, "成功获取作业列表: " + data.size() + " 条记录");
                runOnUiThread(() -> {
                    assignmentList.clear();
                    assignmentList.addAll(data);
                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    updateEmptyView();

                    // 同步更新本地数据库
                    syncAssignmentsToLocalDatabase(data);
                });
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "获取作业列表失败: " + message);
                runOnUiThread(() -> {
                    Toast.makeText(AssignmentActivity.this, "获取作业信息失败，使用本地数据", Toast.LENGTH_SHORT).show();
                    isOfflineMode = true;
                    loadAssignmentsFromLocalDatabase();
                });
            }
        });
    }

    /**
     * 从本地数据库加载作业
     */
    private void loadAssignmentsFromLocalDatabase() {
        Log.d(TAG, "从本地数据库加载作业数据");
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<AssignmentEntity> assignmentEntities = db.assignmentDao().getAllAssignments();

            Log.d(TAG, "本地数据库中找到 " + assignmentEntities.size() + " 条作业记录");

            // 如果本地数据库中有数据，使用本地数据
            if (!assignmentEntities.isEmpty()) {
                assignmentList.clear();

                // 将实体转换为模型对象
                for (AssignmentEntity entity : assignmentEntities) {
                    Assignment assignment = new Assignment(
                            entity.title,
                            "课程ID: " + entity.courseId, // 临时显示，实际应该查询课程名称
                            entity.dueDate,
                            entity.description,
                            true);
                    assignment.setId(entity.id);
                    assignment.setStatus(entity.status);
                    assignment.setCourseId(entity.courseId); // 确保设置courseId

                    Log.d(TAG, "从数据库加载作业: ID=" + entity.id + ", courseId=" + entity.courseId +
                            ", title=" + entity.title + ", status=" + entity.status);

                    assignmentList.add(assignment);
                }

                adapter.notifyDataSetChanged();
                updateEmptyView();
                Log.d(TAG, "成功从本地数据库加载作业数据");
            } else {
                Log.d(TAG, "本地数据库中没有作业数据，使用模拟数据");
                loadOfflineData(); // 使用模拟数据
            }
        } catch (Exception e) {
            Log.e(TAG, "从本地数据库加载作业失败", e);
            loadOfflineData(); // 出错时使用模拟数据
        } finally {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * 将作业列表同步到本地数据库
     */
    private void syncAssignmentsToLocalDatabase(List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            Log.w(TAG, "没有作业数据需要同步到数据库");
            return;
        }

        Log.d(TAG, "开始将 " + assignments.size() + " 条作业同步到本地数据库");
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 清空旧数据
            db.assignmentDao().deleteAll();
            Log.d(TAG, "已清空本地作业数据");

            for (Assignment assignment : assignments) {
                AssignmentEntity entity = new AssignmentEntity();
                entity.id = assignment.getId();

                // 确保courseId设置正确
                int courseId = assignment.getCourseId();
                if (courseId <= 0) {
                    // 如果courseId无效，尝试从courseName中提取
                    String courseName = assignment.getCourseName();
                    if (courseName != null && courseName.contains(":")) {
                        try {
                            String idStr = courseName.split(":")[1].trim();
                            courseId = Integer.parseInt(idStr);
                        } catch (Exception e) {
                            Log.w(TAG, "无法从课程名称中提取ID: " + e.getMessage());
                            courseId = 1; // 默认课程ID
                        }
                    } else {
                        // 根据课程名称匹配ID
                        List<CourseDto> courseDtos = CourseManagementActivity.getSharedCourseDtoList();
                        for (CourseDto dto : courseDtos) {
                            if (dto.getName() != null && dto.getName().equals(courseName)) {
                                courseId = dto.getId();
                                break;
                            }
                        }

                        if (courseId <= 0) {
                            courseId = 1; // 默认课程ID
                        }
                    }

                    // 更新作业对象的courseId
                    assignment.setCourseId(courseId);
                }

                entity.courseId = courseId;
                entity.title = assignment.getTitle();
                entity.description = assignment.getDescription();
                entity.dueDate = assignment.getDeadline();
                entity.status = assignment.getStatus();

                // 插入或更新
                db.assignmentDao().insertOrUpdate(entity);
                Log.d(TAG, "同步作业到数据库: ID=" + entity.id + ", courseId=" + entity.courseId +
                        ", title=" + entity.title + ", status=" + entity.status);
            }

            Log.d(TAG, "成功同步作业数据到本地数据库");
        } catch (Exception e) {
            Log.e(TAG, "同步作业数据到本地数据库失败", e);
        }
    }

    private void loadOfflineData() {
        // 加载本地模拟数据
        assignmentList.clear();
        assignmentList.addAll(MockDataProvider.getMockAssignments());
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (assignmentList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * 显示添加作业对话框
     */
    private void showAddAssignmentDialog() {
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_assignment, null);

        // 获取控件
        TextInputEditText etTitle = dialogView.findViewById(R.id.etAssignmentTitle);
        Spinner spinnerCourse = dialogView.findViewById(R.id.spinnerCourse);
        TextInputEditText etDeadline = dialogView.findViewById(R.id.etAssignmentDeadline);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etAssignmentDescription);
        TextInputEditText etMaxScore = dialogView.findViewById(R.id.etAssignmentMaxScore);

        // 设置课程下拉菜单
        setupCourseSpinner(spinnerCourse);

        // 设置日期选择器
        etDeadline.setFocusable(false);
        etDeadline.setOnClickListener(v -> {
            try {
                // 获取当前日期
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // 创建日期选择器
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        AssignmentActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            try {
                                // 设置时间选择器
                                TimePickerDialog timePickerDialog = new TimePickerDialog(
                                        AssignmentActivity.this,
                                        (timeView, hourOfDay, minute) -> {
                                            // 设置选择的日期和时间
                                            String formattedDate = String.format(Locale.getDefault(),
                                                    "%04d-%02d-%02d %02d:%02d:00",
                                                    selectedYear, selectedMonth + 1, selectedDay,
                                                    hourOfDay, minute);
                                            etDeadline.setText(formattedDate);
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true);
                                timePickerDialog.show();
                            } catch (Exception e) {
                                Log.e(TAG, "显示时间选择器失败: " + e.getMessage());
                                Toast.makeText(AssignmentActivity.this, "无法显示时间选择器", Toast.LENGTH_SHORT).show();
                            }
                        },
                        year, month, day);
                datePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示日期选择器失败: " + e.getMessage());
                Toast.makeText(AssignmentActivity.this, "无法显示日期选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 创建对话框
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("添加作业")
                .setView(dialogView)
                .setPositiveButton("添加", null)
                .setNegativeButton("取消", null)
                .create();

        // 显示对话框
        dialog.show();

        // 设置添加按钮点击事件
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            // 获取输入值
            String title = etTitle.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            // 获取选中的课程
            CourseDto selectedCourse = getSelectedCourse(spinnerCourse);

            // 获取最高分
            int maxScore = 100; // 默认值
            try {
                String maxScoreText = etMaxScore.getText().toString().trim();
                if (!maxScoreText.isEmpty()) {
                    maxScore = Integer.parseInt(maxScoreText);
                    if (maxScore <= 0) {
                        etMaxScore.setError("最高分必须大于0");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                etMaxScore.setError("请输入有效的分数");
                return;
            }

            // 验证输入
            if (title.isEmpty()) {
                etTitle.setError("标题不能为空");
                return;
            }

            if (selectedCourse == null) {
                Toast.makeText(this, "请选择课程", Toast.LENGTH_SHORT).show();
                return;
            }

            if (deadline.isEmpty()) {
                etDeadline.setError("截止日期不能为空");
                return;
            }

            // 验证日期格式
            try {
                SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                apiDateFormat.parse(deadline);
            } catch (ParseException e) {
                etDeadline.setError("日期格式无效，应为：YYYY-MM-DD HH:MM:SS");
                return;
            }

            // 创建临时作业对象
            Assignment assignment = new Assignment(title, selectedCourse.getName(), deadline, description, true);
            assignment.setCourseId(selectedCourse.getId());
            int tempId = assignment.getId();

            Log.d(TAG,
                    "创建临时作业：ID=" + tempId + ", title=" + title + ", course=" + selectedCourse.getName() + ", courseId="
                            + selectedCourse.getId()
                            + ", deadline=" + deadline + ", maxScore=" + maxScore);

            // 添加到列表
            assignmentList.add(0, assignment);
            adapter.notifyItemInserted(0);
            updateEmptyView();

            // 保存到本地数据库
            new Thread(() -> {
                try {
                    // 捕获需要的变量
                    final int assignmentId = tempId;
                    final String assignmentTitle = title;
                    final int assignmentCourseId = selectedCourse.getId();
                    final String assignmentDescription = description;
                    final String assignmentDeadline = deadline;
                    final String assignmentStatus = assignment.getStatus();

                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    AssignmentEntity entity = new AssignmentEntity();
                    entity.id = assignmentId;
                    entity.title = assignmentTitle;
                    entity.courseId = assignmentCourseId;
                    entity.description = assignmentDescription;
                    entity.dueDate = assignmentDeadline;
                    entity.status = assignmentStatus;
                    db.assignmentDao().insert(entity);
                } catch (Exception e) {
                    Log.e(TAG, "保存作业到本地数据库失败", e);
                }
            }).start();

            // 创建AssignmentDto并同步到服务器
            AssignmentDto dto = new AssignmentDto();
            dto.setTitle(title);
            dto.setCourseId(selectedCourse.getId());
            dto.setCourseName(selectedCourse.getName());
            dto.setDeadline(deadline);
            dto.setDescription(description);
            dto.setMaxScore(maxScore); // 使用用户输入的最高分

            // 调用同步方法
            DataManager.createAssignment(dto, new DataManager.DetailCallback<Assignment>() {
                @Override
                public void onSuccess(Assignment result) {
                    Log.d(TAG, "作业创建成功，服务器ID: " + result.getId());
                    runOnUiThread(() -> {
                        Toast.makeText(AssignmentActivity.this, "作业创建成功", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e(TAG, "作业创建失败: " + errorMsg);
                    runOnUiThread(() -> {
                        Toast.makeText(AssignmentActivity.this, "作业创建失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }
            });

            // 关闭对话框
            dialog.dismiss();
        });
    }

    /**
     * 设置课程下拉菜单
     */
    private void setupCourseSpinner(Spinner spinner) {
        // 显示加载中
        List<String> loadingList = new ArrayList<>();
        loadingList.add("正在加载课程...");
        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, loadingList);
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(loadingAdapter);

        // 获取共享的课程列表
        List<String> courseNames = CourseManagementActivity.getSharedCourseNames();
        List<CourseDto> courseDtos = CourseManagementActivity.getSharedCourseDtoList();

        Log.d(TAG, "共享课程列表: " + courseNames.toString());
        Log.d(TAG, "共享课程DTO列表大小: " + courseDtos.size());

        // 检查是否有可用的课程数据
        if (courseNames.isEmpty() || courseDtos.isEmpty() || courseNames.get(0).equals("暂无可选课程")) {
            Log.e(TAG, "没有可用的课程数据");

            // 尝试主动加载一次课程数据
            loadCourseData();

            // 设置一个默认的列表，以便用户可以手动刷新
            List<CourseSpinnerItem> defaultItems = new ArrayList<>();
            defaultItems.add(new CourseSpinnerItem(null, "-- 请选择课程 --"));
            defaultItems.add(new CourseSpinnerItem(null, "无可用课程"));

            ArrayAdapter<CourseSpinnerItem> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, defaultItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            return;
        }

        try {
            // 创建下拉菜单项
            List<CourseSpinnerItem> spinnerItems = new ArrayList<>();
            spinnerItems.add(new CourseSpinnerItem(null, "-- 请选择课程 --"));

            // 添加课程选项
            for (CourseDto courseDto : courseDtos) {
                spinnerItems.add(new CourseSpinnerItem(courseDto.getId(), courseDto.getName()));
                Log.d(TAG, "添加课程选项: ID=" + courseDto.getId() + ", 名称=" + courseDto.getName());
            }

            // 创建适配器
            ArrayAdapter<CourseSpinnerItem> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            // 添加选择监听器
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    CourseSpinnerItem selectedItem = (CourseSpinnerItem) parent.getItemAtPosition(position);
                    Log.d(TAG, "选择了课程: ID=" +
                            (selectedItem.getId() != null ? selectedItem.getId() : "null") +
                            ", 名称=" + selectedItem.getName());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "没有选择任何课程");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "设置课程下拉菜单时出错: " + e.getMessage(), e);

            // 出错时设置一个默认的下拉菜单
            List<String> errorList = new ArrayList<>();
            errorList.add("加载课程失败");
            ArrayAdapter<String> errorAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, errorList);
            errorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(errorAdapter);
        }
    }

    /**
     * 课程下拉菜单项
     */
    private static class CourseSpinnerItem {
        private final Integer id;
        private final String name;

        public CourseSpinnerItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void showEditDialog(Assignment assignment, int position) {
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_assignment, null);

        // 获取输入框
        TextInputEditText etTitle = dialogView.findViewById(R.id.etAssignmentTitle);
        Spinner spinnerCourse = dialogView.findViewById(R.id.spinnerCourse);
        TextInputEditText etDeadline = dialogView.findViewById(R.id.etAssignmentDeadline);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etAssignmentDescription);
        TextInputEditText etMaxScore = dialogView.findViewById(R.id.etAssignmentMaxScore);

        // 设置初始值
        etTitle.setText(assignment.getTitle());
        etDescription.setText(assignment.getDescription());
        etMaxScore.setText("100"); // 默认值

        // 设置课程下拉菜单并选中当前课程
        setupCourseSpinner(spinnerCourse);
        // 选中当前课程
        String currentCourseName = assignment.getCourseName();
        if (currentCourseName != null && !currentCourseName.isEmpty()) {
            for (int i = 0; i < spinnerCourse.getAdapter().getCount(); i++) {
                Object item = spinnerCourse.getAdapter().getItem(i);
                if (item instanceof CourseSpinnerItem) {
                    CourseSpinnerItem courseItem = (CourseSpinnerItem) item;
                    if (currentCourseName.equals(courseItem.getName())) {
                        spinnerCourse.setSelection(i);
                        break;
                    }
                } else if (item instanceof String && currentCourseName.equals(item)) {
                    spinnerCourse.setSelection(i);
                    break;
                }
            }
        }

        // 确保截止日期不为空
        String deadline = assignment.getDeadline();
        if (deadline == null || deadline.trim().isEmpty()) {
            // 如果截止日期为空，使用当前日期时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            deadline = sdf.format(new Date());
            Log.w(TAG, "作业截止日期为空，使用当前时间: " + deadline);
            assignment.setDeadline(deadline);
        }
        etDeadline.setText(deadline);

        // 设置日期选择器
        etDeadline.setFocusable(false);
        etDeadline.setOnClickListener(v -> {
            try {
                // 获取当前日期
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // 创建日期选择器
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        AssignmentActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            try {
                                // 设置时间选择器
                                TimePickerDialog timePickerDialog = new TimePickerDialog(
                                        AssignmentActivity.this,
                                        (timeView, hourOfDay, minute) -> {
                                            // 设置选择的日期和时间
                                            String formattedDate = String.format(Locale.getDefault(),
                                                    "%04d-%02d-%02d %02d:%02d:00",
                                                    selectedYear, selectedMonth + 1, selectedDay,
                                                    hourOfDay, minute);
                                            etDeadline.setText(formattedDate);
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true);
                                timePickerDialog.show();
                            } catch (Exception e) {
                                Log.e(TAG, "显示时间选择器失败: " + e.getMessage());
                                Toast.makeText(AssignmentActivity.this, "无法显示时间选择器", Toast.LENGTH_SHORT).show();
                            }
                        },
                        year, month, day);
                datePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示日期选择器失败: " + e.getMessage());
                Toast.makeText(AssignmentActivity.this, "无法显示日期选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 创建对话框
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("编辑作业")
                .setView(dialogView)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();

        // 显示对话框
        dialog.show();

        // 设置保存按钮点击事件
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            // 获取输入值
            String title = etTitle.getText().toString().trim();
            String newDeadline = etDeadline.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            // 获取选中的课程
            CourseDto selectedCourse = getSelectedCourse(spinnerCourse);

            // 获取最高分
            int maxScore = 100;
            try {
                maxScore = Integer.parseInt(etMaxScore.getText().toString().trim());
                if (maxScore <= 0) {
                    etMaxScore.setError("最高分必须大于0");
                    return;
                }
            } catch (NumberFormatException e) {
                etMaxScore.setError("请输入有效的分数");
                return;
            }

            // 验证输入
            if (title.isEmpty()) {
                etTitle.setError("标题不能为空");
                return;
            }

            if (selectedCourse == null) {
                Toast.makeText(this, "请选择课程", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newDeadline.isEmpty()) {
                etDeadline.setError("截止日期不能为空");
                return;
            }

            // 验证日期格式
            try {
                SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                apiDateFormat.parse(newDeadline);
            } catch (ParseException e) {
                etDeadline.setError("日期格式无效，应为：YYYY-MM-DD HH:MM:SS");
                return;
            }

            // 更新作业对象
            assignment.setTitle(title);
            assignment.setCourseName(selectedCourse.getName());
            assignment.setDeadline(newDeadline);
            assignment.setDescription(description);
            assignment.setCourseId(selectedCourse.getId());

            // 更新列表
            adapter.notifyItemChanged(position);

            // 保存到本地数据库
            new Thread(() -> {
                try {
                    // 捕获需要的变量
                    final int assignmentId = assignment.getId();
                    final String assignmentTitle = title;
                    final int assignmentCourseId = selectedCourse.getId();
                    final String assignmentDescription = description;
                    final String assignmentDeadline = newDeadline;
                    final String assignmentStatus = assignment.getStatus();

                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    AssignmentEntity entity = new AssignmentEntity();
                    entity.id = assignmentId;
                    entity.title = assignmentTitle;
                    entity.courseId = assignmentCourseId;
                    entity.description = assignmentDescription;
                    entity.dueDate = assignmentDeadline;
                    entity.status = assignmentStatus;
                    db.assignmentDao().update(entity);
                } catch (Exception e) {
                    Log.e(TAG, "更新作业到本地数据库失败", e);
                }
            }).start();

            // 创建AssignmentDto并同步到服务器
            AssignmentDto dto = new AssignmentDto();
            dto.setId(assignment.getId());
            dto.setTitle(title);
            dto.setCourseId(selectedCourse.getId());
            dto.setCourseName(selectedCourse.getName());
            dto.setDeadline(newDeadline);
            dto.setDescription(description);
            dto.setMaxScore(maxScore);
            dto.setStatus(assignment.getStatus());
            dto.setCompleted(assignment.isCompleted());

            // 调用同步方法
            DataManager.updateAssignment(assignment.getId(), dto, new DataManager.DetailCallback<Assignment>() {
                @Override
                public void onSuccess(Assignment result) {
                    Log.d(TAG, "作业更新成功，ID: " + result.getId());
                    runOnUiThread(() -> {
                        Toast.makeText(AssignmentActivity.this, "作业更新成功", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e(TAG, "作业更新失败: " + errorMsg);
                    runOnUiThread(() -> {
                        Toast.makeText(AssignmentActivity.this, "作业更新失败，已保存到本地: " + errorMsg, Toast.LENGTH_SHORT)
                                .show();
                    });
                }
            });

            // 关闭对话框
            dialog.dismiss();
        });
    }

    private void deleteAssignment(int position) {
        Assignment assignment = assignmentList.get(position);

        // 尝试解析作业ID
        int assignmentId = assignment.getId();

        new MaterialAlertDialogBuilder(this)
                .setTitle("删除作业")
                .setMessage("确定要删除 \"" + assignmentList.get(position).getTitle() + "\" 吗?")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 判断是否为临时ID
                    if (assignment.isTempId()) {
                        Log.d(TAG, "删除临时作业：" + assignmentId);

                        try {
                            // 直接从本地数据库删除
                            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                            // 首先尝试通过ID直接删除
                            int rowsDeleted = db.assignmentDao().deleteById(assignmentId);
                            if (rowsDeleted > 0) {
                                Log.d(TAG, "成功通过ID从本地数据库删除作业，ID: " + assignmentId + ", 删除行数: " + rowsDeleted);
                            } else {
                                // 如果通过ID删除失败，尝试通过实体删除
                                AssignmentEntity assignmentEntity = db.assignmentDao().getAssignmentById(assignmentId);
                                if (assignmentEntity != null) {
                                    db.assignmentDao().delete(assignmentEntity);
                                    Log.d(TAG, "成功通过实体从本地数据库删除临时作业，ID: " + assignmentId);
                                } else {
                                    Log.d(TAG, "本地数据库中未找到临时作业，ID: " + assignmentId);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "从本地数据库删除临时作业失败", e);
                        }

                        // 从列表中移除
                        assignmentList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateEmptyView();
                        Toast.makeText(AssignmentActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();
                    } else {
                        // 从UI先移除，提高响应速度
                        assignmentList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateEmptyView();

                        // 真实ID发送到服务器删除
                        deleteAssignmentFromServer(assignmentId, position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 从服务器删除作业
     * 
     * @param assignmentId 作业ID
     * @param position     位置
     */
    private void deleteAssignmentFromServer(int assignmentId, int position) {
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 记录请求信息
        Log.d(TAG, "发送删除作业请求，ID: " + assignmentId);

        // 发送请求
        DataManager.deleteAssignment(assignmentId, new DataManager.DetailCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 显示成功消息
                Toast.makeText(AssignmentActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();

                // 从本地数据库删除
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    int rowsDeleted = db.assignmentDao().deleteById(assignmentId);
                    Log.d(TAG, "服务器删除成功后，从本地数据库删除作业，ID: " + assignmentId + ", 删除行数: " + rowsDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "服务器删除成功后，从本地数据库删除作业失败", e);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 如果是404错误（作业不存在），我们已经在DataManager中处理为成功
                if (!errorMessage.contains("404")) {
                    // 显示错误消息
                    Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "删除作业失败: " + errorMessage);

                    // 如果删除失败，需要恢复UI状态
                    // 但是position可能已经不准确，所以最好是重新加载数据
                    loadAssignmentsFromServer();
                } else {
                    // 404错误会被视为成功，所以不需要恢复UI
                    Toast.makeText(AssignmentActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();

                    // 尝试从本地数据库删除
                    try {
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        int rowsDeleted = db.assignmentDao().deleteById(assignmentId);
                        Log.d(TAG, "服务器返回404，从本地数据库删除作业，ID: " + assignmentId + ", 删除行数: " + rowsDeleted);
                    } catch (Exception e) {
                        Log.e(TAG, "服务器返回404，从本地数据库删除作业失败", e);
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 更新作业
     * 
     * @param position 位置
     */
    private void updateAssignment(int position) {
        Assignment assignment = assignmentList.get(position);
        Log.d(TAG, "更新作业：" + assignment.getId());

        // 无论是临时ID还是真实ID，都打开编辑对话框
        showEditDialog(assignment, position);
    }

    @Override
    public void onItemClick(int position) {
        Assignment assignment = assignmentList.get(position);
        Log.d(TAG, "点击作业项: ID=" + assignment.getId() + ", title=" + assignment.getTitle());

        // 创建Intent
        Intent intent = new Intent(this, AssignmentDetailActivity.class);

        // 传递作业对象
        intent.putExtra("assignment", assignment);
        intent.putExtra("position", position);

        // 启动详情页
        startActivityForResult(intent, REQUEST_CODE_ASSIGNMENT_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ASSIGNMENT_DETAIL && resultCode == RESULT_OK && data != null) {
            // 获取更新后的作业
            Assignment updatedAssignment = (Assignment) data.getSerializableExtra("updatedAssignment");
            int position = data.getIntExtra("position", -1);

            if (updatedAssignment != null && position != -1) {
                // 更新列表
                assignmentList.set(position, updatedAssignment);
                adapter.notifyItemChanged(position);

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
                    Log.d(TAG, "从详情页返回后，同步更新本地数据库，ID: " + updatedAssignment.getId() +
                            ", 操作: " + (existingEntity == null ? "插入新记录" : "更新已有记录"));
                } catch (Exception e) {
                    Log.e(TAG, "从详情页返回后，同步更新本地数据库失败", e);
                }
            }
        }
    }

    @Override
    public void onMenuItemClick(int position, String action) {
        Log.d(TAG, "菜单项点击: position=" + position + ", action=" + action);

        if ("edit".equals(action)) {
            updateAssignment(position);
        } else if ("delete".equals(action)) {
            deleteAssignment(position);
        } else if ("complete".equals(action)) {
            toggleAssignmentStatus(position);
        }
    }

    /**
     * 切换作业状态
     * 
     * @param position 位置
     */
    private void toggleAssignmentStatus(int position) {
        Assignment assignment = assignmentList.get(position);
        boolean newStatus = !assignment.isCompleted();

        Log.d(TAG, "切换作业状态: ID=" + assignment.getId() + ", 当前状态=" + assignment.getStatus() +
                ", 新状态=" + (newStatus ? "完成" : "未完成"));

        // 更新作业状态
        assignment.setCompleted(newStatus);

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

                    // 更新UI
                    adapter.notifyItemChanged(position);
                    Toast.makeText(AssignmentActivity.this,
                            newStatus ? "作业已标记为完成" : "作业已标记为未完成",
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

                    // 更新UI
                    adapter.notifyItemChanged(position);
                    Toast.makeText(AssignmentActivity.this,
                            newStatus ? "作业已标记为完成" : "作业已标记为未完成",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "更新本地数据库中的临时作业状态失败", e);
                Toast.makeText(AssignmentActivity.this, "更新作业状态失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // 真实ID发送到服务器更新
            updateAssignmentStatusOnServer(assignment, position);
        }
    }

    /**
     * 在服务器上更新作业状态
     * 
     * @param assignment 作业
     * @param position   位置
     */
    private void updateAssignmentStatusOnServer(Assignment assignment, int position) {
        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);

        // 创建DTO对象
        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setId(assignment.getId());

        // 确保courseId设置正确
        int courseId = assignment.getCourseId();
        if (courseId <= 0) {
            // 如果courseId无效，尝试从courseName中提取
            String courseName = assignment.getCourseName();
            if (courseName != null && courseName.contains(":")) {
                try {
                    String idStr = courseName.split(":")[1].trim();
                    courseId = Integer.parseInt(idStr);
                } catch (Exception e) {
                    Log.w(TAG, "无法从课程名称中提取ID: " + e.getMessage());
                    courseId = 2; // 默认使用ID=2（计算机网络）
                }
            } else {
                // 根据课程名称设置ID
                if (courseName != null) {
                    if (courseName.contains("计算机网络")) {
                        courseId = 2;
                    } else if (courseName.contains("操作系统")) {
                        courseId = 3;
                    } else if (courseName.contains("数据结构")) {
                        courseId = 1;
                    } else {
                        courseId = 2; // 默认使用ID=2
                    }
                } else {
                    courseId = 2; // 默认使用ID=2
                }
            }
            // 更新作业对象的courseId
            assignment.setCourseId(courseId);
        }

        assignmentDto.setCourseId(courseId);
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setDescription(assignment.getDescription());

        // 处理日期格式，确保不会重复添加时间部分
        String deadline = assignment.getDeadline();

        // 检查deadline是否为null或空
        if (deadline == null || deadline.trim().isEmpty()) {
            // 如果截止日期为null或空，使用当前日期时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            deadline = sdf.format(new Date());
            Log.w(TAG, "作业截止日期为空，使用当前时间: " + deadline);
        } else {
            // 如果已经包含T或Z，说明是ISO格式，需要转换为API要求的格式
            if (deadline.contains("T") || deadline.contains("Z")) {
                try {
                    // 尝试解析ISO格式并转换为API需要的格式
                    String[] parts = deadline.split("T");
                    String date = parts[0]; // YYYY-MM-DD
                    String time = "23:59:59"; // 默认时间
                    if (parts.length > 1 && parts[1].length() >= 8) {
                        time = parts[1].substring(0, 8); // HH:MM:SS
                    }
                    deadline = date + " " + time;
                } catch (Exception e) {
                    Log.e(TAG, "日期格式转换失败", e);
                    // 如果转换失败，使用当前时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    deadline = sdf.format(new Date());
                }
            } else if (!deadline.contains(" ")) {
                // 如果只有日期部分，添加默认时间
                deadline = deadline + " 23:59:59";
            }
        }

        assignmentDto.setDeadline(deadline);
        assignmentDto.setCourseName(assignment.getCourseName());

        // 记录请求参数
        Log.d(TAG, "发送更新作业状态请求，ID: " + assignment.getId() +
                ", courseId: " + assignmentDto.getCourseId() +
                ", 新状态: " + assignment.getStatus());

        // 发送请求
        DataManager.updateAssignment(assignment.getId(), assignmentDto, new DataManager.DetailCallback<Assignment>() {
            @Override
            public void onSuccess(Assignment updatedAssignment) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 更新列表
                assignmentList.set(position, updatedAssignment);
                adapter.notifyItemChanged(position);

                // 显示成功消息
                Toast.makeText(AssignmentActivity.this,
                        updatedAssignment.isCompleted() ? "作业已标记为完成" : "作业已标记为未完成",
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
                    Log.d(TAG, "成功同步更新的作业状态到本地数据库，ID: " + updatedAssignment.getId());
                } catch (Exception e) {
                    Log.e(TAG, "同步更新的作业状态到本地数据库失败", e);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 隐藏进度条
                progressBar.setVisibility(View.GONE);

                // 显示错误消息
                Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "更新作业状态失败: " + errorMessage);

                // 恢复原状态
                assignment.setCompleted(!assignment.isCompleted());
                adapter.notifyItemChanged(position);
            }
        });
    }

    // 获取选中的课程
    private CourseDto getSelectedCourse(Spinner spinner) {
        if (spinner.getSelectedItem() == null) {
            return null;
        }

        try {
            if (spinner.getSelectedItem() instanceof CourseSpinnerItem) {
                CourseSpinnerItem selectedItem = (CourseSpinnerItem) spinner.getSelectedItem();
                Integer courseId = selectedItem.getId();
                String courseName = selectedItem.getName();

                if (courseId == null || "-- 请选择课程 --".equals(courseName)) {
                    Log.d(TAG, "未选择有效课程");
                    return null;
                }

                Log.d(TAG, "获取选中课程: ID=" + courseId + ", 名称=" + courseName);

                // 查找对应的CourseDto
                List<CourseDto> courseDtos = CourseManagementActivity.getSharedCourseDtoList();
                for (CourseDto courseDto : courseDtos) {
                    if (courseId.equals(courseDto.getId())) {
                        return courseDto;
                    }
                }

                // 如果找不到，创建一个新的
                CourseDto courseDto = new CourseDto();
                courseDto.setId(courseId);
                courseDto.setName(courseName);
                return courseDto;
            } else if (spinner.getSelectedItem() instanceof String) {
                String courseName = (String) spinner.getSelectedItem();
                if ("-- 请选择课程 --".equals(courseName) || "无可用课程".equals(courseName)) {
                    Log.d(TAG, "未选择有效课程");
                    return null;
                }

                Log.d(TAG, "获取选中课程(字符串): 名称=" + courseName);

                // 查找对应的CourseDto
                List<CourseDto> courseDtos = CourseManagementActivity.getSharedCourseDtoList();
                for (CourseDto courseDto : courseDtos) {
                    if (courseName.equals(courseDto.getName())) {
                        return courseDto;
                    }
                }

                // 如果找不到，创建一个新的
                CourseDto courseDto = new CourseDto();
                courseDto.setId(0); // 临时ID
                courseDto.setName(courseName);
                return courseDto;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取选中课程时出错", e);
        }

        return null;
    }

    /**
     * 从服务器加载课程数据
     */
    private void loadCourseData() {
        Log.d(TAG, "开始加载课程数据");

        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Log.d(TAG, "无网络连接，无法加载课程数据");
            return;
        }

        // 创建CourseApiService实例
        com.example.yidong222.api.CourseApiService courseApiService = com.example.yidong222.api.ApiClient.getClient()
                .create(com.example.yidong222.api.CourseApiService.class);

        // 调用API获取课程列表
        courseApiService.getCourses(1, 100).enqueue(
                new retrofit2.Callback<com.example.yidong222.models.ApiResponseList<com.example.yidong222.models.CourseDto>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.example.yidong222.models.ApiResponseList<com.example.yidong222.models.CourseDto>> call,
                            retrofit2.Response<com.example.yidong222.models.ApiResponseList<com.example.yidong222.models.CourseDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.yidong222.models.ApiResponseList<com.example.yidong222.models.CourseDto> apiResponse = response
                                    .body();

                            if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                                List<com.example.yidong222.models.CourseDto> courseDtos = apiResponse.getData();

                                // 更新共享数据
                                CourseManagementActivity.updateSharedCourseData(courseDtos);

                                Log.d(TAG, "成功从服务器加载课程数据，共 " + courseDtos.size() + " 个课程");
                            } else {
                                Log.e(TAG, "加载课程数据失败: " + apiResponse.getMessage());
                            }
                        } else {
                            Log.e(TAG, "加载课程数据API调用失败: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<com.example.yidong222.models.ApiResponseList<com.example.yidong222.models.CourseDto>> call,
                            Throwable t) {
                        Log.e(TAG, "加载课程数据网络错误: " + t.getMessage());
                    }
                });
    }
}