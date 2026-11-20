export const request = async <T>(method: string, url: string): Promise<T> => {
    const response = await fetch(url, {
        method,
    });

    return await response.json();
}