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

The host `secrets` directory is mode 700; files inside are readable by container UID 10001 through bind mounts. Preserve these permissions. Compose secrets are mounted files, not encrypted storage; protect VM backups. See [Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/) and [Spring configuration trees](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtree).

Review `.env`. Username defaults to `harish`. Production requires a password of at least 16 characters, at most 72 UTF-8 bytes. Secrets, uploads, and local credentials never enter the image.

## Use your existing HTTPS proxy

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
curl --fail http://127.0.0.1:8080/api/health
```

A proxy on the same VM can use `http://127.0.0.1:8080`. Another container can share a Docker network. For a proxy on another machine, set `BIND_ADDRESS` to this VM's private IP and allow port 8080 only from the proxy in your firewall.

Your proxy must overwrite forwarded headers, including `X-Forwarded-Proto`, and allow 21 MB request bodies. Production trusts these headers; keep the backend off the public internet. Keep `SESSION_COOKIE_SECURE=true` for HTTPS. For a temporary private HTTP test, set it false and recreate the app before signing in.

## Optional HTTPS proxy included

If you have no proxy, set `SITE_DOMAIN` in `.env`, point that domain's DNS to your public IP, and forward TCP 80/443 to this VM. Ensure those ports are free. [Caddy obtains and renews certificates automatically](https://caddyserver.com/docs/automatic-https).

```bash
docker compose -f compose.yaml -f compose.https.yaml config --quiet
docker compose -f compose.yaml -f compose.https.yaml up -d --build
```

Use both `-f` arguments for subsequent Compose commands when using Caddy. It reaches `portfolio:8080` privately; the host backend port stays on localhost. Certificates persist in Caddy volumes.

## Owner access

Open `https://harishkiru.dev/owner` directly. The public site has no login panel. Authentication and CSRF protect uploads, caption/date edits, and deletion. Sessions expire after 30 minutes idle and reset on restart. Uploaded pictures are public.

Optionally restrict `/owner`, `/api/login`, `/api/logout`, and non-GET requests under `/api/photos` to your LAN/VPN in the proxy. Keep public gallery GETs accessible. The password remains required.

To rotate a password/API key, edit its file in `secrets/`, then run `docker compose up -d --force-recreate portfolio`.

## Import your existing gallery

Copy the entire Windows `server/gallery-data` directory to `server/gallery-data` here. This includes Switzerland: both `.image` and `.properties` files are required and are intentionally excluded from the image.

After building/creating the volume, import only into an empty gallery with the app stopped:

```bash
docker compose stop portfolio
docker run --rm --user 0 --entrypoint sh \
  -v harish-portfolio-gallery:/data \
  -v "$PWD/server/gallery-data:/source:ro" \
  harish-portfolio:local \
  -c 'mkdir -p /data/gallery && cp -a /source/. /data/gallery/ && chown -R 10001:10001 /data/gallery'
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
  -c 'tar -xzf /backup/gallery.tar.gz -C /data && chown -R 10001:10001 /data/gallery'
docker compose up -d
```

## Updates and checks

Back up first, copy updated source, then:

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 portfolio
```

The app runs non-root, with a read-only root filesystem, dropped capabilities, bounded logs, and temporary storage. Keep the VM/Docker updated; periodically run `docker compose build --pull` for base image updates. Health checks mark a failing app unhealthy but do not restart a process still running; monitor health. `restart: unless-stopped` handles process exits and host restarts.

For login issues check HTTPS, cookie setting, and proxy headers. For missing photos check the volume and ownership UID/GID 10001. For unavailable music check Last.fm's key. Never paste secret files in logs or public issues.

Application tests and the JAR were checked locally. Docker is unavailable on the development computer, so verify the first actual container build/start on this VM before publishing: health reports UP, gallery loads, owner login/upload/date-edit/delete work, and an uploaded image survives container recreation.
