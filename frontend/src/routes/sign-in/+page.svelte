<script lang="ts">
	import AuthFormAlert from './_c/AuthFormAlert.svelte';
	import AuthInlineAction from './_c/AuthInlineAction.svelte';
	import SignInEmailInput from './_c/SignInEmailInput.svelte';
	import AuthSubmitButton from './_c/AuthSubmitButton.svelte';
	import SignInPasswordInput from './_c/SignInPasswordInput.svelte';
	import SignInUniqueNameInput from './_c/SignInUniqueNameInput.svelte';
	import { AuthPageState } from './page-state.svelte';
	import { afterNavigate, onNavigate } from '$app/navigation';

	const pageState = new AuthPageState();

	onNavigate(({ to }) => {
		console.log(to)
		if (!to?.url) {
			return;
		}

		pageState.syncModeFromUrl(to.url);
	});
	
</script>

<main class="min-h-dvh bg-page text-ink">
	<section class="mx-auto grid min-h-dvh w-full grid-cols-2">
		<aside class="block border-r border-line bg-page" aria-hidden="true">
			<div class="auth-reserved-panel h-full w-full"></div>
		</aside>

		<section class="flex items-center justify-center px-6 py-18">
			<div class="w-full max-w-lg">
				<h1 class="mb-10 px-2 text-center text-nowrap text-[2.75rem] font-semibold leading-none tracking-[-0.02em] text-ink">
					{pageState.title}
				</h1>

				{#if pageState.current === 'confirmation-sent'}
					<div class="rounded-3xl border border-line bg-surface px-7 py-8 text-center">
						<div class="mx-auto mb-6 flex size-17 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
							<svg
								class="size-9"
								xmlns="http://www.w3.org/2000/svg"
								viewBox="0 0 24 24"
								fill="none"
								stroke="currentColor"
								stroke-width="1.5"
								aria-hidden="true"
							>
								<path
									d="M5.00035 7L3.78154 7.81253C2.90783 8.39501 2.47097 8.68625 2.23422 9.13041C1.99747 9.57457 1.99923 10.0966 2.00273 11.1406C2.00696 12.3975 2.01864 13.6782 2.05099 14.9741C2.12773 18.0487 2.16611 19.586 3.29651 20.7164C4.42691 21.8469 5.98497 21.8858 9.10108 21.9637C11.0397 22.0121 12.9611 22.0121 14.8996 21.9637C18.0158 21.8858 19.5738 21.8469 20.7042 20.7164C21.8346 19.586 21.873 18.0487 21.9497 14.9741C21.9821 13.6782 21.9937 12.3975 21.998 11.1406C22.0015 10.0966 22.0032 9.57456 21.7665 9.13041C21.5297 8.68625 21.0929 8.39501 20.2191 7.81253L19.0003 7"
									stroke-linejoin="round"
								/>
								<path
									d="M2 10L8.91302 14.1478C10.417 15.0502 11.169 15.5014 12 15.5014C12.831 15.5014 13.583 15.0502 15.087 14.1478L22 10"
									stroke-linejoin="round"
								/>
								<path
									d="M4.99998 12V6C4.99998 4.11438 4.99998 3.17157 5.58577 2.58579C6.17156 2 7.11437 2 8.99998 2H15C16.8856 2 17.8284 2 18.4142 2.58579C19 3.17157 19 4.11438 19 6V12"
								/>
								<path d="M10 10H14M10 6H14" stroke-linecap="round" stroke-linejoin="round" />
							</svg>
						</div>

						<p class="text-lg font-semibold text-ink">We sent you a confirmation email</p>

						<p class="mt-3 text-base leading-7 text-muted">
							We sent an email to
							<span class="font-semibold text-ink">{pageState.email}</span>. Follow the link in that email to finish creating your account.
						</p>
					</div>
				{:else}
					<form onsubmit={(event) => pageState.submit(event)} novalidate>
						<div class="auth-input-bay" data-mode={pageState.current}>
							<div class="auth-input-slot auth-unique" inert={pageState.current !== 'sign-up'}>
								<SignInUniqueNameInput bind:value={pageState.uniqueName} errMsg={pageState.errors.uniqueName} />
							</div>

							<div class="auth-input-slot auth-email">
								<SignInEmailInput bind:value={pageState.email} errMsg={pageState.errors.email} />
							</div>

							<div class="auth-input-slot auth-password">
								<SignInPasswordInput bind:value={pageState.password} errMsg={pageState.errors.password} />
							</div>
						</div>

						<AuthFormAlert msg={pageState.errors.other} />

						<AuthSubmitButton label={pageState.submitLabel} isLoading={pageState.isLoading} />
					</form>
				{/if}

				<div class="mt-4 flex h-10 items-center justify-center">
					<AuthInlineAction label={pageState.modeActionLabel} action={{ type: 'button', onclick: () => pageState.toggleCurrentState() }} />
				</div>

				{#if pageState.current !== 'confirmation-sent'}
					<div
						class="forgot-row flex h-9 items-center justify-center"
						data-visible={pageState.current === 'login' ? true : undefined}
						aria-hidden={pageState.current !== 'login'}
						inert={pageState.current !== 'login'}
					>
						<AuthInlineAction label="I forgot my password" action={{ type: 'link', href: '/password-reset' }} />
					</div>

					<div class="my-7 grid grid-cols-[1fr_auto_1fr] items-center gap-4 text-muted">
						<span class="h-px bg-line"></span>
						<span class="text-[0.68rem] font-semibold uppercase tracking-[0.22em]">or</span>
						<span class="h-px bg-line"></span>
					</div>

					<button
						class="flex min-h-13 w-full items-center justify-center gap-3 rounded-xl border border-line bg-surface px-5 text-base font-semibold text-ink transition-[background-color,border-color] hover:border-line-strong hover:bg-brand-50 active:bg-brand-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
						type="button"
						onclick={() => pageState.signInWithGoogle()}
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

						<span>Continue with Google</span>
					</button>
				{/if}
			</div>
		</section>
	</section>
</main>

<style>
	.auth-reserved-panel {
		background-color: var(--color-page);
	}

	.auth-input-bay {
		--slot-step: 6rem;
		position: relative;
		height: calc(var(--slot-step) * 3);
		margin-bottom: 0.25rem;
	}

	.auth-input-slot {
		position: absolute;
		left: 0;
		right: 0;
		top: 0;
		transition:
			top 180ms ease,
			opacity 140ms ease,
			visibility 0s linear 180ms;
	}

	.auth-unique {
		top: 0;
		opacity: 0;
		pointer-events: none;
		visibility: hidden;
	}

	.auth-email {
		top: calc(var(--slot-step) * 0.5);
	}

	.auth-password {
		top: calc(var(--slot-step) * 1.5);
	}

	.auth-input-bay[data-mode='registration'] .auth-unique {
		opacity: 1;
		pointer-events: auto;
		visibility: visible;
		transition:
			top 180ms ease,
			opacity 140ms ease,
			visibility 0s;
	}

	.auth-input-bay[data-mode='registration'] .auth-email {
		top: var(--slot-step);
	}

	.auth-input-bay[data-mode='registration'] .auth-password {
		top: calc(var(--slot-step) * 2);
	}

	.forgot-row {
		opacity: 0;
		transition: opacity 120ms ease;
	}

	.forgot-row[data-visible='true'] {
		opacity: 1;
	}

	@media (prefers-reduced-motion: reduce) {
		.auth-input-slot,
		.forgot-row {
			transition: none;
		}
	}
</style>
