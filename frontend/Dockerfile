# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build

WORKDIR /app
ARG VITE_GEO_CDN_BASE_URL=
ENV VITE_GEO_CDN_BASE_URL=${VITE_GEO_CDN_BASE_URL}
COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    corepack enable && pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
