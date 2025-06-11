const Assignment = require('../models/assignment.model.js');
const { transformAssignmentFields } = require('../utils/field_mapper.js');

/**
 * 作业控制器
 * 处理作业相关的业务逻辑
 */

/**
 * 获取作业列表（支持分页）
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.getAssignments = async (req, res) => {
  try {
    // 解析分页参数
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    
    // 获取作业列表
    const result = await Assignment.getAll(page, limit);
    
    // 转换字段名称，确保API一致性
    const transformedRows = transformAssignmentFields(result.rows);
    
    res.json({
      status: 'success',
      data: transformedRows,
      pagination: result.pagination
    });
  } catch (err) {
    console.error('获取作业列表失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '获取作业列表失败',
      error: err.message
    });
  }
};

/**
 * 根据课程ID获取作业列表
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.getAssignmentsByCourseId = async (req, res) => {
  try {
    const courseId = parseInt(req.params.courseId);
    
    // 验证课程ID
    if (isNaN(courseId) || courseId <= 0) {
      return res.status(400).json({
        status: 'fail',
        message: '无效的课程ID'
      });
    }
    
    // 获取该课程的所有作业
    const assignments = await Assignment.getByCourseId(courseId);
    
    // 转换字段名称，确保API一致性
    const transformedAssignments = transformAssignmentFields(assignments);
    
    res.json({
      status: 'success',
      data: transformedAssignments
    });
  } catch (err) {
    console.error('获取课程作业列表失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '获取课程作业列表失败',
      error: err.message
    });
  }
};

/**
 * 获取单个作业详情
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.getAssignmentById = async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    
    // 获取作业详情
    const assignment = await Assignment.getById(id);
    
    if (!assignment) {
      return res.status(404).json({
        status: 'fail',
        message: '未找到作业'
      });
    }
    
    // 转换字段名称，确保API一致性
    const transformedAssignment = transformAssignmentFields(assignment);
    
    res.json({
      status: 'success',
      data: transformedAssignment
    });
  } catch (err) {
    console.error('获取作业详情失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '获取作业详情失败',
      error: err.message
    });
  }
};

/**
 * 搜索作业
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.searchAssignments = async (req, res) => {
  try {
    const query = req.query.query || '';
    
    // 如果搜索关键词为空，返回空结果
    if (!query.trim()) {
      return res.json({
        status: 'success',
        data: [],
        message: '搜索关键词不能为空'
      });
    }
    
    // 搜索作业
    const assignments = await Assignment.search(query);
    
    // 转换字段名称，确保API一致性
    const transformedAssignments = transformAssignmentFields(assignments);
    
    res.json({
      status: 'success',
      data: transformedAssignments
    });
  } catch (err) {
    console.error('搜索作业失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '搜索作业失败',
      error: err.message
    });
  }
};

/**
 * 创建新作业
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.createAssignment = async (req, res) => {
  try {
    // 从请求体获取数据
    const { course_id, title, description, due_date, max_score } = req.body;
    
    // 确保截止日期不为null
    let deadline = due_date;
    if (!deadline || deadline === null || deadline === undefined || deadline === '') {
      // 如果截止日期为空，使用当前日期
      const now = new Date();
      deadline = now.toISOString().slice(0, 19).replace('T', ' ');
      console.log('截止日期为空，使用当前日期:', deadline);
    }
    
    // 确保最高分不为null
    let totalScore = max_score;
    if (totalScore === undefined || totalScore === null) {
      totalScore = 100; // 默认值
      console.log('最高分为空，使用默认值:', totalScore);
    }
    
    // 创建作业（注意：映射API字段名到数据库字段名）
    // 再次确认deadline不为null
    if (!deadline || deadline === null || deadline === undefined || deadline === '') {
      const now = new Date();
      deadline = now.toISOString().slice(0, 19).replace('T', ' ');
      console.log('最终检查：截止日期为空，使用当前日期:', deadline);
    }
    
    const result = await Assignment.create({
      course_id,
      title,
      description,
      deadline: deadline,  // 使用处理后的deadline
      total_score: totalScore  // 使用处理后的total_score
    });
    
    // 获取创建后的作业详情
    const newAssignment = await Assignment.getById(result.insertId);
    
    // 转换字段名称，确保API一致性
    const transformedAssignment = transformAssignmentFields(newAssignment);
    
    res.status(201).json({
      status: 'success',
      message: '作业创建成功',
      data: transformedAssignment
    });
  } catch (err) {
    console.error('创建作业失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '创建作业失败',
      error: err.message
    });
  }
};

/**
 * 更新作业
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.updateAssignment = async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    
    // 检查作业是否存在
    const existingAssignment = await Assignment.getById(id);
    if (!existingAssignment) {
      return res.status(404).json({
        status: 'fail',
        message: '未找到作业'
      });
    }
    
    // 从请求体获取更新数据
    const { course_id, title, description, due_date, max_score } = req.body;
    
    // 确保截止日期不为null（如果提供了）
    let deadline = due_date;
    if (due_date !== undefined && !deadline) {
      // 如果截止日期为空，保留原来的值或使用当前日期
      deadline = existingAssignment.deadline || new Date().toISOString().slice(0, 19).replace('T', ' ');
      console.log('更新时截止日期为空，使用:', deadline);
    }
    
    // 确保最高分不为null（如果提供了）
    let totalScore = max_score;
    if (max_score !== undefined && (totalScore === null || totalScore === undefined)) {
      totalScore = existingAssignment.max_score || 100; // 使用原值或默认值
      console.log('更新时最高分为空，使用:', totalScore);
    }
    
    // 更新作业（注意：映射API字段名到数据库字段名）
    const updateData = {};
    
    if (course_id !== undefined) updateData.course_id = course_id;
    if (title !== undefined) updateData.title = title;
    if (description !== undefined) updateData.description = description;
    if (due_date !== undefined) updateData.deadline = deadline;
    if (max_score !== undefined) updateData.total_score = totalScore;
    
    const result = await Assignment.update(id, updateData);
    
    // 获取更新后的作业
    const updatedAssignment = await Assignment.getById(id);
    
    // 转换字段名称，确保API一致性
    const transformedAssignment = transformAssignmentFields(updatedAssignment);
    
    res.json({
      status: 'success',
      message: '作业更新成功',
      data: transformedAssignment
    });
  } catch (err) {
    console.error('更新作业失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '更新作业失败',
      error: err.message
    });
  }
};

/**
 * 删除作业
 * @param {Object} req - Express请求对象
 * @param {Object} res - Express响应对象
 */
exports.deleteAssignment = async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    
    // 检查作业是否存在
    const existingAssignment = await Assignment.getById(id);
    if (!existingAssignment) {
      return res.status(404).json({
        status: 'fail',
        message: '未找到作业'
      });
    }
    
    // 删除作业
    await Assignment.delete(id);
    
    res.json({
      status: 'success',
      message: '作业删除成功'
    });
  } catch (err) {
    console.error('删除作业失败:', err);
    res.status(500).json({
      status: 'fail',
      message: '删除作业失败',
      error: err.message
    });
  }
}; 