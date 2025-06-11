package com.example.yidong222.data.repository;

import android.util.Log;
import com.example.yidong222.data.api.ApiClient;
import com.example.yidong222.data.api.ApiService;
import com.example.yidong222.data.api.request.CourseScheduleRequest;
import com.example.yidong222.data.api.response.CourseScheduleResponse;
import com.example.yidong222.models.CourseSchedule;
import com.example.yidong222.models.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseScheduleRepository {
    private static final String TAG = "CourseScheduleRepository";
    private final ApiService apiService;

    public CourseScheduleRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * 获取课程表列表
     * 
     * @param page     页码
     * @param limit    每页数量
     * @param callback 回调
     */
    public void getCourseSchedules(int page, int limit, RepositoryCallback<CourseScheduleResponse> callback) {
        apiService.getCourseSchedules(page, limit).enqueue(new Callback<ApiResponse<CourseScheduleResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseScheduleResponse>> call,
                    Response<ApiResponse<CourseScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(new Exception("获取课程表失败"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseScheduleResponse>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * 创建课程表
     * 
     * @param courseName  课程名称
     * @param teacherName 教师姓名
     * @param classTime   上课时间
     * @param classroom   教室
     * @param callback    回调
     */
    public void createCourseSchedule(String courseName, String teacherName, String classTime, String classroom,
            RepositoryCallback<CourseScheduleResponse> callback) {
        // 执行输入验证
        if (courseName == null || courseName.trim().isEmpty()) {
            callback.onError(new Exception("课程名称不能为空"));
            return;
        }

        CourseScheduleRequest request = new CourseScheduleRequest();
        request.setCourseName(courseName.trim());
        request.setTeacherName(teacherName != null ? teacherName.trim() : "");
        request.setClassTime(classTime != null ? classTime.trim() : "");
        request.setClassroom(classroom != null ? classroom.trim() : "");

        Log.d(TAG, "创建课程表请求: courseName=" + request.getCourseName() + ", teacherName=" + request.getTeacherName()
                + ", classTime=" + request.getClassTime() + ", classroom=" + request.getClassroom());

        apiService.createCourseSchedule(request).enqueue(new Callback<ApiResponse<CourseScheduleResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseScheduleResponse>> call,
                    Response<ApiResponse<CourseScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CourseScheduleResponse> apiResponse = response.body();
                    Log.d(TAG, "创建课程表响应: status=" + apiResponse.getStatus() + ", message=" + apiResponse.getMessage());

                    if ("success".equals(apiResponse.getStatus())) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(new Exception(apiResponse.getMessage()));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "未知错误";
                        Log.e(TAG, "创建课程表失败: " + errorBody);

                        // 尝试从错误响应中提取错误信息
                        if (errorBody.contains("课程名称不能为空")) {
                            callback.onError(new Exception("课程名称不能为空，请检查输入"));
                        } else {
                            callback.onError(new Exception("创建课程表失败: " + errorBody));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误响应失败", e);
                        callback.onError(new Exception("创建课程表失败"));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseScheduleResponse>> call, Throwable t) {
                Log.e(TAG, "创建课程表网络请求失败", t);
                callback.onError(t);
            }
        });
    }

    /**
     * 更新课程表
     * 
     * @param id          课程表ID
     * @param courseName  课程名称
     * @param teacherName 教师姓名
     * @param classTime   上课时间
     * @param classroom   教室
     * @param callback    回调
     */
    public void updateCourseSchedule(int id, String courseName, String teacherName, String classTime, String classroom,
            RepositoryCallback<CourseScheduleResponse> callback) {
        // 执行输入验证
        if (courseName == null || courseName.trim().isEmpty()) {
            callback.onError(new Exception("课程名称不能为空"));
            return;
        }

        CourseScheduleRequest request = new CourseScheduleRequest();
        request.setCourseName(courseName.trim());
        request.setTeacherName(teacherName != null ? teacherName.trim() : "");
        request.setClassTime(classTime != null ? classTime.trim() : "");
        request.setClassroom(classroom != null ? classroom.trim() : "");

        Log.d(TAG,
                "更新课程表请求: id=" + id + ", courseName=" + request.getCourseName() + ", teacherName="
                        + request.getTeacherName()
                        + ", classTime=" + request.getClassTime() + ", classroom=" + request.getClassroom());

        apiService.updateCourseSchedule(id, request).enqueue(new Callback<ApiResponse<CourseScheduleResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseScheduleResponse>> call,
                    Response<ApiResponse<CourseScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CourseScheduleResponse> apiResponse = response.body();
                    Log.d(TAG, "更新课程表响应: status=" + apiResponse.getStatus() + ", message=" + apiResponse.getMessage());

                    if ("success".equals(apiResponse.getStatus())) {
                        callback.onSuccess(apiResponse.getData());
                    } else {
                        callback.onError(new Exception(apiResponse.getMessage()));
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "未知错误";
                        Log.e(TAG, "更新课程表失败: " + errorBody);

                        // 尝试从错误响应中提取错误信息
                        if (errorBody.contains("课程名称不能为空")) {
                            callback.onError(new Exception("课程名称不能为空，请检查输入"));
                        } else {
                            callback.onError(new Exception("更新课程表失败: " + errorBody));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误响应失败", e);
                        callback.onError(new Exception("更新课程表失败"));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseScheduleResponse>> call, Throwable t) {
                Log.e(TAG, "更新课程表网络请求失败", t);
                callback.onError(t);
            }
        });
    }

    /**
     * 删除课程表
     * 
     * @param id       课程表ID
     * @param callback 回调
     */
    public void deleteCourseSchedule(int id, RepositoryCallback<CourseScheduleResponse> callback) {
        apiService.deleteCourseSchedule(id).enqueue(new Callback<ApiResponse<CourseScheduleResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseScheduleResponse>> call,
                    Response<ApiResponse<CourseScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(new Exception("删除课程表失败"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseScheduleResponse>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);

        void onError(Throwable error);
    }
}