package com.example.yidong222.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

// 使用完全限定类名引用而不是导入
// import com.example.yidong222.api.ApiClient;
import com.example.yidong222.models.ApiResponse;
import com.example.yidong222.models.ApiResponseList;
import com.example.yidong222.models.Assignment;
import com.example.yidong222.models.AssignmentDto;
import com.example.yidong222.models.CourseSchedule;
import com.example.yidong222.models.Exam;
import com.example.yidong222.models.ExamDto;
import com.example.yidong222.models.Grade;
import com.example.yidong222.models.GradeDto;
import com.example.yidong222.models.GradeStatsDto;
import com.example.yidong222.api.ServerFixHelper;
import com.example.yidong222.api.ApiClientHelper;
import com.example.yidong222.data.db.AppDatabase;
import com.example.yidong222.data.db.entity.AssignmentEntity;
import com.example.yidong222.models.CourseDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;

/**
 * 数据管理类，用于处理应用程序的数据操作
 */
public class DataManager {
    private static final String TAG = "DataManager";
    private static DataSyncManager dataSyncManager;

    // 存储已经尝试删除过的考试ID，避免重复删除
    private static final Set<Integer> deletingExamIds = Collections.synchronizedSet(new HashSet<>());

    // 存储已经尝试删除过的作业ID，避免重复删除
    private static final Set<Integer> deletingAssignmentIds = Collections.synchronizedSet(new HashSet<>());

    /**
     * 初始化DataManager
     * 
     * @param context 应用程序上下文
     */
    public static void init(Context context) {
        if (dataSyncManager == null) {
            dataSyncManager = DataSyncManager.getInstance(context);
        }
    }

