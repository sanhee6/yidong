const fetch = require('node-fetch');

// API基础URL
const BASE_URL = 'http://localhost:3000/api';

// 测试创建作业
async function testCreateAssignment() {
  try {
    console.log('测试: 创建新作业');
    const newAssignment = {
      course_id: 2, // 使用存在的课程ID
      title: "测试作业",
      description: "这是一个测试作业描述",
      due_date: "2023-12-31 23:59:59", // 控制器期望的字段名
      max_score: 100 // 控制器期望的字段名
    };
    
    console.log('请求数据:', JSON.stringify(newAssignment, null, 2));
    
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

// 测试创建考试
async function testCreateExam() {
  try {
    console.log('\n测试: 创建新考试');
    const newExam = {
      course_id: 2, // 使用存在的课程ID
      title: "测试考试",
      exam_date: "2023-12-31 10:00:00", // 使用固定日期字符串
      duration: 120,
      location: "测试教室",
      description: "这是一个测试考试描述"
    };
    
    console.log('请求数据:', JSON.stringify(newExam, null, 2));
    
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

// 执行测试
async function runTests() {
  console.log('======= 开始测试作业和考试创建API =======');
  
  // 创建作业
  await testCreateAssignment();
  
  // 创建考试
  await testCreateExam();
  
  console.log('\n======= 测试完成 =======');
}

// 执行测试
runTests(); 