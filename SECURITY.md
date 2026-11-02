# Security Policy

## Supported Versions

Security fixes and patches are applied to the active release stream:

| Version | Supported | Status |
| ------- | --------- | ------ |
| 0.1.1-beta.2 | :white_check_mark: | Current Active Release |
| < 0.1.1-beta.2 | :x: | End of Life |

## Reporting a Vulnerability

We take the security of SpectraEvents seriously. Do not open public GitHub issues for security vulnerabilities.

Please report privately via GitHub Security Advisories:

1. Navigate to the repository's [Security Advisories](https://github.com/kizio806/SpectraEvents/security/advisories).
2. Click **Report a vulnerability**.
3. Provide details about the issue, impact, and reproducible steps.

Alternatively, you may reach out directly to the maintainer via the email address listed in the repository owner profile.

### What to Include in Your Report

- Affected SpectraEvents plugin version and distribution (Paper or Spigot).
- Server implementation and version (e.g., Paper 1.21.4, Purpur, Folia, Spigot).
- Step-by-step reproduction instructions or a minimal proof of concept.
- Impact assessment (e.g., arbitrary code execution, denial of service, data corruption, privilege escalation).

### Disclosure Policy

- The maintainers will acknowledge receipt of your report within 48 hours.
- A fix will be developed in a private security fork.
- Once a patched release is published, a Common Vulnerabilities and Exposures (CVE) identifier or GitHub Security Advisory will be credited to the reporter.

## Secret & Credential Management

- Never hardcode tokens, passwords, or credentials in source code, configuration files, or CI workflow templates.
- All sensitive credentials and tokens are managed exclusively through GitHub Actions Secrets.

### 1. Platform-Generated Tokens
- `GITHUB_TOKEN`: Ephemeral installation token provisioned automatically by GitHub Actions for each workflow execution. It is never manually configured. Used for PR labeling (`labeler.yml`), wiki synchronization (`wiki-sync.yml`), and publishing release tags (`release.yml`).

### 2. External Service Authentication Tokens (Sensitive Secrets)
- `MODRINTH_TOKEN`: Personal API token with version publishing permissions on Modrinth.
- `CURSEFORGE_TOKEN`: API token with version upload permissions on CurseForge.
- `HANGAR_TOKEN`: API token with version upload permissions on PaperMC Hangar.
- `SONAR_TOKEN`: Authentication token for SonarCloud static analysis and code quality reporting.
- `CODECOV_TOKEN`: Repository upload token for Codecov test coverage reports.

### 3. Project Identifiers & Configuration Variables (Non-Token References)
- `MODRINTH_PROJECT_ID`: Unique project identifier (or slug) target on Modrinth.
- `CURSEFORGE_PROJECT_ID`: Numerical or slug project identifier target on CurseForge.
- `HANGAR_PROJECT_SLUG`: Project slug target on Hangar.
