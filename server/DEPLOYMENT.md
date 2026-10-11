# Docker deployment on Proxmox

## Prepare the VM

Use a Debian or Ubuntu Linux VM on Proxmox with Docker Engine and the Compose plugin. Follow the [official Docker installation instructions](https://docs.docker.com/engine/install/). Allocate at least 2 GB RAM for building; the running app is limited to 768 MB. Keep Proxmox management separate from the website.

Copy this project to `/opt/harish-portfolio` in the VM. Include the root source/config files, `scripts/`, `server/pom.xml`, `server/src/`, `deploy/`, and Docker/Compose files. Exclude `node_modules`, `work`, `server/target`, and local credential files. Transfer photos separately below. Node and Java are installed only in the Docker build stages, not on the VM.

All commands below run inside the Linux VM from the project directory with Docker access.

```bash
cd /opt/harish-portfolio
bash deploy/setup.sh
```

Setup creates `.env` if absent, generates an owner password in `secrets/owner.password`, and prompts for your Last.fm key without echoing it. Save the owner password in your password manager. A blank Last.fm key leaves music unavailable but does not prevent startup.

The host `secrets` directory is mode 700. `deploy/start.sh` sets application secret files to mode 600, owned by container UID 10001; the tunnel token uses UID 65532. Preserve these permissions. Compose secrets are mounted files, not encrypted storage; protect VM backups. See [Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/) and [Spring configuration trees](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtree).

Review `.env`. Username defaults to `harish`. Production requires a password of at least 16 characters, at most 72 UTF-8 bytes. Secrets, uploads, and local credentials never enter the image.

## Cloudflare Tunnel (current deployment)

The deployed checkout is `/home/webserver/apps/portfolio_site` on `webserver@10.0.0.72`. The app binds only to `127.0.0.1:8080`. Keep `SESSION_COOKIE_SECURE=true` and do not forward router ports for the website.

In the [Cloudflare dashboard](https://dash.cloudflare.com), go to Networking > Tunnels and create `harish-portfolio`. Select Docker and copy only the token following `--token` in the install command. Save it from an interactive SSH terminal without putting it in shell history:

```bash
cd /home/webserver/apps/portfolio_site
umask 077
read -r -s -p 'Cloudflare tunnel token: ' cf_token
printf '\n'
printf '%s' "$cf_token" > secrets/cloudflare.tunnel-token
unset cf_token
bash deploy/start.sh --tunnel
```

Once the connector is healthy, add a published application route with domain `harishkiru.dev`, an empty subdomain, and service URL `http://portfolio:8080`. Cloudflare creates a proxied tunnel DNS record. If an existing apex A/AAAA record conflicts, replace only that website record; preserve mail and other service records. See [Cloudflare setup](https://developers.cloudflare.com/tunnel/get-started/).

The connector uses outbound connections and exposes no host ports. This keeps the website origin out of current public DNS, but does not remove any previous IP exposures. The token stays outside Git and the image and is mounted as a private file. The connector image is pinned by digest; update that digest deliberately when updating cloudflared.

Use both Compose files for status, logs, restarts, and shutdown:

```bash
docker compose -f compose.yaml -f compose.tunnel.yaml ps
docker compose -f compose.yaml -f compose.tunnel.yaml logs --tail=50 cloudflared
```

For updates, back up the gallery, run `git pull --ff-only`, then `bash deploy/start.sh --tunnel`. To rotate a secret owned by a container UID, remove and recreate its file from the private directory with `umask 077`, then run the start script; do not make it world-readable. Container recreation preserves the gallery volume.

## Use your existing HTTPS proxy

```bash
docker compose config --quiet
bash deploy/start.sh
docker compose ps
curl --fail http://127.0.0.1:8080/api/health
```

A proxy on the same VM can use `http://127.0.0.1:8080`. Another container can share a Docker network. For a proxy on another machine, set `BIND_ADDRESS` to this VM's private IP and allow port 8080 only from the proxy in your firewall.

Your proxy must overwrite forwarded headers, including `X-Forwarded-Proto`, and allow 21 MB request bodies. Production trusts these headers; keep the backend off the public internet. Keep `SESSION_COOKIE_SECURE=true` for HTTPS. For a temporary private HTTP test, set it false and recreate the app before signing in.

## Optional HTTPS proxy included

If you have no proxy, set `SITE_DOMAIN` in `.env`, point that domain's DNS to your public IP, and forward TCP 80/443 to this VM. Ensure those ports are free. [Caddy obtains and renews certificates automatically](https://caddyserver.com/docs/automatic-https).

```bash
docker compose -f compose.yaml -f compose.https.yaml config --quiet
bash deploy/start.sh
docker compose -f compose.yaml -f compose.https.yaml up -d --no-build
```

Use both `-f` arguments for subsequent Compose commands when using Caddy. It reaches `portfolio:8080` privately; the host backend port stays on localhost. Certificates persist in Caddy volumes.

## ShareX with private Cloudflare R2 storage

The VM accepts authenticated uploads at `https://harishkiru.dev/api/sharex/upload`. A separate Cloudflare Worker serves images from a private R2 bucket at `https://img.harishkiru.dev/IMAGE_ID`. Image downloads do not pass through the VM or its Tunnel. Keep the existing apex tunnel route; do not add an img route to the Tunnel.

Create a dedicated `harish-sharex` bucket using Standard storage. Leave public access and r2.dev disabled. Create an R2 Account API token with Object Read & Write permission restricted to that bucket. Save its Access Key ID and Secret Access Key in `secrets/r2.access-key-id` and `secrets/r2.secret-access-key`, using an interactive hidden prompt and `umask 077`. Set `R2_ACCOUNT_ID` (32-character account ID from the S3 endpoint) and `R2_BUCKET=harish-sharex` in `.env`. See [R2 S3 setup](https://developers.cloudflare.com/r2/get-started/s3/).

`deploy/start.sh` generates the separate ShareX upload token in `secrets/sharex.upload-token` and makes secret files mode 600, owned by UID 10001. Blank R2 credentials disable image uploads without stopping the portfolio. The R2 credentials never enter Git, the image, ShareX, or the browser.

Deploy the read-only image Worker from `cloudflare/image-worker` using the Workers Free plan:

```bash
npm ci
npm test
npm run check
npx wrangler login
npm run deploy
```

Wrangler attaches the private `harish-sharex` bucket as binding `IMAGES` and adds custom domain `img.harishkiru.dev`. Disable any old img Tunnel route/DNS record first if it conflicts. Both workers.dev access and preview URLs are disabled. Worker request logs are disabled to avoid retaining screenshot links. Do not add Cache Everything rules or Always Online for the image hostname.

Import `deploy/sharex.sxcu` into ShareX and replace its Authorization header placeholder with `Bearer YOUR_UPLOAD_TOKEN`. Populated SXCU files are credentials: keep them private and out of Git. Select Custom image uploader as the image destination. [ShareX custom uploaders](https://getsharex.com/docs/custom-uploader).

Uploads use multipart field `file`, accept PNG/JPG/GIF/WebP up to 20 MB, and return JSON with `id` and `url`. Each ID uses 24 cryptographically random bytes encoded into 32 URL-safe characters (192 bits). Original filenames are not stored. The Worker serves only exact IDs, allows GET/HEAD, and has no listing or mutation endpoint. Headers include no-store, no-referrer, noindex and nosniff. Links stay available until deletion; anybody who receives one can save or share the screenshot. Crawlers can ignore noindex, so links are not authenticated private storage.

Default storage cap is 10 GB (`SHAREX_CAPACITY_BYTES=10000000000`), matching R2 Standard's free storage allowance. The account-wide free tier also has operation limits; this storage cap is not a billing guarantee. Workers Free has its own daily request limit. Check Cloudflare usage regularly; other buckets share the R2 allowances. See [R2 pricing](https://developers.cloudflare.com/r2/pricing/) and [Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/).

Run one VM application instance and keep it the only writer for this dedicated bucket. Storage usage is reconciled from R2 on the first upload after restart and after failed operations. Uploads return 507 when the cap is reached. Delete an image using its ID:

```bash
bash deploy/delete-screenshot.sh IMAGE_ID_FROM_URL
```

Deletion immediately removes it from R2; previously saved copies cannot be revoked. The local gallery remains in the Docker volume. Back up screenshots separately from R2 if required; VM backups do not include R2 objects. To rotate a key, privately recreate its secret file, run `bash deploy/start.sh --tunnel`, force-recreate portfolio, and update ShareX if its upload token changed.

## Owner access

Open `https://harishkiru.dev/owner` directly. The public site has no login panel. Authentication and CSRF protect uploads, caption/date edits, and deletion. Sessions expire after 30 minutes idle and reset on restart. Uploaded pictures are public.

Optionally restrict `/owner`, `/api/login`, `/api/logout`, and non-GET requests under `/api/photos` to your LAN/VPN in the proxy. Keep public gallery GETs accessible. The password remains required.

To rotate a password/API key, recreate its file in `secrets/` with `umask 077`, run `bash deploy/start.sh` (with `--tunnel` for Cloudflare), then force-recreate `portfolio` using the active Compose files.

## Import your existing gallery

Copy the entire Windows `server/gallery-data` directory to `server/gallery-data` here. This includes Switzerland: both `.image` and `.properties` files are required and are intentionally excluded from the image.

After building/creating the volume, import only into an empty gallery with the app stopped:

```bash
docker compose stop portfolio
docker run --rm --user 0 --entrypoint sh \
  -v harish-portfolio-gallery:/data \
  -v "$PWD/server/gallery-data:/source:ro" \
  harish-portfolio:local \
  -c 'mkdir -p /data/gallery && cp -a /source/. /data/gallery/ && chown -R 10001:10001 /data'
docker compose start portfolio
```

If you change `IMAGE_TAG`, replace `local` in helper commands. Keep Windows originals until photos, dates, and captions are verified. Do not import over a populated production gallery.

## Backups and restore

Pictures persist in named volume `harish-portfolio-gallery`. Run one app instance; the file-based gallery does not support multiple replicas. Back up while stopped to keep metadata and images consistent:

```bash
mkdir -p backups
chmod 700 backups
docker compose stop portfolio
docker run --rm --user 0 --entrypoint tar \
  -v harish-portfolio-gallery:/data:ro \
  -v "$PWD/backups:/backup" \
  harish-portfolio:local -C /data -czf /backup/gallery.tar.gz gallery
docker compose start portfolio
```

Keep dated backups off the VM. Back up `.env`, `secrets`, and Caddy volumes separately or use encrypted Proxmox VM backups. Avoid `docker compose down -v`, which deletes volumes.

Restore on a fresh host/empty volume after building, with the app stopped:

```bash
docker volume create harish-portfolio-gallery
docker run --rm --user 0 --entrypoint sh \
  -v harish-portfolio-gallery:/data \
  -v "$PWD/backups:/backup:ro" \
  harish-portfolio:local \
  -c 'tar -xzf /backup/gallery.tar.gz -C /data && chown -R 10001:10001 /data'
docker compose up -d
```

## Updates and checks

Back up first, copy updated source, then:

```bash
bash deploy/start.sh
docker compose ps
docker compose logs --tail=100 portfolio
```

The app runs non-root, with a read-only root filesystem, dropped capabilities, bounded logs, and temporary storage. Keep the VM/Docker updated; periodically run `docker compose build --pull` for base image updates. Health checks mark a failing app unhealthy but do not restart a process still running; monitor health. `restart: unless-stopped` handles process exits and host restarts.

For login issues check HTTPS, cookie setting, and proxy headers. For missing photos check the volume and ownership UID/GID 10001. For unavailable music check Last.fm's key. Never paste secret files in logs or public issues.

The production image was built on the Ubuntu VM with all 11 backend tests passing, and the running container reports UP. Before publishing, verify gallery rendering, owner login/upload/date-edit/delete, and persistence after container recreation.
