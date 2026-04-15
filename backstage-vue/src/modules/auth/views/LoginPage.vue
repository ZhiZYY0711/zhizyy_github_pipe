<template>
  <div class="login-page">
    <div class="bg-layer bg-grid"></div>
    <div class="bg-layer bg-orbs"></div>
    <div class="bg-layer bg-streak"></div>
    <div class="bg-layer bg-particles"></div>

    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-top-line"></div>
        <p class="brand-tag">PIPELINE SECURITY PLATFORM</p>
        <h1 class="brand-title">油气管道监控系统</h1>
        <p class="brand-subtitle">智能监测 · 实时预警 · 安全运行</p>

        <div class="network-map" aria-hidden="true">
          <span class="node node-1"></span>
          <span class="node node-2"></span>
          <span class="node node-3"></span>
          <span class="node node-4"></span>
          <span class="line line-1"></span>
          <span class="line line-2"></span>
          <span class="line line-3"></span>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-card">
          <h2 class="card-title">欢迎登录</h2>
          <p class="card-subtitle">管网状态实时掌控，立即进入指挥中心</p>

          <form @submit.prevent="handleLogin">
            <div class="form-group">
              <div class="field-wrap" :class="{ filled: !!loginForm.username }">
                <span class="field-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
                    <path d="M4 20a8 8 0 1 1 16 0" />
                  </svg>
                </span>
                <input
                  id="username"
                  type="text"
                  v-model.trim="loginForm.username"
                  placeholder=" "
                  autocomplete="username"
                  required
                />
                <label for="username">账号（6-12位数字）</label>
              </div>
              <div class="error-message" v-if="errors.username">
                {{ errors.username }}
              </div>
            </div>

            <div class="form-group">
              <div class="field-wrap" :class="{ filled: !!loginForm.password }">
                <span class="field-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <rect x="5" y="10" width="14" height="10" rx="2" />
                    <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                  </svg>
                </span>
                <input
                  id="password"
                  :type="showPassword ? 'text' : 'password'"
                  v-model.trim="loginForm.password"
                  placeholder=" "
                  autocomplete="current-password"
                  required
                />
                <label for="password">密码（含大小写与数字）</label>
                <button
                  type="button"
                  class="password-toggle"
                  @click="togglePassword"
                >
                  <svg viewBox="0 0 24 24" fill="none" v-if="!showPassword">
                    <path
                      d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"
                    />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  <svg viewBox="0 0 24 24" fill="none" v-else>
                    <path d="m3 3 18 18" />
                    <path d="M10.6 10.6A3 3 0 0 0 13.4 13.4" />
                    <path
                      d="M9.4 5.3A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a17.7 17.7 0 0 1-3 3.8"
                    />
                    <path
                      d="M6.3 6.3C3.7 8 2 12 2 12s3.5 6 10 6c1.6 0 3-.3 4.2-.8"
                    />
                  </svg>
                </button>
              </div>
              <div class="error-message" v-if="errors.password">
                {{ errors.password }}
              </div>
            </div>

            <div class="form-group">
              <div class="captcha-row">
                <div class="field-wrap" :class="{ filled: !!loginForm.code }">
                  <span class="field-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none">
                      <rect x="3" y="5" width="18" height="14" rx="2" />
                      <path d="m7 12 3 3 7-7" />
                    </svg>
                  </span>
                  <input
                    id="captcha"
                    type="text"
                    v-model.trim="loginForm.code"
                    placeholder=" "
                    maxlength="6"
                    required
                  />
                  <label for="captcha">验证码</label>
                </div>

                <button
                  type="button"
                  class="captcha-card"
                  @click="refreshCaptcha"
                  title="点击刷新验证码"
                >
                  <span class="captcha-code">{{ captchaCode }}</span>
                  <span class="captcha-tip">点击刷新</span>
                </button>
              </div>
              <div class="error-message" v-if="errors.code">
                {{ errors.code }}
              </div>
            </div>

            <div class="forgot-password">
              <a href="javascript:void(0)">忘记密码？联系管理员</a>
            </div>

            <button type="submit" class="login-button" :disabled="isLoading">
              {{ isLoading ? "登录中..." : "登 录" }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import CryptoJS from "crypto-js";

export default {
  name: "LoginPage",
  data() {
    return {
      loginForm: {
        username: "",
        password: "",
        code: "",
      },
      captchaCode: "",
      showPassword: false,
      isLoading: false,
      errors: {
        username: "",
        password: "",
        code: "",
      },
    };
  },
  created() {
    this.refreshCaptcha();
  },
  methods: {
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    createCaptchaCode() {
      const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
      let code = "";
      for (let i = 0; i < 6; i += 1) {
        code += chars[Math.floor(Math.random() * chars.length)];
      }
      return code;
    },
    refreshCaptcha() {
      this.captchaCode = this.createCaptchaCode();
    },
    validateForm() {
      let isValid = true;
      this.errors = {
        username: "",
        password: "",
        code: "",
      };

      // 验证账号：6-12位纯数字
      if (!/^\d{6,12}$/.test(this.loginForm.username)) {
        this.errors.username = "账号必须为6-12位纯数字";
        isValid = false;
      }

      // 验证密码：9-24位，包含数字、大写和小写字母
      if (
        !/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{9,24}$/.test(this.loginForm.password)
      ) {
        this.errors.password = "密码必须为9-24位，包含数字、大写和小写字母";
        isValid = false;
      }

      // 验证验证码：6位字母/数字且与前端验证码一致
      if (!/^[A-Za-z0-9]{6}$/.test(this.loginForm.code)) {
        this.errors.code = "验证码必须为6位字母或数字";
        isValid = false;
      } else if (this.loginForm.code.toUpperCase() !== this.captchaCode) {
        this.errors.code = "验证码错误，请重试";
        isValid = false;
      }

      return isValid;
    },
    encryptPassword(password) {
      // 使用MD5加密密码
      return CryptoJS.MD5(password).toString();
    },
    async handleLogin() {
      if (!this.validateForm()) {
        return;
      }

      this.isLoading = true;

      try {
        // 准备登录数据
        const loginData = {
          username: this.loginForm.username,
          password: this.encryptPassword(this.loginForm.password),
          code: this.loginForm.code,
        };

        // 调用登录API
        const response = await this.$http.post("/manager/login", loginData);

        if (response.code === 200 && response.data) {
          // 登录成功
          const { username, jwt } = response.data;

          // 保存登录状态和token
          localStorage.setItem("isLoggedIn", "true");
          localStorage.setItem("username", username);
          localStorage.setItem("token", jwt);
          localStorage.setItem("jwt", jwt);

          // 设置axios默认header
          this.$http.defaults.headers.common["Authorization"] = `Bearer ${jwt}`;

          // 跳转到数据可视化页面
          this.$router.push("/main/visualization");

          this.$message.success("登录成功");
        } else {
          // 登录失败
          this.$message.error(response.message || "登录失败");
          this.refreshCaptcha();
        }
      } catch (error) {
        console.error("登录失败", error);
        if (
          error.response &&
          error.response.data &&
          error.response.data.message
        ) {
          this.$message.error(error.response.data.message);
        } else {
          this.$message.error("网络错误，请稍后重试");
        }
        this.refreshCaptcha();
      } finally {
        this.isLoading = false;
      }
    },
  },
};
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(20px, 4vw, 56px);
  background: radial-gradient(
    circle at 22% 20%,
    #1b3a6f 0%,
    #0a2540 40%,
    #060b14 88%
  );
  color: #ffffff;
}

