# Android Icon Setup

PEPEW Wallet uses the uploaded PEPEW logo as the Android launcher icon.

## Current Android manifest

`app/src/main/AndroidManifest.xml` currently uses:

```xml
android:icon="@drawable/pepew_logo"
android:roundIcon="@drawable/pepew_logo"
```

## Current logo resource

```text
app/src/main/res/drawable/pepew_logo.png
```

## Asset staging folder

Additional uploaded image assets may also exist under:

```text
app/public/
  favicon.ico
  favicon.svg
  favicon-16x16.png
  favicon-32x32.png
  favicon-48x48.png
  pepew_logo.png
```

`app/public/` is an asset staging folder only. Android launcher icons are not loaded from `app/public/` automatically.

## Better production option

For a production Android release, generate density-specific launcher icons:

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

Binary image files should be moved or replaced through GitHub web UI, GitHub Desktop, Android Studio, or local git. Text-only manifest updates can be safely patched through normal review tools.
