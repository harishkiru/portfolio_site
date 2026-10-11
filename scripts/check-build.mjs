import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const source = await readFile(new URL('../app.ts', import.meta.url), 'utf8');
const ids = new Set([...html.matchAll(/\bid="([\w-]+)"/g)].map(match => match[1]));
for (const match of source.matchAll(/\$[^\n]*?\('#([\w-]+)'\)/g)) {
  assert(ids.has(match[1]), `Missing element #${match[1]}`);
}
for (const name of ['index.html', 'style.css', 'app.js', 'orbit-avatar.svg', 'orbit-avatar-purple.svg', 'THIRD-PARTY-NOTICES.txt']) {
  const original = await readFile(new URL(`../${name}`, import.meta.url));
  const built = await readFile(new URL(`../dist/${name}`, import.meta.url));
  assert(original.equals(built), `Outdated build asset: ${name}`);
}
assert(!html.includes('src/main.jsx'), 'Unused starter entry point must not be served');
console.log('Verified frontend element references and build assets.');
