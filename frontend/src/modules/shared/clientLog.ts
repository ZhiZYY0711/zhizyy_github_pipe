type ClientLogLevel = 'info' | 'warn' | 'error'

type ClientLogPayload = {
  level: ClientLogLevel
  source: string
  message: string
  data?: unknown
}

let installed = false

export function installClientFileLogger() {
  if (!import.meta.env.DEV || installed) {
    return
  }
  installed = true

  const originalWarn = console.warn.bind(console)
  const originalError = console.error.bind(console)

  console.warn = (...args: unknown[]) => {
    postClientLog({ level: 'warn', source: 'console.warn', message: stringifyArgs(args) })
    originalWarn(...args)
  }

  console.error = (...args: unknown[]) => {
    postClientLog({ level: 'error', source: 'console.error', message: stringifyArgs(args) })
    originalError(...args)
  }

  window.addEventListener('error', (event) => {
    postClientLog({
      level: 'error',
      source: 'window.error',
      message: event.message,
      data: { filename: event.filename, lineno: event.lineno, colno: event.colno },
    })
  })

  window.addEventListener('unhandledrejection', (event) => {
    postClientLog({
      level: 'error',
      source: 'window.unhandledrejection',
      message: event.reason instanceof Error ? event.reason.message : String(event.reason),
    })
  })

  postClientLog({ level: 'info', source: 'client-log', message: 'client file logger installed' })
}

export function logClientEvent(source: string, message: string, data?: unknown) {
  postClientLog({ level: 'info', source, message, data })
}

function postClientLog(payload: ClientLogPayload) {
  if (!import.meta.env.DEV) {
    return
  }

  const body = JSON.stringify({
    ...payload,
    path: window.location.pathname,
    timestamp: new Date().toISOString(),
  })

  if (navigator.sendBeacon) {
    navigator.sendBeacon('/__client-log', new Blob([body], { type: 'application/json' }))
    return
  }

  fetch('/__client-log', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true,
  }).catch(() => undefined)
}

function stringifyArgs(args: unknown[]) {
  return args
    .map((item) => {
      if (item instanceof Error) {
        return `${item.name}: ${item.message}`
      }
      if (typeof item === 'string') {
        return item
      }
      try {
        return JSON.stringify(item)
      } catch {
        return String(item)
      }
    })
    .join(' ')
}