.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  background-image: linear-gradient(
      rgba(175, 203, 255, 0.08) 1px,
      transparent 1px
    ),
    linear-gradient(90deg, rgba(175, 203, 255, 0.08) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 95%);
}

.bg-orbs {
  background: radial-gradient(
      circle at 85% 8%,
      rgba(63, 167, 255, 0.22),
      transparent 32%
    ),
    radial-gradient(
      circle at 12% 78%,
      rgba(175, 203, 255, 0.16),
      transparent 30%
    );
  animation: floatOrbs 12s ease-in-out infinite alternate;
}

.bg-streak::before {
  content: "";
  position: absolute;
  top: -30%;
  left: -30%;
  width: 160%;
  height: 160%;
  background: linear-gradient(
    115deg,
    transparent 46%,
    rgba(63, 167, 255, 0.06) 49%,
    rgba(63, 167, 255, 0.18) 51%,
    transparent 54%
  );
  transform: rotate(4deg);
  animation: streakFlow 10s linear infinite;
}

.bg-particles::before,
.bg-particles::after {
  content: "";
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(175, 203, 255, 0.8);
  box-shadow: 14vw 10vh rgba(175, 203, 255, 0.55),
    24vw 38vh rgba(63, 167, 255, 0.65), 38vw 18vh rgba(175, 203, 255, 0.4),
    52vw 46vh rgba(63, 167, 255, 0.52), 65vw 22vh rgba(175, 203, 255, 0.4),
    78vw 52vh rgba(63, 167, 255, 0.6);
  animation: particlesPulse 5.5s ease-in-out infinite;
}

.bg-particles::after {
  left: 20%;
  top: 22%;
  animation-delay: 2.3s;
}

.login-shell {
  width: min(1220px, 100%);
  min-height: min(760px, 100%);
  position: relative;
  z-index: 2;
  display: flex;
  align-items: stretch;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(175, 203, 255, 0.12);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.03),
    rgba(255, 255, 255, 0.01)
  );
}

.brand-panel {
  flex: 0 0 60%;
  padding: clamp(40px, 6vw, 84px);
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-top-line {
  width: 68px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(63, 167, 255, 0.95),
    rgba(175, 203, 255, 0.35)
  );
  margin-bottom: 20px;
}

.brand-tag {
  color: #afcbff;
  letter-spacing: 3px;
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 18px;
}

.brand-title {
  font-size: clamp(38px, 4.2vw, 62px);
  font-weight: 700;
  line-height: 1.15;
  margin-bottom: 16px;
  text-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
}

.brand-subtitle {
  color: rgba(229, 238, 255, 0.86);
  font-size: clamp(16px, 1.6vw, 22px);
  letter-spacing: 1px;
}

