package com.example.yidong222;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.yidong222.adapters.GradeAdapter;
import com.example.yidong222.api.ApiClient;
import com.example.yidong222.api.GradeApiService;
import com.example.yidong222.api.CourseApiService;
import com.example.yidong222.api.AssignmentApiService;
import com.example.yidong222.data.DataManager;
import com.example.yidong222.data.DataSyncManager;
import com.example.yidong222.models.ApiResponse;
import com.example.yidong222.models.ApiResponseList;
import com.example.yidong222.models.GradeDto;
import com.example.yidong222.models.CourseDto;
import com.example.yidong222.models.AssignmentDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GradeActivity extends AppCompatActivity implements GradeAdapter.GradeItemClickListener {
    private Toolbar toolbar;
    private RecyclerView recyclerViewGrades;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyGrades;
    private TextView tvGpaInfo;
    private GradeAdapter adapter;
    private List<Map<String, Object>> gradeDataList;
    private DataSyncManager dataSyncManager;
    private FloatingActionButton fabAddGrade;
    private GradeApiService apiService;
    private AssignmentApiService assignmentApiService;
    private UpdateCourseSelection updateCourseSelectionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade);

        Log.d("GradeActivity", "正在初始化GradeActivity...");

        // 初始化API服务
        apiService = ApiClient.getClient().create(GradeApiService.class);
        assignmentApiService = ApiClient.getClient().create(AssignmentApiService.class);

        // 初始化控件
        toolbar = findViewById(R.id.toolbar);
        recyclerViewGrades = findViewById(R.id.recyclerViewGrades);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyGrades = findViewById(R.id.tvEmptyGrades);
        tvGpaInfo = findViewById(R.id.tvGpaInfo);
        fabAddGrade = findViewById(R.id.fabAddGrade);

        // 设置Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("成绩查询");

        // 初始化数据
        dataSyncManager = DataSyncManager.getInstance(this);
        gradeDataList = new ArrayList<>();
        adapter = new GradeAdapter(this, gradeDataList);
        adapter.setGradeItemClickListener(this);

        // 设置RecyclerView
        recyclerViewGrades.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGrades.setAdapter(adapter);

        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadGradeData();
        });

        // 设置添加按钮点击事件
        fabAddGrade.setOnClickListener(v -> {
            showAddGradeDialog();
        });

        // 检查并加载课程数据
        ensureCoursesLoaded();

        // 加载数据
        loadGradeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureCoursesLoaded();
        loadGradeData();
    }

    // 确保课程数据已加载
    private void ensureCoursesLoaded() {
        // 创建API服务
        CourseApiService courseApiService = ApiClient.getClient().create(CourseApiService.class);

        // 调用API获取最新课程列表
        courseApiService.getCourses(1, 100).enqueue(new Callback<ApiResponseList<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<CourseDto>> call,
                    Response<ApiResponseList<CourseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseList<CourseDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        List<CourseDto> courseDtos = apiResponse.getData();

                        // 更新共享数据
                        CourseManagementActivity.updateSharedCourseData(courseDtos);

                        List<String> courseNames = CourseManagementActivity.getSharedCourseNames();
                        Log.d("GradeActivity", "成功从数据库同步课程数据: " + courseNames.size() + "个");
                    } else {
                        Log.e("GradeActivity", "同步课程数据失败: " + apiResponse.getMessage());
                    }
                } else {
                    Log.e("GradeActivity", "同步课程数据API调用失败");
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<CourseDto>> call, Throwable t) {
                Log.e("GradeActivity", "同步课程数据网络错误: " + t.getMessage());
            }
        });
    }

    private void loadGradeData() {
        swipeRefreshLayout.setRefreshing(true);

        if (!dataSyncManager.isNetworkAvailable()) {
            Toast.makeText(this, "网络不可用，请检查网络连接", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            updateEmptyView(true);
            return;
        }

        // 显示正在加载提示
        Toast.makeText(this, "正在加载成绩数据...", Toast.LENGTH_SHORT).show();

        // 强制更新data/api/ApiClient，确保使用最新的URL
        com.example.yidong222.data.api.ApiClient.resetClient();

        // 检查并同步主ApiClient和DataSyncManager的ApiClient URL
        Log.d("GradeActivity", "主ApiClient URL: " + com.example.yidong222.api.ApiClient.getBaseUrl());
        Log.d("GradeActivity",
                "DataSyncManager ApiClient URL: " + com.example.yidong222.data.api.ApiClient.getBaseUrl());

        // 从API获取成绩
        apiService.getGrades(1).enqueue(new Callback<ApiResponseList<GradeDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<GradeDto>> call,
                    Response<ApiResponseList<GradeDto>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseList<GradeDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus())) {
                        List<GradeDto> grades = apiResponse.getData();
                        gradeDataList.clear();

                        // 转换GradeDto为Map
                        for (GradeDto gradeDto : grades) {
                            Map<String, Object> gradeMap = new HashMap<>();
                            gradeMap.put("id", gradeDto.getId());
                            gradeMap.put("courseName", gradeDto.getCourseName());
                            gradeMap.put("courseType", gradeDto.getGradeType());
                            gradeMap.put("semester", gradeDto.getSubmissionDate());
                            gradeMap.put("score", gradeDto.getScore());
                            gradeMap.put("credit", 3.0); // 默认学分
                            gradeMap.put("gpa", calculateGpa(gradeDto.getScore()));
                            gradeMap.put("courseId", gradeDto.getCourseId());
                            gradeDataList.add(gradeMap);
                        }

                        adapter.notifyDataSetChanged();
                        updateEmptyView(gradeDataList.isEmpty());

                        // 显示GPA信息（需要计算总学分和平均GPA）
                        if (!gradeDataList.isEmpty()) {
                            double totalGpa = 0;
                            double totalCredits = 0;
                            for (Map<String, Object> grade : gradeDataList) {
                                double credit = (double) grade.getOrDefault("credit", 0.0);
                                double gpa = Double.parseDouble(grade.getOrDefault("gpa", "0.0").toString());
                                totalCredits += credit;
                                totalGpa += gpa * credit;
                            }
                            double avgGpa = totalCredits > 0 ? totalGpa / totalCredits : 0;
                            tvGpaInfo.setText(String.format("平均绩点: %.2f  总学分: %.1f", avgGpa, totalCredits));
                            tvGpaInfo.setVisibility(View.VISIBLE);
                        } else {
                            tvGpaInfo.setVisibility(View.GONE);
                        }

                        Toast.makeText(GradeActivity.this, "成功加载 " + gradeDataList.size() + " 条成绩记录",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GradeActivity.this, "加载失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        updateEmptyView(true);
                    }
                } else {
                    Toast.makeText(GradeActivity.this, "加载失败: 服务器错误", Toast.LENGTH_SHORT).show();
                    updateEmptyView(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<GradeDto>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(GradeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
            }
        });
    }

    private String calculateGpa(Double score) {
        if (score == null)
            return "0.0";

        if (score >= 90) {
            return "4.0";
        } else if (score >= 85) {
            return "3.7";
        } else if (score >= 80) {
            return "3.3";
        } else if (score >= 75) {
            return "3.0";
        } else if (score >= 70) {
            return "2.7";
        } else if (score >= 65) {
            return "2.3";
        } else if (score >= 60) {
            return "2.0";
        } else {
            return "0.0";
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            tvEmptyGrades.setVisibility(View.VISIBLE);
            recyclerViewGrades.setVisibility(View.GONE);
        } else {
            tvEmptyGrades.setVisibility(View.GONE);
            recyclerViewGrades.setVisibility(View.VISIBLE);
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

    // 显示添加成绩对话框
    private void showAddGradeDialog() {
        // 检查是否有可用课程
        List<String> courseNames = CourseManagementActivity.getSharedCourseNames();
        if (courseNames.isEmpty() || (courseNames.size() == 1 &&
                (courseNames.get(0).equals("暂无可选课程") || courseNames.get(0).equals("请先添加课程")))) {

            // 提示用户先添加课程
            new AlertDialog.Builder(this)
                    .setTitle("没有可用课程")
                    .setMessage("您需要先添加课程才能录入成绩。是否前往课程管理页面添加课程？")
                    .setPositiveButton("前往添加", (dialog, which) -> {
                        // 跳转到课程管理页面
                        Intent intent = new Intent(this, CourseManagementActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_grade, null);
        builder.setView(dialogView);
        builder.setTitle("添加成绩");

        // 初始化控件
        Spinner spinnerCourseName = dialogView.findViewById(R.id.spinnerCourseName);
        Spinner spinnerAssignment = dialogView.findViewById(R.id.spinnerAssignment);
        TextInputEditText etCourseType = dialogView.findViewById(R.id.etCourseType);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etScore = dialogView.findViewById(R.id.etScore);
        TextInputEditText etCredit = dialogView.findViewById(R.id.etCredit);

        // 设置课程名称下拉列表
        setupCourseNameSpinner(spinnerCourseName, null);

        // 设置更新课程选择的回调
        updateCourseSelectionListener = (newCourseId, newCourseName) -> {
            if (newCourseId != null && newCourseName != null) {
                Log.d("GradeActivity", "更新课程选择为: ID=" + newCourseId + ", 名称=" + newCourseName);

                // 查找课程在下拉列表中的位置
                for (int i = 0; i < spinnerCourseName.getAdapter().getCount(); i++) {
                    String item = spinnerCourseName.getAdapter().getItem(i).toString();
                    if (item.equals(newCourseName)) {
                        spinnerCourseName.setSelection(i);
                        Log.d("GradeActivity", "已设置课程下拉列表选中项: " + newCourseName);
                        break;
                    }
                }
            }
        };

        // 直接加载所有作业列表
        setupAllAssignmentsSpinner(spinnerAssignment, null);

        // 默认值
        etCourseType.setText("必修");
        etSemester.setText("2023-2024学年第一学期");
        etCredit.setText("3.0");

        // 设置按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            // 获取课程ID
            String courseName = spinnerCourseName.getSelectedItem().toString();
            Integer courseId = getCourseIdByName(courseName);

            if (courseId == null) {
                Toast.makeText(this, "请选择有效的课程", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取作业ID
            Integer assignmentId = null;
            if (spinnerAssignment.getSelectedItem() instanceof AssignmentSpinnerItem) {
                AssignmentSpinnerItem selectedAssignment = (AssignmentSpinnerItem) spinnerAssignment.getSelectedItem();
                assignmentId = selectedAssignment.getId();
                Log.d("GradeActivity", "选择的作业ID: " + (assignmentId != null ? assignmentId : "null"));
            }

            // 获取其他字段
            String courseType = etCourseType.getText().toString();
            String semester = etSemester.getText().toString();

            double score;
            try {
                score = Double.parseDouble(etScore.getText().toString());
                if (score < 0 || score > 100) {
                    Toast.makeText(this, "分数必须在0-100之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的分数", Toast.LENGTH_SHORT).show();
                return;
            }

            double credit;
            try {
                credit = Double.parseDouble(etCredit.getText().toString());
                if (credit <= 0) {
                    Toast.makeText(this, "学分必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的学分", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建成绩DTO
            GradeDto gradeDto = new GradeDto();
            gradeDto.setCourseId(courseId);
            gradeDto.setCourseName(courseName);
            gradeDto.setScore(score);
            gradeDto.setGradeType(courseType);

            // 设置日期格式
            String formattedDate = formatSubmissionDate(semester);
            gradeDto.setSubmissionDate(formattedDate);

            // 设置作业ID
            gradeDto.setAssignmentId(assignmentId);

            // 设置学生ID
            gradeDto.setStudentId("2023001");

            // 保存成绩
            createGrade(gradeDto, credit);
        });

        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 创建作业并获取作业ID，然后创建成绩
    private void createAssignmentAndThenGrade(Integer courseId, String courseName, String courseType,
            String semester, double score, double credit) {
        // 显示加载对话框
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在处理...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 先查询已有的作业
        assignmentApiService.getAssignmentsByCourse(courseId).enqueue(new Callback<ApiResponseList<AssignmentDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<AssignmentDto>> call,
                    Response<ApiResponseList<AssignmentDto>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    List<AssignmentDto> assignments = response.body().getData();

                    // 如果课程已有作业，使用第一个作业
                    if (assignments != null && !assignments.isEmpty()) {
                        AssignmentDto existingAssignment = assignments.get(0);
                        Log.d("GradeActivity", "使用已有作业: id=" + existingAssignment.getId());

                        // 创建成绩DTO
                        GradeDto gradeDto = new GradeDto();
                        gradeDto.setCourseId(courseId);
                        gradeDto.setCourseName(courseName);
                        gradeDto.setScore(score);
                        gradeDto.setGradeType(courseType);

                        // 设置日期格式
                        String formattedDate = formatSubmissionDate(semester);
                        gradeDto.setSubmissionDate(formattedDate);

                        // 设置作业ID
                        gradeDto.setAssignmentId(existingAssignment.getId());

                        // 设置学生ID
                        gradeDto.setStudentId("2023001");

                        // 关闭进度对话框
                        progressDialog.dismiss();

                        // 保存成绩
                        createGrade(gradeDto, credit);
                    } else {
                        // 没有找到课程相关的作业，使用null作为作业ID
                        Log.d("GradeActivity", "没有找到课程相关的作业，使用null作为作业ID");

                        // 创建成绩DTO
                        GradeDto gradeDto = new GradeDto();
                        gradeDto.setCourseId(courseId);
                        gradeDto.setCourseName(courseName);
                        gradeDto.setScore(score);
                        gradeDto.setGradeType(courseType);

                        // 设置日期格式
                        String formattedDate = formatSubmissionDate(semester);
                        gradeDto.setSubmissionDate(formattedDate);

                        // 设置作业ID为null
                        gradeDto.setAssignmentId(null);

                        // 设置学生ID
                        gradeDto.setStudentId("2023001");

                        // 关闭进度对话框
                        progressDialog.dismiss();

                        // 保存成绩
                        createGrade(gradeDto, credit);
                    }
                } else {
                    // API调用失败时，尝试不使用作业ID
                    Log.d("GradeActivity", "无法获取课程作业列表，使用null作为作业ID");

                    // 创建成绩DTO
                    GradeDto gradeDto = new GradeDto();
                    gradeDto.setCourseId(courseId);
                    gradeDto.setCourseName(courseName);
                    gradeDto.setScore(score);
                    gradeDto.setGradeType(courseType);

                    // 设置日期格式
                    String formattedDate = formatSubmissionDate(semester);
                    gradeDto.setSubmissionDate(formattedDate);

                    // 设置作业ID为null
                    gradeDto.setAssignmentId(null);

                    // 设置学生ID
                    gradeDto.setStudentId("2023001");

                    // 关闭进度对话框
                    progressDialog.dismiss();

                    // 保存成绩
                    createGrade(gradeDto, credit);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<AssignmentDto>> call, Throwable t) {
                // 网络请求失败时，使用null作为作业ID
                Log.d("GradeActivity", "获取作业列表网络请求失败，使用null作为作业ID");

                // 创建成绩DTO
                GradeDto gradeDto = new GradeDto();
                gradeDto.setCourseId(courseId);
                gradeDto.setCourseName(courseName);
                gradeDto.setScore(score);
                gradeDto.setGradeType(courseType);

                // 设置日期格式
                String formattedDate = formatSubmissionDate(semester);
                gradeDto.setSubmissionDate(formattedDate);

                // 设置作业ID为null
                gradeDto.setAssignmentId(null);

                // 设置学生ID
                gradeDto.setStudentId("2023001");

                // 关闭进度对话框
                progressDialog.dismiss();

                // 保存成绩
                createGrade(gradeDto, credit);
            }
        });
    }

    // 获取已有的作业列表，用于在创建作业失败时作为备选方案
    private void fetchExistingAssignmentsForGrade(Integer courseId, String courseName, String courseType,
            String semester, double score, double credit) {
        assignmentApiService.getAssignments(1, 100).enqueue(new Callback<ApiResponseList<AssignmentDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<AssignmentDto>> call,
                    Response<ApiResponseList<AssignmentDto>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    List<AssignmentDto> assignments = response.body().getData();

                    if (assignments != null && !assignments.isEmpty()) {
                        // 使用第一个作业的ID
                        AssignmentDto firstAssignment = assignments.get(0);
                        Integer assignmentId = firstAssignment.getId();

                        Log.d("GradeActivity", "使用系统中已有的作业: id=" + assignmentId);

                        // 创建成绩DTO
                        GradeDto gradeDto = new GradeDto();
                        gradeDto.setCourseId(courseId);
                        gradeDto.setCourseName(courseName);
                        gradeDto.setScore(score);
                        gradeDto.setGradeType(courseType);

                        // 设置日期格式
                        String formattedDate = formatSubmissionDate(semester);
                        gradeDto.setSubmissionDate(formattedDate);

                        // 设置作业ID
                        gradeDto.setAssignmentId(assignmentId);

                        // 设置学生ID
                        gradeDto.setStudentId("2023001");

                        // 保存成绩
                        createGrade(gradeDto, credit);
                    } else {
                        // 没有作业时，创建成绩但设置作业ID为null
                        Log.d("GradeActivity", "系统中没有可用的作业，使用null作为作业ID");

                        // 创建成绩DTO
                        GradeDto gradeDto = new GradeDto();
                        gradeDto.setCourseId(courseId);
                        gradeDto.setCourseName(courseName);
                        gradeDto.setScore(score);
                        gradeDto.setGradeType(courseType);

                        // 设置日期格式
                        String formattedDate = formatSubmissionDate(semester);
                        gradeDto.setSubmissionDate(formattedDate);

                        // 设置作业ID为null
                        gradeDto.setAssignmentId(null);

                        // 设置学生ID
                        gradeDto.setStudentId("2023001");

                        // 保存成绩
                        createGrade(gradeDto, credit);
                    }
                } else {
                    // API调用失败时，尝试不使用作业ID
                    Log.d("GradeActivity", "无法获取作业列表，使用null作为作业ID");

                    // 创建成绩DTO
                    GradeDto gradeDto = new GradeDto();
                    gradeDto.setCourseId(courseId);
                    gradeDto.setCourseName(courseName);
                    gradeDto.setScore(score);
                    gradeDto.setGradeType(courseType);

                    // 设置日期格式
                    String formattedDate = formatSubmissionDate(semester);
                    gradeDto.setSubmissionDate(formattedDate);

                    // 设置作业ID为null
                    gradeDto.setAssignmentId(null);

                    // 设置学生ID
                    gradeDto.setStudentId("2023001");

                    // 保存成绩
                    createGrade(gradeDto, credit);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<AssignmentDto>> call, Throwable t) {
                // 网络请求失败时，尝试不使用作业ID
                Log.d("GradeActivity", "获取作业列表网络请求失败，使用null作为作业ID");

                // 创建成绩DTO
                GradeDto gradeDto = new GradeDto();
                gradeDto.setCourseId(courseId);
                gradeDto.setCourseName(courseName);
                gradeDto.setScore(score);
                gradeDto.setGradeType(courseType);

                // 设置日期格式
                String formattedDate = formatSubmissionDate(semester);
                gradeDto.setSubmissionDate(formattedDate);

                // 设置作业ID为null
                gradeDto.setAssignmentId(null);

                // 设置学生ID
                gradeDto.setStudentId("2023001");

                // 保存成绩
                createGrade(gradeDto, credit);
            }
        });
    }

    // 显示编辑成绩对话框
    private void showEditGradeDialog(int position, Map<String, Object> grade) {
        // 添加空值检查
        if (grade == null) {
            Log.e("GradeActivity", "成绩对象为空");
            Toast.makeText(this, "无法编辑成绩，数据无效", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_grade, null);
        builder.setView(dialogView);
        builder.setTitle("编辑成绩");

        // 初始化控件
        Spinner spinnerCourseName = dialogView.findViewById(R.id.spinnerCourseName);
        Spinner spinnerAssignment = dialogView.findViewById(R.id.spinnerAssignment);
        TextInputEditText etCourseType = dialogView.findViewById(R.id.etCourseType);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        TextInputEditText etScore = dialogView.findViewById(R.id.etScore);
        TextInputEditText etCredit = dialogView.findViewById(R.id.etCredit);

        // 获取成绩信息，添加空值检查
        final int gradeId;
        String courseName = "";
        String courseType = "";
        String semester = "";
        double score = 0.0;
        double credit = 0.0;
        Integer courseId = 0;

        try {
            // 检查id字段
            if (grade.containsKey("id") && grade.get("id") != null) {
                gradeId = Integer.parseInt(grade.get("id").toString());
            } else {
                Log.e("GradeActivity", "成绩ID为空");
                Toast.makeText(this, "成绩ID无效", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查courseName字段
            if (grade.containsKey("courseName") && grade.get("courseName") != null) {
                courseName = grade.get("courseName").toString();
            } else {
                Log.w("GradeActivity", "课程名称为空，使用默认值");
                courseName = "未知课程";
            }

            // 检查gradeType字段
            if (grade.containsKey("gradeType") && grade.get("gradeType") != null) {
                courseType = grade.get("gradeType").toString();
            } else {
                Log.w("GradeActivity", "课程类型为空，使用默认值");
                courseType = "未知类型";
            }

            // 检查submissionDate字段
            if (grade.containsKey("submissionDate") && grade.get("submissionDate") != null) {
                semester = grade.get("submissionDate").toString();
            } else {
                Log.w("GradeActivity", "学期为空，使用默认值");
                semester = "未知学期";
            }

            // 检查score字段
            if (grade.containsKey("score") && grade.get("score") != null) {
                score = Double.parseDouble(grade.get("score").toString());
            } else {
                Log.w("GradeActivity", "分数为空，使用默认值");
            }

            // 检查credit字段
            if (grade.containsKey("credit") && grade.get("credit") != null) {
                credit = Double.parseDouble(grade.get("credit").toString());
            } else {
                Log.w("GradeActivity", "学分为空，使用默认值");
            }

            // 检查courseId字段
            if (grade.containsKey("courseId") && grade.get("courseId") != null) {
                courseId = Integer.parseInt(grade.get("courseId").toString());
            } else {
                Log.e("GradeActivity", "课程ID为空");
                Toast.makeText(this, "课程ID无效", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Log.e("GradeActivity", "解析成绩数据时出错", e);
            Toast.makeText(this, "成绩数据格式无效", Toast.LENGTH_SHORT).show();
            return;
        } catch (Exception e) {
            Log.e("GradeActivity", "处理成绩数据时出错", e);
            Toast.makeText(this, "无法处理成绩数据", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前作业ID
        Integer currentAssignmentId = null;
        if (grade.containsKey("assignmentId") && grade.get("assignmentId") != null) {
            Object assignmentIdObj = grade.get("assignmentId");
            if (assignmentIdObj instanceof Integer) {
                currentAssignmentId = (Integer) assignmentIdObj;
            } else if (assignmentIdObj instanceof String) {
                try {
                    currentAssignmentId = Integer.parseInt((String) assignmentIdObj);
                } catch (NumberFormatException e) {
                    Log.e("GradeActivity", "无法解析作业ID: " + assignmentIdObj, e);
                }
            }
        }

        Log.d("GradeActivity", "当前成绩对应的作业ID: " + (currentAssignmentId != null ? currentAssignmentId : "null"));

        // 设置课程名称下拉列表
        setupCourseNameSpinner(spinnerCourseName, courseName);

        // 设置更新课程选择的回调
        updateCourseSelectionListener = (newCourseId, newCourseName) -> {
            if (newCourseId != null && newCourseName != null) {
                Log.d("GradeActivity", "更新课程选择为: ID=" + newCourseId + ", 名称=" + newCourseName);

                // 查找课程在下拉列表中的位置
                for (int i = 0; i < spinnerCourseName.getAdapter().getCount(); i++) {
                    String item = spinnerCourseName.getAdapter().getItem(i).toString();
                    if (item.equals(newCourseName)) {
                        spinnerCourseName.setSelection(i);
                        Log.d("GradeActivity", "已设置课程下拉列表选中项: " + newCourseName);
                        break;
                    }
                }
            }
        };

        // 设置作业下拉列表
        setupAllAssignmentsSpinner(spinnerAssignment, currentAssignmentId);

        // 设置其他字段值
        etCourseType.setText(courseType);
        etSemester.setText(semester);
        etScore.setText(String.valueOf(score));
        etCredit.setText(String.valueOf(credit));

        // 设置按钮
        builder.setPositiveButton("更新", (dialog, which) -> {
            // 获取课程ID
            String newCourseName = spinnerCourseName.getSelectedItem().toString();
            Integer newCourseId = getCourseIdByName(newCourseName);

            if (newCourseId == null) {
                Toast.makeText(this, "请选择有效的课程", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取作业ID
            Integer assignmentId = null;
            if (spinnerAssignment.getSelectedItem() instanceof AssignmentSpinnerItem) {
                AssignmentSpinnerItem selectedAssignment = (AssignmentSpinnerItem) spinnerAssignment.getSelectedItem();
                assignmentId = selectedAssignment.getId();
                Log.d("GradeActivity", "选择的作业ID: " + (assignmentId != null ? assignmentId : "null"));
            }

            // 获取其他字段
            String newCourseType = etCourseType.getText().toString();
            String newSemester = etSemester.getText().toString();

            double newScore;
            try {
                newScore = Double.parseDouble(etScore.getText().toString());
                if (newScore < 0 || newScore > 100) {
                    Toast.makeText(this, "分数必须在0-100之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的分数", Toast.LENGTH_SHORT).show();
                return;
            }

            double newCredit;
            try {
                newCredit = Double.parseDouble(etCredit.getText().toString());
                if (newCredit <= 0) {
                    Toast.makeText(this, "学分必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的学分", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建成绩DTO
            GradeDto gradeDto = new GradeDto();
            gradeDto.setCourseId(newCourseId);
            gradeDto.setCourseName(newCourseName);
            gradeDto.setScore(newScore);
            gradeDto.setGradeType(newCourseType);

            // 设置日期格式
            String formattedDate = formatSubmissionDate(newSemester);
            gradeDto.setSubmissionDate(formattedDate);

            // 设置作业ID
            gradeDto.setAssignmentId(assignmentId);

            // 设置学生ID
            gradeDto.setStudentId("2023001");

            // 更新成绩
            updateGrade(position, gradeId, gradeDto, newCredit);
        });

        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 确认删除成绩
    private void confirmDeleteGrade(int position, Map<String, Object> grade) {
        new AlertDialog.Builder(this)
                .setTitle("删除成绩")
                .setMessage("确定要删除这条成绩记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    Integer gradeId = null;
                    try {
                        if (grade != null && grade.containsKey("id") && grade.get("id") != null) {
                            if (grade.get("id") instanceof Integer) {
                                gradeId = (Integer) grade.get("id");
                            } else if (grade.get("id") instanceof String) {
                                gradeId = Integer.parseInt(grade.get("id").toString());
                            } else {
                                gradeId = Integer.parseInt(grade.get("id").toString());
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e("GradeActivity", "解析成绩ID时出错", e);
                    }

                    if (gradeId != null) {
                        deleteGrade(position, gradeId);
                    } else {
                        Toast.makeText(this, "成绩ID无效", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 创建成绩
    private void createGrade(GradeDto gradeDto, double credit) {
        // 在发送请求前记录完整的请求数据
        Log.d("GradeActivity", "准备创建成绩，请求数据: courseId=" + gradeDto.getCourseId()
                + ", courseName=" + gradeDto.getCourseName()
                + ", score=" + gradeDto.getScore()
                + ", gradeType=" + gradeDto.getGradeType()
                + ", submissionDate=" + gradeDto.getSubmissionDate()
                + ", assignmentId=" + gradeDto.getAssignmentId()
                + ", studentId=" + gradeDto.getStudentId());

        apiService.createGrade(gradeDto).enqueue(new Callback<ApiResponse<GradeDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<GradeDto>> call, Response<ApiResponse<GradeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<GradeDto> apiResponse = response.body();
                    Log.d("GradeActivity",
                            "成绩创建响应: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());

                    if ("success".equals(apiResponse.getStatus())) {
                        GradeDto createdGrade = apiResponse.getData();
                        Log.d("GradeActivity", "成功创建成绩: id=" + createdGrade.getId());

                        // 添加到本地数据列表
                        Map<String, Object> gradeMap = new HashMap<>();
                        gradeMap.put("id", createdGrade.getId());
                        gradeMap.put("courseName", createdGrade.getCourseName());
                        gradeMap.put("courseType", createdGrade.getGradeType());
                        gradeMap.put("semester", createdGrade.getSubmissionDate());
                        gradeMap.put("score", createdGrade.getScore());
                        gradeMap.put("credit", credit);
                        gradeMap.put("gpa", calculateGpa(createdGrade.getScore()));
                        gradeMap.put("courseId", createdGrade.getCourseId());

                        gradeDataList.add(gradeMap);
                        adapter.notifyItemInserted(gradeDataList.size() - 1);
                        updateEmptyView(false);

                        Toast.makeText(GradeActivity.this, "成功添加成绩", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("GradeActivity", "添加成绩失败: " + apiResponse.getMessage());
                        Toast.makeText(GradeActivity.this, "添加失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("GradeActivity", "添加成绩错误: " + errorBody);
                            Log.e("GradeActivity", "HTTP状态码: " + response.code());

                            // 处理错误信息
                            handleApiError(errorBody, "添加");
                        } else {
                            Log.e("GradeActivity", "添加成绩失败，没有错误信息，HTTP状态码: " + response.code());
                            Toast.makeText(GradeActivity.this, "添加失败: 服务器错误 " + response.code(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } catch (Exception e) {
                        Log.e("GradeActivity", "解析错误信息失败", e);
                        Toast.makeText(GradeActivity.this, "添加失败: 服务器错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<GradeDto>> call, Throwable t) {
                Log.e("GradeActivity", "添加成绩网络请求失败", t);
                Toast.makeText(GradeActivity.this, "添加失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 更新成绩
    private void updateGrade(int position, int gradeId, GradeDto gradeDto, double credit) {
        // 设置作业ID为null，避免外键约束错误
        gradeDto.setAssignmentId(null);

        apiService.updateGrade(gradeId, gradeDto).enqueue(new Callback<ApiResponse<GradeDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<GradeDto>> call, Response<ApiResponse<GradeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<GradeDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus())) {
                        GradeDto updatedGrade = apiResponse.getData();

                        // 更新本地数据
                        Map<String, Object> gradeMap = gradeDataList.get(position);
                        gradeMap.put("id", updatedGrade.getId());
                        gradeMap.put("courseName", updatedGrade.getCourseName());
                        gradeMap.put("courseType", updatedGrade.getGradeType());
                        gradeMap.put("semester", updatedGrade.getSubmissionDate());
                        gradeMap.put("score", updatedGrade.getScore());
                        gradeMap.put("credit", credit);
                        gradeMap.put("gpa", calculateGpa(updatedGrade.getScore()));
                        gradeMap.put("courseId", updatedGrade.getCourseId());

                        adapter.notifyItemChanged(position);
                        Toast.makeText(GradeActivity.this, "成功更新成绩", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GradeActivity.this, "更新失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("GradeActivity", "更新成绩错误: " + errorBody);

                            // 处理错误信息
                            handleApiError(errorBody, "更新");
                        } else {
                            Toast.makeText(GradeActivity.this, "更新失败: 服务器错误", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("GradeActivity", "解析错误信息失败", e);
                        Toast.makeText(GradeActivity.this, "更新失败: 服务器错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<GradeDto>> call, Throwable t) {
                Toast.makeText(GradeActivity.this, "更新失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 删除成绩
    private void deleteGrade(int position, int gradeId) {
        apiService.deleteGrade(gradeId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus())) {
                        // 从本地数据移除
                        gradeDataList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateEmptyView(gradeDataList.isEmpty());

                        Toast.makeText(GradeActivity.this, "成功删除成绩", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GradeActivity.this, "删除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("GradeActivity", "删除成绩错误: " + errorBody);

                            // 尝试解析错误信息
                            if (errorBody.contains("message")) {
                                String message = errorBody.split("message\":\"")[1].split("\"")[0];
                                Toast.makeText(GradeActivity.this, "删除失败: " + message, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(GradeActivity.this, "删除失败: 服务器错误", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(GradeActivity.this, "删除失败: 服务器错误", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("GradeActivity", "解析错误信息失败", e);
                        Toast.makeText(GradeActivity.this, "删除失败: 服务器错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(GradeActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 设置课程名称下拉列表
    private void setupCourseNameSpinner(Spinner spinner, String currentValue) {
        // 直接从数据库获取最新的课程数据
        loadCoursesFromDatabase(spinner, currentValue);
    }

    // 从数据库加载课程数据
    private void loadCoursesFromDatabase(Spinner spinner, String currentValue) {
        // 创建API服务
        CourseApiService courseApiService = ApiClient.getClient().create(CourseApiService.class);

        // 显示加载提示
        Toast.makeText(this, "正在同步课程数据...", Toast.LENGTH_SHORT).show();

        // 调用API获取最新课程列表
        courseApiService.getCourses(1, 100).enqueue(new Callback<ApiResponseList<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<CourseDto>> call,
                    Response<ApiResponseList<CourseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponseList<CourseDto> apiResponse = response.body();

                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        List<CourseDto> courseDtos = apiResponse.getData();
                        List<String> courseNames = new ArrayList<>();

                        // 提取课程名称
                        for (CourseDto dto : courseDtos) {
                            if (dto.getName() != null && !dto.getName().isEmpty()) {
                                courseNames.add(dto.getName());
                            }
                        }

                        // 更新共享数据
                        CourseManagementActivity.updateSharedCourseData(courseDtos);

                        if (courseNames.isEmpty()) {
                            courseNames.add("暂无课程数据");
                        }

                        Log.d("GradeActivity", "成功从数据库获取课程: " + courseNames.size() + "个");

                        // 创建并设置适配器
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                GradeActivity.this, android.R.layout.simple_spinner_item, courseNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);

                        // 设置当前选中项
                        if (currentValue != null && !currentValue.isEmpty()) {
                            for (int i = 0; i < courseNames.size(); i++) {
                                if (courseNames.get(i).equals(currentValue)) {
                                    spinner.setSelection(i);
                                    break;
                                }
                            }
                        }

                        // 添加点击监听器
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String selectedCourse = parent.getItemAtPosition(position).toString();
                                Log.d("GradeActivity", "选择了课程: " + selectedCourse);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                // 什么都不做
                            }
                        });
                    } else {
                        Log.e("GradeActivity", "获取课程失败: " + apiResponse.getMessage());
                        setupDefaultSpinner(spinner, currentValue);
                    }
                } else {
                    Log.e("GradeActivity", "获取课程API调用失败");
                    setupDefaultSpinner(spinner, currentValue);
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<CourseDto>> call, Throwable t) {
                Log.e("GradeActivity", "获取课程网络错误: " + t.getMessage());
                setupDefaultSpinner(spinner, currentValue);
            }
        });
    }

    // 设置默认的下拉列表（当API调用失败时使用）
    private void setupDefaultSpinner(Spinner spinner, String currentValue) {
        // 从CourseManagementActivity获取共享课程数据
        List<String> courseNames = CourseManagementActivity.getSharedCourseNames();

        if (courseNames.isEmpty() || (courseNames.size() == 1 &&
                (courseNames.get(0).equals("暂无可选课程") || courseNames.get(0).equals("暂无课程数据")))) {
            courseNames = new ArrayList<>();
            courseNames.add("暂无课程数据");
        }

        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, courseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // 设置当前选中项
        if (currentValue != null && !currentValue.isEmpty()) {
            for (int i = 0; i < courseNames.size(); i++) {
                if (courseNames.get(i).equals(currentValue)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        // 添加点击监听器
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = parent.getItemAtPosition(position).toString();
                Log.d("GradeActivity", "选择了课程: " + selectedCourse);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 什么都不做
            }
        });
    }

    // 根据课程名称获取ID
    private Integer getCourseIdByName(String courseName) {
        if (courseName == null || courseName.isEmpty() ||
                courseName.equals("暂无可选课程") || courseName.equals("请先添加课程")) {
            return null;
        }

        List<CourseDto> courseDtos = CourseManagementActivity.getSharedCourseDtoList();
        if (courseDtos == null || courseDtos.isEmpty()) {
            // 如果没有课程数据，提示用户先添加课程
            Toast.makeText(this, "请先在课程管理中添加课程", Toast.LENGTH_SHORT).show();
            return null;
        }

        for (CourseDto courseDto : courseDtos) {
            if (courseDto != null && courseDto.getName() != null &&
                    courseName.equals(courseDto.getName())) {
                return courseDto.getId();
            }
        }

        // 如果找不到匹配的课程
        Toast.makeText(this, "找不到匹配的课程: " + courseName, Toast.LENGTH_SHORT).show();
        return null;
    }

    // GradeAdapter.GradeItemClickListener 接口实现
    @Override
    public void onGradeClick(int position, Map<String, Object> grade) {
        // 查看详情，可以实现查看成绩详细信息的功能
        Toast.makeText(this, "查看成绩详情", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGradeEditClick(int position, Map<String, Object> grade) {
        showEditGradeDialog(position, grade);
    }

    @Override
    public void onGradeDeleteClick(int position, Map<String, Object> grade) {
        confirmDeleteGrade(position, grade);
    }

    // 将中文学期格式转换为日期格式
    private String formatSubmissionDate(String chineseSemester) {
        // 默认日期，如果无法解析则返回
        String defaultDate = "2023-09-01";

        if (chineseSemester == null || chineseSemester.isEmpty()) {
            return defaultDate;
        }

        try {
            // 尝试提取年份信息
            // 例如从 "2023-2024学年第一学期" 提取 2023 和 2024
            if (chineseSemester.contains("学年")) {
                String[] parts = chineseSemester.split("学年");
                if (parts.length > 0) {
                    String yearPart = parts[0]; // 例如 "2023-2024"
                    String semesterPart = parts.length > 1 ? parts[1] : ""; // 例如 "第一学期"

                    String[] years = yearPart.split("-");
                    if (years.length == 2) {
                        int startYear = Integer.parseInt(years[0]);
                        int endYear = Integer.parseInt(years[1]);

                        // 根据学期确定月份 - 第一学期用9月，第二学期用3月
                        int month = semesterPart.contains("第一") ? 9 : 3;
                        int day = 1;

                        // 选择正确的年份
                        int year = semesterPart.contains("第一") ? startYear : endYear;

                        // 格式化为 yyyy-MM-dd
                        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
                    }
                }
            }

            // 如果不符合上述格式，可能是其他格式，直接返回默认日期
            Log.w("GradeActivity", "无法解析学期格式: " + chineseSemester + "，使用默认格式");
            return defaultDate;

        } catch (Exception e) {
            Log.e("GradeActivity", "转换学期格式出错: " + chineseSemester, e);
        }

        return defaultDate;
    }

    // 处理API错误
    private void handleApiError(String errorBody, String operation) {
        try {
            Log.d("GradeActivity", "处理API错误: " + errorBody);

            if (errorBody.contains("foreign key constraint fails")) {
                // 处理外键约束错误
                Toast.makeText(GradeActivity.this, operation + "失败: 引用了不存在的数据记录", Toast.LENGTH_LONG).show();
                Log.e("GradeActivity", "外键约束错误: " + errorBody);
            } else if (errorBody.contains("无效的提交日期格式")) {
                // 处理日期格式错误
                Toast.makeText(GradeActivity.this, operation + "失败: 日期格式不正确，请使用yyyy-MM-dd格式", Toast.LENGTH_LONG).show();
                Log.e("GradeActivity", "日期格式错误: " + errorBody);
            } else if (errorBody.contains("message")) {
                // 尝试提取错误消息
                try {
                    // 尝试解析完整的JSON
                    JSONObject jsonObject = new JSONObject(errorBody);
                    String message = jsonObject.getString("message");
                    String errorDetails = jsonObject.optString("error", "");

                    if (!errorDetails.isEmpty()) {
                        Log.e("GradeActivity", "错误详情: " + errorDetails);
                        Toast.makeText(GradeActivity.this, operation + "失败: " + message + "\n详情: " + errorDetails,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(GradeActivity.this, operation + "失败: " + message, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException jsonEx) {
                    // 如果JSON解析失败，使用简单的字符串分割方法
                    Log.e("GradeActivity", "JSON解析错误", jsonEx);
                    String message = errorBody.split("message\":\"")[1].split("\"")[0];
                    Toast.makeText(GradeActivity.this, operation + "失败: " + message, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(GradeActivity.this, operation + "失败: 服务器错误", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("GradeActivity", "解析错误消息失败", e);
            Toast.makeText(GradeActivity.this, operation + "失败: 服务器错误", Toast.LENGTH_SHORT).show();
        }
    }

    // 更新课程选择的接口
    private interface UpdateCourseSelection {
        void update(Integer courseId, String courseName);
    }

    // 设置作业下拉列表
    private void setupAllAssignmentsSpinner(Spinner spinner, Integer currentAssignmentId) {
        // 显示加载提示
        List<String> loadingList = new ArrayList<>();
        loadingList.add("正在加载作业...");
        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, loadingList);
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(loadingAdapter);

        // 调用API获取所有作业列表
        assignmentApiService = ApiClient.getClient().create(AssignmentApiService.class);
        assignmentApiService.getAssignments(1, 100).enqueue(new Callback<ApiResponseList<AssignmentDto>>() {
            @Override
            public void onResponse(Call<ApiResponseList<AssignmentDto>> call,
                    Response<ApiResponseList<AssignmentDto>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    List<AssignmentDto> assignments = response.body().getData();

                    // 将作业列表转换为可选项列表
                    List<AssignmentSpinnerItem> spinnerItems = new ArrayList<>();

                    // 添加一个"无作业"选项
                    spinnerItems.add(new AssignmentSpinnerItem(null, "-- 无作业 --"));

                    if (assignments != null && !assignments.isEmpty()) {
                        for (AssignmentDto assignment : assignments) {
                            String courseName = assignment.getCourseName() != null ? assignment.getCourseName()
                                    : "未知课程";
                            spinnerItems.add(new AssignmentSpinnerItem(
                                    assignment.getId(),
                                    assignment.getTitle() + " (" + courseName + ")"));
                        }

                        Log.d("GradeActivity", "成功获取所有作业列表，共 " + assignments.size() + " 条");
                    } else {
                        Log.d("GradeActivity", "没有可用的作业");
                    }

                    // 创建适配器
                    ArrayAdapter<AssignmentSpinnerItem> adapter = new ArrayAdapter<>(
                            GradeActivity.this,
                            android.R.layout.simple_spinner_item,
                            spinnerItems);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    // 如果有当前选中的作业ID，设置选中项
                    if (currentAssignmentId != null) {
                        for (int i = 0; i < spinnerItems.size(); i++) {
                            AssignmentSpinnerItem item = spinnerItems.get(i);
                            if (item.getId() != null && item.getId().equals(currentAssignmentId)) {
                                spinner.setSelection(i);
                                break;
                            }
                        }
                    }

                    // 添加选择监听器
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            AssignmentSpinnerItem selectedItem = (AssignmentSpinnerItem) parent
                                    .getItemAtPosition(position);
                            Log.d("GradeActivity", "选择了作业: ID=" +
                                    (selectedItem.getId() != null ? selectedItem.getId() : "null") +
                                    ", 标题=" + selectedItem.getTitle());

                            // 获取选中作业的课程ID和课程名称
                            if (selectedItem.getId() != null && updateCourseSelectionListener != null) {
                                for (AssignmentDto assignment : assignments) {
                                    if (assignment.getId().equals(selectedItem.getId())) {
                                        // 更新课程下拉框选中项
                                        updateCourseSelectionListener.update(assignment.getCourseId(),
                                                assignment.getCourseName());
                                        break;
                                    }
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // 什么都不做
                        }
                    });
                } else {
                    // API调用失败
                    List<String> errorList = new ArrayList<>();
                    errorList.add("获取作业失败");
                    ArrayAdapter<String> errorAdapter = new ArrayAdapter<>(
                            GradeActivity.this, android.R.layout.simple_spinner_item, errorList);
                    errorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(errorAdapter);

                    Log.e("GradeActivity", "获取作业列表失败: " +
                            (response.body() != null ? response.body().getMessage() : "未知错误"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponseList<AssignmentDto>> call, Throwable t) {
                // 网络请求失败
                List<String> errorList = new ArrayList<>();
                errorList.add("网络错误");
                ArrayAdapter<String> errorAdapter = new ArrayAdapter<>(
                        GradeActivity.this, android.R.layout.simple_spinner_item, errorList);
                errorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(errorAdapter);

                Log.e("GradeActivity", "获取作业列表网络请求失败", t);
            }
        });
    }

    // 作业下拉菜单项
    private static class AssignmentSpinnerItem {
        private Integer id;
        private String title;

        public AssignmentSpinnerItem(Integer id, String title) {
            this.id = id;
            this.title = title;
        }

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}