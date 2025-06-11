package com.example.yidong222.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.yidong222.R;
import com.example.yidong222.api.ApiClient;
import com.example.yidong222.models.ApiResponse;
import com.example.yidong222.models.User;
import com.example.yidong222.utils.CaptchaUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText captchaInput;
    private ImageView captchaImage;
    private Button loginButton;
    private ProgressBar loginProgress;
    private TextView loginMessage;

    // 当前验证码
    private String currentCaptcha;

    // 登录结果回调接口
    private LoginResultCallback callback;

    public interface LoginResultCallback {
        void onLoginSuccess(User user);

        void onLoginFailure(String message);
    }

    public LoginFragment() {
        // 必需的空构造函数
    }

    public void setLoginResultCallback(LoginResultCallback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // 初始化视图
        usernameInput = view.findViewById(R.id.username_input);
        passwordInput = view.findViewById(R.id.password_input);
        captchaInput = view.findViewById(R.id.captcha_input);
        captchaImage = view.findViewById(R.id.captcha_image);
        loginButton = view.findViewById(R.id.login_button);
        loginProgress = view.findViewById(R.id.login_progress);
        loginMessage = view.findViewById(R.id.login_message);

        // 生成并显示验证码
        refreshCaptcha();

        // 点击验证码图片刷新验证码
        captchaImage.setOnClickListener(v -> refreshCaptcha());

        // 设置登录按钮的点击事件
        loginButton.setOnClickListener(v -> attemptLogin());

        return view;
    }

    /**
     * 刷新验证码
     */
    private void refreshCaptcha() {
        // 生成新的验证码
        currentCaptcha = CaptchaUtils.generateCaptchaCode();
        // 创建验证码图片
        captchaImage.setImageBitmap(CaptchaUtils.createCaptchaImage(currentCaptcha));
        // 清空验证码输入框
        if (captchaInput != null) {
            captchaInput.setText("");
        }
    }

    private void attemptLogin() {
        // 获取输入
        String username = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String captcha = captchaInput.getText() != null ? captchaInput.getText().toString().trim() : "";

        // 简单验证
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("用户名和密码不能为空");
            return;
        }

        // 验证码验证
        if (captcha.isEmpty()) {
            showMessage("验证码不能为空");
            return;
        }

        // 验证码不区分大小写
        if (!captcha.equalsIgnoreCase(currentCaptcha)) {
            showMessage("验证码错误，请重新输入");
            refreshCaptcha();
            return;
        }

        // 显示进度条
        showLoading(true);

        // 准备登录数据
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        // 发起登录请求
        ApiClient.getUserApiService().login(credentials).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                    @NonNull Response<ApiResponse<User>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        // 登录成功
                        User user = apiResponse.getData();
                        if (callback != null) {
                            callback.onLoginSuccess(user);
                        }
                    } else {
                        // 登录失败，展示错误信息
                        String message = apiResponse.getMessage();
                        showMessage(message != null ? message : "登录失败");
                        if (callback != null) {
                            callback.onLoginFailure(message);
                        }
                        // 刷新验证码
                        refreshCaptcha();
                    }
                } else {
                    // API响应错误
                    String errorMessage = "登录失败，请检查网络连接";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }

                    showMessage(errorMessage);
                    if (callback != null) {
                        callback.onLoginFailure(errorMessage);
                    }
                    // 刷新验证码
                    refreshCaptcha();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "Login request failed", t);
                showLoading(false);

                String errorMessage;
                // 处理登录请求失败的情况，提供更友好的错误信息
                if (t.getMessage() != null && t.getMessage().contains("Expected a boolean but was NUMBER")) {
                    // 处理特定的类型转换错误
                    errorMessage = "登录失败：服务器返回数据格式不正确，请联系管理员";

                    // 尝试再次登录
                    Toast.makeText(getContext(), "正在尝试修复问题...", Toast.LENGTH_SHORT).show();
                    resetForm();
                } else {
                    errorMessage = "登录请求失败: " + t.getMessage();
                }

                showMessage(errorMessage);

                if (callback != null) {
                    callback.onLoginFailure(errorMessage);
                }

                // 刷新验证码
                refreshCaptcha();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showMessage(String message) {
        if (message != null && !message.isEmpty()) {
            loginMessage.setText(message);
            loginMessage.setVisibility(View.VISIBLE);
        } else {
            loginMessage.setVisibility(View.GONE);
        }
    }

    // 重置表单
    public void resetForm() {
        if (usernameInput != null) {
            usernameInput.setText("");
        }
        if (passwordInput != null) {
            passwordInput.setText("");
        }
        if (captchaInput != null) {
            captchaInput.setText("");
        }
        if (loginMessage != null) {
            loginMessage.setVisibility(View.GONE);
        }
        // 刷新验证码
        refreshCaptcha();
    }
}