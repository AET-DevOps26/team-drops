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
let loginRedirectStarted = false;
const pendingRedirect = new Promise(() => {});

function getSilentCheckSsoRedirectUri() {
  return new URL(`${import.meta.env.BASE_URL}silent-check-sso.html`, window.location.origin).href;
}

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
    const instance = getKeycloak();
    instance.onTokenExpired = () => {
      instance.updateToken(0).catch(() => {
        redirectToLoginForExpiredSession();
      });
    };

    initialization = instance.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: getSilentCheckSsoRedirectUri(),
      silentCheckSsoFallback: false,
    });
  }

  return initialization;
}

export async function loginWithKeycloak() {
  await initializeAuth();
  return getKeycloak().login();
}

export function redirectToLoginForExpiredSession() {
  if (!authEnabled) {
    return Promise.resolve();
  }

  if (!loginRedirectStarted) {
    loginRedirectStarted = true;

    Promise.resolve(getKeycloak().login({
      redirectUri: window.location.href,
    })).catch(() => {
      loginRedirectStarted = false;
      window.location.assign(window.location.origin);
    });
  }

  // Keep the request that detected the expired session pending while the
  // browser navigates to Keycloak, so the current page cannot show an error.
  return pendingRedirect;
}

export async function registerWithKeycloak() {
  await initializeAuth();
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
    return redirectToLoginForExpiredSession();
  }

  try {
    await instance.updateToken(30);
  } catch {
    return redirectToLoginForExpiredSession();
  }

  return instance.token ?? redirectToLoginForExpiredSession();
}
