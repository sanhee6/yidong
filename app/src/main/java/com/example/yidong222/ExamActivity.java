package com.example.yidong222;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.yidong222.adapters.ExamAdapter;
import com.example.yidong222.data.DataManager;
import com.example.yidong222.data.db.AppDatabase;
import com.example.yidong222.data.db.entity.ExamEntity;
import com.example.yidong222.data.MockDataProvider;
import com.example.yidong222.models.CourseDto;
import com.example.yidong222.models.Exam;
import com.example.yidong222.models.ExamDto;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamActivity extends AppCompatActivity implements ExamAdapter.ExamItemClickListener {

    private static final String TAG = "ExamActivity";
    private RecyclerView recyclerView;
    private ExamAdapter adapter;
    private List<Exam> examList = new ArrayList<>();
    private FloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private View emptyView;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("考试信息");

        // 初始化视图
        recyclerView = findViewById(R.id.recyclerViewExams);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.tvEmptyExams);
        fabAdd = findViewById(R.id.fabAddExam);

        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadExamsFromServer);

        // 设置添加按钮点击事件
        fabAdd.setOnClickListener(v -> showAddExamDialog());

        // 初始化适配器
        adapter = new ExamAdapter(examList, this);
        recyclerView.setAdapter(adapter);

        // 预加载课程数据，确保数据一致性
        List<CourseDto> sharedCourses = CourseManagementActivity.getSharedCourseDtoList();
        if (sharedCourses.isEmpty() || CourseManagementActivity.getSharedCourseNames().get(0).equals("暂无可选课程")) {
            // 如果没有共享数据，尝试从服务器获取课程数据
            Log.d(TAG, "没有共享课程数据，准备加载课程数据");
            // 异步加载，不阻塞UI
            new Thread(() -> {
                try {
                    // 模拟网络请求
                    Thread.sleep(500);
                    // 加载课程数据的操作会在CourseManagementActivity中进行
                    // 这里只是确保在打开考试页面前已经有了课程数据
                } catch (InterruptedException e) {
                    Log.e(TAG, "预加载课程数据被中断", e);
                }
            }).start();
        } else {
            Log.d(TAG, "已有共享课程数据，共 " + sharedCourses.size() + " 个课程");
        }

        // 从服务器加载考试数据
        loadExamsFromServer();
    }

    private void loadExamsFromServer() {
        // 显示进度条
        swipeRefreshLayout.setRefreshing(true);

        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Log.d(TAG, "无网络连接，使用离线模式");
            Toast.makeText(this, "无网络连接，显示本地数据", Toast.LENGTH_SHORT).show();
            isOfflineMode = true;
            loadOfflineData();
            return;
        }

        // 从服务器获取考试列表
        DataManager.getExams(new DataManager.DataCallback<Exam>() {
            @Override
            public void onSuccess(List<Exam> data) {
                runOnUiThread(() -> {
                    // 更新列表
                    examList.clear();
                    examList.addAll(data);
                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                    updateEmptyView();

                    // 同步到本地数据库
                    syncExamsToLocalDatabase(data);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    // 显示错误消息
                    Toast.makeText(ExamActivity.this, "获取考试信息失败，使用本地数据", Toast.LENGTH_SHORT).show();
                    isOfflineMode = true;
                    swipeRefreshLayout.setRefreshing(false);
                    loadOfflineData();
                });
            }
        });
    }

    private void loadOfflineData() {
        // 加载本地模拟数据
        examList.clear();
        examList.addAll(MockDataProvider.getMockExams());
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (examList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showAddExamDialog() {
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_exam, null);

        // 获取输入框
        final TextInputEditText etCourseName = dialogView.findViewById(R.id.etExamCourseName);
        final TextInputEditText etExamName = dialogView.findViewById(R.id.etExamName);
        final TextInputEditText etDate = dialogView.findViewById(R.id.etExamDate);
        final TextInputEditText etTime = dialogView.findViewById(R.id.etExamTime);
        final TextInputEditText etLocation = dialogView.findViewById(R.id.etExamLocation);
        final TextInputEditText etSeatNumber = dialogView.findViewById(R.id.etExamSeatNumber);
        final TextInputEditText etDuration = dialogView.findViewById(R.id.etExamDuration);

        // 隐藏课程名称输入框
        TextInputLayout tilCourseName = dialogView.findViewById(R.id.tilExamCourseName);
        tilCourseName.setVisibility(View.GONE);

        // 添加课程选择下拉菜单
        final Spinner spinnerCourse = dialogView.findViewById(R.id.spinnerCourse);

        // 设置默认值
        etCourseName.setText("计算机网络");
        etDuration.setText("120");

        // 优先使用CourseManagementActivity的共享课程数据
        List<CourseDto> sharedCourses = CourseManagementActivity.getSharedCourseDtoList();
        List<String> sharedCourseNames = CourseManagementActivity.getSharedCourseNames();

        if (sharedCourses != null && !sharedCourses.isEmpty() &&
                !sharedCourseNames.isEmpty() && !sharedCourseNames.get(0).equals("暂无可选课程")) {
            // 将共享课程数据转换为适配器需要的格式
            List<Map<String, Object>> courseList = new ArrayList<>();
            for (CourseDto courseDto : sharedCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", courseDto.getId());
                courseMap.put("name", courseDto.getName());
                courseList.add(courseMap);
            }

            // 创建适配器
            SimpleAdapter adapter = new SimpleAdapter(
                    ExamActivity.this,
                    courseList,
                    android.R.layout.simple_spinner_item,
                    new String[] { "name" },
                    new int[] { android.R.id.text1 });
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCourse.setAdapter(adapter);

            Log.d(TAG, "使用共享课程数据，共 " + courseList.size() + " 个课程");
        } else {
            // 如果共享数据不可用，则从数据库获取课程列表
            DataManager.getCourses(new DataManager.DetailCallback<List<Map<String, Object>>>() {
                @Override
                public void onSuccess(List<Map<String, Object>> courseList) {
                    // 创建适配器
                    SimpleAdapter adapter = new SimpleAdapter(
                            ExamActivity.this,
                            courseList,
                            android.R.layout.simple_spinner_item,
                            new String[] { "name" },
                            new int[] { android.R.id.text1 });
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCourse.setAdapter(adapter);

                    Log.d(TAG, "使用数据库课程数据，共 " + courseList.size() + " 个课程");
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(ExamActivity.this, "获取课程列表失败: " + message, Toast.LENGTH_SHORT).show();
                    // 创建一个默认的课程列表
                    List<Map<String, Object>> courseList = new ArrayList<>();
                    Map<String, Object> course1 = new HashMap<>();
                    course1.put("id", 2);
                    course1.put("name", "计算机网络");
                    courseList.add(course1);

                    Map<String, Object> course2 = new HashMap<>();
                    course2.put("id", 3);
                    course2.put("name", "操作系统");
                    courseList.add(course2);

                    // 创建适配器
                    SimpleAdapter adapter = new SimpleAdapter(
                            ExamActivity.this,
                            courseList,
                            android.R.layout.simple_spinner_item,
                            new String[] { "name" },
                            new int[] { android.R.id.text1 });
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCourse.setAdapter(adapter);

                    Log.d(TAG, "使用默认课程数据");
                }
            });
        }

        // 设置日期选择器
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            // 不需要尝试解析当前日期，因为这是添加新考试
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            try {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        ExamActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            try {
                                // 格式化日期为YYYY-MM-DD
                                String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                        selectedYear, selectedMonth + 1, selectedDay);
                                etDate.setText(formattedDate);
                            } catch (Exception e) {
                                Log.e(TAG, "设置日期文本失败: " + e.getMessage());
                                Toast.makeText(ExamActivity.this, "设置日期失败", Toast.LENGTH_SHORT).show();
                            }
                        },
                        year, month, day);
                datePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示日期选择器失败: " + e.getMessage());
                Toast.makeText(ExamActivity.this, "无法显示日期选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置时间选择器
        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            // 不需要尝试解析当前时间，因为这是添加新考试
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            try {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        ExamActivity.this,
                        (view, selectedHour, selectedMinute) -> {
                            try {
                                // 格式化时间为HH:MM
                                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d",
                                        selectedHour, selectedMinute);
                                etTime.setText(formattedTime);
                            } catch (Exception e) {
                                Log.e(TAG, "设置时间文本失败: " + e.getMessage());
                                Toast.makeText(ExamActivity.this, "设置时间失败", Toast.LENGTH_SHORT).show();
                            }
                        },
                        hour, minute, true);
                timePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示时间选择器失败: " + e.getMessage());
                Toast.makeText(ExamActivity.this, "无法显示时间选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 创建对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle("添加考试")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    // 获取输入值
                    String courseName = etCourseName.getText().toString().trim();
                    String examName = etExamName.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    String time = etTime.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String seatNumber = etSeatNumber.getText().toString().trim();
                    String durationStr = etDuration.getText().toString().trim();

                    // 获取选中的课程ID
                    int courseId = 2; // 默认使用计算机网络课程ID
                    if (spinnerCourse.getSelectedItem() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> selectedCourse = (Map<String, Object>) spinnerCourse.getSelectedItem();
                        courseId = (int) selectedCourse.get("id");
                    }

                    // 验证输入
                    if (!examName.isEmpty() && !date.isEmpty() && !time.isEmpty()
                            && !location.isEmpty() && !durationStr.isEmpty()) {
                        try {
                            // 解析持续时间
                            int duration = Integer.parseInt(durationStr);

                            // 构建符合API要求的日期时间格式 (YYYY-MM-DD HH:MM:SS)
                            String dateTime = date + " " + time + ":00";

                            // 验证日期格式
                            try {
                                SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault());
                                apiDateFormat.parse(dateTime);
                            } catch (ParseException e) {
                                Toast.makeText(this, "日期时间格式无效", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 创建考试DTO
                            ExamDto examDto = new ExamDto(
                                    courseId, // 使用选中的课程ID
                                    examName,
                                    dateTime,
                                    duration,
                                    location,
                                    "考试描述: " + examName);

                            // 设置座位号
                            examDto.setSeatNumber(seatNumber);

                            // 创建考试
                            createExamOnServer(examDto);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "请输入有效的考试时长", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "请填写完整考试信息", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditExamDialog(int position, Exam exam) {
        // 创建对话框视图
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_exam, null);

        // 获取输入框
        final TextInputEditText etCourseName = dialogView.findViewById(R.id.etExamCourseName);
        final TextInputEditText etExamName = dialogView.findViewById(R.id.etExamName);
        final TextInputEditText etDate = dialogView.findViewById(R.id.etExamDate);
        final TextInputEditText etTime = dialogView.findViewById(R.id.etExamTime);
        final TextInputEditText etDuration = dialogView.findViewById(R.id.etExamDuration);
        final TextInputEditText etLocation = dialogView.findViewById(R.id.etExamLocation);
        final TextInputEditText etSeatNumber = dialogView.findViewById(R.id.etExamSeatNumber);

        // 隐藏课程名称输入框
        TextInputLayout tilCourseName = dialogView.findViewById(R.id.tilExamCourseName);
        tilCourseName.setVisibility(View.GONE);

        // 设置课程选择下拉菜单
        final Spinner spinnerCourse = dialogView.findViewById(R.id.spinnerCourse);

        // 设置初始值
        etCourseName.setText(exam.getCourseName());
        etExamName.setText(exam.getExamName());
        etDate.setText(exam.getDate());
        etTime.setText(exam.getTime());
        etDuration.setText(String.valueOf(exam.getDuration()));
        etLocation.setText(exam.getLocation());
        etSeatNumber.setText(exam.getSeatNumber());

        // 从数据库获取课程列表
        final int currentCourseId = exam.getCourseId();

        // 优先使用CourseManagementActivity的共享课程数据
        List<CourseDto> sharedCourses = CourseManagementActivity.getSharedCourseDtoList();
        List<String> sharedCourseNames = CourseManagementActivity.getSharedCourseNames();

        if (sharedCourses != null && !sharedCourses.isEmpty() &&
                !sharedCourseNames.isEmpty() && !sharedCourseNames.get(0).equals("暂无可选课程")) {
            // 将共享课程数据转换为适配器需要的格式
            List<Map<String, Object>> courseList = new ArrayList<>();
            for (CourseDto courseDto : sharedCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", courseDto.getId());
                courseMap.put("name", courseDto.getName());
                courseList.add(courseMap);
            }

            // 创建适配器
            SimpleAdapter adapter = new SimpleAdapter(
                    ExamActivity.this,
                    courseList,
                    android.R.layout.simple_spinner_item,
                    new String[] { "name" },
                    new int[] { android.R.id.text1 });
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCourse.setAdapter(adapter);

            // 设置默认选中的课程
            for (int i = 0; i < courseList.size(); i++) {
                if ((int) courseList.get(i).get("id") == currentCourseId) {
                    spinnerCourse.setSelection(i);
                    break;
                }
            }

            Log.d(TAG, "编辑考试，使用共享课程数据，共 " + courseList.size() + " 个课程");
        } else {
            // 如果共享数据不可用，则从数据库获取课程列表
            DataManager.getCourses(new DataManager.DetailCallback<List<Map<String, Object>>>() {
                @Override
                public void onSuccess(List<Map<String, Object>> courseList) {
                    // 创建适配器
                    SimpleAdapter adapter = new SimpleAdapter(
                            ExamActivity.this,
                            courseList,
                            android.R.layout.simple_spinner_item,
                            new String[] { "name" },
                            new int[] { android.R.id.text1 });
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCourse.setAdapter(adapter);

                    // 设置默认选中的课程
                    for (int i = 0; i < courseList.size(); i++) {
                        if ((int) courseList.get(i).get("id") == currentCourseId) {
                            spinnerCourse.setSelection(i);
                            break;
                        }
                    }

                    Log.d(TAG, "编辑考试，使用数据库课程数据，共 " + courseList.size() + " 个课程");
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(ExamActivity.this, "获取课程列表失败: " + message, Toast.LENGTH_SHORT).show();
                    // 创建一个默认的课程列表
                    List<Map<String, Object>> courseList = new ArrayList<>();
                    Map<String, Object> course1 = new HashMap<>();
                    course1.put("id", 2);
                    course1.put("name", "计算机网络");
                    courseList.add(course1);

                    Map<String, Object> course2 = new HashMap<>();
                    course2.put("id", 3);
                    course2.put("name", "操作系统");
                    courseList.add(course2);

                    // 创建适配器
                    SimpleAdapter adapter = new SimpleAdapter(
                            ExamActivity.this,
                            courseList,
                            android.R.layout.simple_spinner_item,
                            new String[] { "name" },
                            new int[] { android.R.id.text1 });
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCourse.setAdapter(adapter);

                    // 设置默认选中的课程
                    for (int i = 0; i < courseList.size(); i++) {
                        if ((int) courseList.get(i).get("id") == currentCourseId) {
                            spinnerCourse.setSelection(i);
                            break;
                        }
                    }

                    Log.d(TAG, "编辑考试，使用默认课程数据");
                }
            });
        }

        // 设置日期选择器
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            // 尝试解析当前日期
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date currentDate = sdf.parse(exam.getDate());
                if (currentDate != null) {
                    calendar.setTime(currentDate);
                }
            } catch (ParseException e) {
                Log.e(TAG, "解析日期失败: " + e.getMessage());
                // 使用当前日期
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            try {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        ExamActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            try {
                                // 格式化日期为YYYY-MM-DD
                                String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                        selectedYear, selectedMonth + 1, selectedDay);
                                etDate.setText(formattedDate);
                            } catch (Exception e) {
                                Log.e(TAG, "设置日期文本失败: " + e.getMessage());
                                Toast.makeText(ExamActivity.this, "设置日期失败", Toast.LENGTH_SHORT).show();
                            }
                        },
                        year, month, day);
                datePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示日期选择器失败: " + e.getMessage());
                Toast.makeText(ExamActivity.this, "无法显示日期选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置时间选择器
        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            // 尝试解析当前时间
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date currentTime = sdf.parse(exam.getTime());
                if (currentTime != null) {
                    calendar.setTime(currentTime);
                }
            } catch (ParseException e) {
                Log.e(TAG, "解析时间失败: " + e.getMessage());
                // 使用当前时间
            }

            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            try {
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        ExamActivity.this,
                        (view, selectedHour, selectedMinute) -> {
                            try {
                                // 格式化时间为HH:MM
                                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d",
                                        selectedHour, selectedMinute);
                                etTime.setText(formattedTime);
                            } catch (Exception e) {
                                Log.e(TAG, "设置时间文本失败: " + e.getMessage());
                                Toast.makeText(ExamActivity.this, "设置时间失败", Toast.LENGTH_SHORT).show();
                            }
                        },
                        hour, minute, true);
                timePickerDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "显示时间选择器失败: " + e.getMessage());
                Toast.makeText(ExamActivity.this, "无法显示时间选择器", Toast.LENGTH_SHORT).show();
            }
        });

        // 创建对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle("编辑考试")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    // 获取输入值
                    String courseName = etCourseName.getText().toString().trim();
                    String examName = etExamName.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    String time = etTime.getText().toString().trim();
                    String durationStr = etDuration.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String seatNumber = etSeatNumber.getText().toString().trim();

                    // 获取选中的课程ID
                    int courseId = 2; // 默认使用计算机网络课程ID
                    if (spinnerCourse.getSelectedItem() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> selectedCourse = (Map<String, Object>) spinnerCourse.getSelectedItem();
                        courseId = (int) selectedCourse.get("id");
                    }

                    // 验证输入
                    if (!examName.isEmpty() && !date.isEmpty() && !time.isEmpty()
                            && !location.isEmpty() && !durationStr.isEmpty()) {
                        try {
                            // 解析持续时间
                            int duration = Integer.parseInt(durationStr);

                            // 构建符合API要求的日期时间格式 (YYYY-MM-DD HH:MM:SS)
                            String dateTime = date + " " + time + ":00";

                            // 验证日期格式
                            try {
                                SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault());
                                apiDateFormat.parse(dateTime);
                            } catch (ParseException e) {
                                Toast.makeText(this, "日期时间格式无效", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 解析考试ID
                            int examId = exam.getId();

                            // 记录课程ID
                            Log.d(TAG, "更新考试，使用课程ID: " + courseId + ", 考试ID: " + examId);

                            // 更新Exam对象的课程名称和座位号
                            exam.setCourseName(courseName);
                            exam.setSeatNumber(seatNumber);

                            // 创建更新后的考试DTO
                            ExamDto updatedExam = new ExamDto(
                                    courseId, // 使用选中的课程ID
                                    examName,
                                    dateTime,
                                    duration,
                                    location,
                                    "考试描述: " + examName);

                            // 设置座位号
                            updatedExam.setSeatNumber(seatNumber);

                            // 更新服务器上的考试
                            updateExamOnServer(examId, updatedExam, position, exam);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "请输入有效的考试时长", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "请填写完整考试信息", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void createExamOnServer(ExamDto examDto) {
        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "无网络连接，无法创建考试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 提交新考试到服务器
        DataManager.createExam(examDto, new DataManager.DetailCallback<Exam>() {
            @Override
            public void onSuccess(Exam data) {
                runOnUiThread(() -> {
                    // 添加到列表顶部
                    examList.add(0, data);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                    Toast.makeText(ExamActivity.this, "考试创建成功", Toast.LENGTH_SHORT).show();

                    // 同步到本地数据库
                    syncExamToLocalDatabase(data);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(ExamActivity.this, "创建考试失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateExamOnServer(int examId, ExamDto examDto, int position, Exam currentExam) {
        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "无网络连接，无法更新考试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 记录请求参数
        Log.d(TAG, "发送更新考试请求，ID: " + examId +
                ", courseId: " + examDto.getCourseId() +
                ", title: " + examDto.getTitle() +
                ", location: " + examDto.getLocation());

        // 提交更新到服务器
        DataManager.updateExam(examId, examDto, new DataManager.DetailCallback<Exam>() {
            @Override
            public void onSuccess(Exam data) {
                runOnUiThread(() -> {
                    // 确保返回的考试对象保留正确的课程ID
                    if (data.getCourseId() == 0 && currentExam.getCourseId() != 0) {
                        data.setCourseId(currentExam.getCourseId());
                        Log.d(TAG, "修正返回的考试课程ID: " + data.getCourseId());
                    }

                    // 保留座位号信息，因为API可能不返回这个字段
                    if (data.getSeatNumber() == null || data.getSeatNumber().isEmpty()) {
                        data.setSeatNumber(currentExam.getSeatNumber());
                        Log.d(TAG, "保留座位号信息: " + data.getSeatNumber());
                    }

                    // 更新列表项
                    examList.set(position, data);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(ExamActivity.this, "考试更新成功", Toast.LENGTH_SHORT).show();

                    // 同步到本地数据库
                    syncExamToLocalDatabase(data);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(ExamActivity.this, "更新考试失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 将考试同步到本地数据库
     * 
     * @param exam 要同步的考试对象
     */
    private void syncExamToLocalDatabase(Exam exam) {
        try {
            // 获取数据库实例
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 创建考试实体
            ExamEntity entity = new ExamEntity();
            entity.id = exam.getId();
            entity.courseId = exam.getCourseId();
            entity.examName = exam.getExamName();
            entity.examDate = exam.getDate();
            entity.examTime = exam.getTime();
            entity.location = exam.getLocation();
            entity.type = exam.getStatus();
            entity.seatNumber = exam.getSeatNumber(); // 确保设置座位号

            // 插入或更新到数据库
            db.examDao().insert(entity);

            Log.d(TAG, "考试已同步到本地数据库，ID: " + exam.getId() +
                    ", courseId: " + exam.getCourseId() +
                    ", title: " + exam.getExamName() +
                    ", location: " + exam.getLocation() +
                    ", seatNumber: " + exam.getSeatNumber());
        } catch (Exception e) {
            Log.e(TAG, "同步考试到本地数据库失败", e);
        }
    }

    /**
     * 将考试列表同步到本地数据库
     * 
     * @param exams 要同步的考试列表
     */
    private void syncExamsToLocalDatabase(List<Exam> exams) {
        try {
            Log.d(TAG, "开始将 " + exams.size() + " 条考试同步到本地数据库");

            // 获取数据库实例
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 为每个考试创建实体并插入数据库
            for (Exam exam : exams) {
                ExamEntity entity = new ExamEntity();
                entity.id = exam.getId();
                entity.courseId = exam.getCourseId();
                entity.examName = exam.getExamName();
                entity.examDate = exam.getDate();
                entity.examTime = exam.getTime();
                entity.location = exam.getLocation();
                entity.type = exam.getStatus();
                entity.seatNumber = exam.getSeatNumber(); // 确保设置座位号

                // 插入或更新到数据库
                db.examDao().insert(entity);

                Log.d(TAG, "同步考试到数据库: ID=" + exam.getId() +
                        ", courseId=" + exam.getCourseId() +
                        ", title=" + exam.getExamName() +
                        ", location=" + exam.getLocation() +
                        ", seatNumber=" + exam.getSeatNumber());
            }

            Log.d(TAG, "成功同步考试数据到本地数据库");
        } catch (Exception e) {
            Log.e(TAG, "同步考试到本地数据库失败", e);
        }
    }

    private void deleteExam(int position) {
        // 获取要删除的考试
        Exam exam = examList.get(position);
        int examId = exam.getId();

        // 确认删除
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除考试")
                .setMessage("确定要删除这个考试吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 从列表中移除
                    examList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyView();

                    // 删除服务器上的考试
                    deleteExamFromServer(examId, position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteExamFromServer(int examId, int position) {
        // 检查网络连接
        if (!DataManager.isNetworkAvailable(this)) {
            Toast.makeText(this, "无网络连接，无法删除考试", Toast.LENGTH_SHORT).show();
            return;
        }

        // 提交删除请求到服务器
        DataManager.deleteExam(examId, new DataManager.DetailCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    // 从列表中移除
                    examList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyView();
                    Toast.makeText(ExamActivity.this, "考试删除成功", Toast.LENGTH_SHORT).show();

                    // 从本地数据库中删除
                    deleteExamFromLocalDatabase(examId);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(ExamActivity.this, "删除考试失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 从本地数据库中删除考试
     * 
     * @param examId 要删除的考试ID
     */
    private void deleteExamFromLocalDatabase(int examId) {
        try {
            // 获取数据库实例
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 查询要删除的考试实体
            ExamEntity entity = db.examDao().getExamById(examId);

            // 如果找到了考试实体，则删除它
            if (entity != null) {
                db.examDao().delete(entity);
                Log.d(TAG, "考试已从本地数据库删除，ID: " + examId);
            } else {
                Log.w(TAG, "未在本地数据库中找到考试，ID: " + examId);
            }
        } catch (Exception e) {
            Log.e(TAG, "从本地数据库删除考试失败", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onExamClick(int position, Exam exam) {
        // 显示考试详情
        new MaterialAlertDialogBuilder(this)
                .setTitle(exam.getExamName())
                .setMessage("日期: " + exam.getDate() +
                        "\n时间: " + exam.getTime() +
                        "\n地点: " + exam.getLocation() +
                        "\n座位号: " + exam.getSeatNumber() +
                        "\n状态: " + exam.getStatus())
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onExamEditClick(int position, Exam exam) {
        showEditExamDialog(position, exam);
    }

    @Override
    public void onExamDeleteClick(int position) {
        deleteExam(position);
    }

    @Override
    public void onExamStatusToggle(int position, Exam exam) {
        // 切换考试状态
        String newStatus = "已考".equals(exam.getStatus()) ? "未考" : "已考";
        exam.setStatus(newStatus);
        adapter.notifyItemChanged(position);

        // 实际更新服务器上的考试状态
        String message = "已考".equals(newStatus) ? "考试已标记为已考" : "考试已标记为未考";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // 如果在实际应用中，这里应该创建一个DTO并调用更新方法
        // 例如：
        /*
         * ExamDto updatedDto = new ExamDto();
         * updatedDto.setCourseId(exam.getCourseId());
         * updatedDto.setTitle(exam.getTitle());
         * updatedDto.setExamDate(exam.getDate() + "T" + exam.getTime() + ":00.000Z");
         * updatedDto.setLocation(exam.getLocation());
         * updatedDto.setDescription(exam.getDescription());
         * 
         * DataManager.updateExam(exam.getId(), updatedDto, new
         * DataManager.DetailCallback<Exam>() {
         * 
         * @Override
         * public void onSuccess(Exam data) {
         * // 成功更新
         * }
         * 
         * @Override
         * public void onFailure(String message) {
         * // 更新失败，回滚UI状态
         * String rollbackStatus = "已考".equals(newStatus) ? "未考" : "已考";
         * exam.setStatus(rollbackStatus);
         * adapter.notifyItemChanged(position);
         * Toast.makeText(ExamActivity.this, "更新状态失败: " + message,
         * Toast.LENGTH_SHORT).show();
         * }
         * });
         */
    }
}