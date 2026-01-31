# Admin 用户管理功能实现说明

## 功能概述

为 admin 账号添加了完整的用户管理功能，admin 登录后将直接跳转到用户管理页面，可以管理系统中的所有用户。

## 功能特性

### ✅ 已实现功能

1. **角色分离登录**
   - 管理员（admin）登录 → 跳转到用户管理页面 `/user-management`
   - 普通用户（user/guest）登录 → 跳转到数据大屏 `/`

2. **用户管理功能**
   - ➕ 添加用户
   - ✏️ 编辑用户信息
   - 🔑 重置密码
   - 🚫/✅ 启用/禁用用户
   - 🗑️ 删除用户（逻辑删除）
   - 🔍 搜索用户

3. **权限控制**
   - 仅管理员可访问用户管理页面
   - 非管理员访问自动重定向到首页
   - 路由守卫自动验证权限

## 文件清单

### 前端文件

| 文件 | 说明 |
|------|------|
| `UserManagement.vue` | 用户管理页面组件 |
| `router/index.js` | 添加路由配置和权限守卫 |
| `Login.vue` | 修改登录跳转逻辑 |

### 后端文件

| 文件 | 说明 |
|------|------|
| `UserManagementController.java` | 用户管理API控制器 |
| `UserService.java` | 添加 getUserList 接口 |
| `UserServiceImpl.java` | 实现 getUserList 方法 |
| `SysUserMapper.java` | 添加数据库查询方法 |

## API 接口

### 基础路径
```
http://10.100.111.2:8675/api/users
```

### 接口列表

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/list` | 获取用户列表 | keyword（可选） |
| POST | `/create` | 创建用户 | SysUser 对象 |
| PUT | `/update` | 更新用户 | SysUser 对象 |
| DELETE | `/delete/{userId}` | 删除用户 | userId |
| POST | `/reset-password` | 重置密码 | userId, newPassword |
| GET | `/{userId}` | 获取用户详情 | userId |

### 请求示例

#### 1. 获取用户列表
```javascript
GET /api/users/list?keyword=admin

Response:
{
  "code": "0",
  "msg": "成功",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "role": "admin",
      "status": 1,
      "lastLoginTime": "2024-12-06 16:30:00",
      "loginCount": 10,
      "createTime": "2024-12-06 10:00:00",
      "remark": "超级管理员"
    }
  ]
}
```

#### 2. 创建用户
```javascript
POST /api/users/create

Request Body:
{
  "username": "newuser",
  "password": "123456",
  "role": "user",
  "status": 1,
  "remark": "新用户"
}

Response:
{
  "code": "0",
  "msg": "创建成功",
  "data": null
}
```

#### 3. 重置密码
```javascript
POST /api/users/reset-password

Request Body:
{
  "userId": 2,
  "newPassword": "newpassword123"
}

