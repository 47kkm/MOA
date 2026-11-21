export class ApiError extends Error {
    public readonly status: number;
    public readonly code: string;

    constructor(status: number, code: string) {
        super(`${status} ${code}`);
        this.status = status;
        this.code = code;
    }
}

export const request = async <T>(method: string, url: string, body?: unknown): Promise<T> => {
    let response: Response;
    try {
        response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
    } catch {
        throw new ApiError(0, 'NETWORK');
    }

    if (!response.ok) {
        let code = 'UNKNOWN';

        try {
            const errorBody = await response.json();
            code = errorBody.code ?? 'UNKNOWN';
        } catch {
            // 본문이 JSON이 아니면 UNKNOWN 그대로
        }

        throw new ApiError(response.status, code);
    }

    return response.status === 204 ? undefined as T : await response.json();
}