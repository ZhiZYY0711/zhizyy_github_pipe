import { postLogin } from './api'
import { md5 } from './md5'
import { persistAuthSession } from './storage'
import type { AuthSession, LoginFormModel, LoginResponse } from './types'

function isLoginSuccess(response: LoginResponse) {
  return response.code === 0 || response.code === 200
}

export async function loginWithCredentials(form: LoginFormModel) {
  const response = await postLogin({
    username: form.username.trim(),
    password: md5(form.password),
    code: form.code.trim(),
  })

  if (!isLoginSuccess(response)) {
    throw new Error(response.message || 'Login failed.')
  }

  const token = response.data?.jwt || response.data?.token

  if (!token) {
    throw new Error('Login response did not include a token.')
  }

  const session: AuthSession = {
    token,
    jwt: response.data?.jwt || token,
    username: response.data?.username || form.username.trim(),
    isLoggedIn: 'true',
  }

  persistAuthSession(session)

  return session
}
