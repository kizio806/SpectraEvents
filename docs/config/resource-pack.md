# Resource-Pack Configuration

SpectraEvents builds a local ZIP in `plugins/SpectraEvents/generated/resource-pack/`, but it does not
run an HTTP server or publish the ZIP. Player delivery is opt-in and disabled by default. On first
start, each distribution creates `plugins/SpectraEvents/resource-pack.yml`:

```yaml
# Host the locally generated ZIP on HTTPS, then opt in explicitly.
enabled: false
required: false
url: ""
sha1: ""
prompt: "<yellow>Server resources are required for SpectraEvents.</yellow>"
```

To enable it:

1. Build the pack from verified `.spectra.zip` files.
2. Upload that exact ZIP to an administrator-controlled HTTPS URL ending in `.zip`.
3. Compute the archive SHA-1 (40 hexadecimal characters) and put it in `sha1`.
4. Set `enabled: true`; set `required: true` only when your event requires the client assets.
5. Restart the server and confirm the log says that resource-pack delivery is ready.

The configuration rejects HTTP, a URL with user information or a fragment, a non-`.zip` path, and an
invalid SHA-1. If resolution fails, no descriptor is placed in the cache and no player is asked to
download a pack. After a successful resolve, every joining player receives the configured request;
the adapter records accepted, declined, failed and loaded statuses. A reconnect creates a new request
from the cached verified descriptor.

This is an implementation and test contract, not yet a real-client compatibility claim. Acceptance,
decline, failed download and reconnect must still be verified with a real player before M2 closes.

Modrinth delivery and publishing are deliberately disabled. See
[the Modrinth boundary](../architecture/modrinth-delivery.md).