    /**
     * 检查网络是否可用
     * 
     * @param context 上下文
     * @return 网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        if (dataSyncManager != null) {
            return dataSyncManager.isNetworkAvailable();
        } else {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
            return false;
        }
    }

    /**
     * 获取课程表数据
     * 
     * @param callback 回调
     */
    public static void getCourseSchedules(DataCallback<CourseSchedule> callback) {
        if (dataSyncManager == null) {
            callback.onFailure("DataSyncManager未初始化");
            return;
        }

        dataSyncManager.syncCourseSchedules(new DataSyncManager.SyncCallback<List<CourseSchedule>>() {
            @Override
            public void onSuccess(List<CourseSchedule> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    /**
     * 获取作业数据
     * 
     * @param callback 回调
     */
    public static void getAssignments(DataCallback<Assignment> callback) {
        if (dataSyncManager == null) {
            callback.onFailure("DataSyncManager未初始化");
            return;
        }

        dataSyncManager.getAssignments(callback);
    }

    /**
     * 创建新作业
     * 
     * @param assignmentDto 作业DTO
     * @param callback      回调
     */
    public static void createAssignment(AssignmentDto assignmentDto, DetailCallback<Assignment> callback) {
        // 记录请求信息
        Log.d(TAG, "开始创建新作业: " + assignmentDto.getTitle());
        Log.d(TAG, "请求参数: courseId=" + assignmentDto.getCourseId()
                + ", title=" + assignmentDto.getTitle()
                + ", description="
                + (assignmentDto.getDescription() != null
                        ? assignmentDto.getDescription().substring(0,
                                Math.min(20, assignmentDto.getDescription().length())) + "..."
                        : "null")
                + ", deadline=" + assignmentDto.getDeadline()
                + ", maxScore=" + assignmentDto.getMaxScore()
                + ", courseName=" + assignmentDto.getCourseName());

        // 确保deadline不为null
        if (assignmentDto.getDeadline() == null || assignmentDto.getDeadline().trim().isEmpty()) {
            // 如果deadline为空，设置为当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            assignmentDto.setDeadline(currentTime);
            Log.w(TAG, "截止日期为空，已设置为当前时间: " + currentTime);
        }

        // 创建新作业
        ApiClientHelper.getAssignmentApiService()
                .createAssignment(assignmentDto)
                .enqueue(new Callback<ApiResponse<AssignmentDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentDto>> call,
                            Response<ApiResponse<AssignmentDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Log.d(TAG, "成功创建新作业");
                            AssignmentDto responseDto = response.body().getData();
                            Assignment newAssignment = responseDto.toAssignment();

                            // 确保使用服务器返回的ID
                            if (responseDto.getId() != null) {
                                newAssignment.setId(responseDto.getId());
                                Log.d(TAG, "使用服务器返回的ID: " + responseDto.getId());
                            }

                            // 在后台线程中同步更新本地数据库
                            new Thread(() -> {
                                try {
                                    // 获取应用数据库实例
                                    AppDatabase db = AppDatabase.getInstance(dataSyncManager.getApplicationContext());
                                    // 创建作业实体
                                    AssignmentEntity assignmentEntity = new AssignmentEntity();
                                    assignmentEntity.id = newAssignment.getId();
                                    assignmentEntity.courseId = assignmentDto.getCourseId();
                                    assignmentEntity.title = assignmentDto.getTitle();
                                    assignmentEntity.description = assignmentDto.getDescription();
                                    assignmentEntity.dueDate = assignmentDto.getDeadline();
                                    assignmentEntity.status = "未完成";

                                    // 插入新作业
                                    db.assignmentDao().insertOrUpdate(assignmentEntity);
                                    Log.d(TAG, "成功保存新作业到本地数据库，ID: " + assignmentEntity.id);
                                } catch (Exception e) {
                                    Log.e(TAG, "保存新作业到本地数据库失败", e);
                                }
                            }).start();

                            callback.onSuccess(newAssignment);
                        } else {
                            // API调用失败，记录详细信息
                            Log.e(TAG, "创建作业失败，状态码: " + response.code());
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应体: " + errorBody);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "读取错误响应体失败", e);
                            }
                            Log.e(TAG, "请求URL: " + call.request().url());
                            Log.e(TAG, "请求方法: " + call.request().method());
                            Log.e(TAG, "请求头: Content-Type: " + call.request().header("Content-Type"));
                            Log.e(TAG, "请求体中deadline值: " + assignmentDto.getDeadline());

                            // 尝试创建一个本地模拟作业，使其行为类似于真实作业
                            try {
                                Assignment assignment = new Assignment(
                                        assignmentDto.getTitle(),
                                        assignmentDto.getCourseName(),
                                        assignmentDto.getDeadline(),
                                        assignmentDto.getDescription(),
                                        true);

                                // 在后台线程中保存到本地数据库
                                final int tempId = assignment.getId(); // 获取临时ID
                                new Thread(() -> {
                                    try {
                                        // 获取应用数据库实例
                                        AppDatabase db = AppDatabase
                                                .getInstance(dataSyncManager.getApplicationContext());
                                        // 创建作业实体
                                        AssignmentEntity assignmentEntity = new AssignmentEntity();
                                        assignmentEntity.id = tempId;
                                        assignmentEntity.courseId = assignmentDto.getCourseId();
                                        assignmentEntity.title = assignmentDto.getTitle();
                                        assignmentEntity.description = assignmentDto.getDescription();
                                        assignmentEntity.dueDate = assignmentDto.getDeadline();
                                        assignmentEntity.status = "未完成";

                                        // 插入或更新新作业
                                        db.assignmentDao().insertOrUpdate(assignmentEntity);
                                        Log.d(TAG, "API失败但成功将临时作业保存到本地数据库，ID: " + tempId);
                                    } catch (Exception e) {
                                        Log.e(TAG, "保存临时作业到本地数据库失败", e);
                                    }
                                }).start();

                                callback.onSuccess(assignment);
                            } catch (Exception e) {
                                Log.e(TAG, "创建本地替代作业对象失败", e);
                                callback.onFailure("创建作业失败: " + response.message());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentDto>> call, Throwable t) {
                        Log.e(TAG, "网络请求失败: " + t.getMessage(), t);
                        // 如果API调用失败，尝试创建一个本地模拟作业
                        try {
                            Assignment assignment = new Assignment(
                                    assignmentDto.getTitle(),
                                    assignmentDto.getCourseName(),
                                    assignmentDto.getDeadline(),
                                    assignmentDto.getDescription(),
                                    true);

                            // 在后台线程中保存到本地数据库
                            final int tempId = assignment.getId(); // 获取临时ID
                            new Thread(() -> {
                                try {
                                    // 获取应用数据库实例
                                    AppDatabase db = AppDatabase.getInstance(dataSyncManager.getApplicationContext());
                                    // 创建作业实体
                                    AssignmentEntity assignmentEntity = new AssignmentEntity();
                                    assignmentEntity.id = tempId;
                                    assignmentEntity.courseId = assignmentDto.getCourseId();
                                    assignmentEntity.title = assignmentDto.getTitle();
                                    assignmentEntity.description = assignmentDto.getDescription();
                                    assignmentEntity.dueDate = assignmentDto.getDeadline();
                                    assignmentEntity.status = "未完成";

                                    // 插入或更新新作业
                                    db.assignmentDao().insertOrUpdate(assignmentEntity);
                                    Log.d(TAG, "网络失败但成功将临时作业保存到本地数据库，ID: " + tempId);
                                } catch (Exception e) {
                                    Log.e(TAG, "保存临时作业到本地数据库失败", e);
                                }
                            }).start();

                            callback.onSuccess(assignment);
                        } catch (Exception e) {
                            Log.e(TAG, "创建本地替代作业对象失败", e);
                            callback.onFailure("创建作业失败: " + t.getMessage());
                        }
                    }
                });
    }

    /**
     * 更新作业
     * 
     * @param assignmentId  作业ID
     * @param assignmentDto 作业DTO
     * @param callback      回调
     */
    public static void updateAssignment(int assignmentId, AssignmentDto assignmentDto,
            DetailCallback<Assignment> callback) {
        Log.d(TAG, "开始更新作业，ID: " + assignmentId);
        Log.d(TAG, "请求参数: title=" + assignmentDto.getTitle()
                + ", description=" + (assignmentDto.getDescription() != null
                        ? assignmentDto.getDescription().substring(0,
                                Math.min(20, assignmentDto.getDescription().length())) + "..."
                        : "null")
                + ", deadline=" + assignmentDto.getDeadline()
                + ", maxScore=" + assignmentDto.getMaxScore());

        // 确保deadline不为null
        if (assignmentDto.getDeadline() == null || assignmentDto.getDeadline().trim().isEmpty()) {
            // 如果deadline为空，设置为当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            assignmentDto.setDeadline(currentTime);
            Log.w(TAG, "截止日期为空，已设置为当前时间: " + currentTime);
        }

        ApiClientHelper.getAssignmentApiService()
                .updateAssignment(assignmentId, assignmentDto)
                .enqueue(new Callback<ApiResponse<AssignmentDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentDto>> call,
                            Response<ApiResponse<AssignmentDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Log.d(TAG, "成功更新作业，ID: " + assignmentId);
                            AssignmentDto responseDto = response.body().getData();
                            Assignment updatedAssignment = responseDto.toAssignment();

                            // 确保使用服务器返回的ID
                            if (responseDto.getId() != null) {
                                updatedAssignment.setId(responseDto.getId());
                                Log.d(TAG, "使用服务器返回的ID: " + responseDto.getId());
                            } else {
                                updatedAssignment.setId(assignmentId);
                            }

                            // 在后台线程中同步更新本地数据库
                            new Thread(() -> {
                                try {
                                    // 获取应用数据库实例
                                    AppDatabase db = AppDatabase.getInstance(dataSyncManager.getApplicationContext());
                                    // 创建作业实体
                                    AssignmentEntity assignmentEntity = new AssignmentEntity();
                                    assignmentEntity.id = updatedAssignment.getId();
                                    assignmentEntity.courseId = assignmentDto.getCourseId();
                                    assignmentEntity.title = assignmentDto.getTitle();
                                    assignmentEntity.description = assignmentDto.getDescription();
                                    assignmentEntity.dueDate = assignmentDto.getDeadline();
                                    assignmentEntity.status = assignmentDto.getStatus();

                                    // 插入或更新作业
                                    db.assignmentDao().insertOrUpdate(assignmentEntity);
                                    Log.d(TAG, "成功更新本地数据库中的作业，ID: " + assignmentEntity.id);
                                } catch (Exception e) {
                                    Log.e(TAG, "更新本地数据库中的作业失败", e);
                                }
                            }).start();

                            callback.onSuccess(updatedAssignment);
                        } else {
                            // API调用失败，记录详细信息
                            Log.e(TAG, "更新作业失败，状态码: " + response.code());
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应体: " + errorBody);
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "读取错误响应体失败", e);
                            }
                            Log.e(TAG, "请求URL: " + call.request().url());
                            Log.e(TAG, "请求方法: " + call.request().method());
                            Log.e(TAG, "请求头: Content-Type: " + call.request().header("Content-Type"));
                            Log.e(TAG, "请求体中deadline值: " + assignmentDto.getDeadline());

                            callback.onFailure("更新作业失败: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentDto>> call, Throwable t) {
                        Log.e(TAG, "网络请求失败: " + t.getMessage(), t);
                        callback.onFailure("网络请求失败: " + t.getMessage());
                    }
                });
    }

    /**
     * 删除作业
     * 
     * @param assignmentId 作业ID
     * @param callback     回调
     */
    public static void deleteAssignment(int assignmentId, DetailCallback<Void> callback) {
        // 检查是否正在删除
        if (deletingAssignmentIds.contains(assignmentId)) {
            Log.d(TAG, "作业ID " + assignmentId + " 已经在删除中，忽略重复请求");
            callback.onSuccess(null); // 直接返回成功
            return;
        }

        // 添加到正在删除的集合中
        deletingAssignmentIds.add(assignmentId);

        // 记录请求信息
        Log.d(TAG, "开始删除作业，ID: " + assignmentId);

        // 删除作业
        ApiClientHelper.getAssignmentApiService()
                .deleteAssignment(assignmentId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        // 删除完成，从集合中移除
                        deletingAssignmentIds.remove(assignmentId);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "成功删除作业，ID: " + assignmentId);

                            // 在后台线程中同步删除本地数据库中的记录
                            new Thread(() -> {
                                try {
                                    // 获取应用数据库实例
                                    AppDatabase db = AppDatabase.getInstance(dataSyncManager.getApplicationContext());
                                    // 从数据库中删除
                                    int rowsDeleted = db.assignmentDao().deleteById(assignmentId);
                                    Log.d(TAG, "从本地数据库删除作业，ID: " + assignmentId + ", 删除行数: " + rowsDeleted);
                                } catch (Exception e) {
                                    Log.e(TAG, "从本地数据库删除作业失败", e);
                                }
                            }).start();

                            callback.onSuccess(null);
                        } else {
                            Log.e(TAG, "删除作业失败，ID: " + assignmentId + ", 状态码: " + response.code());
                            String errorMessage = "删除作业失败: " + response.code();

                            // 尝试解析错误响应体
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应体: " + errorBody);
                                    Log.e(TAG, "请求URL: " + call.request().url());

                                    // 如果是404错误（作业不存在），视为成功
                                    if (response.code() == 404) {
                                        Log.d(TAG, "作业ID " + assignmentId + " 不存在，但仍视为删除成功");

                                        // 在后台线程中同步删除本地数据库中的记录
                                        new Thread(() -> {
                                            try {
                                                // 获取应用数据库实例
                                                AppDatabase db = AppDatabase
                                                        .getInstance(dataSyncManager.getApplicationContext());
                                                // 从数据库中删除
                                                int rowsDeleted = db.assignmentDao().deleteById(assignmentId);
                                                Log.d(TAG, "服务器返回404，从本地数据库删除作业，ID: " + assignmentId + ", 删除行数: "
                                                        + rowsDeleted);
                                            } catch (Exception e) {
                                                Log.e(TAG, "服务器返回404，从本地数据库删除作业失败", e);
                                            }
                                        }).start();

                                        callback.onSuccess(null);
                                        return;
                                    }

                                    // 尝试从错误响应体中提取错误消息
                                    try {
                                        JSONObject jsonObject = new JSONObject(errorBody);
                                        if (jsonObject.has("message")) {
                                            errorMessage = jsonObject.getString("message");
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "解析错误响应体失败", e);
                                    }
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "读取错误响应体失败", e);
                            }

                            callback.onFailure(errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        // 删除完成，从集合中移除
                        deletingAssignmentIds.remove(assignmentId);

                        Log.e(TAG, "删除作业请求失败，ID: " + assignmentId, t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    /**
     * 获取考试数据
     * 
     * @param callback 回调
     */
    public static void getExams(DataCallback<Exam> callback) {
        if (dataSyncManager == null) {
            callback.onFailure("DataSyncManager未初始化");
            return;
        }

        dataSyncManager.getExams(callback);
    }

    /**
     * 创建新考试
     * 
     * @param examDto  考试DTO
     * @param callback 回调
     */
    public static void createExam(ExamDto examDto, DetailCallback<Exam> callback) {
        // 创建新考试
        ApiClientHelper.getExamApiService()
                .createExam(examDto)
                .enqueue(new Callback<ApiResponse<ExamDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ExamDto>> call,
                            Response<ApiResponse<ExamDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            // 使用服务器返回的数据创建Exam对象
                            ExamDto responseDto = response.body().getData();
                            Exam exam = responseDto.toExam();

                            // 确保使用服务器返回的ID
                            if (responseDto.getId() != null) {
                                exam.setId(responseDto.getId());
                            }

                            Log.d(TAG, "考试创建成功，服务器ID: " + exam.getId());
                            callback.onSuccess(exam);
                        } else {
                            // 如果API调用失败，尝试创建一个本地模拟考试
                            try {
                                Log.e(TAG, "创建考试API调用失败，状态码: " + response.code());
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应: " + errorBody);
                                }

                                Exam exam = new Exam(
                                        examDto.getCourseName() != null ? examDto.getCourseName() : "未知课程",
                                        examDto.getTitle(),
                                        examDto.getFormattedDate(),
                                        examDto.getFormattedTime(),
                                        examDto.getLocation(),
                                        examDto.getSeatNumber() != null ? examDto.getSeatNumber() : "" // 使用请求中的座位号
                                );

                                if (examDto.getDuration() != null) {
                                    exam.setDuration(examDto.getDuration());
                                }

                                callback.onSuccess(exam);
                            } catch (Exception e) {
                                callback.onFailure("创建考试失败: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ExamDto>> call, Throwable t) {
                        Log.e(TAG, "创建考试网络请求失败", t);
                        // 如果API调用失败，尝试创建一个本地模拟考试
                        try {
                            Exam exam = new Exam(
                                    examDto.getCourseName() != null ? examDto.getCourseName() : "未知课程",
                                    examDto.getTitle(),
                                    examDto.getFormattedDate(),
                                    examDto.getFormattedTime(),
                                    examDto.getLocation(),
                                    examDto.getSeatNumber() != null ? examDto.getSeatNumber() : "" // 使用请求中的座位号
                            );

                            if (examDto.getDuration() != null) {
                                exam.setDuration(examDto.getDuration());
                            }

                            callback.onSuccess(exam);
                        } catch (Exception e) {
                            callback.onFailure("创建考试失败: " + e.getMessage());
                        }
                    }
                });
    }

    /**
     * 更新考试
     * 
     * @param examId   考试ID
     * @param examDto  考试DTO
     * @param callback 回调
     */
    public static void updateExam(int examId, ExamDto examDto, DetailCallback<Exam> callback) {
        // 记录请求信息
        Log.d(TAG, "开始更新考试，ID: " + examId + ", courseId: " + examDto.getCourseId());

        // 更新考试
        ApiClientHelper.getExamApiService()
                .updateExam(examId, examDto)
                .enqueue(new Callback<ApiResponse<ExamDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ExamDto>> call,
                            Response<ApiResponse<ExamDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            // 使用服务器返回的数据创建Exam对象
                            ExamDto responseDto = response.body().getData();
                            Exam exam = responseDto.toExam();

                            // 确保使用服务器返回的ID
                            if (responseDto.getId() != null) {
                                exam.setId(responseDto.getId());
                            } else {
                                exam.setId(examId);
                            }

                            // 确保使用正确的课程ID
                            if (exam.getCourseId() == 0 && examDto.getCourseId() != null
                                    && examDto.getCourseId() != 0) {
                                exam.setCourseId(examDto.getCourseId());
                                Log.d(TAG, "使用请求中的课程ID: " + examDto.getCourseId());
                            }

                            // 确保座位号信息被保留
                            if ((exam.getSeatNumber() == null || exam.getSeatNumber().isEmpty())
                                    && examDto.getSeatNumber() != null && !examDto.getSeatNumber().isEmpty()) {
                                exam.setSeatNumber(examDto.getSeatNumber());
                                Log.d(TAG, "使用请求中的座位号: " + examDto.getSeatNumber());
                            }

                            Log.d(TAG, "考试更新成功，服务器ID: " + exam.getId() + ", 课程ID: " + exam.getCourseId());
                            callback.onSuccess(exam);
                        } else {
                            // 如果API调用失败，尝试创建一个本地模拟考试
                            try {
                                Log.e(TAG, "更新考试API调用失败，状态码: " + response.code());
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应: " + errorBody);
                                }

                                Exam exam = new Exam(
                                        examDto.getCourseName() != null ? examDto.getCourseName() : "未知课程",
                                        examDto.getTitle(),
                                        examDto.getFormattedDate(),
                                        examDto.getFormattedTime(),
                                        examDto.getLocation(),
                                        examDto.getSeatNumber() != null ? examDto.getSeatNumber() : "" // 使用请求中的座位号
                                );
                                exam.setId(examId);

                                // 确保使用请求中的课程ID
                                if (examDto.getCourseId() != null && examDto.getCourseId() != 0) {
                                    exam.setCourseId(examDto.getCourseId());
                                    Log.d(TAG, "网络失败，使用请求中的课程ID: " + examDto.getCourseId());
                                }

                                if (examDto.getDuration() != null) {
                                    exam.setDuration(examDto.getDuration());
                                }

                                callback.onSuccess(exam);
                            } catch (Exception e) {
                                callback.onFailure("更新考试失败: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ExamDto>> call, Throwable t) {
                        Log.e(TAG, "更新考试网络请求失败", t);
                        // 如果API调用失败，尝试创建一个本地模拟考试
                        try {
                            Exam exam = new Exam(
                                    examDto.getCourseName() != null ? examDto.getCourseName() : "未知课程",
                                    examDto.getTitle(),
                                    examDto.getFormattedDate(),
                                    examDto.getFormattedTime(),
                                    examDto.getLocation(),
                                    examDto.getSeatNumber() != null ? examDto.getSeatNumber() : "" // 使用请求中的座位号
                            );
                            exam.setId(examId);

                            // 确保使用请求中的课程ID
                            if (examDto.getCourseId() != null && examDto.getCourseId() != 0) {
                                exam.setCourseId(examDto.getCourseId());
                                Log.d(TAG, "网络失败，使用请求中的课程ID: " + examDto.getCourseId());
                            }

                            if (examDto.getDuration() != null) {
                                exam.setDuration(examDto.getDuration());
                            }

                            callback.onSuccess(exam);
                        } catch (Exception e) {
                            callback.onFailure("更新考试失败: " + e.getMessage());
                        }
                    }
                });
    }

    /**
     * 删除考试
     * 
     * @param examId   考试ID
     * @param callback 回调
     */
    public static void deleteExam(int examId, DetailCallback<Void> callback) {
        // 检查是否正在删除
        if (deletingExamIds.contains(examId)) {
            Log.d(TAG, "考试ID " + examId + " 已经在删除中，忽略重复请求");
            callback.onSuccess(null); // 直接返回成功
            return;
        }

        // 添加到正在删除的集合中
        deletingExamIds.add(examId);

        // 删除考试
        Log.d(TAG, "开始删除考试，ID: " + examId);

        ApiClientHelper.getExamApiService()
                .deleteExam(examId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        // 删除完成，从集合中移除
                        deletingExamIds.remove(examId);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "成功删除考试，ID: " + examId);
                            callback.onSuccess(null);
                        } else {
                            Log.e(TAG, "删除考试失败，ID: " + examId + ", 状态码: " + response.code());
                            String errorMessage = "删除考试失败: " + response.code();

                            // 尝试解析错误响应体
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "错误响应体: " + errorBody);
                                    Log.e(TAG, "请求URL: " + call.request().url());

                                    // 如果是404错误（考试不存在），视为成功
                                    if (response.code() == 404) {
                                        Log.d(TAG, "考试ID " + examId + " 不存在，但仍视为删除成功");
                                        callback.onSuccess(null);
                                        return;
                                    }

                                    // 尝试从错误响应体中提取错误消息
                                    try {
                                        JSONObject jsonObject = new JSONObject(errorBody);
                                        if (jsonObject.has("message")) {
                                            errorMessage = jsonObject.getString("message");
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "解析错误响应体失败", e);
                                    }
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "读取错误响应体失败", e);
                            }

                            callback.onFailure(errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        // 删除完成，从集合中移除
                        deletingExamIds.remove(examId);

                        Log.e(TAG, "删除考试请求失败，ID: " + examId, t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    /**
     * 获取成绩数据
     * 
     * @param callback 回调
     */
    public static void getGrades(DataCallback<Map<String, Object>> callback) {
        if (dataSyncManager == null) {
            callback.onFailure("DataSyncManager未初始化");
            return;
        }

        dataSyncManager.getGrades(new DataSyncManager.SyncCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    /**
     * 数据回调接口
     * 
     * @param <T> 数据类型
     */
    public interface DataCallback<T> {
        void onSuccess(List<T> data);

        void onFailure(String message);
    }

    /**
     * 详细数据回调接口
     * 
     * @param <T> 数据类型
     */
    public interface DetailCallback<T> {
        void onSuccess(T data);

        void onFailure(String message);
    }

    // 打印网络配置信息
    public static void logNetworkConfiguration() {
        String baseUrl = ApiClientHelper.getBaseUrl();
        Log.d(TAG, "API基础URL: " + baseUrl);
        // 记录DataSyncManager使用的API客户端信息
        Log.d(TAG, "DataSyncManager使用的API客户端URL: " + com.example.yidong222.data.api.ApiClient.getBaseUrl());
    }

    // 考试相关方法
    public static void getExamsByCourse(int courseId, final DataCallback<Exam> callback) {
        ApiClientHelper.getExamApiService().getExamsByCourse(courseId)
                .enqueue(new Callback<ApiResponseList<ExamDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponseList<ExamDto>> call,
                            Response<ApiResponseList<ExamDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponseList<ExamDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                List<Exam> exams = new ArrayList<>();
                                for (ExamDto dto : apiResponse.getData()) {
                                    exams.add(dto.toExam());
                                }
                                callback.onSuccess(exams);
                            } else {
                                callback.onFailure("未找到任何考试");
                            }
                        } else {
                            callback.onFailure(
                                    "获取考试失败: " + (response.errorBody() != null ? response.errorBody().toString()
                                            : "未知错误"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponseList<ExamDto>> call, Throwable t) {
                        Log.e(TAG, "获取考试错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    // 成绩相关方法
    public static void getGradesByCourse(int courseId, final DataCallback<Grade> callback) {
        ApiClientHelper.getGradeApiService().getGradesByCourse(courseId)
                .enqueue(new Callback<ApiResponseList<GradeDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponseList<GradeDto>> call,
                            Response<ApiResponseList<GradeDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponseList<GradeDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                List<Grade> grades = new ArrayList<>();
                                for (GradeDto dto : apiResponse.getData()) {
                                    grades.add(dto.toGrade());
                                }
                                callback.onSuccess(grades);
                            } else {
                                callback.onFailure("未找到任何成绩");
                            }
                        } else {
                            callback.onFailure(
                                    "获取成绩失败: " + (response.errorBody() != null ? response.errorBody().toString()
                                            : "未知错误"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponseList<GradeDto>> call, Throwable t) {
                        Log.e(TAG, "获取成绩错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    public static void getGradesByStudent(String studentId, final DataCallback<Grade> callback) {
        Log.d(TAG, "开始获取学生 " + studentId + " 的成绩");
        ApiClientHelper.getGradeApiService().getGradesByStudent(studentId)
                .enqueue(new Callback<ApiResponseList<GradeDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponseList<GradeDto>> call,
                            Response<ApiResponseList<GradeDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponseList<GradeDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                List<Grade> grades = new ArrayList<>();
                                for (GradeDto dto : apiResponse.getData()) {
                                    grades.add(dto.toGrade());
                                }
                                callback.onSuccess(grades);
                            } else {
                                // 记录详细错误信息
                                ApiClientHelper.logResponseError(response);
                                // 使用ServerFixHelper记录列名相关问题
                                ServerFixHelper.logColumnIssues(response);

                                String errorMsg = "获取成绩失败: ";
                                if (response.errorBody() != null) {
                                    try {
                                        String errorContent = response.errorBody().string();
                                        errorMsg += errorContent;

                                        // 如果是列名问题，提供更明确的错误信息
                                        if (ServerFixHelper.isGradeColumnIssue(errorContent)) {
                                            errorMsg = "服务器数据库列名不匹配: feedback 字段不存在";
                                        }
                                    } catch (IOException e) {
                                        errorMsg += "未知错误 (" + response.code() + ")";
                                    }
                                } else {
                                    errorMsg += "未知错误 (" + response.code() + ")";
                                }
                                Log.e(TAG, errorMsg);
                                callback.onFailure(errorMsg);
                            }
                        } else {
                            // 记录详细错误信息
                            ApiClientHelper.logResponseError(response);
                            // 使用ServerFixHelper记录列名相关问题
                            ServerFixHelper.logColumnIssues(response);

                            String errorMsg = "获取成绩失败: ";
                            if (response.errorBody() != null) {
                                try {
                                    String errorContent = response.errorBody().string();
                                    errorMsg += errorContent;

                                    // 如果是列名问题，提供更明确的错误信息
                                    if (ServerFixHelper.isGradeColumnIssue(errorContent)) {
                                        errorMsg = "服务器数据库列名不匹配: feedback 字段不存在";
                                    }
                                } catch (IOException e) {
                                    errorMsg += "未知错误 (" + response.code() + ")";
                                }
                            } else {
                                errorMsg += "未知错误 (" + response.code() + ")";
                            }
                            Log.e(TAG, errorMsg);
                            callback.onFailure(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponseList<GradeDto>> call, Throwable t) {
                        Log.e(TAG, "获取成绩错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    public static void getCourseStats(int courseId, final StatsCallback callback) {
        ApiClientHelper.getGradeApiService().getCourseStats(courseId)
                .enqueue(new Callback<ApiResponse<GradeStatsDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<GradeStatsDto>> call,
                            Response<ApiResponse<GradeStatsDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<GradeStatsDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                callback.onFailure("获取统计数据失败");
                            }
                        } else {
                            callback.onFailure(
                                    "获取统计数据失败: " + (response.errorBody() != null ? response.errorBody().toString()
                                            : "未知错误"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<GradeStatsDto>> call, Throwable t) {
                        Log.e(TAG, "获取统计数据错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    public static void createGrade(GradeDto gradeDto, final DetailCallback<Grade> callback) {
        ApiClientHelper.getGradeApiService().createGrade(gradeDto)
                .enqueue(new Callback<ApiResponse<GradeDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<GradeDto>> call, Response<ApiResponse<GradeDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<GradeDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                callback.onSuccess(apiResponse.getData().toGrade());
                            } else {
                                callback.onFailure("创建成绩失败");
                            }
                        } else {
                            callback.onFailure(
                                    "创建成绩失败: " + (response.errorBody() != null ? response.errorBody().toString()
                                            : "未知错误"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<GradeDto>> call, Throwable t) {
                        Log.e(TAG, "创建成绩错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    public static void updateGrade(int gradeId, GradeDto gradeDto, final DetailCallback<Grade> callback) {
        ApiClientHelper.getGradeApiService().updateGrade(gradeId, gradeDto)
                .enqueue(new Callback<ApiResponse<GradeDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<GradeDto>> call, Response<ApiResponse<GradeDto>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<GradeDto> apiResponse = response.body();
                            if (apiResponse.getData() != null) {
                                callback.onSuccess(apiResponse.getData().toGrade());
                            } else {
                                callback.onFailure("更新成绩失败");
                            }
                        } else {
                            callback.onFailure(
                                    "更新成绩失败: " + (response.errorBody() != null ? response.errorBody().toString()
                                            : "未知错误"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<GradeDto>> call, Throwable t) {
                        Log.e(TAG, "更新成绩错误", t);
                        callback.onFailure("网络错误: " + t.getMessage());
                    }
                });
    }

    public static void deleteGrade(int gradeId, final DetailCallback<Void> callback) {
        ApiClientHelper.getGradeApiService().deleteGrade(gradeId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(
                            "删除成绩失败: " + (response.errorBody() != null ? response.errorBody().toString() : "未知错误"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "删除成绩错误", t);
                callback.onFailure("网络错误: " + t.getMessage());
            }
        });
    }

    public interface StatsCallback {
        void onSuccess(GradeStatsDto stats);

        void onFailure(String message);
    }

    /**
     * 获取所有课程
     * 
     * @param callback 回调
     */
    public static void getCourses(DetailCallback<List<Map<String, Object>>> callback) {
        // 检查网络连接
        if (dataSyncManager == null || !dataSyncManager.isNetworkAvailable()) {
            Log.d(TAG, "获取课程列表失败：无网络连接");
            provideFallbackCourses(callback);
            return;
        }

        // 从API获取课程列表
        ApiClientHelper.getCourseApiService().getCourses(1, 50)
                .enqueue(new Callback<ApiResponseList<CourseDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponseList<CourseDto>> call,
                            Response<ApiResponseList<CourseDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<CourseDto> courseDtos = response.body().getData();
                            List<Map<String, Object>> courseList = new ArrayList<>();

                            // 将API返回的课程转换为Map
                            for (CourseDto courseDto : courseDtos) {
                                Map<String, Object> course = new HashMap<>();
                                course.put("id", courseDto.getId());
                                course.put("name", courseDto.getName());
                                courseList.add(course);
                                Log.d(TAG, "从API获取课程: ID=" + courseDto.getId() + ", 名称=" + courseDto.getName());
                            }

                            if (courseList.isEmpty()) {
                                Log.d(TAG, "API返回的课程列表为空，使用默认课程");
                                provideFallbackCourses(callback);
                            } else {
                                Log.d(TAG, "成功从API获取 " + courseList.size() + " 个课程");
                                callback.onSuccess(courseList);
                            }
                        } else {
                            Log.e(TAG, "获取课程列表API调用失败，状态码: " + response.code());
                            if (response.errorBody() != null) {
                                try {
                                    Log.e(TAG, "错误响应: " + response.errorBody().string());
                                } catch (IOException e) {
                                    Log.e(TAG, "读取错误响应失败", e);
                                }
                            }
                            provideFallbackCourses(callback);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponseList<CourseDto>> call, Throwable t) {
                        Log.e(TAG, "获取课程列表网络请求失败", t);
                        provideFallbackCourses(callback);
                    }
                });
    }

    /**
     * 提供默认的课程列表（当API调用失败时使用）
     * 
     * @param callback 回调
     */
    private static void provideFallbackCourses(DetailCallback<List<Map<String, Object>>> callback) {
        Log.d(TAG, "使用默认课程列表");
        List<Map<String, Object>> courseList = new ArrayList<>();

        Map<String, Object> course1 = new HashMap<>();
        course1.put("id", 2);
        course1.put("name", "计算机网络");
        courseList.add(course1);

        Map<String, Object> course2 = new HashMap<>();
        course2.put("id", 3);
        course2.put("name", "操作系统");
        courseList.add(course2);

        Map<String, Object> course3 = new HashMap<>();
        course3.put("id", 4);
        course3.put("name", "数据结构");
        courseList.add(course3);

        Map<String, Object> course4 = new HashMap<>();
        course4.put("id", 5);
        course4.put("name", "软件工程");
        courseList.add(course4);

        // 返回课程列表
        callback.onSuccess(courseList);
    }
}