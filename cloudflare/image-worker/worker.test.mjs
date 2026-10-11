import test from 'node:test';
import assert from 'node:assert/strict';
import worker from './worker.mjs';

const id = 'aBcDeFgHiJkLmNoPqRsTuVwXyZ012345';
const image = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0]);
function environment() {
  const objects = new Map([[id, {
    size: image.length, httpMetadata: { contentType: 'image/png' }, body: image,
  }]]);
  const calls = [];
  return { objects, calls, IMAGES: {
    async get(key) { calls.push(['get', key]); return objects.get(key) ?? null; },
    async head(key) { calls.push(['head', key]); return objects.get(key) ?? null; },
  } };
}
function request(path, method = 'GET') {
  return new Request(`https://img.harishkiru.dev${path}`, { method });
}

test('an exact link serves an image with privacy headers and no login', async () => {
  const env = environment();
  const response = await worker.fetch(request(`/${id}`), env);
  assert.equal(response.status, 200);
  assert.deepEqual(new Uint8Array(await response.arrayBuffer()), image);
  assert.equal(response.headers.get('Content-Type'), 'image/png');
  assert.equal(response.headers.get('Cache-Control'), 'no-store');
  assert.equal(response.headers.get('Referrer-Policy'), 'no-referrer');
  assert.equal(response.headers.get('X-Robots-Tag'), 'noindex, nofollow, noarchive, noimageindex');
  assert.equal(response.headers.get('Set-Cookie'), null);
  assert.deepEqual(env.calls, [['get', id]]);
});

test('HEAD returns metadata without reading the image body', async () => {
  const env = environment();
  const response = await worker.fetch(request(`/${id}`, 'HEAD'), env);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('Content-Length'), String(image.length));
  assert.equal(await response.text(), '');
  assert.deepEqual(env.calls, [['head', id]]);
});

test('listings, query manipulation, other hosts and mutations cannot access R2', async () => {
  const env = environment();
  for (const path of ['/', '/upload', '/api/photos', '/short', '/../private', `/${id}?list=true`]) {
    assert.equal((await worker.fetch(request(path), env)).status, 404);
  }
  for (const method of ['POST', 'PUT', 'DELETE']) {
    assert.equal((await worker.fetch(request(`/${id}`, method), env)).status, 404);
  }
  assert.equal((await worker.fetch(new Request(`https://other.example/${id}`), env)).status, 404);
  assert.deepEqual(env.calls, []);
});

test('unknown, deleted or non-image objects cannot be served', async () => {
  const env = environment();
  env.objects.delete(id);
  assert.equal((await worker.fetch(request(`/${id}`), env)).status, 404);
  env.objects.set(id, { size: 20, httpMetadata: { contentType: 'text/html' }, body: '<script>bad</script>' });
  assert.equal((await worker.fetch(request(`/${id}`), env)).status, 404);
});

test('robots discourages indexing and storage failures reveal no details', async () => {
  const response = await worker.fetch(request('/robots.txt'), environment());
  assert.match(await response.text(), /Disallow: \/$/m);
  const failed = await worker.fetch(request(`/${id}`), { IMAGES: { async get() { throw Error('private details'); } } });
  assert.equal(failed.status, 503);
  assert.equal(await failed.text(), '');
});
