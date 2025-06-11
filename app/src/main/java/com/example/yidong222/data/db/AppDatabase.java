package com.example.yidong222.data.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.yidong222.data.db.dao.AssignmentDao;
import com.example.yidong222.data.db.dao.CourseDao;
import com.example.yidong222.data.db.dao.ExamDao;
import com.example.yidong222.data.db.entity.AssignmentEntity;
import com.example.yidong222.data.db.entity.CourseEntity;
import com.example.yidong222.data.db.entity.ExamEntity;

import java.io.File;

@Database(entities = {
        CourseEntity.class,
        ExamEntity.class,
        AssignmentEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "course_db";
    private static AppDatabase instance;

    public abstract CourseDao courseDao();

    public abstract ExamDao examDao();

    public abstract AssignmentDao assignmentDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                // 检查数据库文件是否存在
                File dbFile = context.getDatabasePath(DATABASE_NAME);
                boolean dbExists = dbFile.exists();

                Log.d(TAG, "获取数据库实例，数据库文件路径: " + dbFile.getAbsolutePath());
                Log.d(TAG, "数据库文件是否存在: " + dbExists);

                // 构建数据库实例
                instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries() // 允许在主线程访问数据库（不推荐用于生产环境）
                        .build();

                Log.d(TAG, "数据库实例创建成功");
            } catch (Exception e) {
                Log.e(TAG, "创建数据库实例失败", e);
            }
        }
        return instance;
    }
}