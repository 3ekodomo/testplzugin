# Zettel Notes ImgBB Plugin

This project is a standalone buildable version of the ImgBB Zettel Notes plugin.

## GitHub Actions

Push the project to GitHub. The included workflow:

1. Installs JDK 17.
2. Uses the included Gradle wrapper (Gradle 8.7).
3. Builds `:plugin-btn-imgbb:assembleRelease`.
4. Uploads the release APK as the `imgbb-plugin-release` workflow artifact.

No `local.properties`, signing key, external Zettel Notes source checkout, or local version catalog is required.

The plugin uses the official Zettel Notes Plugin API dependency `com.github.damionx7:Zettel-Notes-Plugin-Api:28`.
