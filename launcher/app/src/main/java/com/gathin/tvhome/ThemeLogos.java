package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;

/**
 * A theme-pushed banner for one package: Config.dir()/logos/&lt;pkg&gt;.{png,webp}, delivered live by
 * tools/theme.sh through ThemeProvider, no rebuild. AppAdapter tries this before bundledBanner (a
 * banner_* resource baked into the APK), which it tries before the app's own banner/icon.
 */
final class ThemeLogos {
    private ThemeLogos() {}

    static Drawable load(Context ctx, String pkg) {
        File dir = Config.dir(ctx);
        if (dir == null) return null;
        for (String ext : new String[]{"png", "webp"}) {
            File f = new File(dir, ThemeFiles.LOGOS + "/" + pkg + "." + ext);
            if (!f.isFile()) continue;
            Bitmap bitmap = BitmapFactory.decodeFile(f.getPath());
            if (bitmap != null) return new BitmapDrawable(ctx.getResources(), bitmap);
        }
        return null;
    }
}
