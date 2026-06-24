<script lang="ts">
	import { ApiAuth, ApiVoting } from '$lib/ts/backend';
	import type { InvalidInputErr } from '$lib/ts/core';

	async function handleAuth() {
		const result = await ApiAuth.GET<InvalidInputErr, { healthMsg: string }>('');
		if (result.isOk) {
			console.log(result);
		} else {
			if (result.errKey === 'InvalidInput') {
				console.log('------');
				console.log(result);
			}
		}
	}

	async function handleVoting() {
		const response = await ApiVoting.GET('/health');
		console.log(response);
	}
</script>

<button onclick={handleAuth} class="bg-m-p m-10 hover:bg-m-p-hov cursor-pointer block text-white rounded transition duration-200 px-4"> Auth </button>

<button onclick={handleVoting} class="bg-m-p m-10 hover:bg-m-p-hov cursor-pointer block text-white rounded transition duration-200 px-4">
	Voting
</button>
