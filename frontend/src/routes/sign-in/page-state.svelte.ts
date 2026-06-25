import { browser } from '$app/environment';
import { pushState } from '$app/navigation';
import { ApiAuth } from '$lib/ts/backend';
import { InvalidInputErrList, toJustMsgObj, type InvalidInputErr } from '$lib/ts/core';
import { watch } from 'runed';

export type PageCurrentState =
	| 'sign-up'
	| 'login'
	| 'confirmation-sent';

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


function hrefForMode(mode: PageCurrentState, href: string): string {
	const url = new URL(href, window.location.origin);

	url.searchParams.set('current', mode);

	return `${url.pathname}${url.search}${url.hash}`;
}

export class AuthPageState {
	current = $state<PageCurrentState>('sign-up');

	email = $state('');
	uniqueName = $state('');
	password = $state('');

	errors = $state<PageErrs>(emptyErrors());
	isLoading = $state(false);

	title = $derived(this.current === 'confirmation-sent' ? 'Check your email' : this.current === 'sign-up' ? 'Create a new account' : 'Log into your account');
	submitLabel = $derived(this.current === 'sign-up' ? 'Create account' : 'Log in');
	modeActionLabel = $derived(this.current === 'sign-up' ? 'I already have an account' : "I don't have an account yet");

	constructor() {
		watch(
			() => this.uniqueName,
			() => this.clearFieldErr('uniqueName')
		);
		watch(
			() => this.email,
			() => this.clearFieldErr('email')
		);
		watch(
			() => this.password,
			() => this.clearFieldErr('password')
		);
	}
	toggleMode() {
		this.setCurrentState(this.current === 'sign-up' ? 'login' : 'sign-up', true);
	}

	setCurrentState(mode: PageCurrentState, updateUrl: boolean) {
		if (this.isLoading || mode === this.current) {
			return;
		}

		this.clearErrs();
		this.current = mode;

		if (updateUrl !== false && browser) {
			const url = hrefForMode(mode, window.location.href);
			// eslint-disable-next-line svelte/no-navigation-without-resolve
			pushState(url, {});
		}
	}

	syncModeFromUrl(url: URL) {
		const stateFromUrl = url.searchParams.get('current');
		if (stateFromUrl === this.current) { return; }
		const normalized: PageCurrentState =
			stateFromUrl === 'confirmation-sent' ? stateFromUrl :
		    stateFromUrl === 'login'             ? stateFromUrl :
			'sign-up';
		this.setCurrentState(normalized)


	}

	async submit(event: SubmitEvent) {
		event.preventDefault();
		if (this.isLoading) {
			return;
		}

		this.clearErrs();

		const validationErrors = this.current === 'sign-up' ? this.validateRegistration() : this.validateLogin();

		if (validationErrors.any()) {
			this.setErrs(validationErrors.toJustMsgObj<Partial<PageErrs>>());
			return;
		}

		if (this.current === 'sign-up') { this.makeSignUpRequest(); }
		else if (this.current === 'login') { this.makeLoginRequest(); }
	}

	signInWithGoogle() {
		this.clearErrs();
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

	private async makeSignUpRequest() {
		this.isLoading = true;

		const response = await ApiAuth.POST<InvalidInputErr, AuthSuccess>('/sign-up', {
			email: this.email,
			uniqueName: this.uniqueName,
			password: this.password
		});

		this.isLoading = false;
		console.log(response);

		if (response.isOk) {
			return;
		}

		if (response.errKey === 'InvalidInput') {
			this.setErrs(toJustMsgObj<Partial<PageErrs>>(response));
			return;
		}

		this.errors.other = response.msg ?? 'Could not complete the sign up request. Please try again later';
	}

	private async makeLoginRequest() {
		this.isLoading = true;

		const response = await ApiAuth.POST<InvalidInputErr, AuthSuccess>('/login', {
			email: this.email,
			password: this.password
		})
		this.isLoading = false;
		console.log(response);

		if (response.isOk) {
			return;
		}

		if (response.errKey === 'InvalidInput') {
			this.setErrs(toJustMsgObj<Partial<PageErrs>>(response));
			return;
		}

		this.errors.other = response.msg ?? 'Could not complete the login request. Please try again later';
	}
	private setErrs(errors: Partial<PageErrs>) {
		this.errors = { ...emptyErrors(), ...errors };
	}

	private clearErrs() {
		this.errors = emptyErrors();
	}

	private clearFieldErr(key: keyof Omit<PageErrs, 'other'>) {
		this.errors[key] = '';
		this.errors.other = '';
	}
}
