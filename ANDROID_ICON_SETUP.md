# Android Icon Setup

The PEPEW logo files are currently stored in:

```text
app/public/
  favicon.ico
  favicon.svg
  favicon-16x16.png
  favicon-32x32.png
  favicon-48x48.png
  pepew-logo.png
```

`app/public/` is useful as an asset staging folder, but Android launcher icons are not loaded from this folder automatically.

## Current Android manifest

```xml
android:icon="@drawable/ic_launcher"
android:roundIcon="@drawable/ic_launcher_round"
```

Current files:

```text
app/src/main/res/drawable/ic_launcher.xml
app/src/main/res/drawable/ic_launcher_round.xml
```

## Correct Android resource location

Move or copy the real PNG logo into Android resources, for example:

```text
app/src/main/res/drawable/pepew_logo.png
```

Then update `app/src/main/AndroidManifest.xml`:

```xml
android:icon="@drawable/pepew_logo"
android:roundIcon="@drawable/pepew_logo"
```

## Better production option

Generate density-specific launcher icons from `app/public/pepew-logo.png`:

```text
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
```

Then update the manifest:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher"
```

## Note

The GitHub connector can safely edit text files, but it is not reliable for moving binary PNG/ICO files between paths. Do the binary copy through GitHub web UI, GitHub Desktop, Android Studio, or local git.

After the PNG is placed under `app/src/main/res/drawable/`, the manifest can be safely updated in a small text-only patch.
