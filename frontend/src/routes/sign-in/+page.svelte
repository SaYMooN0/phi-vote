<script lang="ts">
	import { ApiAuth } from '$lib/ts/backend';
	import { InvalidInputErrList, toJustMsgObj, type InvalidInputErr } from '$lib/ts/core';
	import { watch } from 'runed';
	import SignInEmailInput from './_c/SignInEmailInput.svelte';
	import SignInUniqueNameInput from './_c/SignInUniqueNameInput.svelte';
	import SignInPasswordInput from './_c/SignInPasswordInput.svelte';

	let email = $state('');
	let uniqueName = $state('');
	let password = $state('');
	watch(
		() => email,
		() => {
			errMsgsObj.email = '';
		}
	);

	let errMsgsObj = $state({ email: '', uniqueName: '', password: '', other: '' });

	function submit(event: SubmitEvent) {
		event.preventDefault();
		const eList = InvalidInputErrList.Empty()
			.ifOneOfAdd('email', [
				{ cond: email.includes(' '), msg: 'Email cannot contain spaces' },
				{ cond: !email.includes('@'), msg: 'Email must contain the @ symbol ' }
			])
			.ifOneOfAdd('uniqueName', [
				{ cond: uniqueName.includes(' '), msg: 'Unique name cannot contain spaces' },
				{ cond: uniqueName.length <= 3, msg: 'Unique name must be at least 3 characters long' },
				{ cond: uniqueName.length >= 100, msg: 'Unique name is too long' }
			])
			.ifOneOfAdd('password', [
				{ cond: uniqueName.includes(' '), msg: 'Password cannot contain spaces' },
				{ cond: password.length <= 8, msg: 'Password must be at least 8 characters long' },
				{ cond: password.length >= 200, msg: 'Password is a bit too long' }
			]);
		if (eList.any()) {
			errMsgsObj = eList.toJustMsgObj();
		} else {
			makeRequest();
		}
	}
	async function makeRequest() {
		const response = await ApiAuth.POST<InvalidInputErr, { healthMsg: string }>('/sign-up', { email, uniqueName, password });
		console.log(response);
		if (response.isOk) {
		} else if (response.errKey === 'InvalidInput') {
			errMsgsObj = toJustMsgObj(response);
		} else {
			errMsgsObj = { other: response.msg, email: '', uniqueName: '', password: '' };
		}
	}
	function signInWithGoogle() {
		console.log('sign in with google');
	}
</script>

<svelte:head>
	<title>Create account</title>
</svelte:head>

<main class="min-h-dvh bg-auth-paper text-auth-ink">
	<section
		class="
			mx-auto grid min-h-dvh w-full max-w-6xl
			grid-cols-1
			lg:grid-cols-[minmax(0,1fr)_minmax(24rem,32rem)]
		"
	>
		<aside
			class="
				hidden border-x border-auth-line bg-m-p-50
				lg:block
			"
			aria-hidden="true"
		></aside>

		<section class="flex items-center justify-center px-6 py-12 sm:px-8 lg:px-12">
			<div class="w-full max-w-md">
				<header class="mb-10">
					<p class="mb-3 text-sm font-semibold uppercase tracking-wider text-m-p-700">New account</p>

					<h1 class="text-5xl font-semibold tracking-tight text-auth-ink sm:text-6xl">Sign up</h1>

					<p class="mt-5 max-w-sm text-base leading-7 text-auth-muted">Create your profile with a unique name and secure password.</p>
				</header>

				<form class="space-y-5" onsubmit={submit} novalidate>
					<SignInUniqueNameInput bind:value={uniqueName} errMsg={errMsgsObj.uniqueName} />
					<SignInEmailInput bind:value={email} errMsg={errMsgsObj.email} />
					<SignInPasswordInput bind:value={password} errMsg={errMsgsObj.password} />
					<p>{errMsgsObj.other}</p>
					<button
						class="
							mt-2 flex min-h-13 w-full items-center justify-center rounded-xl
							bg-m-p-700 px-5 text-base font-semibold text-white
							transition-colors
							hover:bg-m-p-800
							active:bg-m-p-900
							focus-visible:outline-2
							focus-visible:outline-offset-2
							focus-visible:outline-m-p-700
						"
						type="submit"
					>
						Create account
					</button>
				</form>

				<a
					class="
						mx-auto mt-5 block w-fit text-sm font-medium text-auth-muted
						transition-colors
						hover:text-m-p-700
						focus-visible:outline-2
						focus-visible:outline-offset-2
						focus-visible:outline-m-p-700
					"
					href="/login"
				>
					I already have an account
				</a>

				<div class="my-7 grid grid-cols-[1fr_auto_1fr] items-center gap-4 text-auth-muted">
					<span class="h-px bg-auth-line"></span>
					<span class="text-xs font-semibold uppercase tracking-widest">or</span>
					<span class="h-px bg-auth-line"></span>
				</div>

				<button
					class="
						flex min-h-13 w-full items-center justify-center gap-3 rounded-xl
						border border-auth-line bg-auth-surface px-5 text-base font-semibold
						text-auth-ink transition-colors
						hover:border-auth-line-strong
						hover:bg-m-p-50
						active:bg-m-p-100
						focus-visible:outline-2
						focus-visible:outline-offset-2
						focus-visible:outline-m-p-700
					"
					type="button"
					onclick={signInWithGoogle}
				>
					<svg class="size-5" viewBox="0 0 24 24" aria-hidden="true">
						<path
							fill="#4285F4"
							d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
						/>
						<path
							fill="#34A853"
							d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
						/>
						<path
							fill="#FBBC05"
							d="M5.84 14.1c-.22-.66-.35-1.36-.35-2.1s.13-1.44.35-2.1V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l3.66-2.84z"
						/>
						<path
							fill="#EA4335"
							d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06L5.84 9.9C6.71 7.3 9.14 5.38 12 5.38z"
						/>
					</svg>

					<span>Sign in with Google</span>
				</button>
			</div>
		</section>
	</section>
</main>
