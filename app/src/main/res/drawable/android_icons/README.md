# Android Icons - Vector Drawables

This folder contains all the icon files you need for your Android app as **Vector Drawables** (XML format).

## ✨ What Are Vector Drawables?

Vector drawables are XML files that define images using geometric shapes and paths. They're better than PNG images because:
- ✅ **Scalable** - Look perfect on any screen size
- ✅ **Small file size** - Much smaller than PNG/JPG
- ✅ **Editable** - Easy to change colors and shapes
- ✅ **Professional** - Industry standard for Android icons

## 📁 Files Included

### Navigation Icons (5 files):
- `ic_home.xml` - Home tab icon (white house)
- `ic_profile.xml` - Profile tab icon (white person)
- `ic_request.xml` - Request tab icon (white clipboard)
- `ic_service.xml` - Service tab icon (white wrench)
- `ic_post.xml` - Post tab icon (white plus sign)

### Action Icons (3 files):
- `ic_settings.xml` - Settings button (black gear)
- `ic_delete.xml` - Delete icon in list items (red trash)
- `ic_delete_main.xml` - Delete icon top right (red trash)

### Category Icons (9 files):
- `img_plumbing.xml` - Blue background with wrench
- `img_painting.xml` - Orange background with paint brush
- `img_carpentry.xml` - Brown background with hammer
- `img_gardening.xml` - Green background with plant
- `img_electric.xml` - Yellow background with lightning bolt
- `img_mechanic.xml` - Gray background with gear
- `img_cleaning.xml` - Cyan background with spray bottle
- `img_handyman.xml` - Orange background with screwdriver
- `img_hvac.xml` - Blue-gray background with fan

### Background (1 file):
- `img_service_background.xml` - Purple gradient with circles

## 🚀 How to Use

### Method 1: Copy to Android Studio (Recommended)

1. **Open Android Studio**
2. Navigate to `app/src/main/res/drawable/`
3. **Drag and drop** all `.xml` files from this folder into `drawable/`
4. Click **OK** when prompted
5. **Sync Project**: File → Sync Project with Gradle Files
6. **Done!** All icons are now available in your app

### Method 2: Using Terminal/Command Line

```bash
# Navigate to your Android project
cd /path/to/YourAndroidProject/app/src/main/res/drawable/

# Copy all icon files
cp /path/to/android_icons/*.xml .

# Back to Android Studio and sync
```

### Method 3: Manual Copy

1. Open the `android_icons` folder
2. Select all `.xml` files
3. Copy them (Ctrl+C or Cmd+C)
4. In Android Studio, right-click on `app/src/main/res/drawable/`
5. Paste (Ctrl+V or Cmd+V)
6. Sync project

## ✅ Verify Installation

After copying the files:

1. Open any XML file in `drawable/` folder
2. You should see no errors
3. In your layout files, you can now use:
   ```xml
   android:src="@drawable/ic_home"
   android:src="@drawable/img_plumbing"
   ```
4. Android Studio should autocomplete the icon names

## 🎨 Customizing Colors

You can easily change icon colors by editing the XML files.

### Example: Change Home Icon Color

Open `ic_home.xml` and change the `fillColor`:

```xml
<!-- Change from white to purple -->
<path
    android:fillColor="#8778BF"  <!-- was #FFFFFF -->
    android:pathData="M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"/>
```

### Example: Change Category Background

Open `img_plumbing.xml` and change the background color:

```xml
<!-- Change from blue to red -->
<path
    android:fillColor="#F44336"  <!-- was #2196F3 -->
    android:pathData="M0,0h83v63h-83z"/>
```

## 📐 Resizing Icons

The icons are already sized correctly for your layout, but you can adjust:

```xml
<!-- In the vector drawable file -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"   <!-- Change size here -->
    android:height="48dp"  <!-- Change size here -->
    android:viewportWidth="24"
    android:viewportHeight="24">
```

## 🔧 Troubleshooting

### Icons not showing?
1. Make sure files are in `app/src/main/res/drawable/`
2. File names must be lowercase with underscores only
3. Sync project: **File → Sync Project with Gradle Files**
4. Clean and rebuild: **Build → Clean Project** then **Build → Rebuild Project**

### "Cannot resolve symbol @drawable/ic_home"?
1. Check the file name matches exactly (case-sensitive)
2. Make sure the file is `.xml` not `.txt`
3. Try invalidating cache: **File → Invalidate Caches / Restart**

### Icons look wrong?
1. Make sure you're using the correct file names in your layout
2. Check the `android:src` or `android:background` attribute
3. Verify the XML is valid (no syntax errors)

## 📱 Using in Your App

These icons are ready to use in `activity_request_main.xml`:

```xml
<!-- Example usage -->
<ImageView
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:src="@drawable/ic_settings"
    android:contentDescription="Settings" />

<ImageView
    android:layout_width="83dp"
    android:layout_height="63dp"
    android:src="@drawable/img_plumbing"
    android:contentDescription="Plumbing" />
```

## 🎯 Next Steps

After adding these icons:
1. ✅ Your layout will display all icons correctly
2. ✅ App will work on all screen sizes
3. ✅ No blurry images on high-res devices
4. ✅ Small APK file size

## 💡 Want Different Icons?

If you want to replace any icon:
1. Visit https://materialdesignicons.com/
2. Find the icon you want
3. Download as SVG
4. Convert SVG to Android Vector Drawable (use Android Studio's Vector Asset tool)
5. Replace the corresponding `.xml` file

## 📞 Need Help?

If you have any issues:
- Check that all `.xml` files are in the `drawable/` folder
- Make sure file names match exactly
- Verify no syntax errors in XML
- Sync and rebuild your project

---

**All icons are ready to use!** Just copy them to your `drawable/` folder and you're done! 🚀
