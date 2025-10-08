<template>
  <view class="login-container">
    <!-- 系统标题 -->
    <view class="system-title">
      <text class="title-text">油气管道监控系统</text>
      <view class="title-underline"></view>
    </view>
    
    <!-- 登录表单容器 -->
    <view class="login-form">
      <!-- 账号输入框 -->
      <view class="input-container">
        <input 
          class="input-field" 
          type="text" 
          placeholder="请输入账号"
          v-model="loginForm.username"
        />
      </view>
      
      <!-- 密码输入框 -->
      <view class="input-container password-container">
        <input 
          class="input-field" 
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入密码"
          v-model="loginForm.password"
        />
        <view class="eye-icon" @click="togglePassword">
          <text class="eye-text">{{ showPassword ? '👁️' : '👁️‍🗨️' }}</text>
        </view>
      </view>
      
      <!-- 验证码容器 -->
      <view class="captcha-container">
        <view class="captcha-input-container">
          <input 
            class="input-field captcha-input" 
            type="text" 
            placeholder="验证码"
            v-model="loginForm.captcha"
          />
        </view>
        <view class="captcha-image" @click="refreshCaptcha">
          <text class="captcha-text">{{ captchaCode }}</text>
        </view>
      </view>
      
      <!-- 忘记密码链接 -->
      <view class="forgot-password">
        <text class="forgot-text" @click="forgotPassword">忘记密码？</text>
      </view>
      
      <!-- 登录按钮 -->
      <view class="login-button" @click="handleLogin">
        <text class="login-button-text">登录</text>
      </view>
    </view>
    
    <!-- 装饰管道 -->
    <view class="decoration-pipe-horizontal"></view>
    <view class="decoration-pipe-vertical"></view>
  </view>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      showPassword: false,
      captchaCode: 'A8B9',
      loginForm: {
        username: '',
        password: '',
        captcha: ''
      }
    }
  },
  methods: {
    // 切换密码显示/隐藏
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    
    // 刷新验证码
    refreshCaptcha() {
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
      let result = '';
      for (let i = 0; i < 4; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      this.captchaCode = result;
    },
    
    // 忘记密码
    forgotPassword() {
      uni.showToast({
        title: '请联系管理员重置密码',
        icon: 'none'
      });
    },
    
    // 处理登录
    handleLogin() {
      if (!this.loginForm.username) {
        uni.showToast({
          title: '请输入账号',
          icon: 'none'
        });
        return;
      }
      
      if (!this.loginForm.password) {
        uni.showToast({
          title: '请输入密码',
          icon: 'none'
        });
        return;
      }
      
      if (!this.loginForm.captcha) {
        uni.showToast({
          title: '请输入验证码',
          icon: 'none'
        });
        return;
      }
      
      if (this.loginForm.captcha.toUpperCase() !== this.captchaCode) {
        uni.showToast({
          title: '验证码错误',
          icon: 'none'
        });
        this.refreshCaptcha();
        return;
      }
      
      // 模拟登录成功
      uni.showToast({
        title: '登录成功',
        icon: 'success'
      });
      
      // 跳转到首页
      setTimeout(() => {
        uni.switchTab({
          url: '/pages/index/index'
        });
      }, 1500);
    }
  },
  
  onLoad() {
    // 页面加载时生成验证码
    this.refreshCaptcha();
  }
}
</script>

<style scoped>
.login-container {
  width: 375px;
  height: 812px;
  background-color: #FFFFFF;
  position: relative;
  margin: 0 auto;
}

/* 系统标题 */
.system-title {
  position: absolute;
  left: 24px;
  top: 40px;
}

.title-text {
  font-family: Inter, sans-serif;
  font-weight: 700;
  font-size: 28px;
  line-height: 1.21;
  color: #1A3366;
}

.title-underline {
  width: 140px;
  height: 3px;
  background-color: rgba(26, 77, 153, 0.15);
  border-radius: 1.5px;
  margin-top: 4px;
}

/* 登录表单容器 */
.login-form {
  position: absolute;
  left: 24px;
  top: 120px;
  width: 327px;
  height: 400px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  box-sizing: border-box;
}

/* 输入框容器 */
.input-container {
  width: 279px;
  height: 48px;
  background-color: #F5F5F5;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
}

.input-field {
  flex: 1;
  border: none;
  background: transparent;
  font-family: Inter, sans-serif;
  font-weight: 400;
  font-size: 16px;
  color: #333;
  outline: none;
}

.input-field::placeholder {
  color: #999999;
}

/* 密码输入框 */
.password-container {
  justify-content: space-between;
}

.eye-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.eye-text {
  font-size: 16px;
  color: #999999;
}

/* 验证码容器 */
.captcha-container {
  width: 279px;
  height: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.captcha-input-container {
  width: 180px;
  height: 48px;
  background-color: #F5F5F5;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
}

.captcha-input {
  width: 100%;
  border: none;
  background: transparent;
  font-family: Inter, sans-serif;
  font-weight: 400;
  font-size: 16px;
  color: #333;
  outline: none;
}

.captcha-image {
  width: 87px;
  height: 48px;
  background-color: #E6E6E6;
  border: 1px solid #D9D9D9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.captcha-text {
  font-family: Inter, sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #4D4D4D;
}

/* 忘记密码链接 */
.forgot-password {
  width: 279px;
  display: flex;
  justify-content: flex-end;
}

.forgot-text {
  font-family: Inter, sans-serif;
  font-weight: 400;
  font-size: 14px;
  color: #3366CC;
  cursor: pointer;
}

/* 登录按钮 */
.login-button {
  width: 279px;
  height: 48px;
  background-color: #1A4D99;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.login-button:active {
  background-color: #164080;
}

.login-button-text {
  font-family: Inter, sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #FFFFFF;
}

/* 装饰管道 */
.decoration-pipe-horizontal {
  position: absolute;
  left: 241px;
  top: 776px;
  width: 110px;
  height: 6px;
  background-color: rgba(38, 89, 178, 0.12);
  border: 1px solid rgba(38, 89, 178, 0.2);
  border-radius: 3px;
}

.decoration-pipe-vertical {
  position: absolute;
  left: 357px;
  top: 720px;
  width: 6px;
  height: 50px;
  background-color: rgba(38, 89, 178, 0.12);
  border: 1px solid rgba(38, 89, 178, 0.2);
  border-radius: 3px;
}
</style>