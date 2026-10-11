const lastFmUsername = 'McGodFTW';

interface TripPhoto {
    id: string;
    name: string;
    blob?: Blob;
    url?: string;
    album: string;
    date: string;
    location?: string;
    caption: string;
    created: number;
}

function $<T extends HTMLElement = HTMLElement>(selector: string): T {
    const element = document.querySelector<T>(selector);
    if (!element) throw new Error(`Missing portfolio element: ${selector}`);
    return element;
}

interface PortfolioProject {
    name: string;
    url: string;
    description: string;
    category: string;
    stack: string[];
    theme: 'road' | 'night' | 'grow' | 'chef';
    coverLines: string[];
    symbol: string;
}
const portfolioProjects: PortfolioProject[] = [
    {
        name: 'ChatRoom',
        url: 'https://github.com/harishkiru/ChatRoom',
        description: 'Live chatroom to connect with friends, family and meet new people.',
        category: 'Messaging Platform',
        stack: ['Java', 'Jakarta EE', 'WebSockets', 'Maven'],
        theme: 'road',
        coverLines: ['Chat', 'Room.'],
        symbol: '☏'
    },
    {
        name: 'SpamDetection',
        url: 'https://github.com/harishkiru/SpamDetection',
        description: 'Unigram based spam classification with probabilities and model metrics.',
        category: 'Email Filtering',
        stack: ['Java', 'JAX-RS', 'JavaScript', 'Parcel'],
        theme: 'night',
        coverLines: ['Spam', 'Detection.'],
        symbol: '⌕'
    },
    {
        name: 'musicVisualized',
        url: 'https://github.com/harishkiru/musicVisualized',
        description: 'Explore listening habits, top artists, and tracks with Spotify and Last.fm data.',
        category: 'Spotify Analytics',
        stack: ['Flutter', 'Dart', 'Firebase', 'SQLite'],
        theme: 'grow',
        coverLines: ['Music', 'Visualized.'],
        symbol: '♫'
    },
    {
        name: 'HelloChef',
        url: 'https://github.com/harishkiru/HelloChef',
        description: 'Learn cooking skills with interactive lessons, quizzes, and gamified progress.',
        category: 'Learn to Cook',
        stack: ['Flutter', 'Dart', 'Supabase', 'Provider'],
        theme: 'chef',
        coverLines: ['Hello', 'Chef.'],
        symbol: '♨'
    }
];

interface PublicRepository {
    name: string;
    url: string;
    category: string;
    theme: PortfolioProject['theme'];
    symbol: string;
}
const publicRepositories: PublicRepository[] = [
    ...portfolioProjects,
    {
        name: 'Sit', url: 'https://github.com/harishkiru/Sit',
        category: 'Minecraft Plugin', theme: 'grow', symbol: 'grass-block'
    },
];

function projectLink(project: Pick<PublicRepository, 'name' | 'url'>, className: string): HTMLAnchorElement {
    const link = document.createElement('a');
    link.className = className;
    link.href = project.url;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.setAttribute('aria-label', `${project.name} on GitHub (opens in a new tab)`);
    return link;
}

function renderProjects() {
    const cards = $('#portfolio-projects');
    cards.replaceChildren();
    portfolioProjects.forEach(project => {
        const link = projectLink(project, 'collection project-card');
        const cover = document.createElement('div');
        cover.className = `cover project-cover ${project.theme}`;
        const category = document.createElement('small');
        category.textContent = project.category.toUpperCase();
        const symbol = document.createElement('span');
        symbol.className = 'project-symbol';
        symbol.textContent = project.symbol;
        symbol.setAttribute('aria-hidden', 'true');
        const coverTitle = document.createElement('strong');
        project.coverLines.forEach((line, index) => {
            if (index) coverTitle.append(document.createElement('br'));
            coverTitle.append(document.createTextNode(line));
        });
        const action = document.createElement('small');
        action.textContent = 'VIEW REPOSITORY ↗';
        const arrow = document.createElement('span');
        arrow.className = 'cover-arrow';
        arrow.textContent = '↗';
        arrow.setAttribute('aria-hidden', 'true');
        cover.append(category, symbol, coverTitle, action, arrow);
        const title = document.createElement('h3');
        title.textContent = project.name;
        const description = document.createElement('p');
        description.textContent = project.description;
        const details = document.createElement('div');
        details.className = 'project-details';
        const stack = document.createElement('ul');
        stack.className = 'project-stack';
        stack.setAttribute('aria-label', `${project.name} technology stack`);
        project.stack.forEach(technology => {
            const badge = document.createElement('li');
            badge.textContent = technology;
            stack.append(badge);
        });
        details.append(title, description, stack);
        link.append(cover, details);
        cards.append(link);
    });
}

