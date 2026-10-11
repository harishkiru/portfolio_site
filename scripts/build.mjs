import { mkdir, copyFile } from 'node:fs/promises';

await mkdir(new URL('../dist/', import.meta.url), { recursive: true });
for (const file of ['index.html', 'style.css', 'app.js', 'orbit-avatar.svg', 'orbit-avatar-purple.svg', 'THIRD-PARTY-NOTICES.txt']) {
  await copyFile(new URL(`../${file}`, import.meta.url), new URL(`../dist/${file}`, import.meta.url));
}
console.log('Portfolio built in dist/');
