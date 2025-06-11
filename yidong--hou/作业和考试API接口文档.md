# 作业和考试API接口文档

## 基础信息

- 基础URL: `http://localhost:3000/api`
- 数据格式: JSON
- 认证方式: 无（可根据需要添加）

## 一、作业管理API

### 1. 获取所有作业

获取系统中的所有作业信息，支持分页。

**请求方法**: `GET`

**URL**: `/assignments`

**查询参数**:

| 参数名 | 类型   | 必填 | 描述                |
|--------|--------|------|---------------------|
| page   | Number | 否   | 页码（默认值: 1）    |
| limit  | Number | 否   | 每页数量（默认值: 10）|

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 10,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "网络拓扑设计",
      "description": "设计一个小型企业网络拓扑结构",
      "deadline": "2023-04-10T15:59:59.000Z",
      "total_score": 100,
      "created_at": "2025-05-03T18:21:27.000Z",
      "updated_at": "2025-05-03T18:21:27.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 10,
    "totalPages": 1
  }
}
```

### 2. 获取指定作业

根据ID获取单个作业的详细信息。

**请求方法**: `GET`

**URL**: `/assignments/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述    |
|--------|--------|------|---------|
| id     | Number | 是   | 作业ID  |

**响应示例**:

```json
{
  "status": "success",
  "data": {
    "id": 10,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "网络拓扑设计",
    "description": "设计一个小型企业网络拓扑结构",
    "deadline": "2023-04-10T15:59:59.000Z",
    "total_score": 100,
    "created_at": "2025-05-03T18:21:27.000Z",
    "updated_at": "2025-05-03T18:21:27.000Z"
  }
}
```

### 3. 按课程获取作业

获取指定课程的所有作业列表。

**请求方法**: `GET`

**URL**: `/assignments/course/:courseId`

**路径参数**:

| 参数名    | 类型   | 必填 | 描述   |
|-----------|--------|------|--------|
| courseId  | Number | 是   | 课程ID |

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 10,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "网络拓扑设计",
      "description": "设计一个小型企业网络拓扑结构",
      "deadline": "2023-04-10T15:59:59.000Z",
      "total_score": 100,
      "created_at": "2025-05-03T18:21:27.000Z",
      "updated_at": "2025-05-03T18:21:27.000Z"
    }
  ]
}
```

### 4. 搜索作业

根据关键词搜索作业。

**请求方法**: `GET`

**URL**: `/assignments/search`

**查询参数**:

| 参数名 | 类型   | 必填 | 描述     |
|--------|--------|------|----------|
| query  | String | 是   | 搜索关键词 |

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 10,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "网络拓扑设计",
      "description": "设计一个小型企业网络拓扑结构",
      "deadline": "2023-04-10T15:59:59.000Z",
      "total_score": 100,
      "created_at": "2025-05-03T18:21:27.000Z",
      "updated_at": "2025-05-03T18:21:27.000Z"
    }
  ]
}
```

### 5. 创建作业

创建一个新的作业记录。

**请求方法**: `POST`

**URL**: `/assignments`

**请求体**:

| 字段名      | 类型   | 必填 | 描述                     |
|-------------|--------|------|--------------------------|
| course_id   | Number | 是   | 关联的课程ID             |
| title       | String | 是   | 作业标题                 |
| description | String | 否   | 作业描述                 |
| due_date    | String | 是   | 截止日期（YYYY-MM-DD HH:MM:SS格式）|
| max_score   | Number | 是   | 总分值                   |

**请求示例**:

```json
{
  "course_id": 2,
  "title": "测试作业",
  "description": "这是一个测试作业描述",
  "due_date": "2023-12-31 23:59:59",
  "max_score": 100
}
```

**响应示例**:

