import {
  defineComponent,
  h,
  inject,
  markRaw,
  readonly,
  shallowRef,
  type App as VueApp,
  type InjectionKey,
} from 'vue'
import { getAuthRedirect } from '../modules/auth/routeGuards'
import { routes } from './routes'
import type { AppRouteLocation, AppRouteRecord } from './types'

type AppRouter = {
  install(app: VueApp): void
  push(path: string): void
  replace(path: string): void
  resolve(path: string): AppRouteLocation
  currentRoute: Readonly<{ value: AppRouteLocation }>
}

const fallbackRoute: AppRouteRecord = {
  path: '/login',
  name: 'login',
  redirect: '/login',
}

const routerKey: InjectionKey<AppRouter> = Symbol('app-router')
const routeKey: InjectionKey<{ value: AppRouteLocation }> = Symbol('app-route')

function normalizePath(path: string) {
  const [withoutQuery] = path.split(/[?#]/)
  const normalized = withoutQuery.startsWith('/') ? withoutQuery : `/${withoutQuery}`

  if (normalized.length > 1 && normalized.endsWith('/')) {
    return normalized.slice(0, -1)
  }

  return normalized || '/'
}

function matchRoutePath(routePath: string, path: string) {
  const routeParts = routePath.split('/').filter(Boolean)
  const pathParts = path.split('/').filter(Boolean)
  if (routeParts.length !== pathParts.length) {
    return null
  }
  const params: Record<string, string> = {}
  for (let index = 0; index < routeParts.length; index += 1) {
    const routePart = routeParts[index]
    const pathPart = pathParts[index]
    if (routePart.startsWith(':')) {
      params[routePart.slice(1)] = decodeURIComponent(pathPart)
      continue
    }
    if (routePart !== pathPart) {
      return null
    }
  }
  return params
}

function getMatchingRoute(path: string) {
  for (const route of routes) {
    const params = matchRoutePath(route.path, path)
    if (params) {
      return { route, params }
    }
  }
  return { route: fallbackRoute, params: {} }
}

function resolveRoute(path: string, depth = 0): AppRouteLocation {
  const normalizedPath = normalizePath(path)
  const { route: matchedRoute, params } = getMatchingRoute(normalizedPath)

  if (matchedRoute.redirect) {
    if (depth > 10) {
      throw new Error('Route redirect loop detected.')
    }

    const redirectTarget =
      typeof matchedRoute.redirect === 'function'
        ? matchedRoute.redirect()
        : matchedRoute.redirect

    return resolveRoute(redirectTarget, depth + 1)
  }

  const routeMeta = matchedRoute.meta || {}
  const guardRedirect = getAuthRedirect(routeMeta)

  if (guardRedirect) {
    if (depth > 10) {
      throw new Error('Route guard redirect loop detected.')
    }

    return resolveRoute(guardRedirect, depth + 1)
  }

  if (!matchedRoute.component) {
    throw new Error(`Route "${matchedRoute.name}" is missing a component.`)
  }

  return {
    path: normalizedPath,
    name: matchedRoute.name,
    component: markRaw(matchedRoute.component),
    meta: routeMeta,
    params,
  }
}

function getCurrentPath() {
  if (typeof window === 'undefined') {
    return '/'
  }

  return normalizePath(window.location.pathname || '/')
}

const RouterView = defineComponent({
  name: 'RouterView',
  setup() {
    const currentRoute = inject(routeKey)

    if (!currentRoute) {
      throw new Error('RouterView requires the app router to be installed.')
    }

    return () =>
      h(currentRoute.value.component, {
        key: currentRoute.value.path,
      })
  },
})

const currentRoute = shallowRef<AppRouteLocation>(resolveRoute(getCurrentPath()))

function navigate(path: string, replace = false) {
  const nextRoute = resolveRoute(path)
  const targetPath = nextRoute.path

  if (typeof window !== 'undefined' && getCurrentPath() !== targetPath) {
    const method = replace ? 'replaceState' : 'pushState'
    window.history[method]({}, '', targetPath)
  }

  currentRoute.value = nextRoute
}

function handlePopState() {
  navigate(getCurrentPath(), true)
}

let isListening = false

const router: AppRouter = {
  install(app) {
    if (typeof window !== 'undefined' && !isListening) {
      window.addEventListener('popstate', handlePopState)
      isListening = true
      navigate(getCurrentPath(), true)
    }

    app.provide(routerKey, router)
    app.provide(routeKey, currentRoute)
    app.component('RouterView', RouterView)
  },
  push(path) {
    navigate(path, false)
  },
  replace(path) {
    navigate(path, true)
  },
  resolve(path) {
    return resolveRoute(path)
  },
  currentRoute: readonly(currentRoute) as AppRouter['currentRoute'],
}

export function useRouter() {
  const instance = inject(routerKey)

  if (!instance) {
    throw new Error('App router is not available.')
  }

  return instance
}

export default router
