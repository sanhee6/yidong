package com.example.yidong222;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.yidong222.adapters.CourseScheduleAdapter;
import com.example.yidong222.data.DataSyncManager;
import com.example.yidong222.data.repository.CourseScheduleRepository;
import com.example.yidong222.models.CourseSchedule;
import com.example.yidong222.models.CourseDto;
import com.example.yidong222.api.ApiClientHelper;
import com.example.yidong222.api.CourseApiService;
import com.example.yidong222.api.ApiClient;
import com.example.yidong222.models.ApiResponseList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class CourseScheduleActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;
    private static final int IMPORT_TIMETABLE_REQUEST_CODE = 1001;

    private DataSyncManager dataSyncManager;
    private RecyclerView recyclerView;
    private CourseScheduleAdapter adapter;
    private List<CourseSchedule> courseList;
    private FloatingActionButton fabAddCourse;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyView;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private Button btnImport;
    private ImageButton btnBatchDelete;

    // 添加API服务和课程列表
    private CourseApiService courseApiService;
    private List<CourseDto> courseDtoList = new ArrayList<>();
    private List<String> allCourseNames = new ArrayList<>();

    private int currentPage = 1;

    // 批量删除模式
    private boolean batchDeleteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_schedule);

        // 初始化视图组件
        recyclerView = findViewById(R.id.recycler_view);
        fabAddCourse = findViewById(R.id.fab_add_course);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvEmptyView = findViewById(R.id.tv_empty_view);
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        btnImport = findViewById(R.id.btn_import);
        btnBatchDelete = findViewById(R.id.btn_batch_delete);

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 初始化数据
        dataSyncManager = DataSyncManager.getInstance(this);
        courseList = new ArrayList<>();
        adapter = new CourseScheduleAdapter(this, courseList);

        // 初始化API服务
        courseApiService = ApiClient.getClient().create(CourseApiService.class);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 设置删除按钮点击监听器
        adapter.setOnDeleteClickListener((course, position) -> {
            showDeleteConfirmDialog(course);
        });

        // 设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        // 设置添加按钮点击监听器
        fabAddCourse.setOnClickListener(v -> showAddDialog());

        // 设置列表项点击监听器
        adapter.setOnItemClickListener((course, position) -> {
            showEditDialog(course);
        });

        // 设置导入按钮点击监听器
        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(CourseScheduleActivity.this, ImportTimetableActivity.class);
            startActivityForResult(intent, IMPORT_TIMETABLE_REQUEST_CODE);
        });

        // 设置批量删除按钮点击监听器
        btnBatchDelete.setOnClickListener(v -> {
            toggleBatchDeleteMode();
        });

        // 加载课程表数据
        loadData();

        // 加载课程信息数据，用于填充下拉菜单
        loadCourseData();
    }

    // 切换批量删除模式
    private void toggleBatchDeleteMode() {
        batchDeleteMode = !batchDeleteMode;
        adapter.setMultiSelectMode(batchDeleteMode);

        if (batchDeleteMode) {
            // 进入批量删除模式
            toolbarTitle.setText("选择要删除的课程");
            fabAddCourse.setVisibility(View.GONE);
            btnImport.setVisibility(View.GONE);
            btnBatchDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

            // 添加确认删除按钮
            FloatingActionButton fabConfirmDelete = new FloatingActionButton(this);
            fabConfirmDelete.setId(R.id.fab_confirm_delete);
            fabConfirmDelete.setImageResource(android.R.drawable.ic_menu_delete);
            fabConfirmDelete.setOnClickListener(v -> {
                if (adapter.getSelectedItemCount() > 0) {
                    showBatchDeleteConfirmDialog();
                } else {
                    Toast.makeText(this, "请至少选择一个课程", Toast.LENGTH_SHORT).show();
                }
            });

            // 添加到布局中
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            params.setMargins(0, 0, 16, 16);

            CoordinatorLayout coordinatorLayout = findViewById(android.R.id.content);
            coordinatorLayout.addView(fabConfirmDelete, params);
        } else {
            // 退出批量删除模式
            toolbarTitle.setText("课程表");
            fabAddCourse.setVisibility(View.VISIBLE);
            btnImport.setVisibility(View.VISIBLE);
            btnBatchDelete.setImageResource(android.R.drawable.ic_menu_delete);

            // 移除确认删除按钮
            View fabConfirmDelete = findViewById(R.id.fab_confirm_delete);
            if (fabConfirmDelete != null) {
                CoordinatorLayout coordinatorLayout = findViewById(android.R.id.content);
                coordinatorLayout.removeView(fabConfirmDelete);
            }
        }
    }

    // 显示批量删除确认对话框
    private void showBatchDeleteConfirmDialog() {
        List<CourseSchedule> selectedCourses = adapter.getSelectedItems();

        new AlertDialog.Builder(this)
                .setTitle("批量删除课程")
                .setMessage("确定要删除选中的 " + selectedCourses.size() + " 个课程吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    batchDeleteCourses(selectedCourses);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 批量删除课程
    private void batchDeleteCourses(List<CourseSchedule> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }

        // 显示进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("正在删除")
                .setMessage("正在删除所选课程...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // 记录删除成功和失败的数量
        final int[] successCount = { 0 };
        final int[] failCount = { 0 };
        final int totalCount = courses.size();

        // 创建数据仓库
        CourseScheduleRepository repository = new CourseScheduleRepository(
                ApiClient.getClient().create(com.example.yidong222.data.api.ApiService.class));

        // 遍历删除每个课程
        for (CourseSchedule course : courses) {
            repository.deleteCourseSchedule(course.getId(),
                    new CourseScheduleRepository.RepositoryCallback<com.example.yidong222.data.api.response.CourseScheduleResponse>() {
                        @Override
                        public void onSuccess(com.example.yidong222.data.api.response.CourseScheduleResponse result) {
                            successCount[0]++;
                            checkCompletion();
                        }

                        @Override
                        public void onError(Throwable error) {
                            failCount[0]++;
                            checkCompletion();
                        }

                        private void checkCompletion() {
                            if (successCount[0] + failCount[0] == totalCount) {
                                // 所有删除操作完成
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();

                                    // 显示结果
                                    String message = "成功删除 " + successCount[0] + " 个课程";
                                    if (failCount[0] > 0) {
                                        message += "，" + failCount[0] + " 个课程删除失败";
                                    }

                                    Toast.makeText(CourseScheduleActivity.this, message, Toast.LENGTH_SHORT).show();

                                    // 退出批量删除模式
                                    toggleBatchDeleteMode();

                                    // 刷新数据
                                    loadData();
                                });
                            }
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMPORT_TIMETABLE_REQUEST_CODE && resultCode == RESULT_OK) {
            // 导入课表成功，刷新数据
            loadData();
            Toast.makeText(this, "课表导入成功", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次恢复页面时刷新数据，确保与服务器同步
        loadData();
        loadCourseData();
    }

    /**
     * 从API加载课程信息，用于填充下拉菜单
     */
    private void loadCourseData() {
        // 首先尝试从CourseManagementActivity获取共享数据
        List<CourseDto> sharedCourses = CourseManagementActivity.getSharedCourseDtoList();
        List<String> sharedNames = CourseManagementActivity.getSharedCourseNames();

        // 如果共享数据中有课程信息，则直接使用
        if (!sharedCourses.isEmpty() && !sharedNames.isEmpty() && !sharedNames.get(0).equals("暂无可选课程")) {
            courseDtoList = new ArrayList<>(sharedCourses);
            allCourseNames = new ArrayList<>(sharedNames);
            Log.d("CourseScheduleActivity", "从共享数据加载到 " + courseDtoList.size() + " 个课程信息");
            return;
        }

        // 如果共享数据为空或只有默认项，则进行网络请求
        if (!dataSyncManager.isNetworkAvailable()) {
            Log.d("CourseScheduleActivity", "网络不可用，无法加载课程信息");
            // 即使网络不可用，也初始化一个空列表避免空指针异常
            allCourseNames = new ArrayList<>();
            allCourseNames.add("暂无可选课程");
            return;
        }

        // 显示加载进度提示
        swipeRefreshLayout.setRefreshing(true);

        // 使用CourseApiService直接获取课程管理页面使用的课程数据
        courseApiService.getCourses(1, 100).enqueue(new Callback<ApiResponseList<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<CourseDto>> call,
                    Response<ApiResponseList<CourseDto>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseList<CourseDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        courseDtoList = apiResponse.getData();
                        allCourseNames = new ArrayList<>(); // 创建新的列表实例

                        // 提取课程名称
                        for (CourseDto courseDto : courseDtoList) {
                            if (courseDto.getName() != null && !courseDto.getName().isEmpty()) {
                                allCourseNames.add(courseDto.getName());
                            }
                        }

                        // 如果没有课程数据，添加一个提示选项
                        if (allCourseNames.isEmpty()) {
                            allCourseNames.add("暂无可选课程");
                        }

                        Log.d("CourseScheduleActivity", "成功加载 " + courseDtoList.size() + " 个课程信息");
                    } else {
                        Log.e("CourseScheduleActivity", "加载课程信息失败: " +
                                (apiResponse.getMessage() != null ? apiResponse.getMessage() : "未知错误"));
                        allCourseNames = new ArrayList<>();
                        allCourseNames.add("暂无可选课程");
                    }
                } else {
                    Log.e("CourseScheduleActivity", "加载课程信息HTTP请求失败: " + response.code());
                    allCourseNames = new ArrayList<>();
                    allCourseNames.add("暂无可选课程");
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<CourseDto>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e("CourseScheduleActivity", "加载课程信息网络请求失败", t);
                allCourseNames = new ArrayList<>();
                allCourseNames.add("暂无可选课程");
            }
        });
    }

    /**
     * 设置课程名称下拉菜单适配器
     */
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
                Log.d("CourseScheduleActivity", "选择了课程: " + selectedCourseName + " 位置: " + position);

                // 如果选择了有效的课程名称（非提示信息）
                if (!selectedCourseName.equals("暂无可选课程")) {
                    // 找到对应的课程信息
                    for (CourseDto courseDto : courseDtoList) {
                        if (selectedCourseName.equals(courseDto.getName())) {
                            try {
                                // 找到匹配的课程，可以填充其他字段
                                EditText etCourseName = ((View) spinner.getParent()).findViewById(R.id.et_course_name);
                                EditText etTeacherName = ((View) spinner.getParent())
                                        .findViewById(R.id.et_teacher_name);
                                EditText etClassroom = ((View) spinner.getParent()).findViewById(R.id.et_classroom);

                                // 设置值
                                if (etCourseName != null)
                                    etCourseName.setText(courseDto.getName());
                                if (etTeacherName != null)
                                    etTeacherName.setText(courseDto.getTeacher());
                                if (etClassroom != null)
                                    etClassroom.setText(courseDto.getClassroom());

                                // 根据课程信息自动设置课程时间
                                if (courseDto.getWeekday() != null &&
                                        courseDto.getStartSection() != null &&
                                        courseDto.getEndSection() != null) {

                                    // 获取星期几文本
                                    String weekdayText = "";
                                    switch (courseDto.getWeekday()) {
                                        case 1:
                                            weekdayText = "周一";
                                            break;
                                        case 2:
                                            weekdayText = "周二";
                                            break;
                                        case 3:
                                            weekdayText = "周三";
                                            break;
                                        case 4:
                                            weekdayText = "周四";
                                            break;
                                        case 5:
                                            weekdayText = "周五";
                                            break;
                                        case 6:
                                            weekdayText = "周六";
                                            break;
                                        case 7:
                                            weekdayText = "周日";
                                            break;
                                    }

                                    // 生成课程时间文本
                                    String classTime = weekdayText + " " +
                                            courseDto.getStartSection() + "-" +
                                            courseDto.getEndSection() + "节";

                                    // 设置课程时间
                                    EditText etClassTime = ((View) spinner.getParent())
                                            .findViewById(R.id.et_class_time);
                                    if (etClassTime != null)
                                        etClassTime.setText(classTime);

                                    // 设置星期几下拉菜单
                                    Spinner spWeekday = ((View) spinner.getParent()).findViewById(R.id.spWeekday);
                                    if (spWeekday != null && courseDto.getWeekday() >= 1
                                            && courseDto.getWeekday() <= 7) {
                                        spWeekday.setSelection(courseDto.getWeekday() - 1);
                                    }

                                    // 设置开始和结束节次下拉菜单
                                    Spinner spStartSection = ((View) spinner.getParent())
                                            .findViewById(R.id.spStartSection);
                                    Spinner spEndSection = ((View) spinner.getParent()).findViewById(R.id.spEndSection);

                                    if (spStartSection != null && courseDto.getStartSection() >= 1
                                            && courseDto.getStartSection() <= 12) {
                                        spStartSection.setSelection(courseDto.getStartSection() - 1);
                                    }

                                    if (spEndSection != null && courseDto.getEndSection() >= 1
                                            && courseDto.getEndSection() <= 12) {
                                        spEndSection.setSelection(courseDto.getEndSection() - 1);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("CourseScheduleActivity", "设置课程信息时出错", e);
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
     * 刷新课程表数据
     */
    private void loadData() {
        swipeRefreshLayout.setRefreshing(true);

        if (!dataSyncManager.isNetworkAvailable()) {
            Toast.makeText(this, "网络不可用，显示本地数据", Toast.LENGTH_SHORT).show();
            courseList.clear();
            courseList.addAll(dataSyncManager.getLocalCourseSchedules());
            adapter.notifyDataSetChanged();
            updateEmptyView();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        dataSyncManager.syncCourseSchedules(new DataSyncManager.SyncCallback<List<CourseSchedule>>() {
            @Override
            public void onSuccess(List<CourseSchedule> result) {
                courseList.clear();
                courseList.addAll(result);

                // 确保RecyclerView设置了适配器
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setAdapter(adapter);
                }

                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyView();
            }

            @Override
            public void onError(Throwable error) {
                swipeRefreshLayout.setRefreshing(false);

                // 显示更友好的错误信息
                String errorMessage = error.getMessage();
                if (errorMessage != null && errorMessage.contains("API路径错误")) {
                    Log.e("CourseScheduleActivity", "API路径错误: " + errorMessage);
                    Toast.makeText(CourseScheduleActivity.this, "服务器配置错误，暂时无法获取课程表", Toast.LENGTH_LONG).show();

                    // 尝试刷新API客户端
                    ApiClientHelper.refreshClient();
                } else if (errorMessage != null && errorMessage.contains("API路径格式错误")) {
                    Log.e("CourseScheduleActivity", "API路径格式错误: " + errorMessage);
                    Toast.makeText(CourseScheduleActivity.this, "应用程序配置错误，请更新应用", Toast.LENGTH_LONG).show();
                } else if (errorMessage != null && errorMessage.contains("网络不可用")) {
                    Log.e("CourseScheduleActivity", "网络不可用: " + errorMessage);
                    Toast.makeText(CourseScheduleActivity.this, "网络不可用，请检查网络连接", Toast.LENGTH_LONG).show();
                } else {
                    Log.e("CourseScheduleActivity", "加载失败: " + errorMessage);
                    Toast.makeText(CourseScheduleActivity.this, "加载失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                }

                // 加载失败时尝试显示本地数据
                courseList.clear();
                courseList.addAll(dataSyncManager.getLocalCourseSchedules());

                // 确保RecyclerView设置了适配器
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setAdapter(adapter);
                }

                adapter.notifyDataSetChanged();
                updateEmptyView();
            }
        });
    }

    private void updateEmptyView() {
        // 显示或隐藏空视图
        if (courseList.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 显示添加课程对话框
     */
    private void showAddDialog() {
        if (!dataSyncManager.isNetworkAvailable()) {
            Toast.makeText(this, "网络不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框视图
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_course, null);

        // 获取输入框
        final EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        final EditText etTeacherName = dialogView.findViewById(R.id.et_teacher_name);
        final EditText etClassTime = dialogView.findViewById(R.id.et_class_time);
        final EditText etClassroom = dialogView.findViewById(R.id.et_classroom);

        // 获取下拉菜单
        final Spinner spWeekday = dialogView.findViewById(R.id.spWeekday);
        final Spinner spStartSection = dialogView.findViewById(R.id.spStartSection);
        final Spinner spEndSection = dialogView.findViewById(R.id.spEndSection);
        final Spinner spCourseNames = dialogView.findViewById(R.id.sp_course_names);

        // 设置课程名称下拉菜单
        setupCourseNameSpinner(spCourseNames, null);

        // 设置星期下拉菜单
        String[] weekdays = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };
        ArrayAdapter<String> weekdayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weekdays);
        weekdayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWeekday.setAdapter(weekdayAdapter);

        // 设置节次下拉菜单
        String[] sections = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStartSection.setAdapter(sectionAdapter);
        spEndSection.setAdapter(sectionAdapter);

        // 设置监听器，自动生成课程时间
        final Runnable updateClassTime = () -> {
            String weekday = (String) spWeekday.getSelectedItem();
            String startSection = (String) spStartSection.getSelectedItem();
            String endSection = (String) spEndSection.getSelectedItem();

            // 确保结束节次不小于开始节次
            int start = Integer.parseInt(startSection);
            int end = Integer.parseInt(endSection);
            if (end < start) {
                spEndSection.setSelection(spStartSection.getSelectedItemPosition());
                end = start;
            }

            String classTime = weekday + " " + start + "-" + end + "节";
            etClassTime.setText(classTime);
        };

        spWeekday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spStartSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spEndSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 初始化课程时间
        updateClassTime.run();

        // 创建并显示对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("添加课程")
                .setView(dialogView)
                .setPositiveButton("添加", null) // 先设置为null，后面手动处理点击事件
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 手动处理确定按钮点击事件，以便进行输入验证
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // 获取输入值
            String courseName = etCourseName.getText().toString().trim();
            String teacherName = etTeacherName.getText().toString().trim();
            String classTime = etClassTime.getText().toString().trim();
            String classroom = etClassroom.getText().toString().trim();

            // 验证输入
            if (courseName.isEmpty()) {
                etCourseName.setError("课程名称不能为空");
                Toast.makeText(CourseScheduleActivity.this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示加载提示
            swipeRefreshLayout.setRefreshing(true);

            // 添加课程
            dataSyncManager.addCourseSchedule(courseName, teacherName, classTime, classroom,
                    new DataSyncManager.SyncCallback<CourseSchedule>() {
                        @Override
                        public void onSuccess(CourseSchedule result) {
                            runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                // 刷新列表
                                courseList.clear();
                                courseList.addAll(dataSyncManager.getLocalCourseSchedules());
                                adapter.notifyDataSetChanged();
                                updateEmptyView();
                                Toast.makeText(CourseScheduleActivity.this, "课程添加成功", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(Throwable error) {
                            runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                String errorMsg = error.getMessage();
                                if (errorMsg != null && errorMsg.contains("课程名称不能为空")) {
                                    etCourseName.setError("课程名称不能为空");
                                    Toast.makeText(CourseScheduleActivity.this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(CourseScheduleActivity.this, "添加失败: " + error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        });
    }

    /**
     * 显示编辑课程对话框
     * 
     * @param course 要编辑的课程
     */
    private void showEditDialog(CourseSchedule course) {
        if (!dataSyncManager.isNetworkAvailable()) {
            Toast.makeText(this, "网络不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框视图
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_course, null);

        // 获取输入框
        final EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        final EditText etTeacherName = dialogView.findViewById(R.id.et_teacher_name);
        final EditText etClassTime = dialogView.findViewById(R.id.et_class_time);
        final EditText etClassroom = dialogView.findViewById(R.id.et_classroom);

        // 获取下拉菜单
        final Spinner spWeekday = dialogView.findViewById(R.id.spWeekday);
        final Spinner spStartSection = dialogView.findViewById(R.id.spStartSection);
        final Spinner spEndSection = dialogView.findViewById(R.id.spEndSection);
        final Spinner spCourseNames = dialogView.findViewById(R.id.sp_course_names);

        // 设置课程名称下拉菜单
        setupCourseNameSpinner(spCourseNames, course.getCourseName());

        // 设置星期下拉菜单
        String[] weekdays = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };
        ArrayAdapter<String> weekdayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weekdays);
        weekdayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWeekday.setAdapter(weekdayAdapter);

        // 设置节次下拉菜单
        String[] sections = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" };
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStartSection.setAdapter(sectionAdapter);
        spEndSection.setAdapter(sectionAdapter);

        // 设置初始值
        etCourseName.setText(course.getCourseName());
        etTeacherName.setText(course.getTeacherName());
        etClassTime.setText(course.getClassTime());
        etClassroom.setText(course.getClassroom());

        // 解析课程时间并设置下拉菜单初始值
        try {
            String classTime = course.getClassTime();
            if (classTime != null && !classTime.isEmpty()) {
                // 解析格式如 "周一 1-5节"
                String[] parts = classTime.split(" ");
                if (parts.length >= 2) {
                    String weekday = parts[0]; // 周一
                    String sectionPart = parts[1].replace("节", ""); // 1-5
                    String[] sectionRange = sectionPart.split("-");
                    if (sectionRange.length >= 2) {
                        String startSection = sectionRange[0]; // 1
                        String endSection = sectionRange[1]; // 5

                        // 设置星期下拉菜单
                        for (int i = 0; i < weekdays.length; i++) {
                            if (weekdays[i].equals(weekday)) {
                                spWeekday.setSelection(i);
                                break;
                            }
                        }

                        // 设置节次下拉菜单
                        for (int i = 0; i < sections.length; i++) {
                            if (sections[i].equals(startSection)) {
                                spStartSection.setSelection(i);
                                break;
                            }
                        }

                        for (int i = 0; i < sections.length; i++) {
                            if (sections[i].equals(endSection)) {
                                spEndSection.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CourseScheduleActivity", "解析课程时间失败", e);
        }

        // 设置监听器，自动生成课程时间
        final Runnable updateClassTime = () -> {
            String weekday = (String) spWeekday.getSelectedItem();
            String startSection = (String) spStartSection.getSelectedItem();
            String endSection = (String) spEndSection.getSelectedItem();

            // 确保结束节次不小于开始节次
            int start = Integer.parseInt(startSection);
            int end = Integer.parseInt(endSection);
            if (end < start) {
                spEndSection.setSelection(spStartSection.getSelectedItemPosition());
                end = start;
            }

            String classTime = weekday + " " + start + "-" + end + "节";
            etClassTime.setText(classTime);
        };

        spWeekday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spStartSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spEndSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateClassTime.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 创建并显示对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑课程")
                .setView(dialogView)
                .setPositiveButton("保存", null) // 先设置为null，后面手动处理点击事件
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 手动处理确定按钮点击事件，以便进行输入验证
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // 获取输入值
            String courseName = etCourseName.getText().toString().trim();
            String teacherName = etTeacherName.getText().toString().trim();
            String classTime = etClassTime.getText().toString().trim();
            String classroom = etClassroom.getText().toString().trim();

            // 验证输入
            if (courseName.isEmpty()) {
                etCourseName.setError("课程名称不能为空");
                Toast.makeText(CourseScheduleActivity.this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示加载提示
            swipeRefreshLayout.setRefreshing(true);

            // 更新课程
            dataSyncManager.updateCourseSchedule(course.getId(), courseName, teacherName, classTime, classroom,
                    new DataSyncManager.SyncCallback<CourseSchedule>() {
                        @Override
                        public void onSuccess(CourseSchedule result) {
                            runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                // 刷新列表
                                courseList.clear();
                                courseList.addAll(dataSyncManager.getLocalCourseSchedules());
                                adapter.notifyDataSetChanged();
                                updateEmptyView();
                                Toast.makeText(CourseScheduleActivity.this, "课程更新成功", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(Throwable error) {
                            runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                String errorMsg = error.getMessage();
                                if (errorMsg != null && errorMsg.contains("课程名称不能为空")) {
                                    etCourseName.setError("课程名称不能为空");
                                    Toast.makeText(CourseScheduleActivity.this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(CourseScheduleActivity.this, "更新失败: " + error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
        });
    }

    /**
     * 显示删除确认对话框
     * 
     * @param course 要删除的课程
     */
    private void showDeleteConfirmDialog(CourseSchedule course) {
        new AlertDialog.Builder(this)
                .setTitle("删除课程")
                .setMessage("确定要删除课程 " + course.getCourseName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (!dataSyncManager.isNetworkAvailable()) {
                        Toast.makeText(this, "网络不可用", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    swipeRefreshLayout.setRefreshing(true);
                    dataSyncManager.deleteCourseSchedule(course.getId(), new DataSyncManager.SyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            courseList.clear();
                            courseList.addAll(dataSyncManager.getLocalCourseSchedules());
                            adapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(CourseScheduleActivity.this, "课程删除成功", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Throwable error) {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(CourseScheduleActivity.this, "删除失败: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}