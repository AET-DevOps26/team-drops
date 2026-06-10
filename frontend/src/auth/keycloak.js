import Keycloak from 'keycloak-js';

const runtimeConfig = window.__APP_CONFIG__ ?? {};

function configValue(key, fallback) {
  return runtimeConfig[key] ?? import.meta.env[`VITE_${key}`] ?? fallback;
}

export const authEnabled = configValue('AUTH_ENABLED', 'false') === 'true';

const keycloakConfig = {
  url: configValue('KEYCLOAK_URL', 'http://localhost:8090'),
  realm: configValue('KEYCLOAK_REALM', 'team-drops'),
  clientId: configValue('KEYCLOAK_CLIENT_ID', 'team-drops'),
};

let keycloak;
let initialization;

function getKeycloak() {
  if (!keycloak) {
    keycloak = new Keycloak(keycloakConfig);
  }

  return keycloak;
}

export async function initializeAuth() {
  if (!authEnabled) {
    return false;
  }

  if (!initialization) {
    initialization = getKeycloak().init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }

  return initialization;
}

export function loginWithKeycloak() {
  return getKeycloak().login();
}

export function registerWithKeycloak() {
  return getKeycloak().register();
}

export function logoutFromKeycloak() {
  if (!authEnabled) {
    return Promise.resolve();
  }

  return getKeycloak().logout({
    redirectUri: window.location.origin,
  });
}

export async function getValidAccessToken() {
  if (!authEnabled) {
    return null;
  }

  const instance = getKeycloak();
  if (!instance.authenticated) {
    return null;
  }

  await instance.updateToken(30);
  return instance.token ?? null;
}
