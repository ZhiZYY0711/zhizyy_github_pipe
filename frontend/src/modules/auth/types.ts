export type LoginFormModel = {
  username: string
  password: string
  code: string
}

export type LoginErrorState = {
  username: string
  password: string
  code: string
  form: string
}

export type LoginRequest = {
  username: string
  password: string
  code: string
}

export type LoginResponseData = {
  username?: string
  jwt?: string
  token?: string
}

export type LoginResponse = {
  code: number
  message: string
  data?: LoginResponseData | null
}

export type AuthSession = {
  token: string
  jwt: string
  username: string
  isLoggedIn: 'true'
}
