package com.example.yidong222.api;

import android.util.Log;
import com.example.yidong222.models.AssignmentDto;
import com.example.yidong222.models.GradeDto;
import com.google.gson.Gson;
import retrofit2.Response;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 服务器修复帮助类
 * 用于处理服务器端数据库列名不匹配的问题
 */
public class ServerFixHelper {
    private static final String TAG = "ServerFixHelper";

    /**
     * 检查是否是Assignment表的列名问题
     * 
     * @param errorMessage 错误信息
     * @return 是否是列名问题
     */
    public static boolean isAssignmentColumnIssue(String errorMessage) {
        return errorMessage != null && errorMessage.contains("Unknown column 'a.due_date'");
    }

    /**
     * 检查是否是Grade表的列名问题
     * 
     * @param errorMessage 错误信息
     * @return 是否是列名问题
     */
    public static boolean isGradeColumnIssue(String errorMessage) {
        return errorMessage != null && errorMessage.contains("Unknown column 'g.feedback'");
    }

    /**
     * 记录服务器列名问题的详细信息
     * 
     * @param <T>      响应类型
     * @param response API响应
     */
    public static <T> void logColumnIssues(Response<T> response) {
        try {
            if (response.errorBody() != null) {
                String errorContent = response.errorBody().string();
                if (isAssignmentColumnIssue(errorContent)) {
                    Log.e(TAG, "服务器数据库列名问题: 作业表的 'due_date' 字段不存在，" +
                            "客户端使用的是 'deadline'");
                    Log.e(TAG, "建议修复: 修改服务器SQL查询，将 a.due_date 改为 a.deadline");
                } else if (isGradeColumnIssue(errorContent)) {
                    Log.e(TAG, "服务器数据库列名问题: 成绩表的 'feedback' 字段不存在");
                    Log.e(TAG, "建议修复: 修改服务器SQL查询，移除 g.feedback 字段");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析服务器错误信息失败", e);
        }
    }

    /**
     * 打印AssignmentDto和GradeDto的字段信息，以帮助调试
     */
    public static void logDtoFields() {
        try {
            AssignmentDto assignmentDto = new AssignmentDto(1, "测试", "测试描述", "2023-01-01T00:00:00.000Z", 100);
            GradeDto gradeDto = new GradeDto("1", 1, null, null, 90.0, "test");

            Gson gson = new Gson();
            Log.d(TAG, "AssignmentDto字段: " + gson.toJson(assignmentDto));
            Log.d(TAG, "GradeDto字段: " + gson.toJson(gradeDto));
        } catch (Exception e) {
            Log.e(TAG, "日志记录失败", e);
        }
    }

    /**
     * 修复作业请求参数
     * 将客户端的deadline字段重命名为服务器需要的due_date字段
     * 
     * @param requestJson 原始请求JSON字符串
     * @return 修复后的JSON字符串
     */
    public static String fixAssignmentRequest(String requestJson) {
        try {
            Log.d(TAG, "修复前的请求JSON: " + requestJson);

            JSONObject jsonObject = new JSONObject(requestJson);

            // 检查是否有deadline字段
            if (jsonObject.has("deadline")) {
                // 获取deadline字段值
                String deadline = jsonObject.getString("deadline");
                // 移除deadline字段
                jsonObject.remove("deadline");
                // 添加due_date字段，确保格式正确
                if (deadline != null && !deadline.trim().isEmpty() && !deadline.equals("null")) {
                    jsonObject.put("due_date", deadline);
                    Log.d(TAG, "已将请求参数中的deadline字段修改为due_date: " + deadline);
                } else {
                    // 如果deadline为空或null，设置当前日期
                    String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault())
                            .format(new java.util.Date());
                    jsonObject.put("due_date", currentDate);
                    Log.d(TAG, "deadline为空，已设置为当前日期: " + currentDate);
                }
            } else if (!jsonObject.has("due_date")) {
                // 既没有deadline也没有due_date字段，添加默认值
                String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault())
                        .format(new java.util.Date());
                jsonObject.put("due_date", currentDate);
                Log.d(TAG, "未找到deadline或due_date字段，已添加默认due_date值: " + currentDate);
            } else {
                // 已有due_date字段，检查是否为null或空
                String dueDate = jsonObject.optString("due_date");
                if (dueDate == null || dueDate.trim().isEmpty() || dueDate.equals("null")) {
                    String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault())
                            .format(new java.util.Date());
                    jsonObject.put("due_date", currentDate);
                    Log.d(TAG, "due_date为空，已设置为当前日期: " + currentDate);
                }
            }

            // 检查max_score字段
            if (jsonObject.has("max_score")) {
                Object maxScore = jsonObject.get("max_score");
                // 确保max_score不为null
                if (maxScore == null || maxScore.toString().equals("null") || maxScore.toString().isEmpty()) {
                    jsonObject.put("max_score", 100);
                    maxScore = 100;
                    Log.d(TAG, "max_score为空，已设置默认值: 100");
                }
                // 添加total_score字段，与max_score保持一致
                jsonObject.put("total_score", maxScore);
                Log.d(TAG, "已将max_score字段(" + maxScore + ")复制为total_score");
            } else {
                // 如果没有max_score字段，添加默认值
                jsonObject.put("max_score", 100);
                jsonObject.put("total_score", 100);
                Log.d(TAG, "未找到max_score字段，已添加默认值: 100");
            }

            String result = jsonObject.toString();
            Log.d(TAG, "修复后的请求JSON: " + result);
            return result;
        } catch (JSONException e) {
            Log.e(TAG, "修复作业请求参数失败", e);
            return requestJson; // 如果出错，返回原始字符串
        }
    }

    /**
     * 修复作业响应数据
     * 将服务器返回的due_date字段重命名为客户端使用的deadline字段
     * 
     * @param responseJson 原始响应JSON字符串
     * @return 修复后的JSON字符串
     */
    public static String fixAssignmentResponse(String responseJson) {
        try {
            JSONObject jsonObject = new JSONObject(responseJson);

            // 检查是否有data字段
            if (jsonObject.has("data")) {
                JSONObject dataObject = jsonObject.getJSONObject("data");

                // 检查data对象中是否有due_date字段
                if (dataObject.has("due_date")) {
                    // 获取due_date字段值
                    String dueDate = dataObject.getString("due_date");
                    // 移除due_date字段
                    dataObject.remove("due_date");
                    // 添加deadline字段
                    dataObject.put("deadline", dueDate);

                    Log.d(TAG, "已将响应数据中的due_date字段修改为deadline");
                }
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "修复作业响应数据失败", e);
            return responseJson; // 如果出错，返回原始字符串
        }
    }
}