<script lang="ts">
	import { ApiAuth, ApiVoting, toCoreLikeErr, type BackendE } from '$lib/ts/backend';
	import { toast } from 'svelte-sonner';

	async function handleAuth() {
		const promise = ApiAuth.GET<BackendE, { healthMsg: string }>('/health').then((res) => {
			if (res.isOk) {
				return res.healthMsg;
			}
			const err = toCoreLikeErr(res);
			const errMsg = err.isBackendFetchErr ? err.msg : 'Unknown error';
			throw errMsg;
		});

		toast.promise(promise, {
			loading: 'Loading...',
			success: (msg) => msg,
			error: (msg) => msg as string
		});
	}

	async function handleVoting() {
		const response = await ApiVoting.GET('/health');
		console.log(response);
	}
</script>

<button
	onclick={handleAuth}
	class="bg-m-p m-10 hover:bg-m-p-hov cursor-pointer block text-white rounded transition duration-200 px-4"
>
	Auth
</button>

<button
	onclick={handleVoting}
	class="bg-m-p m-10 hover:bg-m-p-hov cursor-pointer block text-white rounded transition duration-200 px-4"
>
	Voting
</button>
