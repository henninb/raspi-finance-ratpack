#!/bin/bash

# Podman deployment script for raspi-finance-ratpack
# Drop-in replacement for raspi-finance-endpoint (same container name and port).

REMOTE_HOST="debian-dockerserver"
REMOTE_USER="henninb"
REMOTE_DIR="/home/${REMOTE_USER}/raspi-finance-ratpack"
HOST_IP="192.168.10.10"
ENV_FILE="env.prod"
APP="raspi-finance-ratpack"
CONTAINER_NAME="raspi-finance-endpoint"

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

validate_ssl_certs() {
  local cert_path="ssl/bhenning.fullchain.pem"
  local key_path="ssl/bhenning.privkey.pem"

  log "Validating SSL certificates..."

  if [ ! -d "ssl" ]; then
    log_error "SSL directory 'ssl/' not found!"
    return 1
  fi

  if [ ! -f "$cert_path" ]; then
    log_error "SSL certificate not found: $cert_path"
    return 1
  fi

  if [ ! -f "$key_path" ]; then
    log_error "SSL private key not found: $key_path"
    return 1
  fi

  log "Checking certificate expiration..."
  if ! openssl x509 -in "$cert_path" -noout -checkend 86400 >/dev/null 2>&1; then
    log_error "SSL certificate is expired or will expire within 24 hours!"
    openssl x509 -in "$cert_path" -noout -dates 2>/dev/null | grep -E "notAfter|notBefore" || true
    return 1
  fi

  local cert_expiry
  cert_expiry=$(openssl x509 -in "$cert_path" -noout -enddate 2>/dev/null | cut -d= -f2)
  log "Certificate is valid until: $cert_expiry"

  log "Validating certificate and private key match..."
  local cert_hash key_hash
  cert_hash=$(openssl x509 -in "$cert_path" -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)
  key_hash=$(openssl pkey -in "$key_path" -pubout 2>/dev/null | openssl sha256 2>/dev/null)

  if [ "$cert_hash" != "$key_hash" ]; then
    log_error "SSL certificate and private key do not match!"
    return 1
  fi

  local cert_subject
  cert_subject=$(openssl x509 -in "$cert_path" -noout -subject 2>/dev/null | sed 's/subject=//')
  log "Certificate subject: $cert_subject"
  log "SSL certificate validation passed"
  return 0
}

setup_ssh() {
  log "Testing SSH connectivity to ${REMOTE_HOST}..."
  if ! ssh -q -o BatchMode=yes -o ConnectTimeout=5 "${REMOTE_HOST}" exit 2>/dev/null; then
    log_error "Cannot connect to ${REMOTE_HOST} via SSH"
    log "Please ensure:"
    log "  1. ${REMOTE_HOST} is defined in ~/.ssh/config"
    log "  2. SSH service is running on the target host"
    log "  3. SSH key authentication is configured"
    exit 1
  fi
  log "SSH connectivity verified for ${REMOTE_HOST}"

  if ! ssh-add -l >/dev/null 2>&1; then
    local key_path="$HOME/.ssh/id_rsa"
    if [ -f "$key_path" ]; then
      log "Starting SSH agent and adding default key..."
      eval "$(ssh-agent -s)"
      export SSH_AUTH_SOCK SSH_AGENT_PID
      ssh-add "$key_path" 2>/dev/null || log "SSH key requires passphrase - please run: ssh-add $key_path"
    fi
  else
    log "SSH agent is running with keys loaded"
  fi

  CURRENT_UID="$(ssh "${REMOTE_HOST}" id -u 2>/dev/null)"
  CURRENT_GID="$(ssh "${REMOTE_HOST}" id -g 2>/dev/null)"
  USERNAME="$(ssh "${REMOTE_HOST}" whoami 2>/dev/null)"

  if [ -z "$CURRENT_UID" ] || [ -z "$CURRENT_GID" ] || [ -z "$USERNAME" ]; then
    log_error "Failed to retrieve user information from ${REMOTE_HOST}"
    exit 1
  fi

  export CURRENT_UID CURRENT_GID USERNAME
  log "Remote user: ${USERNAME} (UID=${CURRENT_UID}, GID=${CURRENT_GID})"
}

create_remote_secrets() {
  log "Creating Podman secrets on ${REMOTE_HOST}..."
  printf '%s' "$DATASOURCE_PASSWORD" | ssh "${REMOTE_USER}@${REMOTE_HOST}" "podman secret create --replace DATASOURCE_PASSWORD -"
  printf '%s' "$JWT_KEY"             | ssh "${REMOTE_USER}@${REMOTE_HOST}" "podman secret create --replace JWT_KEY -"
  log "All Podman secrets created on ${REMOTE_HOST}"
}