function minecraftBlockIcon(): SVGSVGElement {
    const namespace = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(namespace, 'svg');
    svg.setAttribute('viewBox', '0 0 32 36');
    svg.setAttribute('width', '25');
    svg.setAttribute('height', '28');
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '1.8');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.setAttribute('stroke-linecap', 'round');
    const shapes = [
        'M2 10 16 2 30 10V26L16 34 2 26Z',
        'M2 10 16 18 30 10M16 18V34',
        'M2 15 6 17V20L10 22V20L16 24 22 20V22L26 20V17L30 15'
    ];
    shapes.forEach(shape => {
        const path = document.createElementNS(namespace, 'path');
        path.setAttribute('d', shape);
        svg.append(path);
    });
    return svg;
}

function renderRepositoryLibrary() {
    const library = $('#project-library');
    library.replaceChildren();
    publicRepositories.forEach(repository => {
        const entry = projectLink(repository, 'library-card');
        const icon = document.createElement('i');
        icon.className = repository.theme;
        if (repository.symbol === 'grass-block') {
            icon.append(minecraftBlockIcon());
        } else {
            icon.textContent = repository.symbol;
        }
        icon.setAttribute('aria-hidden', 'true');
        const text = document.createElement('span');
        text.append(document.createTextNode(repository.name));
        const subtext = document.createElement('small');
        subtext.textContent = repository.category;
        text.append(subtext);
        entry.append(icon, text);
        library.append(entry);
    });
}

renderProjects();
renderRepositoryLibrary();

let photos: TripPhoto[] = [];
let owner = false;
let csrfToken = '';
let csrfHeader = 'X-CSRF-TOKEN';
let sessionReady = false;
let manageRequested = false;
let localPhotos: TripPhoto[] = [];
let db: IDBDatabase | undefined;
let selectedFiles: File[] = [];
let activePhoto: TripPhoto | null = null;
let photoUrls: string[] = [];
let adventureUrl: string | null = null;
let featuredAdventure: TripPhoto | undefined;
let toastTimer: ReturnType<typeof setTimeout> | undefined;
const motionQuery = matchMedia('(prefers-reduced-motion: reduce)');
let paused = motionQuery.matches;