```json
{
  "status": "success",
  "message": "作业创建成功",
  "data": {
    "id": 12,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "测试作业",
    "description": "这是一个测试作业描述",
    "deadline": "2023-12-31T15:59:59.000Z",
    "total_score": 100,
    "created_at": "2025-06-10T13:26:04.000Z",
    "updated_at": "2025-06-10T13:26:04.000Z"
  }
}
```

### 6. 更新作业

更新现有的作业信息。

**请求方法**: `PUT`

**URL**: `/assignments/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述   |
|--------|--------|------|--------|
| id     | Number | 是   | 作业ID |

**请求体**:

| 字段名      | 类型   | 必填 | 描述                    |
|-------------|--------|------|-------------------------|
| course_id   | Number | 否   | 关联的课程ID            |
| title       | String | 否   | 作业标题                |
| description | String | 否   | 作业描述                |
| due_date    | String | 否   | 截止日期（YYYY-MM-DD HH:MM:SS格式）|
| max_score   | Number | 否   | 总分值                  |

**请求示例**:

```json
{
  "title": "更新后的作业标题",
  "description": "这是更新后的作业描述"
}
```

**响应示例**:

```json
{
  "status": "success",
  "message": "作业更新成功",
  "data": {
    "id": 12,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "更新后的作业标题",
    "description": "这是更新后的作业描述",
    "deadline": "2023-12-31T15:59:59.000Z",
    "total_score": 100,
    "created_at": "2025-06-10T13:26:04.000Z",
    "updated_at": "2025-06-10T13:30:00.000Z"
  }
}
```

### 7. 删除作业

删除指定的作业记录。

**请求方法**: `DELETE`

**URL**: `/assignments/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述   |
|--------|--------|------|--------|
| id     | Number | 是   | 作业ID |

**响应示例**:

```json
{
  "status": "success",
  "message": "作业删除成功"
}
```

## 二、考试管理API

### 1. 获取所有考试

获取系统中的所有考试信息，支持分页。

**请求方法**: `GET`

**URL**: `/exams`

**查询参数**:

| 参数名 | 类型   | 必填 | 描述                |
|--------|--------|------|---------------------|
| page   | Number | 否   | 页码（默认值: 1）    |
| limit  | Number | 否   | 每页数量（默认值: 10）|

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 4,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "计算机网络期末考试",
      "exam_date": "2023-06-22T06:00:00.000Z",
      "duration": 150,
      "location": "B204",
      "description": "闭卷考试，只允许带计算器",
      "created_at": "2025-05-03T16:33:52.000Z",
      "updated_at": "2025-06-10T12:56:48.000Z"
    }
  ],
  "pagination": {
    "total": 6,
    "page": 1,
    "limit": 10,
    "totalPages": 1
  }
}
```

### 2. 获取指定考试

根据ID获取单个考试的详细信息。

**请求方法**: `GET`

**URL**: `/exams/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述    |
|--------|--------|------|---------|
| id     | Number | 是   | 考试ID  |

**响应示例**:

```json
{
  "status": "success",
  "data": {
    "id": 4,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "计算机网络期末考试",
    "exam_date": "2023-06-22T06:00:00.000Z",
    "duration": 150,
    "location": "B204",
    "description": "闭卷考试，只允许带计算器",
    "created_at": "2025-05-03T16:33:52.000Z",
    "updated_at": "2025-06-10T12:56:48.000Z"
  }
}
```

### 3. 按课程获取考试

获取指定课程的所有考试列表。

**请求方法**: `GET`

**URL**: `/exams/course/:courseId`

**路径参数**:

