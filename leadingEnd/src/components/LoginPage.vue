<template>
  <div class="login-container">
    <div class="background">
      <div class="system-title">
        <h1>油气管道监控系统</h1>
      </div>
    </div>
    
    <div class="login-box">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>管理员类型</label>
          <select v-model="loginForm.adminType" required>
            <option value="global">全局管理员</option>
            <option value="regional">区域管理员</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>账号</label>
          <input 
            type="text" 
            v-model="loginForm.username" 
            placeholder="请输入账号" 
            required
          />
          <div class="error-message" v-if="errors.username">{{ errors.username }}</div>
        </div>
        
        <div class="form-group">
          <label>密码</label>
          <div class="password-input">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="loginForm.password" 
              placeholder="请输入密码" 
              required
            />
            <span class="password-toggle" @click="togglePassword">
              <i :class="showPassword ? 'eye-open' : 'eye-close'">👁️</i>
            </span>
          </div>
          <div class="error-message" v-if="errors.password">{{ errors.password }}</div>
        </div>
        
        <div class="form-group">
          <label>验证码</label>
          <div class="captcha-container">
            <input 
              type="text" 
              v-model="loginForm.captcha" 
              placeholder="请输入验证码" 
              required
            />
            <div class="captcha-image">验证码图片</div>
          </div>
        </div>
        
        <div class="forgot-password">
          <a href="#">忘记密码点我</a>
        </div>
        
        <button type="submit" class="login-button">登录</button>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginPage',
  data() {
    return {
      loginForm: {
        adminType: 'global',
        username: '',
        password: '',
        captcha: ''
      },
      showPassword: false,
      errors: {
        username: '',
        password: ''
      }
    }
  },
  methods: {
    togglePassword() {
      this.showPassword = !this.showPassword
    },
    validateForm() {
      let isValid = true
      this.errors = {
        username: '',
        password: ''
      }
      
      // 验证账号：6-12位纯数字
      if (!/^\d{6,12}$/.test(this.loginForm.username)) {
        this.errors.username = '账号必须为6-12位纯数字'
        isValid = false
      }
      
      // 验证密码：9-24位，包含数字、大写和小写字母
      if (!/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{9,24}$/.test(this.loginForm.password)) {
        this.errors.password = '密码必须为9-24位，包含数字、大写和小写字母'
        isValid = false
      }
      
      return isValid
    },
    handleLogin() {
      if (this.validateForm()) {
        // 使用Vuex的action进行登录
        this.$store.dispatch('login', this.loginForm)
          .then(() => {
            // 登录成功后，将登录状态保存到localStorage
            localStorage.setItem('isLoggedIn', 'true')
            localStorage.setItem('adminType', this.loginForm.adminType)
            
            // 跳转到数据可视化页面
            this.$router.push('/main/visualization')
          })
          .catch(error => {
            console.error('登录失败', error)
          })
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
}

.background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #0a2a5e;
  background-image: linear-gradient(135deg, #0a2a5e 0%, #0d3b7c 50%, #0a2a5e 100%);
  z-index: 0;
}

.system-title {
  position: absolute;
  top: 50%;
  left: 15%;
  transform: translateY(-50%);
  color: white;
  z-index: 1;
}

.system-title h1 {
  font-size: 3rem;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.login-box {
  position: absolute;
  top: 50%;
  right: 15%;
  transform: translateY(-50%);
  width: 400px;
  padding: 30px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  z-index: 2;
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #0a2a5e;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #0a2a5e;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 16px;
}

.password-input {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
}

.captcha-container {
  display: flex;
  gap: 10px;
}

.captcha-container input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.forgot-password {
  text-align: right;
  margin-bottom: 20px;
}

.forgot-password a {
  color: #0a2a5e;
  text-decoration: none;
}

.forgot-password a:hover {
  text-decoration: underline;
}

.login-button {
  width: 100%;
  padding: 12px;
  background-color: #0a2a5e;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.login-button:hover {
  background-color: #0d3b7c;
}

.error-message {
  color: red;
  font-size: 12px;
  margin-top: 5px;
}

/* 背景动画效果 */
@keyframes backgroundAnimation {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.background {
  background-size: 400% 400%;
  animation: backgroundAnimation 15s ease infinite;
}
</style>