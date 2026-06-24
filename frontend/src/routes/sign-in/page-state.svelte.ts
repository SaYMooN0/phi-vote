import { browser } from '$app/environment';
import { pushState } from '$app/navigation';
import { ApiAuth } from '$lib/ts/backend';
import { InvalidInputErrList, toJustMsgObj, type InvalidInputErr } from '$lib/ts/core';
import { watch } from 'runed';

export type AuthMode = 'registration' | 'login';

type PageErrs = {
	email: string;
	uniqueName: string;
	password: string;
	other: string;
};

type AuthSuccess = { healthMsg?: string };

function emptyErrors(): PageErrs {
	return { email: '', uniqueName: '', password: '', other: '' };
}

function msgFromBackendErr(err: { msg?: string; errKey: string }): string {
	return err.msg ?? 'Could not complete the request. Please try again.';
}


function modeFromSearchParams(searchParams: URLSearchParams): AuthMode {
	return searchParams.get('current') === 'login' ? 'login' : 'registration';
}

function hrefForMode(mode: AuthMode, href: string): string {
	const url = new URL(href, window.location.origin);

	url.searchParams.set('current', mode);

	return `${url.pathname}${url.search}${url.hash}`;
}

export class AuthPageState {
	current = $state<AuthMode>('registration');

	email = $state('');
	uniqueName = $state('');
	password = $state('');

	errors = $state<PageErrs>(emptyErrors());
	isLoading = $state(false);

	title = $derived(this.current === 'registration' ? 'Create a new account' : 'Log into your account');
	submitLabel = $derived(this.current === 'registration' ? 'Create account' : 'Log in');
	modeActionLabel = $derived(this.current === 'registration' ? 'I already have an account' : "I don't have an account yet");

	constructor() {
		watch(
			() => this.uniqueName,
			() => this.clearFieldError('uniqueName')
		);
		watch(
			() => this.email,
			() => this.clearFieldError('email')
		);
		watch(
			() => this.password,
			() => this.clearFieldError('password')
		);
	}
	toggleMode() {
		this.setMode(this.current === 'registration' ? 'login' : 'registration');
	}

	setMode(mode: AuthMode, options: { updateUrl?: boolean } = { updateUrl: true }) {
		if (this.isLoading || mode === this.current) return;

		this.clearErrors();
		this.current = mode;

		if (options.updateUrl !== false && browser) {
			const url = hrefForMode(mode, window.location.href);
			// eslint-disable-next-line svelte/no-navigation-without-resolve
			pushState(url, {});
		}
	}

	syncModeFromUrl(url: URL) {
		const mode = modeFromSearchParams(url.searchParams);

		if (mode === this.current) return;

		this.setMode(mode, { updateUrl: false });
	}

	async submit(event: SubmitEvent) {
		event.preventDefault();

		if (this.isLoading) return;

		this.clearErrors();

		const validationErrors = this.current === 'registration' ? this.validateRegistration() : this.validateLogin();

		if (validationErrors.any()) {
			this.setErrors(validationErrors.toJustMsgObj<Partial<PageErrs>>());
			return;
		}

		await this.makeRequest();
	}

	signInWithGoogle() {
		this.clearErrors();
		console.log('sign in with google');
	}

	private validateRegistration() {
		return InvalidInputErrList.Empty()
			.ifOneOfAdd('uniqueName', [
				{ cond: this.uniqueName.includes(' '), msg: 'Unique name cannot contain spaces' },
				{ cond: this.uniqueName.trim().length < 3, msg: 'Unique name must be at least 3 characters long' },
				{ cond: this.uniqueName.length >= 100, msg: 'Unique name is too long' }
			])
			.ifOneOfAdd('email', [
				{ cond: this.email.includes(' '), msg: 'Email cannot contain spaces' },
				{ cond: !this.email.includes('@'), msg: 'Email must contain the @ symbol' }
			])
			.ifOneOfAdd('password', [
				{ cond: this.password.includes(' '), msg: 'Password cannot contain spaces' },
				{ cond: this.password.length < 8, msg: 'Password must be at least 8 characters long' },
				{ cond: this.password.length >= 200, msg: 'Password is a bit too long' }
			]);
	}

	private validateLogin() {
		return InvalidInputErrList.Empty()
			.ifOneOfAdd('email', [
				{ cond: this.email.includes(' '), msg: 'Email cannot contain spaces' },
				{ cond: !this.email.includes('@'), msg: 'Email must contain the @ symbol' }
			])
			.ifOneOfAdd('password', [
				{ cond: this.password.length === 0, msg: 'Password is required' },
				{ cond: this.password.includes(' '), msg: 'Password cannot contain spaces' }
			]);
	}

	private async makeRequest() {
		this.isLoading = true;

		try {
			const response = this.current === 'registration'
				? await ApiAuth.POST<InvalidInputErr, AuthSuccess>('/sign-up', {
					email: this.email,
					uniqueName: this.uniqueName,
					password: this.password
				})
				: await ApiAuth.POST<InvalidInputErr, AuthSuccess>('/login', {
					email: this.email,
					password: this.password
				});

			if (response.isOk) return;

			if (response.errKey === 'InvalidInput') {
				this.setErrors(toJustMsgObj<Partial<PageErrs>>(response));
				return;
			}

			this.errors.other = msgFromBackendErr(response);
		} finally {
			this.isLoading = false;
		}
	}

	private setErrors(errors: Partial<PageErrs>) {
		this.errors = { ...emptyErrors(), ...errors };
	}

	private clearErrors() {
		this.errors = emptyErrors();
	}

	private clearFieldError(key: keyof Omit<PageErrs, 'other'>) {
		this.errors[key] = '';
		this.errors.other = '';
	}
}
