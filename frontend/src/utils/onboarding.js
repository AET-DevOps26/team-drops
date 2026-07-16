const registrationIntentKey = 'teamDropsIntroRegistrationIntent';
const pendingPrefix = 'teamDropsIntroPending:';
const completedPrefix = 'teamDropsIntroCompleted:';

function userKey(prefix, userId) {
  return `${prefix}${userId}`;
}

export function beginRegistrationIntro(storage) {
  storage.setItem(registrationIntentKey, 'true');
}

export function cancelRegistrationIntro(storage) {
  storage.removeItem(registrationIntentKey);
}

export function shouldShowIntro(storage, user) {
  if (user?.id == null) {
    return false;
  }

  const pendingKey = userKey(pendingPrefix, user.id);
  const completedKey = userKey(completedPrefix, user.id);
  const registrationIntent = storage.getItem(registrationIntentKey) === 'true';

  if (user.new_user === true || registrationIntent) {
    storage.setItem(pendingKey, 'true');
  }
  storage.removeItem(registrationIntentKey);

  return storage.getItem(pendingKey) === 'true'
    && storage.getItem(completedKey) !== 'true';
}

export function completeIntro(storage, userId) {
  if (userId == null) {
    return;
  }

  storage.setItem(userKey(completedPrefix, userId), 'true');
  storage.removeItem(userKey(pendingPrefix, userId));
  storage.removeItem(registrationIntentKey);
}
