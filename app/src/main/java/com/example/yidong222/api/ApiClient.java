package com.example.yidong222.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;
import android.util.Log;
import retrofit2.Response;
import okhttp3.ResponseBody;
import java.io.IOException;
import android.os.Build;
import okio.Buffer;

public class ApiClient {
    // 更新服务器地址配置，修复不同组件使用不同地址的问题
    private static String baseUrl = "http://10.0.2.2:3000/"; // 默认模拟器地址
    // 备用服务器地址列表
    private static final String[] FALLBACK_URLS = {
            "http://10.0.2.2:3000/", // 模拟器访问本地主机
            "http://192.168.0.100:3000/", // 局域网地址示例
            "https://api.example.com/" // 生产环境地址示例
    };
    private static int currentUrlIndex = 0;

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;
    private static final String TAG = "ApiClient";

    // API服务接口实例
    private static CourseApiService courseApiService = null;
    private static ExamApiService examApiService = null;
    private static AssignmentApiService assignmentApiService = null;
    private static GradeApiService gradeApiService = null;
    private static UserApiService userApiService = null;

    // 网络请求超时时间 - 减少超时时间，避免长时间等待
    private static final int CONNECT_TIMEOUT = 20; // 秒
    private static final int READ_TIMEOUT = 20; // 秒
    private static final int WRITE_TIMEOUT = 20; // 秒

    // 初始化API基础地址
    static {
        initBaseUrl();
    }

    /**
     * 初始化API基础地址，根据设备环境选择合适的服务器地址
     */
    private static void initBaseUrl() {
        // 检测是否在模拟器环境中运行
        boolean isEmulator = isRunningOnEmulator();

        if (!isEmulator) {
            // 在真实设备上，使用第二个地址（局域网地址）
            currentUrlIndex = 1;
            baseUrl = FALLBACK_URLS[currentUrlIndex];
            Log.d(TAG, "运行在真实设备上，使用服务器地址: " + baseUrl);
        } else {
            // 在模拟器上始终使用10.0.2.2
            currentUrlIndex = 0;
            baseUrl = FALLBACK_URLS[currentUrlIndex];
            Log.d(TAG, "运行在模拟器上，使用模拟器专用地址: " + baseUrl);
        }
    }

    /**
     * 判断当前是否运行在模拟器上
     * 
     * @return 是否是模拟器
     */
    private static boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.PRODUCT.contains("sdk")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    public static void resetClient() {
        retrofit = null;
        okHttpClient = null;
        courseApiService = null;
        examApiService = null;
        assignmentApiService = null;
        gradeApiService = null;
        userApiService = null;
        Log.d(TAG, "Retrofit客户端已重置");
    }

    /**
     * 尝试下一个备用服务器地址
     */
    public static boolean tryNextServer() {
        // 保存旧地址，以便于日志
        String oldUrl = baseUrl;

        // 切换到下一个服务器
        currentUrlIndex = (currentUrlIndex + 1) % FALLBACK_URLS.length;
        baseUrl = FALLBACK_URLS[currentUrlIndex];

        Log.d(TAG, "切换服务器: " + oldUrl + " -> " + baseUrl);
        resetClient();
        return true;
    }

    /**
     * 强制使用模拟器地址
     */
    public static void forceEmulatorUrl() {
        baseUrl = FALLBACK_URLS[0]; // 使用模拟器地址
        currentUrlIndex = 0;
        Log.d(TAG, "强制使用模拟器地址: " + baseUrl);
        resetClient();
    }

