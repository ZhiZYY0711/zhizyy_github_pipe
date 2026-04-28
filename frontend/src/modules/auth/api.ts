import { apiRequest } from '../shared/apiClient'
import type { LoginRequest, LoginResponse } from './types'

export function postLogin(payload: LoginRequest) {
  return apiRequest<LoginResponse>('/login', {
    method: 'POST',
    body: payload,
    requiresAuth: false,
  })
}
