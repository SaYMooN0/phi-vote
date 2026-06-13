import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import fs from 'fs';
import path from 'path';


export default defineConfig({
	plugins: [
		sveltekit(),
		tailwindcss(),
		forbiddenFileStructurePlugin()
	],
	server: {
		proxy: {
			...createProxyEntry('/api/auth', 8180),
			...createProxyEntry('/api/voting', 8181)
		}
	}
});

const COMPONENTS_FOLDER_PREF = '_c';
const TS_FOLDER_PREF = '_ts';

function hasFolderPrefix(name: string, pref: string): boolean {
	return name === pref || name.startsWith(`${pref}_`);
}

function isComponentsFolderName(name: string): boolean {
	return hasFolderPrefix(name, COMPONENTS_FOLDER_PREF);
}

function isTSFolderName(name: string): boolean {
	return hasFolderPrefix(name, TS_FOLDER_PREF);
}

function forbiddenFileStructurePlugin() {
	return {
		name: 'validate-file-structure',
		buildStart() {
			console.log('Checking file structure...');
			checkFiles('src/routes', false, false);
		},
		handleHotUpdate({ file }: { file: string }) {
			if (file.includes(path.normalize('src/routes'))) {
				console.log(`Hot update: checking ${file}`);
				checkFiles('src/routes', false, false);
			}
		}
	};
};
function checkFiles(
	dir: string,
	insideComponentsFolder: boolean,
	insideTSFolder: boolean
) {
	const entries = fs.readdirSync(dir, { withFileTypes: true });

	for (const entry of entries) {
		const fullPath = path.join(dir, entry.name);

		if (/[а-яА-ЯёЁ]/.test(entry.name)) {
			throw new Error(`Cyrillic characters are not allowed: ${fullPath}`);
		}

		if (entry.isFile()) {
			if (insideComponentsFolder && entry.name.startsWith('+')) {
				throw new Error(
					`Files inside '${COMPONENTS_FOLDER_PREF}' folders cannot start with '+': ${fullPath}`
				);
			}

			if (insideTSFolder && !entry.name.endsWith('.ts')) {
				throw new Error(
					`Files inside '${TS_FOLDER_PREF}' folders must end with '.ts': ${fullPath}`
				);
			}
		}

		if (entry.isDirectory()) {
			const isComponentsFolder = isComponentsFolderName(entry.name);
			const isTSFolder = isTSFolderName(entry.name);

			if (
				insideComponentsFolder &&
				!isComponentsFolder &&
				!isTSFolder
			) {
				throw new Error(
					`Subfolders inside '${COMPONENTS_FOLDER_PREF}' folders must start with ` +
					`'${COMPONENTS_FOLDER_PREF}' or '${TS_FOLDER_PREF}': ${fullPath}`
				);
			}

			if (insideTSFolder && isComponentsFolder) {
				throw new Error(
					`'${TS_FOLDER_PREF}' folders cannot contain '${COMPONENTS_FOLDER_PREF}' folders: ${fullPath}`
				);
			}

			if (
				entry.name.startsWith('_') &&
				!isComponentsFolder &&
				!isTSFolder
			) {
				throw new Error(
					`Folders starting with '_' must use a valid prefix ` +
					`('${COMPONENTS_FOLDER_PREF}', '${COMPONENTS_FOLDER_PREF}_...', ` +
					`'${TS_FOLDER_PREF}' or '${TS_FOLDER_PREF}_...'): ${fullPath}`
				);
			}

			checkFiles(
				fullPath,
				insideComponentsFolder || isComponentsFolder,
				insideTSFolder || isTSFolder
			);
		}
	}
}

function createProxyEntry(
	basePath: string,
	port: number,
	customRewrite?: (path: string) => string
) {
	const basePathRegex = `^${basePath}`;

	return {
		[basePathRegex]: {
			target: `http://localhost:${port}/`,
			secure: false,
			changeOrigin: true,
			rewrite:
				customRewrite ??
				((path) => path.replace(new RegExp(basePathRegex), ''))
		}
	};
}
