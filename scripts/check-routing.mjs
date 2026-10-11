import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import vm from 'node:vm';

const source = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const routing = source.slice(source.indexOf('function routeName('), source.indexOf('function updateMotion('));
const pages = ['home', 'photos', 'contact'].map(name => ({ id: `${name}-view`, hidden: false }));
const navigation = [];
let opened = 0;
const location = {};
function address(path) {
  const url = new URL(path, 'https://harishkiru.dev');
  Object.assign(location, { pathname: url.pathname, search: url.search, hash: url.hash });
}
const context = vm.createContext({
  location, document: { querySelectorAll: selector => selector === '.view' ? pages : [] },
  history: {
    replaceState: (_, __, path) => { navigation.push(['replace', path]); address(path); },
    pushState: (_, __, path) => { navigation.push(['push', path]); address(path); },
  },
  window: { scrollTo() {} }, renderPhotos() {},
  $: () => ({ showModal: () => opened++ }), sessionReady: true, owner: false, manageRequested: false,
});
vm.runInContext(routing, context);
function load(path) { address(path); vm.runInContext("view(routeName(), 'replace')", context); }
function visible() { return pages.find(page => !page.hidden)?.id; }

load('/photos#home');
assert.equal(location.pathname, '/home'); assert.equal(location.hash, ''); assert.equal(visible(), 'home-view');
vm.runInContext("view('photos')", context);
assert.equal(location.pathname, '/photos'); assert.equal(visible(), 'photos-view');
assert.deepEqual(navigation.at(-1), ['push', '/photos']);
const count = navigation.length;
address('/home'); vm.runInContext("view(routeName(), 'none')", context);
assert.equal(visible(), 'home-view'); assert.equal(navigation.length, count);
load('/contact'); assert.equal(visible(), 'contact-view');
load('/#about'); assert.equal(location.pathname, '/contact'); assert.equal(location.hash, '');
load('/owner#photos'); assert.equal(location.pathname, '/owner'); assert.equal(visible(), 'photos-view'); assert.equal(opened, 1);
vm.runInContext("view('home')", context); assert.equal(location.pathname, '/home');
console.log('Verified clean navigation, legacy URL migration, direct routes, Back/Forward rendering, and owner entry.');
