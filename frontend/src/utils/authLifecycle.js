export function shouldReloadForAuthRestore(event, session) {
  return Boolean(event?.persisted && !session);
}