| 参数名    | 类型   | 必填 | 描述   |
|-----------|--------|------|--------|
| courseId  | Number | 是   | 课程ID |

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 4,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "计算机网络期末考试",
      "exam_date": "2023-06-22T06:00:00.000Z",
      "duration": 150,
      "location": "B204",
      "description": "闭卷考试，只允许带计算器",
      "created_at": "2025-05-03T16:33:52.000Z",
      "updated_at": "2025-06-10T12:56:48.000Z"
    },
    {
      "id": 3,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "计算机网络期中考试",
      "exam_date": "2023-04-16T01:00:00.000Z",
      "duration": 120,
      "location": "B203",
      "description": "开卷考试，可带教材",
      "created_at": "2025-05-03T16:33:52.000Z",
      "updated_at": "2025-05-03T16:33:52.000Z"
    }
  ]
}
```

### 4. 搜索考试

根据关键词搜索考试。

**请求方法**: `GET`

**URL**: `/exams/search`

**查询参数**:

| 参数名 | 类型   | 必填 | 描述     |
|--------|--------|------|----------|
| query  | String | 是   | 搜索关键词 |

**响应示例**:

```json
{
  "status": "success",
  "data": [
    {
      "id": 4,
      "course_id": 2,
      "course_name": "计算机网络",
      "title": "计算机网络期末考试",
      "exam_date": "2023-06-22T06:00:00.000Z",
      "duration": 150,
      "location": "B204",
      "description": "闭卷考试，只允许带计算器",
      "created_at": "2025-05-03T16:33:52.000Z",
      "updated_at": "2025-06-10T12:56:48.000Z"
    }
  ]
}
```

### 5. 创建考试

创建一个新的考试记录。

**请求方法**: `POST`

**URL**: `/exams`

**请求体**:

| 字段名      | 类型   | 必填 | 描述                     |
|-------------|--------|------|--------------------------|
| course_id   | Number | 是   | 关联的课程ID             |
| title       | String | 是   | 考试标题                 |
| exam_date   | String | 是   | 考试日期时间（YYYY-MM-DD HH:MM:SS格式）|
| duration    | Number | 是   | 考试时长（分钟）         |
| location    | String | 是   | 考试地点                 |
| description | String | 否   | 考试说明                 |

**请求示例**:

```json
{
  "course_id": 2,
  "title": "测试考试",
  "exam_date": "2023-12-31 10:00:00",
  "duration": 120,
  "location": "测试教室",
  "description": "这是一个测试考试描述"
}
```

**响应示例**:

```json
{
  "status": "success",
  "message": "考试创建成功",
  "data": {
    "id": 21,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "测试考试",
    "exam_date": "2023-12-31T02:00:00.000Z",
    "duration": 120,
    "location": "测试教室",
    "description": "这是一个测试考试描述",
    "created_at": "2025-06-10T13:16:06.000Z",
    "updated_at": "2025-06-10T13:16:06.000Z"
  }
}
```

### 6. 更新考试

更新现有的考试信息。

**请求方法**: `PUT`

**URL**: `/exams/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述   |
|--------|--------|------|--------|
| id     | Number | 是   | 考试ID |

**请求体**:

| 字段名      | 类型   | 必填 | 描述                    |
|-------------|--------|------|-------------------------|
| course_id   | Number | 否   | 关联的课程ID            |
| title       | String | 否   | 考试标题                |
| exam_date   | String | 否   | 考试日期时间（YYYY-MM-DD HH:MM:SS格式）|
| duration    | Number | 否   | 考试时长（分钟）        |
| location    | String | 否   | 考试地点                |
| description | String | 否   | 考试说明                |

**请求示例**:

```json
{
  "title": "更新后的考试标题",
  "location": "更新后的考试地点",
  "description": "这是更新后的考试描述"
}
```

**响应示例**:

```json
{
  "status": "success",
  "message": "考试更新成功",
  "data": {
    "id": 21,
    "course_id": 2,
    "course_name": "计算机网络",
    "title": "更新后的考试标题",
    "exam_date": "2023-12-31T02:00:00.000Z",
    "duration": 120,
    "location": "更新后的考试地点",
    "description": "这是更新后的考试描述",
    "created_at": "2025-06-10T13:16:06.000Z",
    "updated_at": "2025-06-10T13:35:00.000Z"
  }
}
```

### 7. 删除考试

删除指定的考试记录。

