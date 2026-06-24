<script lang="ts">
	interface Props {
		label: string;
		action: { type: 'button'; onclick: () => void } | { type: 'link'; href: string };
	}

	let { label, action }: Props = $props();
</script>

{#if action.type === 'link'}
	<a class="auth-inline-action" href={action.href}>
		{label}
	</a>
{:else if action.type === 'button'}
	<button class="auth-inline-action" type="button" onclick={action.onclick}>
		{label}
	</button>
{/if}

<style>
	.auth-inline-action {
		display: block;
		width: fit-content;
		padding: 0.125rem 1rem;
		margin: 0.5rem auto 0;
		border: 0;
		border-radius: 0.375rem;
		background-color: transparent;
		color: var(--color-muted);
		font: inherit;
		font-size: 1rem;
		line-height: 1.5;
		text-align: center;
		text-decoration: underline;
		text-decoration-thickness: 0.06em;
		text-underline-offset: 0.18em;
		cursor: pointer;
		transition:
			background-color 0.12s ease,
			color 0.12s ease,
			transform 0.12s ease;
	}

	.auth-inline-action:hover {
		background-color: var(--secondary, var(--color-app-surface-soft));
		color: var(--primary, var(--color-brand-600));
	}

	.auth-inline-action:active {
		color: var(--primary-hov, var(--color-brand-700));
		transform: scale(0.98);
	}

	.auth-inline-action:focus-visible {
		outline: 2px solid var(--primary, var(--color-brand-600));
		outline-offset: 3px;
	}

	@media (prefers-reduced-motion: reduce) {
		.auth-inline-action {
			transition: none;
		}

		.auth-inline-action:active {
			transform: none;
		}
	}
</style>
