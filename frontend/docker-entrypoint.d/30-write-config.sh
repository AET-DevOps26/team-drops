#!/bin/sh
set -eu

cat > /usr/share/nginx/html/config.js <<EOF
window.__APP_CONFIG__ = {
  AUTH_ENABLED: "${VITE_AUTH_ENABLED:-false}",
  KEYCLOAK_URL: "${VITE_KEYCLOAK_URL:-http://localhost:8090}",
  KEYCLOAK_REALM: "${VITE_KEYCLOAK_REALM:-team-drops}",
  KEYCLOAK_CLIENT_ID: "${VITE_KEYCLOAK_CLIENT_ID:-team-drops}",
};
EOF
