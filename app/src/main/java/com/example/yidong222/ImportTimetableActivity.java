package com.example.yidong222;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yidong222.api.ApiClient;
import com.example.yidong222.data.api.ApiService;
import com.example.yidong222.data.api.request.CourseScheduleRequest;
import com.example.yidong222.data.repository.CourseScheduleRepository;
import com.example.yidong222.fragments.TimetableFragment;
import com.example.yidong222.models.CourseSchedule;
import com.example.yidong222.models.TimetableCourse;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportTimetableActivity extends AppCompatActivity {

    private static final String TAG = "ImportTimetableActivity";

    private ImageView ivBack;
    private TextInputEditText etSchool, etStudentId, etPassword;
    private Button btnImportFromSystem, btnSelectFile, btnImportFromExcel;
    private TextView tvSelectedFile, tvDownloadTemplate;

    private Uri selectedFileUri;
    private CourseScheduleRepository courseRepository;

    private ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        String fileName = getFileNameFromUri(selectedFileUri);
                        tvSelectedFile.setText("已选择: " + fileName);
                        tvSelectedFile.setVisibility(View.VISIBLE);
                        btnImportFromExcel.setEnabled(true);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_timetable);

        // 初始化数据仓库
        courseRepository = new CourseScheduleRepository(
                ApiClient.getClient().create(ApiService.class));

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etSchool = findViewById(R.id.etSchool);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        btnImportFromSystem = findViewById(R.id.btnImportFromSystem);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnImportFromExcel = findViewById(R.id.btnImportFromExcel);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        tvDownloadTemplate = findViewById(R.id.tvDownloadTemplate);

        // 默认禁用导入按钮，直到选择了文件
        btnImportFromExcel.setEnabled(false);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 教务系统导入
        btnImportFromSystem.setOnClickListener(v -> {
            String school = etSchool.getText() != null ? etSchool.getText().toString().trim() : "";
            String studentId = etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (school.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写完整的登录信息", Toast.LENGTH_SHORT).show();
                return;
            }

            // 实际从教务系统导入
            importFromEducationSystem(school, studentId, password);
        });

        // 选择Excel文件
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = { "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            filePickerLauncher.launch(intent);
        });

        // 从Excel导入
        btnImportFromExcel.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                importFromExcel(selectedFileUri);
            } else {
                Toast.makeText(this, "请先选择Excel文件", Toast.LENGTH_SHORT).show();
            }
        });

        // 下载模板
        tvDownloadTemplate.setOnClickListener(v -> {
            // 显示模板说明对话框
            new AlertDialog.Builder(this)
                    .setTitle("Excel导入模板说明")
                    .setMessage("Excel文件需要包含以下列：\n\n" +
                            "1. 课程名称 (必填)\n" +
                            "2. 教师姓名\n" +
                            "3. 上课时间 (格式：周x 第y-z节)\n" +
                            "4. 教室\n\n" +
                            "请确保第一行为表头，从第二行开始为数据。")
                    .setPositiveButton("我知道了", null)
                    .show();
        });
    }

    // 从教务系统导入课表
    private void importFromEducationSystem(String school, String studentId, String password) {
        // 显示加载对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("正在导入")
                .setMessage("正在从教务系统导入课表...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // 模拟导入过程
        new android.os.Handler().postDelayed(() -> {
            progressDialog.dismiss();
            Toast.makeText(this, "教务系统导入功能尚未实现，请等待后续更新", Toast.LENGTH_LONG).show();
        }, 2000);
    }

    // 从Excel导入课表
    private void importFromExcel(Uri fileUri) {
        // 显示加载对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("正在导入")
                .setMessage("正在解析Excel文件...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        try {
            // 获取Excel文件内容
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 判断文件类型
            String fileName = getFileNameFromUri(fileUri);
            Workbook workbook;
            try {
                if (fileName.toLowerCase().endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(inputStream);
                } else if (fileName.toLowerCase().endsWith(".xls")) {
                    workbook = new HSSFWorkbook(inputStream);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "不支持的文件格式，请使用.xls或.xlsx格式", Toast.LENGTH_SHORT).show();
                    inputStream.close();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "解析Excel文件失败", e);
                progressDialog.dismiss();
                Toast.makeText(this, "无法解析Excel文件，请确保文件格式正确", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            // 解析Excel
            List<CourseScheduleRequest> courses = parseExcel(workbook);
            workbook.close();
            inputStream.close();

            if (courses.isEmpty()) {
                progressDialog.dismiss();
                Toast.makeText(this, "未找到有效的课程数据", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新进度对话框
            progressDialog.setMessage("正在导入课程数据...");

            // 导入课程数据
            importCourses(courses, progressDialog);

        } catch (Exception e) {
            Log.e(TAG, "导入Excel失败", e);
            progressDialog.dismiss();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 解析Excel文件
    private List<CourseScheduleRequest> parseExcel(Workbook workbook) {
        List<CourseScheduleRequest> courses = new ArrayList<>();

        try {
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return courses;
            }

            // 跳过表头行
            boolean isFirstRow = true;

            // 遍历行
            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                try {
                    // 读取单元格数据
                    String courseName = getCellValue(row.getCell(0));
                    String teacherName = getCellValue(row.getCell(1));
                    String classTime = getCellValue(row.getCell(2));
                    String classroom = getCellValue(row.getCell(3));

                    // 课程名称是必填的
                    if (courseName.isEmpty()) {
                        continue;
                    }

                    // 创建课程请求对象
                    CourseScheduleRequest course = new CourseScheduleRequest(
                            courseName, teacherName, classTime, classroom);
                    courses.add(course);

                    Log.d(TAG, "解析到课程: " + courseName + ", " + teacherName + ", " + classTime + ", " + classroom);
                } catch (Exception e) {
                    Log.e(TAG, "解析行数据失败", e);
                    // 继续解析下一行
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析Excel失败", e);
        }

        return courses;
    }

    // 获取单元格的值
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            // 针对Apache POI 4.1.2版本的API
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    return String.valueOf((int) cell.getNumericCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "获取单元格值失败", e);
            return "";
        }
    }

    // 导入课程数据到系统
    private void importCourses(List<CourseScheduleRequest> courses, AlertDialog progressDialog) {
        if (courses.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "没有课程数据可导入", Toast.LENGTH_SHORT).show();
            return;
        }

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);
        final AtomicInteger processedCount = new AtomicInteger(0);
        final int totalCount = courses.size();

        for (CourseScheduleRequest course : courses) {
            courseRepository.createCourseSchedule(
                    course.getCourseName(),
                    course.getTeacherName(),
                    course.getClassTime(),
                    course.getClassroom(),
                    new CourseScheduleRepository.RepositoryCallback<com.example.yidong222.data.api.response.CourseScheduleResponse>() {
                        @Override
                        public void onSuccess(com.example.yidong222.data.api.response.CourseScheduleResponse result) {
                            successCount.incrementAndGet();
                            checkCompletion();
                        }

                        @Override
                        public void onError(Throwable error) {
                            failCount.incrementAndGet();
                            Log.e(TAG, "导入课程失败: " + error.getMessage());
                            checkCompletion();
                        }

                        private void checkCompletion() {
                            int processed = processedCount.incrementAndGet();

                            // 更新进度
                            runOnUiThread(() -> {
                                progressDialog.setMessage("正在导入课程数据... (" + processed + "/" + totalCount + ")");
                            });

                            if (processed == totalCount) {
                                // 所有课程处理完毕
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();

                                    String message = "成功导入 " + successCount.get() + " 个课程";
                                    if (failCount.get() > 0) {
                                        message += "，" + failCount.get() + " 个课程导入失败";
                                    }

                                    // 显示结果对话框
                                    new AlertDialog.Builder(ImportTimetableActivity.this)
                                            .setTitle("导入结果")
                                            .setMessage(message)
                                            .setPositiveButton("确定", (dialog, which) -> {
                                                // 设置结果并关闭页面
                                                if (successCount.get() > 0) {
                                                    setResult(RESULT_OK);
                                                }
                                                finish();
                                            })
                                            .setCancelable(false)
                                            .show();
                                });
                            }
                        }
                    });
        }
    }

    // 从Uri获取文件名
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }
}