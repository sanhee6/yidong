const Assignment = require('./models/assignment.model');

async function testCreateAssignment() {
  try {
    console.log('测试: 直接使用模型创建作业');
    
    // 创建作业数据
    const assignmentData = {
      course_id: 2,
      title: "测试作业（模型层）",
      description: "这是一个通过模型层直接创建的测试作业",
      due_date: "2023-12-31 23:59:59", // 模型期望的字段名
      max_score: 100 // 模型期望的字段名
    };
    
    console.log('作业数据:', assignmentData);
    
    // 创建作业
    const result = await Assignment.create(assignmentData);
    console.log('创建结果:', result);
    
    if (result && result.insertId) {
      // 获取创建的作业
      const assignment = await Assignment.getById(result.insertId);
      console.log('创建的作业:', assignment);
    }
    
    console.log('测试完成');
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 执行测试
testCreateAssignment(); 