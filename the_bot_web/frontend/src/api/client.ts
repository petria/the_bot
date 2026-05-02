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

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly authenticationRequired = false,
  ) {
    super(message);
  }
}
