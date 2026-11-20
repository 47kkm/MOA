export const request = async <T>(method: string, url: string, body?: unknown): Promise<T> => {
    const response = await fetch(url, {
        method,
        body: JSON.stringify(body),
    });

    return await response.json();
}