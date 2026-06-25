<script lang="ts">
	interface Props {
		label: string;
		isLoading: boolean;
	}

	let { label, isLoading }: Props = $props();
</script>
<button
	class="
		flex min-h-14 w-full items-center justify-center overflow-hidden
		rounded-xl bg-brand-500 px-5 text-white
		transition-colors
		hover:bg-brand-600
		active:bg-brand-700
		focus-visible:outline-2
		focus-visible:outline-offset-2
		focus-visible:outline-brand-500
		disabled:bg-brand-400
		disabled:hover:bg-brand-400
	"
	type="submit"
	disabled={isLoading}
>
	{#if isLoading}
		<span class="auth-submit-loading" role="status">
			<span class="auth-submit-spinner" aria-hidden="true"></span>
			<span>Working</span>
		</span>
	{:else}
		<span class="auth-submit-label">
			{label}
		</span>
	{/if}
</button>

<style>
	.auth-submit-label {
		font-size: 1.25rem;
		font-weight: 500;
		letter-spacing: 0.075em;
		line-height: 1;
		transform-origin: center;
	}

	.auth-submit-loading {
		display: inline-flex;
		min-height: 1.5rem;
		align-items: center;
		justify-content: center;
		gap: 0.65rem;
		font-size: 1rem;
		font-weight: 800;
		line-height: 1;
		letter-spacing: 0.02em;
	}

	.auth-submit-spinner {
		width: 1.15rem;
		height: 1.15rem;
		border-radius: 9999px;
		border: 2px solid rgb(255 255 255 / 0.38);
		border-top-color: #fff;
		animation: auth-submit-spin 0.7s linear infinite;
	}

	@keyframes auth-submit-spin {
		to {
			transform: rotate(360deg);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.auth-submit-spinner {
			animation-duration: 1.4s;
		}
	}
</style>