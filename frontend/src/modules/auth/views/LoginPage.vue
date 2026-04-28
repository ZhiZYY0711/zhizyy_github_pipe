<script setup lang="ts">
import { reactive, ref } from 'vue'
import LoginAccessCard from '../../../components/login/LoginAccessCard.vue'
import LoginStory from '../../../components/login/LoginStory.vue'
import { loginAccessMeta, loginForm, loginStory } from '../../../data/mockShell'
import { useRouter } from '../../../router'
import { loginWithCredentials } from '../service'
import type { LoginErrorState, LoginFormModel } from '../types'

const router = useRouter()

const form = reactive<LoginFormModel>({
  username: '',
  password: '',
  code: '',
})

const errors = reactive<LoginErrorState>({
  username: '',
  password: '',
  code: '',
  form: '',
})

const isSubmitting = ref(false)
const captchaValue = ref(createCaptchaCode())

function createCaptchaCode() {
  const characters = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
  let nextCode = ''

  for (let index = 0; index < 6; index += 1) {
    nextCode += characters[Math.floor(Math.random() * characters.length)]
  }

  return nextCode
}

function refreshCaptcha() {
  captchaValue.value = createCaptchaCode()
  form.code = ''
  errors.code = ''
}

function updateForm(nextForm: LoginFormModel) {
  form.username = nextForm.username
  form.password = nextForm.password
  form.code = nextForm.code

  errors.username = ''
  errors.password = ''
  errors.code = ''
  errors.form = ''
}

function validateForm() {
  let isValid = true

  errors.username = ''
  errors.password = ''
  errors.code = ''
  errors.form = ''

  if (!form.username.trim()) {
    errors.username = '请输入账号。'
    isValid = false
  }

  if (!form.password) {
    errors.password = '请输入密码。'
    isValid = false
  }

  if (!form.code.trim()) {
    errors.code = '请输入验证码。'
    isValid = false
  } else if (form.code.trim().toUpperCase() !== captchaValue.value) {
    errors.code = '验证码不匹配，请重新输入。'
    isValid = false
  }

  return isValid
}

async function submitLogin() {
  if (isSubmitting.value || !validateForm()) {
    return
  }

  isSubmitting.value = true

  try {
    await loginWithCredentials(form)
    router.replace('/dashboard')
  } catch (error) {
    errors.form =
      error instanceof Error ? error.message : '登录失败，请稍后重试。'
    refreshCaptcha()
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="login-page-shell">
    <LoginStory
      :eyebrow="loginStory.eyebrow"
      :title="loginStory.title"
      :lead="loginStory.lead"
      :ring-label="loginStory.ringLabel"
      :ring-value="loginStory.ringValue"
      :ring-meta="loginStory.ringMeta"
      :meta-items="loginStory.metaItems"
      :tags="loginStory.tags"
    />

    <LoginAccessCard
      :form="loginForm"
      :model-value="form"
      :captcha-value="captchaValue"
      :meta-items="loginAccessMeta"
      :errors="errors"
      :is-submitting="isSubmitting"
      @update:model-value="updateForm"
      @refresh-captcha="refreshCaptcha"
      @submit="submitLogin"
    />
  </main>
</template>
