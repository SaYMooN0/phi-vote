import type { FoResult, HasErrKey } from './core';



export class BackendService {
    #baseUrl: string;
    constructor(baseUrl: string) {
        this.#baseUrl = baseUrl;
    }

    async GET<E extends HasErrKey, A>(url: string) { return this.serverGET<E, A>(fetch, url); }
    async serverGET<E extends HasErrKey, A>(fetchFunc: typeof fetch, url: string) {
        return this.#fetchResponse<E, A>(fetchFunc, url, { method: 'GET', credentials: 'include' });
    }
    async PUT<E extends HasErrKey, A>(url: string, bodyData: unknown) { return this.serverPOST<E, A>(fetch, url, bodyData); }
    async serverPUT<E extends HasErrKey, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('PUT', bodyData));
    }
    async POST<E extends HasErrKey, A>(url: string, bodyData: unknown) { return this.serverPOST<E, A>(fetch, url, bodyData); }
    async serverPOST<E extends HasErrKey, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('POST', bodyData));
    }
    async PATCH(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverPATCH<E extends HasErrKey, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('PATCH', bodyData));
    }
    async DELETE(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverDELETE<E extends HasErrKey, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('DELETE', bodyData));
    }

    async #fetchResponse<E extends HasErrKey, A>(
        fetchFunc: typeof fetch,
        url: string,
        options: RequestInit
    ): Promise<Result<E, A>> {
        try {
            const response = await fetchFunc(`/api/${this.#baseUrl}` + url, options);
            const text = await response.text();
            if (text.length === 0 && response.status === 502) {
                return {
                    isOk: false,
                    errKey: 'FetchErr',
                    fetchErrCase: 'BackendNotRunning',
                    msg: "Server request error: server is down. Please try again later"
                }
            }
            try {
                const data = JSON.parse(text) as Result<E, A>;
                return data;

            } catch (e: unknown) {
                console.log(e);

                return {
                    isOk: false,
                    errKey: 'FetchErr',
                    fetchErrCase: 'BackendCannotDeserializeResponse',
                    msg: "Server request error: Unexpected response format"
                }
            }

        } catch (e: unknown) {
            console.log(e);
            if (e instanceof TypeError && e.message === "Failed to fetch") {
                return { isOk: false, errKey: 'FetchErr', fetchErrCase: 'BackendCannotConnect', msg: "Server request error: Could not connect. Please check your connection or try again later" }
            }

            if (e instanceof DOMException && e.name === "AbortError") {
                return { isOk: false, errKey: 'FetchErr', fetchErrCase: 'BackendFetchAborted', msg: "Server request error: aborted" };
            }

            return { isOk: false, errKey: 'FetchErr', fetchErrCase: 'BackendUnexpectedError', msg: "Server request error: Unexpected error" };
        }
    }
    static #reqJsonOpt(method: string, bodyData: unknown): RequestInit {
        return { method: method, credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(bodyData) }
    }
}
export const ApiAuth = new BackendService('auth');
export const ApiVoting = new BackendService('voting');

export type Result<E extends HasErrKey, A> =
    | { isOk: true; } & A
    | { isOk: false } & (FetchErr | E);

export type BackendErr = FetchErr | HasErrKey;

export type FetchErr = {
    msg: string,
    errKey: 'FetchErr',
    fetchErrCase:
    | 'BackendNotRunning'
    | 'BackendCannotConnect'
    | 'BackendFetchAborted'
    | 'BackendUnexpectedError'
    | 'BackendCannotDeserializeResponse'
};


export function toFoResult<E extends { errKey: string; }, A>(
    result: Result<E, A>
): FoResult<E & { isFromBackendFetch: boolean }, A> {

    if (!result.isOk && result.errKey === 'FetchErr') {
        return { ...result, isFromBackendFetch: true };
    }
    return { ...result, isFromBackendFetch: false };

}