log "=== Podman Deployment to ${REMOTE_HOST} (${APP}) ==="

# --- Load secrets from gopass ---
if ! command -v gopass >/dev/null 2>&1; then
  log_error "gopass is not installed."
  exit 1
fi
log "Loading secrets from gopass..."
DATASOURCE_PASSWORD=$(gopass show -o raspi-finance-endpoint/postgresql) || { log_error "Failed to retrieve secret 'raspi-finance-endpoint/postgresql'"; exit 1; }
JWT_KEY=$(gopass show -o raspi-finance-endpoint/jwt-key)               || { log_error "Failed to retrieve secret 'raspi-finance-endpoint/jwt-key'"; exit 1; }
export DATASOURCE_PASSWORD JWT_KEY
log "Secrets loaded from gopass"

# --- Step 0: SSL validation ---
log "Step 0: SSL Certificate Validation"
if ! validate_ssl_certs; then
  log_error "SSL certificate validation failed!"
  log_error ""
  log_error "Please ensure:"
  log_error "  1. Let's Encrypt certificates are current and not expired"
  log_error "  2. Certificate files exist in ssl/ directory:"
  log_error "     - ssl/bhenning.fullchain.pem"
  log_error "     - ssl/bhenning.privkey.pem"
  log_error "  3. OpenSSL is installed"
  exit 1
fi
log "SSL certificate validation completed"

# --- Step 1: SSH setup ---
log "Step 1: SSH Setup"
setup_ssh

# --- Step 2: Gradle build ---
log "Step 2: Building application with Gradle (shadowJar)..."
export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
export PATH="${JAVA_HOME}/bin:${PATH}"
if ! ./gradlew clean shadowJar -x test; then
  log_error "Gradle build failed"
  exit 1
fi
log "Gradle build succeeded: build/libs/${APP}.jar"

# --- Step 3: rsync ---
log "Step 3: Syncing project files to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}..."
ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_DIR}"
rsync -av \
  --exclude='.git/' \
  --exclude='logs/' \
  --exclude='build/' \
  --exclude='.gradle/' \
  ./ "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/"
ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_DIR}/build/libs"
rsync -av "build/libs/${APP}.jar" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/build/libs/"
log "Files synced to ${REMOTE_DIR}"

# --- Step 3.5: Create Podman secrets on remote ---
create_remote_secrets

# --- Step 4: Remote deployment ---
log "Step 4: Building and deploying container on ${REMOTE_HOST}..."
ssh -T "${REMOTE_USER}@${REMOTE_HOST}" \
  REMOTE_DIR="${REMOTE_DIR}" \
  CURRENT_UID="${CURRENT_UID}" \
  CURRENT_GID="${CURRENT_GID}" \
  USERNAME="${USERNAME}" \
  HOST_IP="${HOST_IP}" \
  ENV_FILE="${ENV_FILE}" \
  APP="${APP}" \
  CONTAINER_NAME="${CONTAINER_NAME}" \
  'bash -s' << 'ENDSSH'
set -e

cd "${REMOTE_DIR}"

echo "Creating finance-lan network if needed..."
if ! podman network ls --filter "name=^finance-lan$" -q | grep -q .; then
  podman network create finance-lan
  echo "Created finance-lan network"
else
  echo "finance-lan network already exists"
fi

echo "Stopping and removing existing container..."
if podman ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
  echo "  Stopping and removing ${CONTAINER_NAME}..."
  podman stop "${CONTAINER_NAME}" 2>/dev/null || true
  podman rm -f "${CONTAINER_NAME}" 2>/dev/null || true
fi

echo "Removing old ${APP} image..."
podman rmi -f "${APP}" 2>/dev/null || true

echo "Cleaning up dangling images..."
dangling=$(podman images -q -f dangling=true 2>/dev/null)
if [ -n "${dangling}" ]; then
  podman rmi -f ${dangling} 2>/dev/null || true
fi

echo "Building ${APP} image..."
if ! podman build \
  --build-arg APP="${APP}" \
  --build-arg TIMEZONE=America/Chicago \
  --build-arg USERNAME="${USERNAME}" \
  --build-arg CURRENT_UID="${CURRENT_UID}" \
  --build-arg CURRENT_GID="${CURRENT_GID}" \
  -t "${APP}" .; then
  echo "ERROR: podman build failed"
  exit 1
fi
echo "Image built: ${APP}"

echo "Writing Quadlet file for auto-start on boot..."
mkdir -p ~/.config/containers/systemd

cat > ~/.config/containers/systemd/raspi-finance-endpoint.container << EOF
[Unit]
Description=Raspi Finance Endpoint (Ratpack)
After=network-online.target postgresql-server.service

