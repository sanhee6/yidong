const fetch = require('node-fetch');

// API基础URL
const BASE_URL = 'http://localhost:3000/api';

// =========================
// 作业API测试
// =========================

// 测试获取所有作业
async function testGetAllAssignments() {
  try {
    console.log('测试: 获取所有作业');
    const response = await fetch(`${BASE_URL}/assignments`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试获取单个作业
async function testGetAssignmentById(id) {
  try {
    console.log(`\n测试: 获取ID为${id}的作业`);
    const response = await fetch(`${BASE_URL}/assignments/${id}`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试按课程ID获取作业
async function testGetAssignmentsByCourse(courseId) {
  try {
    console.log(`\n测试: 获取课程ID为${courseId}的所有作业`);
    const response = await fetch(`${BASE_URL}/assignments/course/${courseId}`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

  // 测试创建作业
async function testCreateAssignment() {
  try {
    console.log('\n测试: 创建新作业');
    // 创建一个日期对象，格式化为YYYY-MM-DD HH:MM:SS
    const dueDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    const formattedDate = dueDate.getFullYear() + '-' + 
                         String(dueDate.getMonth() + 1).padStart(2, '0') + '-' + 
                         String(dueDate.getDate()).padStart(2, '0') + ' ' + 
                         String(dueDate.getHours()).padStart(2, '0') + ':' + 
                         String(dueDate.getMinutes()).padStart(2, '0') + ':' + 
                         String(dueDate.getSeconds()).padStart(2, '0');
    
    // 注意：API接收deadline和total_score字段
    const newAssignment = {
      course_id: 1,
      title: "测试作业",
      description: "这是一个测试作业描述",
      deadline: formattedDate, // 数据库中的字段名
      total_score: 100 // 数据库中的字段名
    };
    
    const response = await fetch(`${BASE_URL}/assignments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(newAssignment)
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data.data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试更新作业
async function testUpdateAssignment(id) {
  try {
    console.log(`\n测试: 更新ID为${id}的作业`);
    const updateData = {
      title: "更新后的作业标题",
      description: "这是更新后的作业描述"
    };
    
    const response = await fetch(`${BASE_URL}/assignments/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updateData)
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试删除作业
async function testDeleteAssignment(id) {
  try {
    console.log(`\n测试: 删除ID为${id}的作业`);
    const response = await fetch(`${BASE_URL}/assignments/${id}`, {
      method: 'DELETE'
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// =========================
// 考试API测试
// =========================

// 测试获取所有考试
async function testGetAllExams() {
  try {
    console.log('\n测试: 获取所有考试');
    const response = await fetch(`${BASE_URL}/exams`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试获取单个考试
async function testGetExamById(id) {
  try {
    console.log(`\n测试: 获取ID为${id}的考试`);
    const response = await fetch(`${BASE_URL}/exams/${id}`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试按课程ID获取考试
async function testGetExamsByCourse(courseId) {
  try {
    console.log(`\n测试: 获取课程ID为${courseId}的所有考试`);
    const response = await fetch(`${BASE_URL}/exams/course/${courseId}`);
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

  // 测试创建考试
async function testCreateExam() {
  try {
    console.log('\n测试: 创建新考试');
    // 创建一个日期对象，格式化为YYYY-MM-DD HH:MM:SS
    const examDate = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000);
    const formattedDate = examDate.getFullYear() + '-' + 
                         String(examDate.getMonth() + 1).padStart(2, '0') + '-' + 
                         String(examDate.getDate()).padStart(2, '0') + ' ' + 
                         String(examDate.getHours()).padStart(2, '0') + ':' + 
                         String(examDate.getMinutes()).padStart(2, '0') + ':' + 
                         String(examDate.getSeconds()).padStart(2, '0');
    
    const newExam = {
      course_id: 1,
      title: "测试考试",
      exam_date: formattedDate, // 使用格式化的日期
      duration: 120,
      location: "测试教室",
      description: "这是一个测试考试描述"
    };
    
    const response = await fetch(`${BASE_URL}/exams`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(newExam)
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data.data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试更新考试
async function testUpdateExam(id) {
  try {
    console.log(`\n测试: 更新ID为${id}的考试`);
    const updateData = {
      title: "更新后的考试标题",
      location: "更新后的考试地点",
      description: "这是更新后的考试描述"
    };
    
    const response = await fetch(`${BASE_URL}/exams/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updateData)
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// 测试删除考试
async function testDeleteExam(id) {
  try {
    console.log(`\n测试: 删除ID为${id}的考试`);
    const response = await fetch(`${BASE_URL}/exams/${id}`, {
      method: 'DELETE'
    });
    
    const data = await response.json();
    console.log('状态码:', response.status);
    console.log('响应数据:', JSON.stringify(data, null, 2));
    return data;
  } catch (error) {
    console.error('测试失败:', error);
  }
}

// =========================
// 运行测试
// =========================
async function runAssignmentTests() {
  console.log('======= 开始测试作业API =======');
  
  // 获取所有作业
  await testGetAllAssignments();
  
  // 获取第一个作业的详情
  const assignmentsData = await testGetAllAssignments();
  if (assignmentsData && assignmentsData.data && assignmentsData.data.rows && assignmentsData.data.rows.length > 0) {
    const firstAssignmentId = assignmentsData.data.rows[0].id;
    await testGetAssignmentById(firstAssignmentId);
    
    // 获取该作业所属课程的所有作业
    const courseId = assignmentsData.data.rows[0].course_id;
    await testGetAssignmentsByCourse(courseId);
  }
  
  // 创建新作业
  const createdAssignment = await testCreateAssignment();
  
  if (createdAssignment && createdAssignment.id) {
    // 更新刚创建的作业
    await testUpdateAssignment(createdAssignment.id);
    
    // 删除刚创建的作业
    await testDeleteAssignment(createdAssignment.id);
  }
  
  console.log('\n======= 作业API测试完成 =======');
}

async function runExamTests() {
  console.log('\n======= 开始测试考试API =======');
  
  // 获取所有考试
  await testGetAllExams();
  
  // 获取第一个考试的详情
  const examsData = await testGetAllExams();
  if (examsData && examsData.data && examsData.data.rows && examsData.data.rows.length > 0) {
    const firstExamId = examsData.data.rows[0].id;
    await testGetExamById(firstExamId);
    
    // 获取该考试所属课程的所有考试
    const courseId = examsData.data.rows[0].course_id;
    await testGetExamsByCourse(courseId);
  }
  
  // 创建新考试
  const createdExam = await testCreateExam();
  
  if (createdExam && createdExam.id) {
    // 更新刚创建的考试
    await testUpdateExam(createdExam.id);
    
    // 删除刚创建的考试
    await testDeleteExam(createdExam.id);
  }
  
  console.log('\n======= 考试API测试完成 =======');
}

// 执行所有测试
async function runAllTests() {
  await runAssignmentTests();
  await runExamTests();
  console.log('\n所有API测试已完成!');
}

// 执行测试
runAllTests(); 