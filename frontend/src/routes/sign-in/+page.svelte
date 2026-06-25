<script lang="ts">
	import AuthFormAlert from './_c/AuthFormAlert.svelte';
	import AuthInlineAction from './_c/AuthInlineAction.svelte';
	import SignInEmailInput from './_c/SignInEmailInput.svelte';
	import AuthSubmitButton from './_c/AuthSubmitButton.svelte';
	import SignInPasswordInput from './_c/SignInPasswordInput.svelte';
	import SignInUniqueNameInput from './_c/SignInUniqueNameInput.svelte';
	import { AuthPageState } from './page-state.svelte';
	import { afterNavigate } from '$app/navigation';

	const pageState = new AuthPageState();

	afterNavigate(({ to }) => {
		if (!to?.url) {
			return;
		}

		pageState.syncModeFromUrl(to.url);
	});
</script>

<main class="min-h-dvh bg-page text-ink">
	<section class="mx-auto grid min-h-dvh w-full grid-cols-2">
		<aside class="border-r border-line bg-page block" aria-hidden="true">
			<div class="auth-reserved-panel h-full w-full"></div>
		</aside>

		<section class="flex items-center justify-center px-6 py-18">
			<div class="w-full max-w-lg">
				<h1 class="mb-10 px-2 text-center text-nowrap text-[2.75rem] fs font-semibold leading-none tracking-[-0.02em] text-ink">
					{pageState.title}
				</h1>
				{#if pageState.current === 'confirmation-sent'}

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

					<div class="mt-4 flex h-10 items-center justify-center">
						<AuthInlineAction label={pageState.modeActionLabel} action={{ type: 'button', onclick: () => pageState.toggleMode() }} />
					</div>

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
