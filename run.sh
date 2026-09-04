#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
exec ./mvnw spring-boot:run "$@"
