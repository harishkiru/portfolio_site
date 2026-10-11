const idPattern = /^\/[A-Za-z0-9_-]{32}$/;
const imageTypes = new Set(['image/png', 'image/jpeg', 'image/gif', 'image/webp']);

function headers() {
  return new Headers({
    'Cache-Control': 'no-store',
    'Referrer-Policy': 'no-referrer',
    'X-Robots-Tag': 'noindex, nofollow, noarchive, noimageindex',
    'X-Content-Type-Options': 'nosniff',
    'Content-Security-Policy': "default-src 'none'; frame-ancestors 'none'; sandbox",
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const responseHeaders = headers();
    if (url.hostname !== 'img.harishkiru.dev' || !['GET', 'HEAD'].includes(request.method)) {
      return new Response(null, { status: 404, headers: responseHeaders });
    }
    if (url.pathname === '/robots.txt') {
      responseHeaders.set('Content-Type', 'text/plain; charset=utf-8');
      return new Response(request.method === 'HEAD' ? null : 'User-agent: *\nDisallow: /\n',
        { headers: responseHeaders });
    }
    if (!idPattern.test(url.pathname) || url.search) {
      return new Response(null, { status: 404, headers: responseHeaders });
    }
    try {
      const id = url.pathname.slice(1);
      const object = request.method === 'HEAD' ? await env.IMAGES.head(id) : await env.IMAGES.get(id);
      if (!object || !imageTypes.has(object.httpMetadata?.contentType)) {
        return new Response(null, { status: 404, headers: responseHeaders });
      }
      responseHeaders.set('Content-Type', object.httpMetadata.contentType);
      responseHeaders.set('Content-Length', String(object.size));
      responseHeaders.set('Content-Disposition', 'inline');
      return new Response(request.method === 'HEAD' ? null : object.body, { headers: responseHeaders });
    } catch {
      return new Response(null, { status: 503, headers: responseHeaders });
    }
  },
};
