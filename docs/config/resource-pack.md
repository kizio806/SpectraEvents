# Resource Pack Configuration

SpectraEvents allows you to manage how the resource pack is delivered to your players via the `resource-pack` section in `config.yml`.

There are two primary source types: `modrinth` (for the official baseline pack) and `manual` (for custom server asset packs).

## 1. Official Modrinth Source

If you are using the official SpectraEvents content (e.g. built-in Meteor or Metin), use the `modrinth` source type. This fetches the correct resource pack directly from the Modrinth CDN.

```yaml
resource-pack:
  enabled: true
  required: true

  source:
    type: modrinth
    project-id: "spectraevents-assets"

  prompt: "<gold>This server uses SpectraEvents assets."
  failurePolicy: "kick"
```

- `project-id`: Must be the exact Modrinth project ID or slug for the official pack (`spectraevents-assets`).
- **Server Owners**: You do not need to download or host anything manually!

## 2. Custom Manual Source

If you have created your own 3D models or modified the resource pack, you cannot upload them to the official Modrinth project. You must host your resource pack on your own web server (e.g., Apache, NGINX, Amazon S3, Cloudflare R2).

```yaml
resource-pack:
  enabled: true
  required: true

  source:
    type: manual
    url: "https://example.com/my-custom-pack.zip"
    sha1: "abc123def456..."

  prompt: "<gold>Please accept the custom SpectraEvents asset pack."
  failurePolicy: "deny-assets"
```

- `url`: A direct link to your `.zip` file.
- `sha1`: The exact SHA-1 hash of the zip file. This is **required** by Minecraft to update the pack on the client.

## Additional Options
- `required`: If `true`, the pack uses the native Minecraft 1.20+ mandatory prompt. If the player declines, they may be disconnected by the client.
- `prompt`: The message displayed above the Accept/Decline buttons. Supports MiniMessage format.
- `failurePolicy`: What SpectraEvents should do if the player fails to load the pack (e.g., `kick`, `ignore`, `deny-assets`).