.network-map {
  position: absolute;
  inset: 16% 14% 18% 12%;
  opacity: 0.22;
}

.network-map .node {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3fa7ff;
  box-shadow: 0 0 16px #3fa7ff;
}

.node-1 {
  top: 16%;
  left: 18%;
}

.node-2 {
  top: 48%;
  left: 40%;
}

.node-3 {
  top: 26%;
  right: 22%;
}

.node-4 {
  bottom: 18%;
  right: 10%;
}

.network-map .line {
  position: absolute;
  height: 2px;
  background: linear-gradient(
    90deg,
    rgba(63, 167, 255, 0.05),
    rgba(63, 167, 255, 0.9),
    rgba(63, 167, 255, 0.05)
  );
  transform-origin: left center;
}

.line-1 {
  width: 31%;
  top: 18%;
  left: 20%;
  transform: rotate(31deg);
}

.line-2 {
  width: 28%;
  top: 50%;
  left: 41%;
  transform: rotate(-28deg);
}

.line-3 {
  width: 38%;
  bottom: 20%;
  left: 43%;
  transform: rotate(5deg);
}

.login-panel {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(20px, 3vw, 40px);
}

.login-card {
  width: min(430px, 100%);
  padding: clamp(30px, 3vw, 40px);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(175, 203, 255, 0.28);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.34);
}

.card-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 10px;
}

.card-subtitle {
  color: rgba(231, 239, 255, 0.72);
  font-size: 14px;
  margin-bottom: 26px;
}

.form-group {
  margin-bottom: 16px;
}

.field-wrap {
  position: relative;
}

.field-wrap input {
  width: 100%;
  height: 54px;
  border: 1px solid rgba(175, 203, 255, 0.28);
  border-radius: 12px;
  padding: 16px 44px 8px 44px;
  font-size: 15px;
  color: #ffffff;
  background: rgba(7, 20, 37, 0.62);
  transition: border-color 0.25s ease, box-shadow 0.25s ease,
    background-color 0.25s ease;
}

.field-wrap input:focus {
  outline: none;
  border-color: #3fa7ff;
  box-shadow: 0 0 0 3px rgba(63, 167, 255, 0.2),
    0 0 16px rgba(63, 167, 255, 0.25);
  background: rgba(7, 20, 37, 0.8);
}

.field-wrap label {
  position: absolute;
  left: 44px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(216, 228, 255, 0.66);
  font-size: 14px;
  pointer-events: none;
  transition: all 0.2s ease;
}

.field-wrap:not(.filled) input:focus + label {
  color: #3fa7ff;
}

.field-wrap.filled label {
  opacity: 0;
}

.field-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: rgba(175, 203, 255, 0.92);
}

.field-icon svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  color: rgba(175, 203, 255, 0.88);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.password-toggle svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-row .field-wrap {
  flex: 1;
}

.captcha-card {
  width: 124px;
  height: 54px;
  border-radius: 12px;
  border: 1px solid rgba(63, 167, 255, 0.5);
  background: linear-gradient(
    145deg,
    rgba(63, 167, 255, 0.22),
    rgba(27, 58, 111, 0.2)
  );
  color: #ffffff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.captcha-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(16, 41, 72, 0.45);
}

.captcha-code {
  font-size: 18px;
  line-height: 1;
  letter-spacing: 2px;
  font-weight: 700;
}

.captcha-tip {
  font-size: 11px;
  color: rgba(227, 237, 255, 0.76);
  margin-top: 4px;
}

.forgot-password {
  text-align: right;
  margin-bottom: 18px;
}

.forgot-password a {
  color: #afcbff;
  text-decoration: none;
  font-size: 13px;
}

.forgot-password a:hover {
  color: #3fa7ff;
}

.login-button {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 12px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #1b3a6f, #3fa7ff);
  transition: transform 0.2s ease, filter 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 12px 26px rgba(23, 74, 130, 0.45);
}

.login-button:hover:not(:disabled) {
  filter: brightness(1.08);
  transform: scale(1.015);
}

.login-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.error-message {
  color: #ff9ca9;
  font-size: 12px;
  margin-top: 6px;
  padding-left: 2px;
}

@keyframes floatOrbs {
  from {
    transform: translateY(-8px);
  }
  to {
    transform: translateY(10px);
  }
}

@keyframes streakFlow {
  from {
    transform: translateX(-12%) rotate(4deg);
  }
  to {
    transform: translateX(12%) rotate(4deg);
  }
}

@keyframes particlesPulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 0.9;
  }
}

@media (max-width: 1024px) {
  .login-shell {
    flex-direction: column;
  }

  .brand-panel,
  .login-panel {
    flex: none;
  }

  .brand-panel {
    min-height: 320px;
  }

  .brand-title {
    font-size: clamp(30px, 7vw, 42px);
  }
}

@media (max-width: 560px) {
  .login-page {
    padding: 14px;
  }

  .login-card {
    padding: 24px 18px;
  }

  .captcha-row {
    flex-direction: column;
  }

  .captcha-card {
    width: 100%;
  }
}
</style>