# ─── Stage 1: Build Angular app ──────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies first (layer cache: only invalidated when package files change)
COPY package*.json ./
RUN npm ci --quiet

# Copy source and build for production
COPY . .
RUN npm run build -- --configuration production

# ─── Stage 2: Serve with Nginx ────────────────────────────────────────────────
FROM nginx:alpine
# Copy Angular build output to Nginx web root
COPY --from=builder /app/dist/pricing-engine-frontend/browser /usr/share/nginx/html
# Override default Nginx config for Angular SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
