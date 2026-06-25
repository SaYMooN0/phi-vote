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
			placeholder=" "
			aria-invalid={errMsg ? true : undefined}
			aria-describedby={`${inputName}-error`}
			class="input-field peer block w-full rounded-xl border border-line bg-field px-4 py-2.75 text-lg text-ink outline-none transition-[background-color,border-color]
		    hover:border-line-strong
		 	focus:border-brand-600 not-placeholder-shown:border-brand-600
			data-[error=true]:border-danger data-[error=true]:bg-danger-soft data-[error=true]:focus:shadow-[0_0_0_4px_var(--color-danger-soft)]"
		/>

		<span
			class="input-caption pointer-events-none absolute left-4 top-0 flex -translate-y-1/2 items-center gap-1.5 rounded-lg px-0.5 text-sm leading-none text-brand-600 transition-[top,color,font-size,background,box-shadow]
			[&_svg]:size-5 [&_svg]:shrink-0
			peer-placeholder-shown:top-1/2 peer-placeholder-shown:text-base peer-placeholder-shown:text-muted peer-focus:text-brand-600 peer-focus:top-0! peer-focus:text-sm!
			peer-data-[error=true]:text-danger!"
		>
			{@render icon()}
			<span>{fieldName}</span>
		</span>
	</span>

	<span
		id={`${inputName}-error`}
		aria-live="polite"
		class="mt-1.5 block h-12 overflow-hidden px-1 text-sm leading-5 text-danger empty:invisible"
		title={errMsg ?? ''}
	>
		{errMsg ?? ''}
	</span>
</label>

<style>
	.input-caption {
		background: transparent;
	}

	.input-field:focus + .input-caption,
	.input-field:not(:placeholder-shown) + .input-caption {
		background: var(--color-field);
		box-shadow: 0px -3px 7px 5px var(--color-field);
	}

	.input-field[data-error='true']:focus + .input-caption,
	.input-field[data-error='true']:not(:placeholder-shown) + .input-caption {
		background: var(--color-danger-soft);
	}

	.input-label input:-webkit-autofill,
	.input-label input:-webkit-autofill:hover,
	.input-label input:-webkit-autofill:focus,
	.input-label input:-webkit-autofill:active {
		-webkit-box-shadow: 0 0 0 1000px var(--color-field) inset !important;
		-webkit-text-fill-color: var(--color-ink) !important;
		transition: background-color 5000s ease-in-out 0s;
	}

	.input-label input[data-error='true']:-webkit-autofill,
	.input-label input[data-error='true']:-webkit-autofill:hover,
	.input-label input[data-error='true']:-webkit-autofill:focus,
	.input-label input[data-error='true']:-webkit-autofill:active {
		-webkit-box-shadow: 0 0 0 1000px var(--color-danger-soft) inset !important;
	}
</style>
