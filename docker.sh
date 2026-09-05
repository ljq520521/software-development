#!/bin/sh
set -eu
cd "$(dirname "$0")"

if [ "${1:-}" = init ]; then
  if [ -e .env.docker ]; then
    echo '.env.docker 已存在，保留原有配置。'
    exit 0
  fi
  command -v openssl >/dev/null 2>&1 || {
    echo '需要 openssl 生成随机密码；也可复制 .env.docker.example 为 .env.docker 后手动填写。' >&2
    exit 1
  }
  admin_password=$(openssl rand -hex 24)
  mysql_password=$(openssl rand -hex 24)
  mysql_root_password=$(openssl rand -hex 24)
  umask 077
  set -C
  {
    printf '%s\n' 'APP_BIND_ADDRESS=127.0.0.1' 'APP_PORT=8081' 'ADMIN_EMAIL=admin@example.com'
    printf 'ADMIN_PASSWORD=%s\nMYSQL_PASSWORD=%s\nMYSQL_ROOT_PASSWORD=%s\n' \
      "$admin_password" "$mysql_password" "$mysql_root_password"
    printf '%s\n' 'COOKIE_SECURE=false' 'FORWARD_HEADERS_STRATEGY=none'
    printf '%s\n' 'PUBLIC_BASE_URL=http://127.0.0.1:8081' 'SMTP_HOST=' 'SMTP_PORT=587'
    printf '%s\n' 'SMTP_USERNAME=' 'SMTP_PASSWORD=' 'MAIL_FROM=no-reply@example.com' 'SMTP_STARTTLS=true'
  } > .env.docker
  echo '已生成 .env.docker（仅当前用户可读写）；管理员账号和密码请在该文件查看。'
  exit 0
fi

if [ ! -f .env.docker ]; then
  echo '缺少 .env.docker，请先执行 ./docker.sh init。' >&2
  exit 1
fi
command -v docker >/dev/null 2>&1 || {
  echo '请安装并启动 Docker Desktop，或 Docker Engine 与 Compose 插件。' >&2
  exit 1
}
if [ "$#" -eq 0 ]; then
  echo '用法：./docker.sh init | up -d --build --wait | ps | logs -f app | down'
  exit 0
fi
exec docker compose --env-file .env.docker "$@"
