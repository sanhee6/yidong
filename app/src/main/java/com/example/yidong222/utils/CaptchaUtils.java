package com.example.yidong222.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class CaptchaUtils {
    private static final char[] CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private static final int DEFAULT_CODE_LENGTH = 4;
    private static final int DEFAULT_FONT_SIZE = 50;
    private static final int DEFAULT_LINE_NUMBER = 3;
    private static final int BASE_PADDING_LEFT = 20;
    private static final int RANGE_PADDING_LEFT = 35;
    private static final int BASE_PADDING_TOP = 70;
    private static final int RANGE_PADDING_TOP = 15;
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;

    private static final Random random = new Random();

    /**
     * 生成验证码图片
     */
    public static Bitmap createCaptchaImage(String code) {
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 设置背景颜色
        canvas.drawColor(Color.rgb(240, 240, 240));

        // 创建画笔
        Paint paint = new Paint();
        paint.setTextSize(DEFAULT_FONT_SIZE);

        // 绘制干扰线
        for (int i = 0; i < DEFAULT_LINE_NUMBER; i++) {
            int startX = random.nextInt(width);
            int startY = random.nextInt(height);
            int stopX = random.nextInt(width);
            int stopY = random.nextInt(height);

            paint.setStrokeWidth(2);
            paint.setColor(getRandomColor());
            canvas.drawLine(startX, startY, stopX, stopY, paint);
        }

        // 绘制验证码
        for (int i = 0; i < code.length(); i++) {
            paint.setColor(getRandomColor());
            paint.setFakeBoldText(random.nextBoolean());
            float skewX = random.nextInt(11) / 10;
            skewX = random.nextBoolean() ? skewX : -skewX;
            paint.setTextSkewX(skewX);

            int padding_left = BASE_PADDING_LEFT + random.nextInt(RANGE_PADDING_LEFT);
            int padding_top = BASE_PADDING_TOP + random.nextInt(RANGE_PADDING_TOP);

            canvas.drawText(String.valueOf(code.charAt(i)), padding_left + i * width / DEFAULT_CODE_LENGTH, padding_top,
                    paint);
        }

        return bitmap;
    }

    /**
     * 生成随机验证码
     */
    public static String generateCaptchaCode() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < DEFAULT_CODE_LENGTH; i++) {
            buffer.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return buffer.toString();
    }

    /**
     * 获取随机颜色
     */
    private static int getRandomColor() {
        return Color.rgb(random.nextInt(200), random.nextInt(200), random.nextInt(200));
    }
}