<script lang="ts">
	import { StrUtils } from '$lib/ts/utils/str-utils';
	import type { Snippet } from 'svelte';

	interface Props {
		type: 'text' | 'password' | 'email';
		fieldName: string;
		value: string;
		icon: Snippet;
		errMsg?: string;
	}

	let { type, fieldName, value = $bindable(''), icon, errMsg = undefined }: Props = $props();

	let inputName = StrUtils.rndStr(10);
</script>

<label class="input-label block w-full cursor-text select-none" for={inputName}>
	<span class="relative block">
		<input
			id={inputName}
			name={inputName}
			data-error={errMsg ? true : undefined}
			bind:value
			{type}
			required
			autocomplete="off"
			placeholder=" "
			aria-invalid={errMsg ? true : undefined}
			class="
				peer block w-full rounded-xl border border-auth-line bg-auth-field
				px-4 py-2.5 text-lg text-auth-ink outline-none
				transition-colors
				hover:border-auth-line-strong
				focus:border-m-p-700
				not-placeholder-shown:border-m-p-700
				data-[error=true]:border-auth-danger
				data-[error=true]:bg-auth-danger-soft
			"
		/>

		<span
			class="
				pointer-events-none absolute left-3 top-0 flex -translate-y-1/2
				items-center gap-1.5 bg-auth-field px-1 text-sm leading-none text-m-p-800
				transition-all
				[&_svg]:size-5 [&_svg]:shrink-0

				peer-placeholder-shown:top-1/2
				peer-placeholder-shown:text-base
				peer-placeholder-shown:text-auth-muted

				peer-focus:top-0!
				peer-focus:text-sm!
				peer-focus:text-m-p-800!

				peer-data-[error=true]:bg-auth-danger-soft
				peer-data-[error=true]:text-auth-danger!
			"
		>
			{@render icon()}
			<span>{fieldName}</span>
		</span>
	</span>

	<span
		id={`${inputName}-error`}
		aria-live="polite"
		class="
			mt-1.5 block min-h-10 px-1 text-sm leading-5
			text-auth-danger
			empty:invisible
		"
	>
		{errMsg ?? ''}
	</span>
</label>

<style>
	.input-label input:-webkit-autofill,
	.input-label input:-webkit-autofill:hover,
	.input-label input:-webkit-autofill:focus,
	.input-label input:-webkit-autofill:active {
		-webkit-box-shadow: 0 0 0 1000px var(--color-auth-field) inset !important;
		-webkit-text-fill-color: var(--color-auth-ink) !important;
		transition: background-color 5000s ease-in-out 0s;
	}

	.input-label input[data-error='true']:-webkit-autofill,
	.input-label input[data-error='true']:-webkit-autofill:hover,
	.input-label input[data-error='true']:-webkit-autofill:focus,
	.input-label input[data-error='true']:-webkit-autofill:active {
		-webkit-box-shadow: 0 0 0 1000px var(--color-auth-danger-soft) inset !important;
	}
</style>
