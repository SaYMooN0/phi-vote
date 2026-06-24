<script lang="ts">
	interface Props {
		msg: string;
	}

	let { msg = '' }: Props = $props();
</script>

<p
	role={msg ? 'alert' : undefined}
	aria-live="polite"
	data-visible={msg ? true : undefined}
	class="
		flex h-11 items-center rounded-xl border border-danger/25
		bg-danger-soft px-4 text-sm leading-5 text-danger
		opacity-0 transition-opacity
		data-[visible=true]:opacity-100
		mb-2
	"
>
	<span class="block max-h-10 overflow-hidden">
		{msg || ' '}
	</span>
</p>
