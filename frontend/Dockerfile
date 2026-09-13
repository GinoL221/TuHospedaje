FROM node:20-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM dependencies AS dev
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

FROM dependencies AS build
COPY . .
ARG VITE_API_URL=/api
ARG VITE_WHATSAPP_NUMBER=
ENV VITE_API_URL=$VITE_API_URL \
    VITE_WHATSAPP_NUMBER=$VITE_WHATSAPP_NUMBER
RUN npm run build

FROM nginx:alpine
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