Response:
{
  "code": "0",
  "msg": "密码重置成功",
  "data": null
}
```

## 路由配置

### 用户管理路由
```javascript
{
  path: '/user-management',
  name: 'user-management',
  component: () => import('../views/UserManagement.vue'),
  meta: { 
    title: '用户管理',
    requiresAuth: true,
    requiresAdmin: true  // 仅管理员可访问
  }
}
```

### 权限守卫逻辑

```javascript
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)

  if (requiresAuth) {
    if (isLoggedIn()) {
      if (requiresAdmin) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        if (userInfo.role === 'admin') {
          next()  // 管理员通过
        } else {
          next({ path: '/' })  // 非管理员重定向
        }
      } else {
        next()
      }
    } else {
      next({ path: '/login' })
    }
  } else {
    next()
  }
})
```

## 使用流程

### 管理员登录

1. **访问登录页面**
   ```
   http://localhost:8080/login
   ```

2. **输入管理员凭据**
   - 用户名：`admin`
   - 密码：`123456`
   - 验证码：根据图片输入

3. **自动跳转**
   - 登录成功后自动跳转到 `/user-management`
   - 显示用户管理界面

### 用户管理操作

#### 添加用户
1. 点击"➕ 添加用户"按钮
2. 填写用户信息：
   - 用户名（必填）
   - 密码（必填，至少6位）
   - 角色（admin/user/guest）
   - 状态（启用/禁用）
   - 备注（可选）
3. 点击"确定"创建

#### 编辑用户
1. 点击用户行的"✏️"按钮
2. 修改用户信息（用户名不可修改）
3. 点击"确定"保存

#### 重置密码
1. 点击用户行的"🔑"按钮
2. 输入新密码（至少6位）
3. 确认密码
4. 点击"确定"重置

#### 启用/禁用用户
1. 点击用户行的"🚫"或"✅"按钮
2. 确认操作
3. 状态立即生效

#### 删除用户
1. 点击用户行的"🗑️"按钮
2. 确认删除（不可恢复）
3. 用户被逻辑删除（is_deleted = 1）

#### 搜索用户
1. 在搜索框输入关键字
2. 点击"🔍 搜索"或按回车
3. 显示匹配的用户列表

## 角色权限说明

### 角色类型

| 角色 | 代码 | 权限 | 说明 |
|------|------|------|------|
| 管理员 | admin | * | 拥有所有权限，包括用户管理 |
| 普通用户 | user | read, write | 可读写数据，无法管理用户 |
| 访客 | guest | read | 只读权限 |

### 权限矩阵

| 功能 | admin | user | guest |
|------|-------|------|-------|
| 用户管理 | ✅ | ❌ | ❌ |
| 数据大屏 | ✅ | ✅ | ✅ |
| 修改自己密码 | ✅ | ✅ | ✅ |
| 重置他人密码 | ✅ | ❌ | ❌ |

## 安全特性

### 1. 密码加密
- 使用 BCrypt 算法加密存储
- 每次加密生成不同的哈希值
- 不可逆加密，无法解密查看原密码

### 2. 权限验证
- 前端路由守卫验证
- 后端接口权限检查
- Token 验证

### 3. 逻辑删除
- 删除用户不物理删除数据
- 设置 is_deleted = 1
- 可在数据库中恢复

### 4. 操作日志
- 所有操作记录在后端日志
- 包含时间、用户、操作类型

## 数据结构

### SysUser 实体

```java
public class SysUser {
    private Long id;              // 用户ID
    private String username;      // 用户名
    private String password;      // 密码（BCrypt加密）
    private String role;          // 角色
    private Integer status;       // 状态：0-禁用，1-启用
    private LocalDateTime lastLoginTime;  // 最后登录时间
    private String lastLoginIp;   // 最后登录IP
    private Integer loginCount;   // 登录次数
    private LocalDateTime createTime;     // 创建时间
    private LocalDateTime updateTime;     // 更新时间
    private Integer isDeleted;    // 是否删除：0-否，1-是
    private String remark;        // 备注
}
```

## 测试指南

### 1. 功能测试

#### 测试登录跳转
```bash
1. 使用 admin/123456 登录
   期望：跳转到 /user-management

2. 使用 user/123456 登录（需先创建user账号）
   期望：跳转到 /

3. 使用 guest/123456 登录（需先创建guest账号）
   期望：跳转到 /
```

#### 测试权限控制
```bash
1. 以 user 身份登录
2. 访问 http://localhost:8080/user-management
   期望：自动重定向到 /
```

#### 测试用户CRUD
```bash
1. 添加用户：创建一个新用户
2. 编辑用户：修改用户信息
3. 重置密码：为用户重置密码
4. 启用/禁用：切换用户状态
5. 删除用户：删除测试用户
```

### 2. API测试

使用 Postman 或 curl 测试：

```bash
# 获取用户列表
curl -X GET "http://10.100.111.2:8675/api/users/list" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 创建用户
curl -X POST "http://10.100.111.2:8675/api/users/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "role": "user",
    "status": 1
  }'

# 重置密码
curl -X POST "http://10.100.111.2:8675/api/users/reset-password" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": 2,
    "newPassword": "newpass123"
  }'
```

## 故障排查

### 常见问题

#### 1. 登录后未跳转到用户管理页面
**原因**：用户角色不是 admin
**解决**：检查数据库 sys_user 表中的 role 字段是否为 'admin'

#### 2. 用户管理页面显示空白
**原因**：后端服务未启动或端口不正确
**解决**：确保后端服务在 8675 端口运行

#### 3. 创建用户失败
**原因**：密码未加密或参数验证失败
**解决**：检查前端传递的参数格式，确保密码至少6位

#### 4. 权限验证失败
**原因**：Token 过期或无效
**解决**：重新登录获取新 Token

## 后续优化建议

### 1. 功能增强
- [ ] 批量操作（批量启用/禁用/删除）
- [ ] 导出用户列表（Excel/CSV）
- [ ] 用户活动日志查看
- [ ] 密码强度要求配置
- [ ] 密码过期策略

### 2. UI优化
- [ ] 分页功能
- [ ] 高级筛选（按角色、状态）
- [ ] 排序功能（按创建时间、登录次数）
- [ ] 用户头像上传

### 3. 安全增强
- [ ] 两步验证（2FA）
- [ ] 登录IP白名单
- [ ] 登录失败次数限制
- [ ] 密码找回功能
- [ ] 审计日志

## 总结

通过以上实现，完成了：

✅ admin 账号登录后跳转到用户管理页面
✅ 完整的用户 CRUD 功能
✅ 基于角色的权限控制
✅ 前后端分离架构
✅ 友好的用户界面

系统现在支持管理员通过图形界面管理所有用户，无需直接操作数据库。
