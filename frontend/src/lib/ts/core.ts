export type FoResult<E extends ResultErr, A> =
    | { isOk: true; } & A
    | { isOk: false } & E;

type ResultErr = { isFromBackendFetch?: boolean; };

export type HasErrKey = { errKey: string; };

type InputKey = string;
export type InvalidInputErr = { errKey: 'InvalidInput'; inputs: Record<InputKey, { msg: string, fixRec?: string }>; };

export function toJustMsgObj<T extends Partial<Record<InputKey, string>>>(err: InvalidInputErr): T {
    return Object.fromEntries(Object.entries(err.inputs).map(([k, v]) => [k, v.msg])) as T;
}

export class InvalidInputErrList {
    #inputs: Record<InputKey, { msg: string, fixRec?: string }>;
    constructor(inputErrs: Record<InputKey, { msg: string, fixRec?: string }>) {
        this.#inputs = inputErrs;
    }
    static Empty() { return new InvalidInputErrList({}); }
    static FromInvalidInputErr(e: InvalidInputErr) { return new InvalidInputErrList(e.inputs); }
    add(k: InputKey, msg: string, fixRec?: string) {
        if (!this.#inputs[k]) {
            this.#inputs[k] = { msg, fixRec };
        }
        return this;
    }

    addWithOverride(k: InputKey, msg: string, fixRec?: string) {
        this.#inputs[k] = { msg, fixRec };
        return this;
    }
    ifAdd(k: InputKey, cond: boolean, msg: string, fixRec?: string): InvalidInputErrList {
        if (cond) { return this.add(k, msg, fixRec); }
        return this;
    }
    ifOneOfAdd(k: InputKey, options: { cond: boolean, msg: string, fixRec?: string }[]): InvalidInputErrList {
        for (const { cond: condition, msg, fixRec } of options) {
            if (condition) { return this.add(k, msg, fixRec); }
        }
        return this;
    }

    any() { return Object.keys(this.#inputs).length > 0; }
    toObj<T extends Partial<Record<InputKey, { msg: string; fixRec?: string }>>>() {
        return this.#inputs as T;
    }


    toJustMsgObj<T extends Partial<Record<InputKey, string>>>(): T {
        return Object.fromEntries(
            Object.entries(this.#inputs).map(([k, v]) => [k, v.msg])
        ) as T;
    }
}
