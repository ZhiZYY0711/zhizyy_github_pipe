# 认证模块 (Auth Module)

## 模块概述

认证模块负责用户身份验证、登录状态管理和权限控制，是系统安全的核心模块。

## 目录结构

```
auth/
├── api/
│   └── index.js          # 认证相关API接口
├── store/
│   └── index.js          # 认证状态管理
└── views/
    └── LoginPage.vue     # 登录页面组件
```

## 功能特性

### 🔐 用户认证
- **登录验证**: 用户名/密码登录
- **JWT Token**: 基于JWT的身份认证
- **自动登录**: 记住登录状态
- **登录过期**: 自动检测token过期并重新登录

### 🛡️ 安全机制
- **密码加密**: 前端密码加密传输
- **Token刷新**: 自动刷新过期token
- **登录拦截**: 未登录用户自动跳转登录页
- **权限验证**: 基于角色的权限控制

## API接口

### 登录接口
```javascript
// 用户登录
login(credentials)
// 参数: { username: string, password: string }
// 返回: { token: string, userInfo: object }

// 获取用户信息
getUserInfo()
// 返回: { id, username, role, permissions }

// 用户登出
logout()
// 清除本地token和用户信息
```

## 状态管理

### State
```javascript
{
  token: null,           // JWT token
  userInfo: null,        // 用户信息
  isLoggedIn: false,     // 登录状态
  loginLoading: false    // 登录加载状态
}
```

### Mutations
- `SET_TOKEN`: 设置token
- `SET_USER_INFO`: 设置用户信息
- `SET_LOGIN_STATUS`: 设置登录状态
- `SET_LOGIN_LOADING`: 设置登录加载状态
- `CLEAR_AUTH`: 清除认证信息

### Actions
- `login`: 用户登录
- `logout`: 用户登出
- `getUserInfo`: 获取用户信息
- `checkAuth`: 检查认证状态

## 组件说明

### LoginPage.vue
登录页面组件，包含：
- 用户名/密码输入表单
- 登录按钮和加载状态
- 错误信息显示
- 记住密码功能

## 使用示例

### 在组件中使用认证状态
```javascript
import { mapState, mapActions } from 'vuex'

export default {
  computed: {
    ...mapState('auth', ['isLoggedIn', 'userInfo'])
  },
  methods: {
    ...mapActions('auth', ['login', 'logout']),
    
    async handleLogin() {
      try {
        await this.login({
          username: this.username,
          password: this.password
        })
        this.$router.push('/dashboard')
      } catch (error) {
        this.$toast.error('登录失败')
      }
    }
  }
}
```

### 路由守卫
```javascript
// 在router/index.js中使用
router.beforeEach((to, from, next) => {
  const isLoggedIn = store.getters['auth/isLoggedIn']
  
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})
```

## 配置说明

### Token存储
- 使用localStorage存储JWT token
- 自动在axios请求头中添加Authorization
- 响应拦截器处理token过期

### 路由配置
- 登录页路由: `/login`
- 登录成功后默认跳转: `/dashboard`
- 需要认证的路由添加`meta.requiresAuth: true`