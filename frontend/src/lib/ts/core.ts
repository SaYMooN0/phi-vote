export type Result<E extends HasErrCategory, A> =
    | { isOk: true; } & A
    | { isOk: false } & E;

export type HasErrCategory = { eCat: ErrCategory; };

export type ErrCategory =
    | 'Unauthenticated'
    | 'Domain'
    | 'Other';

export function hasMsgField(err: HasErrCategory) { return 'msg' in err; }