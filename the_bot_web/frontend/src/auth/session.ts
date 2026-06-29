const RETURN_TO_KEY = 'the-bot-auth-return-to';

let redirectingToLogin = false;

export function handleAuthenticationRequired() {
  if (redirectingToLogin || typeof window === 'undefined') {
    return;
  }

  redirectingToLogin = true;
  storeReturnTarget();
  window.location.replace('/login?expired=1');
}

export function consumeAuthReturnTarget(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const target = window.sessionStorage.getItem(RETURN_TO_KEY);
    window.sessionStorage.removeItem(RETURN_TO_KEY);
    return isReturnTargetAllowed(target) ? target : null;
  } catch {
    return null;
  }
}

function storeReturnTarget() {
  const target = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (!isReturnTargetAllowed(target)) {
    return;
  }

  try {
    window.sessionStorage.setItem(RETURN_TO_KEY, target);
  } catch {
    // Ignore storage failures; the login redirect still fixes the session.
  }
}

function isReturnTargetAllowed(target: string | null | undefined): target is string {
  if (!target || !target.startsWith('/') || target.startsWith('//')) {
    return false;
  }

  return !target.startsWith('/login') && !target.startsWith('/generated/');
}
