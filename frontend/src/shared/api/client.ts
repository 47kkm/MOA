class ApiError extends Error {
    public readonly status: number;
    public readonly code: string;

    constructor(status: number, code: string) {
        super(`${status} ${code}`);
        this.status = status;
        this.code = code;
    }
}

export const request = async <T>(method: string, url: string, body?: unknown): Promise<T> => {
    const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    if (!response.ok) {
        const errorBody = await response.json()
        throw new ApiError(response.status, errorBody.code);
    }

    return response.status === 204 ? undefined as T : await response.json();
}