function toast(message: string) {
    $('#toast').textContent = message;
    $('#toast').classList.add('visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => $('#toast').classList.remove('visible'), 6000)
}

function routeName() {
    const legacy = location.hash.slice(1);
    if (location.pathname === '/owner' && (!legacy || legacy === 'photos')) return 'manage';
    if (['home', 'photos', 'contact', 'about', 'manage'].includes(legacy)) return legacy;
    return location.pathname.slice(1).replace(/\/$/, '') || 'home';
}

function view(name: string | undefined, navigation: 'push' | 'replace' | 'none' = 'push') {
    const ownerPage = name === 'manage' || name === 'owner';
    manageRequested = ownerPage;
    if (ownerPage) {
        name = 'photos';
        if (sessionReady && !owner) $<HTMLDialogElement>('#owner-dialog').showModal();
    }
    if (name === 'about') name = 'contact';
    if (!name || !['home', 'photos', 'contact'].includes(name)) name = 'home';
    document.querySelectorAll<HTMLElement>('.view').forEach(s => s.hidden = s.id !== name + '-view');
    document.querySelectorAll<HTMLElement>('[data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === name));
    const path = ownerPage ? '/owner' : '/' + name;
    if (navigation !== 'none' && location.pathname + location.search + location.hash !== path) {
        if (navigation === 'replace') history.replaceState(null, '', path);
        else history.pushState(null, '', path);
    }
    if (name === 'photos') renderPhotos();
    window.scrollTo({top: 0, behavior: 'instant'})
}

function updateMotion() {
    document.body.classList.toggle('paused', paused);
    $('#toggle-motion').textContent = paused ? '▷' : 'Ⅱ';
    $('#toggle-motion').setAttribute('aria-label', paused ? 'Resume background animation' : 'Pause background animation')
}

$('#toggle-motion').addEventListener('click', () => {
    paused = !paused;
    updateMotion()
});
motionQuery.addEventListener('change', e => {
    paused = e.matches;
    updateMotion()
});
updateMotion();
function random(min: number, max: number) {
    return Math.random() * (max - min) + min
}

function renderStars() {
    const sky = $('#stars');
    sky.replaceChildren();
    const count = Math.ceil(innerWidth * innerHeight / 4000);
    for (let i = 0; i < count; i++) {
        const star = document.createElement('span'), size = Math.random() < .5 ? 1 : 2;
        star.className = 'star';
        star.style.cssText = `left:${random(0, 99)}%;top:${random(0, 99)}%;opacity:${random(.5, 1)};width:${size}px;height:${size}px`;
        if (Math.random() < .2) star.style.animation = `twinkle${Math.floor(random(1, 5))} 5s infinite`;
        sky.append(star);
    }
}

renderStars();
let resizeTimer: ReturnType<typeof setTimeout> | undefined;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(renderStars, 150)
});
/*
Pure CSS Shooting Star Animation Effect Copyright (c) 2021 by Delroy Prithvi (https://codepen.io/delroyprithvi/pen/LYyJROR)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

for (let i = 0; i < 4; i++) {
    const shootingstar = document.createElement('span');
    shootingstar.className = 'shootingstar';

    function position() {
        if (Math.random() < .75) {
            shootingstar.style.top = '-4px';
            shootingstar.style.right = `${random(0, 90)}%`
        } else {
            shootingstar.style.top = `${random(0, 50)}%`;
            shootingstar.style.right = '-4px'
        }
    }

    position();
    shootingstar.style.boxShadow = '0 0 0 4px rgba(255,255,255,.1),0 0 0 8px rgba(255,255,255,.1),0 0 20px rgba(255,255,255,.1)';
    shootingstar.style.animationDuration = `${Math.floor(random(3, 6))}s`;
    shootingstar.style.animationDelay = `${Math.floor(random(0, 7))}s`;
    $('.cosmos').append(shootingstar);
    shootingstar.addEventListener('animationend', () => {
        position();
        shootingstar.style.animation = 'none';
        void shootingstar.offsetWidth;
        shootingstar.style.animation = '';
        shootingstar.style.animationDuration = `${Math.floor(random(3, 7))}s`
    });
}
document.querySelectorAll<HTMLElement>('[data-view]').forEach(b => b.addEventListener('click', () => view(b.dataset.view)));
function latestAdventure(items: TripPhoto[]): TripPhoto | undefined {
    const tripTime = (photo: TripPhoto) => photo.date
        ? new Date(photo.date + 'T12:00:00').getTime()
        : photo.created;
    return [...items].sort((a, b) => tripTime(b) - tripTime(a) || b.created - a.created || a.id.localeCompare(b.id))[0];
}

function photoText(element: HTMLElement, value: string) {
    element.replaceChildren();
    value.split(/(\p{Regional_Indicator}{2})/u).forEach(text => {
        if (/^\p{Regional_Indicator}{2}$/u.test(text)) {
            const emoji = document.createElement('span');
            emoji.className = 'flag-emoji';
            emoji.textContent = text;
            emoji.setAttribute('role', 'img');
            const region = [...text].map(character => String.fromCharCode(character.codePointAt(0)! - 0x1F1E6 + 65)).join('');
            const regionName = new Intl.DisplayNames(['en'], {type: 'region'}).of(region) || region;
            emoji.setAttribute('aria-label', `${regionName} flag`);
            element.append(emoji);
        } else element.append(document.createTextNode(text));
    });
}

function renderAdventure() {
    if (adventureUrl) {
        URL.revokeObjectURL(adventureUrl);
        adventureUrl = null;
    }
    featuredAdventure = latestAdventure(photos);
    const art = $('#adventure-art');
    art.replaceChildren();
    art.classList.toggle('has-photo', !!featuredAdventure);
    if (!featuredAdventure) {
        const icon = document.createElement('span');
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = '▧';
        art.append(icon);
        $('#adventure-label').textContent = 'YOUR NEXT CHAPTER';
        $('#adventure-title').textContent = 'Somewhere worth remembering.';
        $('#adventure-meta').textContent = 'Your travel journal starts here.';
        $('#adventure-caption').textContent = owner ? 'Add a picture to give your latest adventure a place in the spotlight.' : 'The next adventure is still to come.';
        $('#view-adventure').textContent = owner ? 'Add your first trip ↗' : 'Explore the gallery ↗';
        return;
    }
    const photo = featuredAdventure;
    const image = document.createElement('img');
    adventureUrl = photoSource(photo);
    image.src = adventureUrl;
    image.alt = photo.caption || `${photo.album} — ${photo.name}`;
    art.append(image);
    $('#adventure-label').textContent = 'FROM THE TRAVEL JOURNAL';
    photoText($('#adventure-title'), photo.album);
    photoText($('#adventure-meta'), [photo.location, photo.date ? dateText(photo.date) : 'Date not added'].filter(Boolean).join(' · '));
    photoText($('#adventure-caption'), photo.caption || 'A moment worth keeping.');
    $('#view-adventure').textContent = 'View this trip ↗';
}

$('#view-adventure').addEventListener('click', () => {
    const photo = featuredAdventure;
    view('photos');
    if (photo) {
        $<HTMLSelectElement>('#album-filter').value = photo.album;
        renderPhotos();
        $('#photo-grid').scrollIntoView({behavior: paused ? 'instant' : 'smooth', block: 'start'});
    } else if (owner) {
        pickFiles();
    }
});

function openDb(): Promise<IDBDatabase> {
    return new Promise<IDBDatabase>((resolve, reject) => {
        const r = indexedDB.open('harish-trip-gallery', 1);
        r.onupgradeneeded = () => r.result.createObjectStore('photos', {keyPath: 'id'});
        r.onsuccess = () => resolve(r.result);
        r.onerror = () => reject(r.error)
    })
}

function transaction<T = undefined>(mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest<T> | void): Promise<T | undefined> {
    return new Promise<T | undefined>((resolve, reject) => {
        if (!db) {
            reject(new Error('Gallery storage is not ready'));
            return;
        }
        const tx = db.transaction('photos', mode);
        let request: IDBRequest<T> | void;
        try {
            request = action(tx.objectStore('photos'))
        } catch (error) {
            reject(error);
            return
        }
        tx.oncomplete = () => resolve(request?.result);
        tx.onerror = () => reject(tx.error);
        tx.onabort = () => reject(tx.error || new Error('Storage failed'))
    })
}

function albums() {
    const current = $<HTMLSelectElement>('#album-filter').value;
    $<HTMLSelectElement>('#album-filter').replaceChildren(new Option('All', 'all'));
    [...new Set(photos.map(p => p.album))].sort().forEach(album => $<HTMLSelectElement>('#album-filter').add(new Option(album, album)));
    if ([...$<HTMLSelectElement>('#album-filter').options].some(o => o.value === current)) $<HTMLSelectElement>('#album-filter').value = current
}

let activeAlbumOption = 0;
function highlightAlbum(index: number) {
    const choices = [...$('#album-listbox').querySelectorAll<HTMLElement>('[role="option"]')];
    if (!choices.length) return;
    activeAlbumOption = Math.max(0, Math.min(index, choices.length - 1));
    choices.forEach((choice, position) => choice.classList.toggle('is-active', position === activeAlbumOption));
    $('#album-trigger').setAttribute('aria-activedescendant', choices[activeAlbumOption].id);
    choices[activeAlbumOption].scrollIntoView({block: 'nearest'});
}

function setAlbumOpen(open: boolean) {
    $('#album-listbox').hidden = !open;
    $('#album-trigger').setAttribute('aria-expanded', String(open));
    if (open) highlightAlbum($<HTMLSelectElement>('#album-filter').selectedIndex);
    else $('#album-trigger').removeAttribute('aria-activedescendant');
}

function chooseAlbum(index: number) {
    $<HTMLSelectElement>('#album-filter').selectedIndex = index;
    setAlbumOpen(false);
    $<HTMLSelectElement>('#album-filter').dispatchEvent(new Event('change'));
    $('#album-trigger').focus();
}

function renderAlbumPicker() {
    const select = $<HTMLSelectElement>('#album-filter');
    photoText($('#album-value'), select.selectedOptions[0]?.textContent || 'All');
    const list = $('#album-listbox');
    list.replaceChildren();
    [...select.options].forEach((option, index) => {
        const choice = document.createElement('button');
        choice.type = 'button'; choice.className = 'album-option'; choice.id = `album-choice-${index}`;
        choice.setAttribute('role', 'option'); choice.setAttribute('aria-selected', String(option.selected));
        choice.tabIndex = -1;
        photoText(choice, option.textContent || '');
        choice.addEventListener('mousedown', event => event.preventDefault());
        choice.addEventListener('pointermove', () => highlightAlbum(index));
        choice.addEventListener('click', () => chooseAlbum(index));
        list.append(choice);
    });
    setAlbumOpen(false);
}

$('#album-trigger').addEventListener('click', () => setAlbumOpen($('#album-listbox').hidden !== false));
$('#album-trigger').addEventListener('keydown', event => {
    const open = !$('#album-listbox').hidden;
    const count = $<HTMLSelectElement>('#album-filter').options.length;
    if (event.key === 'Escape') { setAlbumOpen(false); return; }
    if (['Enter', ' '].includes(event.key)) {
        event.preventDefault();
        if (open) chooseAlbum(activeAlbumOption); else setAlbumOpen(true);
    } else if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
        event.preventDefault();
        if (!open) setAlbumOpen(true);
        if (event.key === 'Home') highlightAlbum(0);
        else if (event.key === 'End') highlightAlbum(count - 1);
        else if (open) highlightAlbum((activeAlbumOption + (event.key === 'ArrowDown' ? 1 : -1) + count) % count);
    }
});
document.addEventListener('pointerdown', event => {
    if (event.target instanceof Element && !event.target.closest('.collection-select')) setAlbumOpen(false);
});
$('#album-trigger').addEventListener('blur', () => setAlbumOpen(false));

function dateText(date: string) {
    return date ? new Date(date + 'T12:00:00').toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    }) : 'A moment worth keeping'
}

function photoSource(photo: TripPhoto): string {
    if (photo.url) return photo.url;
    if (photo.blob) return URL.createObjectURL(photo.blob);
    throw new Error('Photo image is unavailable');
}

function updateOwnerControls() {
    document.querySelectorAll<HTMLElement>('[data-owner]').forEach(element => element.hidden = !owner);
    $('#import-photos').hidden = !owner || !localPhotos.length;
    $('.storage-note').textContent = owner
        ? 'Owner mode · Pictures you publish are visible to visitors.'
        : 'A few places, people, and moments worth keeping.';
    if (!owner) $('#delete-confirm').hidden = true;
    renderAdventure();
}

async function loadSession() {
    try {
        const response = await fetch('/api/session', { cache: 'no-store' });
        if (!response.ok) throw new Error('Session unavailable');
        const session: { owner: boolean; csrfToken: string; csrfHeader: string } = await response.json();
        owner = session.owner === true;
        csrfToken = session.csrfToken; csrfHeader = session.csrfHeader;
    } catch { owner = false; csrfToken = ''; }
    sessionReady = true;
    updateOwnerControls();
}

async function ownerRequest<T = undefined>(url: string, options: RequestInit): Promise<T> {
    if (!owner || !csrfToken) throw new Error('Owner sign-in required');
    const headers = new Headers(options.headers);
    headers.set(csrfHeader, csrfToken);
    const response = await fetch(url, { ...options, headers });
    if (response.status === 401 || response.status === 403) {
        await loadSession();
        throw new Error('Please sign in again');
    }
    if (!response.ok) throw new Error('Gallery request failed');
    return response.status === 204 ? undefined as T : await response.json() as T;
}

async function loadGallery() {
    try {
        const response = await fetch('/api/photos', { cache: 'no-store' });
        if (!response.ok) throw new Error('Gallery unavailable');
        photos = await response.json() as TripPhoto[];
        albums(); renderPhotos();
    } catch {
        $('.storage-note').textContent = 'The gallery is temporarily unavailable. Please try again later.';
    }
}

$('#owner-form').addEventListener('submit', async event => {
    event.preventDefault();
    const button = $<HTMLButtonElement>('#owner-submit');
    button.disabled = true;
    $('#owner-error').hidden = true;
    try {
        await loadSession();
        if (!csrfToken) throw new Error('Sign-in service unavailable');
        const response = await fetch('/api/login', {
            method: 'POST', headers: { [csrfHeader]: csrfToken },
            body: new URLSearchParams({ username: $<HTMLInputElement>('#owner-username').value,
                password: $<HTMLInputElement>('#owner-password').value })
        });
        if (!response.ok) throw new Error('Sign-in failed');
        $<HTMLInputElement>('#owner-password').value = '';
        await loadSession();
        if (!owner) throw new Error('Sign-in failed');
        $<HTMLDialogElement>('#owner-dialog').close();
        await loadGallery();
        toast('Signed in. Your gallery is ready to manage.');
    } catch {
        $('#owner-error').textContent = 'Could not sign in. Check your username and password and try again.';
        $('#owner-error').hidden = false;
    } finally { button.disabled = false; }
});

$('#owner-logout').addEventListener('click', async () => {
    try {
        await ownerRequest('/api/logout', { method: 'POST' });
        owner = false;
        $<HTMLDialogElement>('#upload-dialog').close();
        $<HTMLDialogElement>('#photo-dialog').close();
        await loadSession();
        toast('Signed out.');
    } catch { toast('Could not sign out. Please try again.'); }
});

const importedLocalIds = new Set<string>();
try {
    const saved: unknown = JSON.parse(localStorage.getItem('harish-published-photos') || '[]');
    if (Array.isArray(saved)) saved.forEach(id => { if (typeof id === 'string') importedLocalIds.add(id); });
} catch {  }
$('#import-photos').addEventListener('click', async () => {
    if (!owner) return;
    const button = $<HTMLButtonElement>('#import-photos');
    button.disabled = true;
    let published = 0;
    try {
        for (const photo of localPhotos) {
            if (!photo.blob || importedLocalIds.has(photo.id)) continue;
            const form = new FormData();
            form.append('file', photo.blob, photo.name); form.append('album', photo.album);
            form.append('date', photo.date); form.append('location', photo.location || '');
            form.append('caption', photo.caption);
            await ownerRequest('/api/photos', { method: 'POST', body: form });
            importedLocalIds.add(photo.id); published++;
            try { localStorage.setItem('harish-published-photos', JSON.stringify([...importedLocalIds])); } catch {}
        }
        toast(`${published} saved ${published === 1 ? 'picture' : 'pictures'} published. Your browser originals are kept.`);
    } catch { toast('Some saved pictures could not be published. Sign in and try again.'); }
    finally {
        localPhotos = localPhotos.filter(photo => !importedLocalIds.has(photo.id));
        updateOwnerControls(); await loadGallery(); button.disabled = false;
    }
});

function renderPhotos() {
    renderAlbumPicker();
    renderAdventure();
    photoUrls.forEach(URL.revokeObjectURL);
    photoUrls = [];
    $('#photo-grid').replaceChildren();
    const album = $<HTMLSelectElement>('#album-filter').value;
    const filtered = photos.filter(p => album === 'all' || p.album === album);
    filtered.forEach(p => {
        const card = document.createElement('button');
        card.className = 'photo-card';
        card.setAttribute('aria-label', `Open ${p.caption || p.name} from ${p.album}`);
        const img = document.createElement('img');
        img.src = photoSource(p);
        photoUrls.push(img.src);
        img.alt = p.caption || p.album + ' — ' + p.name;
        img.loading = 'lazy';
        const info = document.createElement('div');
        info.className = 'photo-info';
        const title = document.createElement('h3');
        photoText(title, p.album);
        const caption = document.createElement('p');
        photoText(caption, p.caption || dateText(p.date));
        info.append(title, caption);
        card.append(img, info);
        card.addEventListener('click', () => openPhoto(p));
        $('#photo-grid').append(card)
    });
    $('#photo-count').textContent = filtered.length + (filtered.length === 1 ? ' picture' : ' pictures');
    $('#empty-gallery').hidden = filtered.length > 0;
    $('#empty-gallery h2').textContent = photos.length ? 'No pictures match this view.' : 'Your world, frame by frame.';
    $('#empty-gallery p').textContent = photos.length ? 'Try a different collection.' : 'Your gallery is waiting for its first adventure.'
}

$<HTMLSelectElement>('#album-filter').addEventListener('change', renderPhotos);

function pickFiles() {
    if (!owner) return;
    view('photos');
    $<HTMLInputElement>('#file-input').click()
}

['#upload-button', '#browse'].forEach(s => $(s).addEventListener('click', pickFiles));

function stageFiles(files: FileList | null | undefined) {
    if (!owner) return;
    if (!files?.length) return;
    selectedFiles = Array.from(files).filter(f => ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(f.type) && f.size <= 20 * 1024 * 1024);
    if (files.length !== selectedFiles.length) toast('Some files were skipped. Use supported pictures under 20 MB.');
    if (!selectedFiles.length) return;
    $('#selected-count').textContent = selectedFiles.length + (selectedFiles.length === 1 ? ' picture ready to add.' : ' pictures ready to add.');
    $<HTMLInputElement>('#album-name').value = '';
    $<HTMLInputElement>('#trip-date').value = '';
    $<HTMLInputElement>('#trip-location').value = '';
    $<HTMLDialogElement>('#upload-dialog').showModal();
    $<HTMLInputElement>('#album-name').focus()
}

$<HTMLInputElement>('#file-input').addEventListener('change', () => {
    const input = $<HTMLInputElement>('#file-input');
    stageFiles(input.files);
    input.value = ''
});
const drop = $('#dropzone');
(['dragenter', 'dragover'] as const).forEach(name => drop.addEventListener(name, e => {
    e.preventDefault();
    drop.classList.add('dragover')
}));
(['dragleave', 'drop'] as const).forEach(name => drop.addEventListener(name, e => {
    e.preventDefault();
    drop.classList.remove('dragover')
}));
drop.addEventListener('drop', e => stageFiles(e.dataTransfer?.files));
(['dragover', 'drop'] as const).forEach(name => window.addEventListener(name, e => {
    if ([...(e.dataTransfer?.types ?? [])].includes('Files')) e.preventDefault()
}));

async function validImage(file: File) {
    const img = new Image(), url = URL.createObjectURL(file);
    try {
        await new Promise((resolve, reject) => {
            img.onload = resolve;
            img.onerror = reject;
            img.src = url
        })
    } finally {
        URL.revokeObjectURL(url)
    }
}

$('#upload-form').addEventListener('submit', async e => {
    e.preventDefault();
    if (!owner) return;
    const album = $<HTMLInputElement>('#album-name').value.trim();
    if (!album) {
        $<HTMLInputElement>('#album-name').focus();
        return
    }
    const button = $<HTMLButtonElement>('#save-photos');
    button.disabled = true;
    button.textContent = 'Saving…';
    try {
        const added: TripPhoto[] = [];
        for (const file of selectedFiles) {
            await validImage(file);
            const form = new FormData();
            form.append('file', file); form.append('album', album);
            form.append('date', $<HTMLInputElement>('#trip-date').value);
            form.append('location', $<HTMLInputElement>('#trip-location').value.trim());
            added.push(await ownerRequest<TripPhoto>('/api/photos', { method: 'POST', body: form }));
        }
        await loadGallery();
        $<HTMLSelectElement>('#album-filter').value = 'all';
        renderPhotos();
        $<HTMLDialogElement>('#upload-dialog').close();
        selectedFiles = [];
        toast(added.length + ' memories published to your gallery.')
    } catch {
        await loadGallery();
        toast('Some pictures could not be published. Check your connection and sign-in, then try again.')
    } finally {
        button.disabled = false;
        button.textContent = 'Save to my gallery ↗'
    }
});

function openPhoto(p: TripPhoto) {
    activePhoto = p;
    $<HTMLImageElement>('#full-photo').src = photoSource(p);
    $<HTMLImageElement>('#full-photo').alt = p.caption || p.name;
    photoText($('#full-title'), p.album);
    $('#full-date').textContent = dateText(p.date);
    $<HTMLInputElement>('#caption').value = p.caption;
    $<HTMLInputElement>('#edit-photo-date').value = p.date;
    photoText($('#public-caption'), p.caption);
    $('#public-caption').hidden = !p.caption;
    $<HTMLAnchorElement>('#download-photo').href = $<HTMLImageElement>('#full-photo').src;
    $<HTMLAnchorElement>('#download-photo').download = p.name;
    $('#delete-confirm').hidden = true;
    $<HTMLDialogElement>('#photo-dialog').showModal();
}

$<HTMLDialogElement>('#photo-dialog').addEventListener('close', () => {
    URL.revokeObjectURL($<HTMLImageElement>('#full-photo').src);
    $<HTMLImageElement>('#full-photo').removeAttribute('src');
    activePhoto = null
});
$('#save-caption').addEventListener('click', async () => {
    if (!activePhoto || !owner) return;
    const dateInput = $<HTMLInputElement>('#edit-photo-date');
    if (!dateInput.reportValidity()) return;
    const button = $<HTMLButtonElement>('#save-caption');
    button.disabled = true;
    button.textContent = 'Saving…';
    try {
        const updated = await ownerRequest<TripPhoto>(`/api/photos/${activePhoto.id}`, {
            method: 'PATCH', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({caption: $<HTMLInputElement>('#caption').value.trim(), date: dateInput.value})
        });
        photos = photos.map(p => p.id === updated.id ? updated : p);
        renderPhotos();
        $<HTMLDialogElement>('#photo-dialog').close();
        toast('Photo details saved.')
    } catch {
        toast('Could not save the changes. Check the date and your sign-in, then try again.')
    } finally {
        button.disabled = false;
        button.textContent = 'Save changes';
    }
});
$('#delete-photo').addEventListener('click', () => { if (owner) $('#delete-confirm').hidden = false; });
$('#cancel-delete').addEventListener('click', () => $('#delete-confirm').hidden = true);
$('#confirm-delete').addEventListener('click', async () => {
    if (!activePhoto || !owner) return;
    try {
        const removedId = activePhoto.id;
        await ownerRequest(`/api/photos/${removedId}`, { method: 'DELETE' });
        photos = photos.filter(p => p.id !== removedId);
        albums();
        renderPhotos();
        $<HTMLDialogElement>('#photo-dialog').close();
        toast('Picture removed from this gallery.')
    } catch {
        toast('Could not remove this picture. Please try again.')
    }
});
document.querySelectorAll<HTMLElement>('.close').forEach(b => b.addEventListener('click', () => b.closest('dialog')?.close()));
document.querySelectorAll<HTMLDialogElement>('dialog').forEach(d => d.addEventListener('click', e => {
    const r = d.getBoundingClientRect();
    if (e.target === d && (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)) d.close()
}));
$('#year').textContent = String(new Date().getFullYear());

interface RecentTrack {
    name: string;
    artist?: { '#text'?: string };
    album?: { '#text'?: string };
    url?: string;
    image?: { '#text'?: string; size?: string }[];
    date?: { uts?: string };
    '@attr'?: { nowplaying?: string };
}
interface LastFmResponse {
    username: string;
    track: {
        name: string; artist: string; album: string; url: string;
        coverUrl: string; playedAt: number | null; nowPlaying: boolean;
    } | null;
}
let recentTrack: RecentTrack | undefined;
let lastFmConfirmedAt = 0;
let lastFmFetching = false;
let lastFmUnavailable = false;

function relativeListenTime(timestamp: number, now = Date.now()): string {
    const minutes = Math.floor(Math.max(0, now - timestamp) / 60000);
    if (minutes < 1) return 'Just listened';
    if (minutes < 60) return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`;
    const days = Math.floor(hours / 24);
    return `${days} ${days === 1 ? 'day' : 'days'} ago`;
}

function refreshListeningTime() {
    const status = $('#lastfm-status');
    const live = !lastFmUnavailable && recentTrack?.['@attr']?.nowplaying === 'true'
        && Date.now() - lastFmConfirmedAt < 120000;
    status.classList.toggle('is-live', live);
    if (lastFmUnavailable) status.textContent = 'Last.fm temporarily unavailable';
    else if (live) status.textContent = 'Listening now';
    else if (recentTrack) {
        const timestamp = Number(recentTrack.date?.uts) * 1000;
        status.textContent = Number.isFinite(timestamp) && timestamp > 0
            ? relativeListenTime(timestamp) : 'Recently played';
    }
}

function safeHttpsUrl(value: string | undefined): string | undefined {
    try {
        const url = new URL(value ?? '');
        return url.protocol === 'https:' ? url.href : undefined;
    } catch { return undefined; }
}

function renderRecentTrack(track: RecentTrack) {
    recentTrack = track;
    $('#lastfm-song').textContent = track.name;
    $('#lastfm-artist').textContent = track.artist?.['#text'] || 'Unknown artist';
    const album = $('#lastfm-album');
    album.textContent = track.album?.['#text'] || '';
    album.hidden = !album.textContent;
    const cover = $<HTMLImageElement>('#lastfm-cover');
    const artwork = safeHttpsUrl([...(track.image ?? [])].reverse().find(image => image['#text'])?.['#text']);
    if (artwork) {
        if (cover.getAttribute('src') !== artwork) {
            cover.hidden = true;
            cover.src = artwork;
        }
    } else {
        cover.removeAttribute('src');
        cover.hidden = true;
    }
    cover.alt = track.album?.['#text'] ? `${track.album['#text']} cover art` : `${track.name} cover art`;
    const link = $<HTMLAnchorElement>('#lastfm-card');
    link.href = safeHttpsUrl(track.url) || `https://www.last.fm/user/${encodeURIComponent(lastFmUsername)}`;
    link.setAttribute('aria-label', `${track.name} by ${track.artist?.['#text'] || 'unknown artist'} on Last.fm (opens in a new tab)`);
    refreshListeningTime();
}

async function loadRecentTrack() {
    if (lastFmFetching || document.hidden) return;
    lastFmFetching = true;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    try {
        const response = await fetch('/api/lastfm/recent', { signal: controller.signal, cache: 'no-store' });
        if (!response.ok) throw new Error('Last.fm request failed');
        const result: LastFmResponse = await response.json();
        if (!result || !('track' in result)) throw new Error('Last.fm unavailable');
        const track = result.track;
        lastFmUnavailable = false;
        lastFmConfirmedAt = Date.now();
        if (track && typeof track.name === 'string') renderRecentTrack({
            name: track.name, artist: { '#text': track.artist }, album: { '#text': track.album },
            url: track.url, image: [{ '#text': track.coverUrl }],
            date: track.playedAt ? { uts: String(track.playedAt / 1000) } : undefined,
            '@attr': { nowplaying: String(track.nowPlaying) }
        });
        else {
            recentTrack = undefined;
            $('#lastfm-song').textContent = 'Nothing played yet';
            $('#lastfm-artist').textContent = 'The next song is waiting.';
            $('#lastfm-album').hidden = true;
            $<HTMLImageElement>('#lastfm-cover').hidden = true;
            $('#lastfm-status').classList.remove('is-live');
            $('#lastfm-status').textContent = 'No recent tracks';
            $<HTMLAnchorElement>('#lastfm-card').href = `https://www.last.fm/user/${encodeURIComponent(lastFmUsername)}`;
        }
    } catch {
        lastFmUnavailable = true;
        refreshListeningTime();
    } finally {
        clearTimeout(timeout);
        lastFmFetching = false;
    }
}

$('#lastfm-cover').addEventListener('load', () => $('#lastfm-cover').hidden = false);
$('#lastfm-cover').addEventListener('error', () => $('#lastfm-cover').hidden = true);
{
    $('#lastfm-status').textContent = 'Finding the latest track…';
    void loadRecentTrack();
    setInterval(refreshListeningTime, 30000);
}
view(routeName(), 'replace');
window.addEventListener('popstate', () => view(routeName(), 'none'));
window.addEventListener('hashchange', () => {
    if (['home', 'photos', 'contact', 'about', 'manage'].includes(location.hash.slice(1))) view(routeName(), 'replace');
});
(async () => {
    await Promise.all([loadSession(), loadGallery()]);
    try {
        db = await openDb();
        localPhotos = (await transaction<TripPhoto[]>('readonly', s => s.getAll()) ?? [])
            .filter(photo => !importedLocalIds.has(photo.id));
        updateOwnerControls();
    } catch {  }
    if (manageRequested && !owner) $<HTMLDialogElement>('#owner-dialog').showModal();
})();