[Container]
Image=localhost/${APP}
ContainerName=${CONTAINER_NAME}
HostName=hornsup-endpoint
PublishPort=192.168.10.10:8443:8443
Network=finance-lan
Secret=DATASOURCE_PASSWORD,type=env
Secret=JWT_KEY,type=env
EnvironmentFile=${REMOTE_DIR}/${ENV_FILE}
AddHost=postgresql.bhenning.com:${HOST_IP}
NoNewPrivileges=true
DropCapability=ALL
AddCapability=CHOWN
AddCapability=SETGID
AddCapability=SETUID
User=${CURRENT_UID}:${CURRENT_GID}
Tmpfs=/tmp:noexec,nosuid,size=100m

[Service]
Restart=no
TimeoutStartSec=120

[Install]
WantedBy=default.target
EOF

echo "Quadlet file written to ~/.config/containers/systemd/"

export XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/run/user/$(id -u)}
export DBUS_SESSION_BUS_ADDRESS=${DBUS_SESSION_BUS_ADDRESS:-unix:path=/run/user/$(id -u)/bus}
systemctl --user daemon-reload 2>/dev/null || true
echo "Systemd user daemon reloaded"

mkdir -p "${REMOTE_DIR}/json_in"
echo "Starting ${CONTAINER_NAME}..."
podman run -d \
  --replace \
  --name "${CONTAINER_NAME}" \
  --hostname hornsup-endpoint \
  -p 192.168.10.10:8443:8443 \
  --network finance-lan \
  --secret DATASOURCE_PASSWORD,type=env \
  --secret JWT_KEY,type=env \
  --env-file "${REMOTE_DIR}/${ENV_FILE}" \
  --add-host "postgresql.bhenning.com:${HOST_IP}" \
  --pull never \
  --security-opt no-new-privileges:true \
  --cap-drop ALL \
  --cap-add CHOWN \
  --cap-add SETGID \
  --cap-add SETUID \
  --user "${CURRENT_UID}:${CURRENT_GID}" \
  --tmpfs /tmp:noexec,nosuid,size=100m \
  --restart unless-stopped \
  "localhost/${APP}"
echo "${CONTAINER_NAME} started"

echo "NOTE: Run 'sudo loginctl enable-linger ${USERNAME}' on this host to enable auto-start on reboot."

podman ps -a
ENDSSH

# --- Step 5: Verify ---
log "Step 5: Verifying deployment..."
sleep 5

log "Checking container status..."
app_status=$(ssh "${REMOTE_HOST}" "podman ps --filter name=${CONTAINER_NAME} --format '{{.Status}}'" 2>/dev/null)

if echo "$app_status" | grep -q "Up"; then
  log "${CONTAINER_NAME} is running: $app_status"
else
  log_error "${CONTAINER_NAME} is not running"
  log "Check logs: ssh ${REMOTE_HOST} 'podman logs ${CONTAINER_NAME}'"
  exit 1
fi

log "Testing application health..."
if ssh "${REMOTE_HOST}" 'curl -k -f -s https://localhost:8443/api/account/totals >/dev/null 2>&1'; then
  log "Application health check passed"
else
  log "Health check failed - application may still be starting"
fi

log "Testing LAN access..."
if curl -k -f -s --connect-timeout 10 "https://${HOST_IP}:8443/account/totals" >/dev/null 2>&1; then
  log "LAN HTTPS access working: https://${HOST_IP}:8443/"
else
  log "LAN HTTPS access test failed (may be normal if DNS/routing not configured)"
fi

log ""
log "=== Deployment Summary ==="
log "Host: ${REMOTE_HOST} (${HOST_IP})"
log "Remote directory: ${REMOTE_DIR}"
log "Application: $(echo "$app_status" | head -1)"
log ""
log "Access URLs:"
log "  HTTPS:          https://${HOST_IP}:8443/"
log "  Account totals: https://${HOST_IP}:8443/account/totals"
log "  Categories:     https://${HOST_IP}:8443/categories"
log ""
log "Monitoring commands:"
log "  App logs:         ssh ${REMOTE_HOST} 'podman logs -f ${CONTAINER_NAME}'"
log "  Container status: ssh ${REMOTE_HOST} 'podman ps'"
log "  Network info:     ssh ${REMOTE_HOST} 'podman network inspect finance-lan'"
log ""
log "Quadlet (auto-start on reboot):"
log "  Files: ssh ${REMOTE_HOST} 'ls ~/.config/containers/systemd/'"
log "  Enable linger: ssh ${REMOTE_HOST} 'sudo loginctl enable-linger ${USERNAME}'"
log ""
log "Troubleshooting:"
log "  Re-run deployment: ./deploy-podman.sh"
log "  App diagnostics: ssh ${REMOTE_HOST} 'curl -k -s https://localhost:8443/api/account/totals'"

exit 0
