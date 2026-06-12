import type { ErrCategory, HasErrCategory, Result } from './core';


export type BackendE = BackendFetchErr | { eCat: ErrCategory; }

export class BackendService {
    #baseUrl: string;
    constructor(baseUrl: string) {
        this.#baseUrl = baseUrl;
    }

    async GET<E extends BackendE, A>(url: string) { return this.serverGET<E, A>(fetch, url); }
    async serverGET<E extends BackendE, A>(fetchFunc: typeof fetch, url: string) {
        return this.#fetchResponse<E, A>(fetchFunc, url, { method: 'GET', credentials: 'include' });
    }
    async PUT(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverPUT<E extends BackendE, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('PUT', bodyData));
    }
    async POST(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverPOST<E extends BackendE, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('POST', bodyData));
    }
    async PATCH(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverPATCH<E extends BackendE, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('PATCH', bodyData));
    }
    async DELETE(url: string, bodyData: unknown) { return this.serverPOST(fetch, url, bodyData); }
    async serverDELETE<E extends BackendE, A>(fetchFunc: typeof fetch, url: string, bodyData: unknown) {
        return this.#fetchResponse<E, A>(fetchFunc, url, BackendService.#reqJsonOpt('DELETE', bodyData));
    }

    async #fetchResponse<E extends BackendE, A>(
        fetchFunc: typeof fetch,
        url: string,
        options: RequestInit
    ): Promise<BackendFetchResult<E, A>> {
        try {
            const response = await fetchFunc(`/api/${this.#baseUrl}` + url, options);
            const text = await response.text();

            try {
                const data = JSON.parse(text) as BackendFetchResult<E, A>;
                return data;

            } catch {
                console.error(text);
                return {
                    eCat: 'BackendCannotDeserializeResponse',
                    msg: "Server request error: Unexpected response format",
                    isOk: false
                };
            }

        } catch (e: unknown) {
            if (e instanceof TypeError && e.message === "Failed to fetch") {
                return { eCat: 'BackendCannotConnect', msg: "Server request error: Could not connect. Please check your connection or try again later", isOk: false }
            }

            if (e instanceof DOMException && e.name === "AbortError") {
                return { eCat: 'BackendFetchAborted', msg: "Server request error: aborted", isOk: false };
            }

            return { eCat: 'BackendUnexpectedError', msg: "Server request error: Unexpected error", isOk: false };
        }
    }
    static #reqJsonOpt(method: string, bodyData: unknown): RequestInit {
        return { method: method, credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(bodyData) }
    }
}


export type BackendFetchResult<E extends BackendE, A> =
    | { isOk: true; } & A
    | { isOk: false } & E;


export type BackendFetchErr = {
    msg: string,
    eCat:
    | 'BackendCannotConnect'
    | 'BackendFetchAborted'
    | 'BackendUnexpectedError'
    | 'BackendCannotDeserializeResponse'
};


export function toCoreLikeResult<E extends HasErrCategory, A>(
    result: BackendFetchResult<E | BackendFetchErr, A>
):
    | { isOk: false; isBackendFetchErr: true, msg: string }
    | { isOk: false; isBackendFetchErr: false } & E
    | { isOk: true; } & A {
    if (result.isOk) {
        return result
    }
    return { isOk: false, ...toCoreLikeErr<E>(result) };

}

export function toCoreLikeErr<E extends HasErrCategory>(
    err: E | BackendFetchErr
):
    | { isBackendFetchErr: true, msg: string }
    | { isBackendFetchErr: false } & E {

    switch (err.eCat) {
        case 'BackendCannotConnect':
        case 'BackendFetchAborted':
        case 'BackendUnexpectedError':
        case 'BackendCannotDeserializeResponse':
            return {
                isBackendFetchErr: true,
                msg: err.msg,
            };

        default:
            return { isBackendFetchErr: false, ...err };
    }
}
export const ApiAuth = new BackendService('auth');
export const ApiVoting = new BackendService('voting');
