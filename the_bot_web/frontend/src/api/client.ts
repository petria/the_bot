export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: {
      Accept: 'application/json',
    },
    credentials: 'same-origin',
  });

  if (response.redirected || response.url.includes('/login')) {
    throw new ApiError('Authentication required', response.status, true);
  }

  if (!response.ok) {
    throw new ApiError(`${response.status} ${response.statusText}`, response.status, response.status === 401);
  }

  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    throw new ApiError('Expected JSON response', response.status, true);
  }

  return response.json() as Promise<T>;
}

export async function putJson<T>(path: string, body: unknown): Promise<T> {
  const request = async (refreshCsrf: boolean) => fetch(path, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(await csrfHeader(refreshCsrf)),
    },
    body: JSON.stringify(body),
    credentials: 'same-origin',
  });

  let response = await request(false);
  if (response.status === 403) {
    response = await request(true);
  }

  if (!response.ok) {
    throw new ApiError(`${response.status} ${response.statusText}`, response.status, response.status === 401);
  }

  return response.json() as Promise<T>;
}

export async function postForm(path: string, form: URLSearchParams = new URLSearchParams()): Promise<Response> {
  const request = async (refreshCsrf: boolean) => fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      ...(await csrfHeader(refreshCsrf)),
    },
    body: form,
    credentials: 'same-origin',
    redirect: 'manual',
  });

  let response = await request(false);
  if (response.status === 403) {
    response = await request(true);
  }

  if (!response.ok && response.status !== 0 && response.status !== 302) {
    throw new ApiError(`${response.status} ${response.statusText}`, response.status, response.status === 401);
  }

  return response;
}

async function csrfHeader(refresh = false): Promise<Record<string, string>> {
  const token = readCookie('XSRF-TOKEN');
  if (token && !refresh) {
    return { 'X-XSRF-TOKEN': token };
  }

  const response = await fetch('/api/web/csrf', {
    credentials: 'same-origin',
  });
  if (!response.ok) {
    return {};
  }

  const csrf = await response.json() as { headerName: string; token: string };
  return csrf.token ? { [csrf.headerName]: csrf.token } : {};
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  return document.cookie
      .split(';')
      .map((cookie) => cookie.trim())
      .find((cookie) => cookie.startsWith(prefix))
      ?.slice(prefix.length) ?? null;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly authenticationRequired = false,
  ) {
    super(message);
  }
}
