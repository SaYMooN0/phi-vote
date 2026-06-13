
export const StrUtils = {
    rndStr,
    rndStrWithPref,
    pascalToKebab
};
function rndStr(length: number = 8): string {
    const characters = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';

    for (let i = 0; i < length; i++) {
        const randomIndex = Math.floor(Math.random() * characters.length);
        result += characters[randomIndex];
    }

    return result;
}

function rndStrWithPref(prefix: string, length: number = 8): string {
    return prefix + rndStr(length);
}

function pascalToKebab(input: string): string {
    return input.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}
