package com.example.yidong222;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.yidong222.adapters.CourseManagementAdapter;
import com.example.yidong222.api.ApiClient;
import com.example.yidong222.api.CourseApiService;
import com.example.yidong222.models.ApiResponse;
import com.example.yidong222.models.ApiResponseList;
import com.example.yidong222.models.Course;
import com.example.yidong222.models.CourseDto;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseManagementActivity extends AppCompatActivity
        implements CourseManagementAdapter.CourseItemClickListener {

    private RecyclerView recyclerView;
    private CourseManagementAdapter adapter;
    private List<Course> courseList = new ArrayList<>();
    private FloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CourseApiService apiService;
    private List<CourseDto> courseDtoList = new ArrayList<>();
    private List<String> allCourseNames = new ArrayList<>();

    // 静态共享数据，方便其他组件访问
    private static List<CourseDto> sharedCourseDtoList = new ArrayList<>();
    private static List<String> sharedCourseNames = new ArrayList<>();

    /**
     * 获取共享的课程DTO列表
     */
    public static List<CourseDto> getSharedCourseDtoList() {
        return new ArrayList<>(sharedCourseDtoList);
    }

    /**
     * 获取共享的课程名称列表
     */
    public static List<String> getSharedCourseNames() {
        return new ArrayList<>(sharedCourseNames);
    }

    /**
     * 更新共享的课程数据
     */
    public static void updateSharedCourseData(List<CourseDto> courseDtos) {
        // 添加更多详细日志
        Log.d("CourseManagement", "开始更新共享课程数据，当前共享数据大小: " + sharedCourseDtoList.size());

        if (courseDtos == null) {
            Log.e("CourseManagement", "传入的课程数据为null");
        } else {
            Log.d("CourseManagement", "传入的课程数据大小: " + courseDtos.size());
        }

        sharedCourseDtoList.clear();
        sharedCourseNames.clear();

        if (courseDtos != null) {
            // 添加所有课程数据
            sharedCourseDtoList.addAll(courseDtos);

            // 按照图二中显示的顺序排列课程（先显示数字课程，再显示文字课程）
            List<String> numericCourses = new ArrayList<>();
            List<String> textCourses = new ArrayList<>();

            // 分类课程名称
            for (CourseDto dto : courseDtos) {
                if (dto.getName() != null && !dto.getName().isEmpty()) {
                    String name = dto.getName();
                    // 如果课程名称是纯数字，放入数字课程列表
                    if (name.matches("\\d+")) {
                        numericCourses.add(name);
                    } else {
                        // 否则放入文字课程列表
                        textCourses.add(name);
                    }
                }
            }

            // 对数字课程按数值排序
            numericCourses.sort((a, b) -> {
                try {
                    return Integer.parseInt(a) - Integer.parseInt(b);
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });

            // 添加数字课程
            sharedCourseNames.addAll(numericCourses);
            // 添加文字课程
            sharedCourseNames.addAll(textCourses);

            Log.d("CourseManagement", "处理后的数字课程: " + numericCourses.toString());
            Log.d("CourseManagement", "处理后的文字课程: " + textCourses.toString());
        }

        // 如果没有课程数据，添加一个提示选项
        if (sharedCourseNames.isEmpty()) {
            sharedCourseNames.add("暂无可选课程");
            Log.d("CourseManagement", "无课程数据，添加默认选项");
        }

        // 记录日志
        Log.d("CourseManagement", "更新共享课程数据完成，共 " + sharedCourseDtoList.size() +
                " 个课程, 课程名称: " + sharedCourseNames.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_management);

        // 初始化API服务
        apiService = ApiClient.getClient().create(CourseApiService.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("课程管理");

        recyclerView = findViewById(R.id.recyclerViewCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化下拉刷新
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadCourses);

        // 检查网络连接
        if (!isNetworkAvailable()) {
            TextView tvEmptyCourses = findViewById(R.id.tvEmptyCourses);
            tvEmptyCourses.setVisibility(View.VISIBLE);
            tvEmptyCourses.setText("无网络连接，请检查网络后下拉刷新");
            Toast.makeText(this, "无网络连接，请检查网络设置", Toast.LENGTH_LONG).show();
        } else {
            // 加载API数据
            loadCourses();
        }

        adapter = new CourseManagementAdapter(courseList, this);
        recyclerView.setAdapter(adapter);

        fabAdd = findViewById(R.id.fabAddCourse);
        fabAdd.setOnClickListener(v -> showAddCourseDialog());
    }

    private void loadCourses() {
        swipeRefreshLayout.setRefreshing(true);
        TextView tvEmptyCourses = findViewById(R.id.tvEmptyCourses);
        tvEmptyCourses.setVisibility(View.GONE);

        // 添加调试日志
        Log.d("CourseManagement", "开始加载课程数据，BASE_URL: " + ApiClient.getBaseUrl());

        apiService.getCourses(1, 100).enqueue(new Callback<ApiResponseList<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<CourseDto>> call,
                    Response<ApiResponseList<CourseDto>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseList<CourseDto> apiResponse = response.body();

                    Log.d("CourseManagement", "获取课程响应: " + response.code() + ", 状态: " + apiResponse.getStatus());

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        courseDtoList = apiResponse.getData();
                        courseList.clear();
                        allCourseNames = new ArrayList<>(); // 创建新的列表实例，避免引用问题

                        // 将API返回的课程DTO转换为UI使用的Course对象，并存储课程名称
                        for (CourseDto courseDto : courseDtoList) {
                            courseList.add(courseDto.toCourse());
                            if (courseDto.getName() != null && !courseDto.getName().isEmpty()) {
                                allCourseNames.add(courseDto.getName());
                            }
                        }

                        // 更新静态共享数据
                        updateSharedCourseData(courseDtoList);

                        // 同步所有课程到本地数据库
                        syncCoursesToLocalDatabase(courseDtoList);

                        // 如果没有课程数据，添加一个提示选项
                        if (allCourseNames.isEmpty()) {
                            allCourseNames.add("暂无可选课程");
                        }

                        adapter.notifyDataSetChanged();

                        // 显示或隐藏空数据提示
                        if (courseList.isEmpty()) {
                            tvEmptyCourses.setVisibility(View.VISIBLE);
                            tvEmptyCourses.setText("暂无课程数据");
                        } else {
                            tvEmptyCourses.setVisibility(View.GONE);
                        }
                    } else {
                        // API返回失败
                        tvEmptyCourses.setVisibility(View.VISIBLE);
                        tvEmptyCourses.setText("获取课程失败: " +
                                (apiResponse.getMessage() != null ? apiResponse.getMessage() : "未知错误"));
                        Toast.makeText(CourseManagementActivity.this, "获取课程失败: " +
                                (apiResponse.getMessage() != null ? apiResponse.getMessage() : "未知错误"),
                                Toast.LENGTH_SHORT).show();

                        // 确保有一个默认的下拉选项
                        allCourseNames = new ArrayList<>();
                        allCourseNames.add("暂无可选课程");

                        // 清空共享数据
                        updateSharedCourseData(null);
                    }
                } else {
                    // HTTP请求失败
                    tvEmptyCourses.setVisibility(View.VISIBLE);
                    String errorMsg = "获取课程失败: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("CourseManagement", "读取错误响应失败", e);
                    }
                    tvEmptyCourses.setText(errorMsg);
                    Log.e("CourseManagement", errorMsg);
                    Toast.makeText(CourseManagementActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

                    // 确保有一个默认的下拉选项
                    allCourseNames = new ArrayList<>();
                    allCourseNames.add("暂无可选课程");

                    // 清空共享数据
                    updateSharedCourseData(null);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<CourseDto>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                TextView tvEmptyCourses = findViewById(R.id.tvEmptyCourses);
                tvEmptyCourses.setVisibility(View.VISIBLE);
                tvEmptyCourses.setText("网络错误: " + t.getMessage());
                Toast.makeText(CourseManagementActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CourseManagement", "获取课程列表失败", t);

                // 确保有一个默认的下拉选项
                allCourseNames = new ArrayList<>();
                allCourseNames.add("暂无可选课程");

                // 清空共享数据
                updateSharedCourseData(null);
            }
        });
    }

    private void showAddCourseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        TextInputEditText etTeacher = dialogView.findViewById(R.id.et_teacher_name);
        TextInputEditText etClassroom = dialogView.findViewById(R.id.et_classroom);
        Spinner spWeekday = dialogView.findViewById(R.id.spWeekday);
        Spinner spStartSection = dialogView.findViewById(R.id.spStartSection);
        Spinner spEndSection = dialogView.findViewById(R.id.spEndSection);
        Spinner spCourseNames = dialogView.findViewById(R.id.sp_course_names);

        // 设置下拉菜单
        setupCourseNameSpinner(spCourseNames, null);

        new MaterialAlertDialogBuilder(this)
                .setTitle("添加课程")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String name = etCourseName.getText().toString().trim();
                    String teacher = etTeacher.getText().toString().trim();
                    String classroom = etClassroom.getText().toString().trim();
                    int weekday = spWeekday.getSelectedItemPosition() + 1;
                    int startSection = spStartSection.getSelectedItemPosition() + 1;
                    int endSection = spEndSection.getSelectedItemPosition() + 1;
                    int startWeek = 1;
                    int endWeek = 16;
                    String semesterId = "2023-1";

                    if (!name.isEmpty() && !teacher.isEmpty()) {
                        CourseDto newCourse = new CourseDto(
                                name, teacher, classroom, weekday,
                                startSection, endSection, startWeek,
                                endWeek, semesterId);

                        createCourse(newCourse);
                    } else {
                        Toast.makeText(this, "请填写必填的课程信息", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void createCourse(CourseDto courseDto) {
        apiService.createCourse(courseDto).enqueue(new Callback<ApiResponse<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseDto>> call, Response<ApiResponse<CourseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CourseDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        CourseDto newCourseDto = apiResponse.getData();
                        courseDtoList.add(newCourseDto);
                        courseList.add(newCourseDto.toCourse());
                        adapter.notifyItemInserted(courseList.size() - 1);

                        // 添加课程名称到列表
                        if (newCourseDto.getName() != null && !newCourseDto.getName().isEmpty()
                                && !allCourseNames.contains(newCourseDto.getName())) {
                            allCourseNames.add(newCourseDto.getName());
                        }

                        // 更新共享数据
                        updateSharedCourseData(courseDtoList);

                        // 保存到本地数据库
                        saveCourseToLocalDatabase(newCourseDto);

                        Toast.makeText(CourseManagementActivity.this, "课程添加成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CourseManagementActivity.this,
                                apiResponse.getMessage() != null ? apiResponse.getMessage() : "课程添加失败",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CourseManagementActivity.this, "课程添加失败: " + response.message(), Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseDto>> call, Throwable t) {
                Log.e("CourseManagement", "添加课程失败", t);
                Toast.makeText(CourseManagementActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 保存课程到本地数据库
     */
    private void saveCourseToLocalDatabase(CourseDto courseDto) {
        try {
            // 创建数据库实体对象
            com.example.yidong222.data.db.entity.CourseEntity entity = new com.example.yidong222.data.db.entity.CourseEntity();
            entity.id = courseDto.getId();
            entity.name = courseDto.getName();
            entity.teacher = courseDto.getTeacher();
            entity.classroom = courseDto.getClassroom();
            entity.weekday = courseDto.getWeekday();
            entity.startSection = courseDto.getStartSection();
            entity.endSection = courseDto.getEndSection();
            entity.startWeek = courseDto.getStartWeek();
            entity.endWeek = courseDto.getEndWeek();
            entity.semesterId = courseDto.getSemesterId();

            // 在后台线程中执行数据库操作
            new Thread(() -> {
                try {
                    com.example.yidong222.data.db.AppDatabase db = com.example.yidong222.data.db.AppDatabase
                            .getInstance(getApplicationContext());
                    db.courseDao().insert(entity);
                    Log.d("CourseManagement", "成功保存课程到本地数据库: ID=" + entity.id + ", 名称=" + entity.name);
                } catch (Exception e) {
                    Log.e("CourseManagement", "保存课程到本地数据库失败", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e("CourseManagement", "准备保存课程到数据库时出错", e);
        }
    }

    private void showEditCourseDialog(int position, Course course) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        TextInputEditText etTeacher = dialogView.findViewById(R.id.et_teacher_name);
        TextInputEditText etClassroom = dialogView.findViewById(R.id.et_classroom);
        Spinner spWeekday = dialogView.findViewById(R.id.spWeekday);
        Spinner spStartSection = dialogView.findViewById(R.id.spStartSection);
        Spinner spEndSection = dialogView.findViewById(R.id.spEndSection);
        Spinner spCourseNames = dialogView.findViewById(R.id.sp_course_names);

        etCourseName.setText(course.getName());
        etTeacher.setText(course.getTeacher());
        etClassroom.setText(course.getRoom());
        spWeekday.setSelection(course.getDay() - 1);
        spStartSection.setSelection(course.getStartSection() - 1);
        spEndSection.setSelection(course.getEndSection() - 1);

        // 设置下拉菜单，传入当前课程名称
        setupCourseNameSpinner(spCourseNames, course.getName());

        CourseDto originalCourse = getCourseDtoById(course.getId());
        if (originalCourse == null) {
            Toast.makeText(this, "无法找到原始课程数据", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("编辑课程")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etCourseName.getText().toString().trim();
                    String teacher = etTeacher.getText().toString().trim();
                    String classroom = etClassroom.getText().toString().trim();
                    int weekday = spWeekday.getSelectedItemPosition() + 1;
                    int startSection = spStartSection.getSelectedItemPosition() + 1;
                    int endSection = spEndSection.getSelectedItemPosition() + 1;
                    int startWeek = originalCourse.getStartWeek();
                    int endWeek = originalCourse.getEndWeek();
                    String semesterId = originalCourse.getSemesterId();

                    if (!name.isEmpty() && !teacher.isEmpty()) {
                        CourseDto updatedCourse = new CourseDto(
                                course.getId(), name, teacher, classroom, weekday,
                                startSection, endSection, startWeek, endWeek, semesterId);
                        updateCourse(position, course.getId(), updatedCourse);
                    } else {
                        Toast.makeText(this, "请填写必填的课程信息", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateCourse(int position, int courseId, CourseDto courseDto) {
        apiService.updateCourse(courseId, courseDto).enqueue(new Callback<ApiResponse<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseDto>> call, Response<ApiResponse<CourseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CourseDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        CourseDto updatedCourseDto = apiResponse.getData();
                        courseDtoList.set(position, updatedCourseDto);
                        courseList.set(position, updatedCourseDto.toCourse());
                        adapter.notifyItemChanged(position);

                        // 更新课程名称列表
                        allCourseNames.clear();
                        for (CourseDto dto : courseDtoList) {
                            if (dto.getName() != null && !dto.getName().isEmpty()
                                    && !allCourseNames.contains(dto.getName())) {
                                allCourseNames.add(dto.getName());
                            }
                        }

                        // 更新共享数据
                        updateSharedCourseData(courseDtoList);

                        // 保存到本地数据库
                        saveCourseToLocalDatabase(updatedCourseDto);

                        Toast.makeText(CourseManagementActivity.this, "课程更新成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CourseManagementActivity.this,
                                apiResponse.getMessage() != null ? apiResponse.getMessage() : "课程更新失败",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CourseManagementActivity.this, "课程更新失败: " + response.message(), Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseDto>> call, Throwable t) {
                Log.e("CourseManagement", "更新课程失败", t);
                Toast.makeText(CourseManagementActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCourse(int position) {
        CourseDto courseDto = courseDtoList.get(position);
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除课程")
                .setMessage("确定要删除该课程吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    apiService.deleteCourse(courseDto.getId()).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ApiResponse<Void> apiResponse = response.body();

                                if ("success".equals(apiResponse.getStatus())) {
                                    courseDtoList.remove(position);
                                    courseList.remove(position);
                                    adapter.notifyItemRemoved(position);

                                    // 更新课程名称列表
                                    allCourseNames.clear();
                                    for (CourseDto dto : courseDtoList) {
                                        if (dto.getName() != null && !dto.getName().isEmpty()
                                                && !allCourseNames.contains(dto.getName())) {
                                            allCourseNames.add(dto.getName());
                                        }
                                    }

                                    // 更新共享数据
                                    updateSharedCourseData(courseDtoList);

                                    // 从本地数据库删除
                                    deleteCourseFromLocalDatabase(courseDto.getId());

                                    Toast.makeText(CourseManagementActivity.this, "课程已删除", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(CourseManagementActivity.this,
                                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "课程删除失败",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(CourseManagementActivity.this, "课程删除失败: " + response.message(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Log.e("CourseManagement", "删除课程失败", t);
                            Toast.makeText(CourseManagementActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 从本地数据库删除课程
     */
    private void deleteCourseFromLocalDatabase(int courseId) {
        new Thread(() -> {
            try {
                com.example.yidong222.data.db.AppDatabase db = com.example.yidong222.data.db.AppDatabase
                        .getInstance(getApplicationContext());

                // 创建临时实体用于删除
                com.example.yidong222.data.db.entity.CourseEntity entity = new com.example.yidong222.data.db.entity.CourseEntity();
                entity.id = courseId;

                // 执行删除操作
                db.courseDao().delete(entity);
                Log.d("CourseManagement", "成功从本地数据库删除课程: ID=" + courseId);
            } catch (Exception e) {
                Log.e("CourseManagement", "从本地数据库删除课程失败", e);
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadCourses();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCourseClick(int position, Course course) {
        CourseDto courseDto = courseDtoList.get(position);

        // 显示课程详情
        new MaterialAlertDialogBuilder(this)
                .setTitle(courseDto.getName())
                .setMessage("教师: " + courseDto.getTeacher() +
                        "\n教室: " + courseDto.getClassroom() +
                        "\n时间: 周" + courseDto.getWeekday() +
                        " 第" + courseDto.getStartSection() + "-" + courseDto.getEndSection() + "节" +
                        "\n周次: 第" + courseDto.getStartWeek() + "-" + courseDto.getEndWeek() + "周" +
                        "\n学期: " + courseDto.getSemesterId())
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onCourseEditClick(int position, Course course) {
        showEditCourseDialog(position, course);
    }

    @Override
    public void onCourseDeleteClick(int position) {
        deleteCourse(position);
    }

    // 检查网络连接
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            Log.d("CourseManagement", "网络连接状态: " + (isConnected ? "已连接" : "未连接"));
            return isConnected;
        }
        return false;
    }

    private CourseDto getCourseDtoById(int id) {
        for (CourseDto dto : courseDtoList) {
            if (dto.getId() == id) {
                return dto;
            }
        }
        return null;
    }

    // 设置课程名称下拉菜单适配器
    private void setupCourseNameSpinner(Spinner spinner, String currentValue) {
        // 确保列表已初始化
        if (allCourseNames == null) {
            allCourseNames = new ArrayList<>();
        }

        if (allCourseNames.isEmpty()) {
            // 如果没有课程数据，添加一个提示选项
            allCourseNames.add("暂无可选课程");
        }

        // 使用自定义适配器
        com.example.yidong222.adapters.CourseSpinnerAdapter adapter = new com.example.yidong222.adapters.CourseSpinnerAdapter(
                this, new ArrayList<>(allCourseNames));
        spinner.setAdapter(adapter);

        // 如果有当前值，则设置为当前选择
        if (currentValue != null && !currentValue.isEmpty()) {
            int position = allCourseNames.indexOf(currentValue);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }

        // 添加选择监听器
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= allCourseNames.size()) {
                    return; // 防止越界
                }

                String selectedCourseName = allCourseNames.get(position);

                // 添加调试日志
                Log.d("CourseManagementActivity", "选择了课程: " + selectedCourseName + " 位置: " + position);

                // 如果选择了有效的课程名称（非提示信息）
                if (!selectedCourseName.equals("暂无可选课程")) {
                    // 找到对应的课程信息
                    for (CourseDto courseDto : courseDtoList) {
                        if (selectedCourseName.equals(courseDto.getName())) {
                            try {
                                // 找到匹配的课程，可以填充其他字段
                                TextInputEditText etCourseName = ((View) spinner.getParent())
                                        .findViewById(R.id.et_course_name);
                                TextInputEditText etTeacher = ((View) spinner.getParent())
                                        .findViewById(R.id.et_teacher_name);
                                TextInputEditText etClassroom = ((View) spinner.getParent())
                                        .findViewById(R.id.et_classroom);
                                Spinner spWeekday = ((View) spinner.getParent()).findViewById(R.id.spWeekday);
                                Spinner spStartSection = ((View) spinner.getParent()).findViewById(R.id.spStartSection);
                                Spinner spEndSection = ((View) spinner.getParent()).findViewById(R.id.spEndSection);

                                // 设置值
                                if (etCourseName != null)
                                    etCourseName.setText(courseDto.getName());
                                if (etTeacher != null)
                                    etTeacher.setText(courseDto.getTeacher());
                                if (etClassroom != null)
                                    etClassroom.setText(courseDto.getClassroom());

                                // 设置星期几
                                if (spWeekday != null && courseDto.getWeekday() != null &&
                                        courseDto.getWeekday() >= 1 && courseDto.getWeekday() <= 7) {
                                    spWeekday.setSelection(courseDto.getWeekday() - 1);
                                }

                                // 设置开始节次
                                if (spStartSection != null && courseDto.getStartSection() != null &&
                                        courseDto.getStartSection() >= 1 && courseDto.getStartSection() <= 12) {
                                    spStartSection.setSelection(courseDto.getStartSection() - 1);
                                }

                                // 设置结束节次
                                if (spEndSection != null && courseDto.getEndSection() != null &&
                                        courseDto.getEndSection() >= 1 && courseDto.getEndSection() <= 12) {
                                    spEndSection.setSelection(courseDto.getEndSection() - 1);
                                }
                            } catch (Exception e) {
                                Log.e("CourseManagementActivity", "设置课程信息时出错", e);
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不需要处理
            }
        });
    }

    /**
     * 将所有课程同步到本地数据库
     */
    private void syncCoursesToLocalDatabase(List<CourseDto> courses) {
        if (courses == null || courses.isEmpty()) {
            Log.w("CourseManagement", "没有课程数据需要同步到数据库");
            return;
        }

        Log.d("CourseManagement", "开始同步 " + courses.size() + " 个课程到本地数据库");

        // 在后台线程中执行数据库操作
        new Thread(() -> {
            try {
                com.example.yidong222.data.db.AppDatabase db = com.example.yidong222.data.db.AppDatabase
                        .getInstance(getApplicationContext());

                // 清空现有数据
                db.courseDao().deleteAll();
                Log.d("CourseManagement", "已清空本地课程数据");

                // 添加所有新课程
                for (CourseDto courseDto : courses) {
                    // 创建数据库实体对象
                    com.example.yidong222.data.db.entity.CourseEntity entity = new com.example.yidong222.data.db.entity.CourseEntity();
                    entity.id = courseDto.getId();
                    entity.name = courseDto.getName();
                    entity.teacher = courseDto.getTeacher();
                    entity.classroom = courseDto.getClassroom();
                    entity.weekday = courseDto.getWeekday();
                    entity.startSection = courseDto.getStartSection();
                    entity.endSection = courseDto.getEndSection();
                    entity.startWeek = courseDto.getStartWeek();
                    entity.endWeek = courseDto.getEndWeek();
                    entity.semesterId = courseDto.getSemesterId();

                    // 保存到数据库
                    db.courseDao().insert(entity);
                    Log.d("CourseManagement", "已保存课程到数据库: ID=" + entity.id + ", 名称=" + entity.name);
                }

                Log.d("CourseManagement", "成功同步所有课程到本地数据库");
            } catch (Exception e) {
                Log.e("CourseManagement", "同步课程到本地数据库失败", e);
            }
        }).start();
    }
}