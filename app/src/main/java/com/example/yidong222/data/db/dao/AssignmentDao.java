package com.example.yidong222.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.yidong222.data.db.entity.AssignmentEntity;

import java.util.List;

@Dao
public interface AssignmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AssignmentEntity assignment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AssignmentEntity> assignments);

    @Update
    void update(AssignmentEntity assignment);

    @Delete
    void delete(AssignmentEntity assignment);

    /**
     * 根据ID删除作业
     * 
     * @param assignmentId 作业ID
     * @return 删除的行数
     */
    @Query("DELETE FROM assignments WHERE id = :assignmentId")
    int deleteById(int assignmentId);

    /**
     * 插入或更新作业
     * 如果作业ID已存在，则更新；否则插入新记录
     * 
     * @param assignment 作业实体
     */
    @Transaction
    default void insertOrUpdate(AssignmentEntity assignment) {
        AssignmentEntity existing = getAssignmentById(assignment.id);
        if (existing != null) {
            update(assignment);
        } else {
            insert(assignment);
        }
    }

    @Query("SELECT * FROM assignments WHERE id = :assignmentId")
    AssignmentEntity getAssignmentById(int assignmentId);

    @Query("SELECT * FROM assignments WHERE courseId = :courseId")
    List<AssignmentEntity> getAssignmentsByCourseId(int courseId);

    @Query("SELECT * FROM assignments WHERE status = :status")
    List<AssignmentEntity> getAssignmentsByStatus(String status);

    @Query("DELETE FROM assignments WHERE courseId = :courseId")
    void deleteByCourse(int courseId);

    @Query("DELETE FROM assignments")
    void deleteAll();

    @Query("SELECT * FROM assignments")
    List<AssignmentEntity> getAllAssignments();
}