**请求方法**: `DELETE`

**URL**: `/exams/:id`

**路径参数**:

| 参数名 | 类型   | 必填 | 描述   |
|--------|--------|------|--------|
| id     | Number | 是   | 考试ID |

**响应示例**:

```json
{
  "status": "success",
  "message": "考试删除成功"
}
```

## 三、测试示例

### 作业API测试示例

```javascript
// 测试创建作业
async function testCreateAssignment() {
  try {
    const response = await fetch(`http://localhost:3000/api/assignments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        course_id: 2,
        title: "测试作业",
        description: "这是一个测试作业描述",
        due_date: "2023-12-31 23:59:59",
        max_score: 100
      })
    });
    
    const data = await response.json();
    console.log('作业创建响应:', data);
    return data.data;
  } catch (error) {
    console.error('作业创建失败:', error);
  }
}

// 测试获取作业
async function testGetAssignment(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/assignments/${id}`);
    const data = await response.json();
    console.log('作业详情响应:', data);
    return data.data;
  } catch (error) {
    console.error('获取作业失败:', error);
  }
}

// 测试更新作业
async function testUpdateAssignment(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/assignments/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: "更新后的作业标题",
        description: "这是更新后的作业描述"
      })
    });
    
    const data = await response.json();
    console.log('作业更新响应:', data);
    return data.data;
  } catch (error) {
    console.error('更新作业失败:', error);
  }
}

// 测试删除作业
async function testDeleteAssignment(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/assignments/${id}`, {
      method: 'DELETE'
    });
    
    const data = await response.json();
    console.log('作业删除响应:', data);
    return data;
  } catch (error) {
    console.error('删除作业失败:', error);
  }
}
```

### 考试API测试示例

```javascript
// 测试创建考试
async function testCreateExam() {
  try {
    const response = await fetch(`http://localhost:3000/api/exams`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        course_id: 2,
        title: "测试考试",
        exam_date: "2023-12-31 10:00:00",
        duration: 120,
        location: "测试教室",
        description: "这是一个测试考试描述"
      })
    });
    
    const data = await response.json();
    console.log('考试创建响应:', data);
    return data.data;
  } catch (error) {
    console.error('考试创建失败:', error);
  }
}

// 测试获取考试
async function testGetExam(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/exams/${id}`);
    const data = await response.json();
    console.log('考试详情响应:', data);
    return data.data;
  } catch (error) {
    console.error('获取考试失败:', error);
  }
}

// 测试更新考试
async function testUpdateExam(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/exams/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: "更新后的考试标题",
        location: "更新后的考试地点",
        description: "这是更新后的考试描述"
      })
    });
    
    const data = await response.json();
    console.log('考试更新响应:', data);
    return data.data;
  } catch (error) {
    console.error('更新考试失败:', error);
  }
}

// 测试删除考试
async function testDeleteExam(id) {
  try {
    const response = await fetch(`http://localhost:3000/api/exams/${id}`, {
      method: 'DELETE'
    });
    
    const data = await response.json();
    console.log('考试删除响应:', data);
    return data;
  } catch (error) {
    console.error('删除考试失败:', error);
  }
}
```

## 四、注意事项

1. **日期格式**：
   - 创建和更新作业/考试时，日期字段应使用 `YYYY-MM-DD HH:MM:SS` 格式
   - 例如：`2023-12-31 23:59:59`

2. **字段映射**：
   - 作业API中，请求时使用 `due_date` 和 `max_score` 字段
   - 响应中会返回 `deadline` 和 `total_score` 字段

3. **课程ID**：
   - 创建作业和考试时，必须提供有效的课程ID
   - 可以通过课程API获取可用的课程ID

4. **错误处理**：
   - 所有API在出错时会返回适当的HTTP状态码和错误信息
   - 请求参数验证失败时会返回400错误
   - 资源不存在时会返回404错误
   - 服务器内部错误会返回500错误 