    /**
     * 刷新API客户端，尝试修复API连接问题
     */
    public static void refreshClient() {
        Log.d(TAG, "正在刷新API客户端...");
        resetClient();
        // 重新获取客户端，同时连接测试
        Retrofit client = getClient();
        if (client != null) {
            Log.d(TAG, "API客户端刷新成功, BASE_URL: " + baseUrl);
        } else {
            Log.e(TAG, "API客户端刷新失败");
        }
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            if (okHttpClient == null) {
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

                // 设置超时
                httpClient.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
                httpClient.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
                httpClient.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);

                // 添加日志拦截器
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.addInterceptor(loggingInterceptor);

                // 添加字段转换拦截器
                httpClient.addInterceptor(chain -> {
                    okhttp3.Request originalRequest = chain.request();

                    // 检查是否是作业相关的请求
                    String requestUrl = originalRequest.url().toString();
                    if (requestUrl.contains("/api/assignments") &&
                            (originalRequest.method().equals("POST") || originalRequest.method().equals("PUT"))) {

                        // 获取原始请求体
                        okhttp3.RequestBody originalBody = originalRequest.body();
                        if (originalBody != null) {
                            // 读取请求体内容
                            okhttp3.MediaType mediaType = originalBody.contentType();
                            Buffer buffer = new Buffer();
                            originalBody.writeTo(buffer);
                            String bodyString = buffer.readUtf8();

                            // 使用ServerFixHelper修复字段名称
                            String fixedBodyString = ServerFixHelper.fixAssignmentRequest(bodyString);

                            // 创建新的请求体
                            okhttp3.RequestBody newBody = okhttp3.RequestBody.create(mediaType, fixedBodyString);

                            // 创建新的请求
                            okhttp3.Request newRequest = originalRequest.newBuilder()
                                    .method(originalRequest.method(), newBody)
                                    .build();

                            // 发送修改后的请求
                            Log.d(TAG, "已修复作业请求参数的字段名称");
                            okhttp3.Response response = chain.proceed(newRequest);

                            // 处理响应
                            if (response.body() != null) {
                                try {
                                    // 读取原始响应体
                                    String responseString = response.body().string();

                                    // 检查是否是错误响应
                                    if (!response.isSuccessful()) {
                                        // 对于错误响应，记录详细信息但不修改内容
                                        Log.e(TAG, "作业请求失败: " + response.code() + " " + response.message());
                                        Log.e(TAG, "错误响应: " + responseString);

                                        // 重新创建响应体，因为原始响应体已被消费
                                        okhttp3.ResponseBody newErrorBody = okhttp3.ResponseBody.create(
                                                response.body().contentType(),
                                                responseString);

                                        return response.newBuilder()
                                                .body(newErrorBody)
                                                .build();
                                    }

                                    // 修复响应中的字段名称
                                    String fixedResponseString = ServerFixHelper.fixAssignmentResponse(responseString);

                                    // 创建新的响应体
                                    okhttp3.ResponseBody newResponseBody = okhttp3.ResponseBody.create(
                                            response.body().contentType(),
                                            fixedResponseString);

                                    // 创建新的响应
                                    response = response.newBuilder()
                                            .body(newResponseBody)
                                            .build();

                                    Log.d(TAG, "已修复作业响应数据的字段名称");
                                } catch (Exception e) {
                                    Log.e(TAG, "处理响应体时出错", e);
                                    // 如果处理出错，返回原始响应
                                    return response;
                                }
                            }

                            return response;
                        }
                    }

                    // 对于非作业请求或无请求体的请求，直接处理
                    return chain.proceed(originalRequest);
                });

                okHttpClient = httpClient.build();
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // 获取课程API服务
    public static CourseApiService getCourseApiService() {
        if (courseApiService == null) {
            courseApiService = getClient().create(CourseApiService.class);
        }
        return courseApiService;
    }

    // 获取考试API服务
    public static ExamApiService getExamApiService() {
        if (examApiService == null) {
            examApiService = getClient().create(ExamApiService.class);
        }
        return examApiService;
    }

    // 获取作业API服务
    public static AssignmentApiService getAssignmentApiService() {
        if (assignmentApiService == null) {
            assignmentApiService = getClient().create(AssignmentApiService.class);
        }
        return assignmentApiService;
    }

    // 获取成绩API服务
    public static GradeApiService getGradeApiService() {
        if (gradeApiService == null) {
            gradeApiService = getClient().create(GradeApiService.class);
        }
        return gradeApiService;
    }

    // 获取用户API服务
    public static UserApiService getUserApiService() {
        if (userApiService == null) {
            userApiService = getClient().create(UserApiService.class);
        }
        return userApiService;
    }

    // 用于调试的日志工具方法
    public static <T> void logResponseError(Response<T> response) {
        if (!response.isSuccessful()) {
            try {
                ResponseBody errorBody = response.errorBody();
                if (errorBody != null) {
                    String errorContent = errorBody.string();
                    Log.e(TAG, "API错误(HTTP " + response.code() + "): " + errorContent);

                    // 检查API路径问题
                    if (errorContent.contains("找不到路径")) {
                        Log.e(TAG, "API路径错误，请检查路径是否正确");

                        // 记录请求URL
                        String requestUrl = response.raw().request().url().toString();
                        Log.e(TAG, "请求URL: " + requestUrl);

                        // 检查具体的API路径问题
                        if (requestUrl.contains("/api/course-schedules")) {
                            Log.e(TAG, "建议修复: 使用 /api/course_schedules 替代 /api/course-schedules");
                        } else if (requestUrl.contains("/api/course_schedules")) {
                            Log.e(TAG, "API路径'/api/course_schedules'已正确设置，但服务器可能不存在该路径");
                        } else if (requestUrl.contains("/api/student-grades")) {
                            Log.e(TAG, "建议修复: 使用 /api/grades 替代 /api/student-grades");
                        }
                    }
                } else {
                    Log.e(TAG, "API错误(HTTP " + response.code() + "): 无错误信息");
                }
            } catch (IOException e) {
                Log.e(TAG, "解析错误响应失败: " + e.getMessage());
            }
        }
    }
}