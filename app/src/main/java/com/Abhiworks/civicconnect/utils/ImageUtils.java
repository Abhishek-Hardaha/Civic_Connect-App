package com.Abhiworks.civicconnect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Image processing utilities.
 * All methods are blocking — call from a background thread (ExecutorService), never from main thread.
 */
public class ImageUtils {

    private ImageUtils() {}

    /**
     * Reads the image at the given Uri, scales it down so the longest side
     * is at most MAX_IMAGE_SIZE_PX (preserving aspect ratio), compresses to
     * JPEG at JPEG_QUALITY, and returns the byte array.
     *
     * @param ctx context needed to open the content URI
     * @param uri content:// or file:// URI of the source image
     * @return compressed JPEG bytes
     * @throws IOException if reading or decoding fails
     */
    public static byte[] compressBitmap(Context ctx, Uri uri) throws IOException {
        // Decode bounds first to calculate sample size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream boundsStream = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(boundsStream, null, options);
        }

        int srcWidth  = options.outWidth;
        int srcHeight = options.outHeight;
        int maxSide   = AppConstants.MAX_IMAGE_SIZE_PX;

        // Calculate sample size (power of 2) for memory-efficient decode
        int sampleSize = 1;
        if (srcWidth > maxSide || srcHeight > maxSide) {
            int halfWidth  = srcWidth / 2;
            int halfHeight = srcHeight / 2;
            while ((halfWidth / sampleSize) >= maxSide
                    || (halfHeight / sampleSize) >= maxSide) {
                sampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;

        Bitmap bitmap;
        try (InputStream decodeStream = ctx.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(decodeStream, null, options);
        }

        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI: " + uri);
        }

        // Fine-scale if still larger than maxSide after sampling
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w > maxSide || h > maxSide) {
            float scale = (float) maxSide / Math.max(w, h);
            int newW = Math.round(w * scale);
            int newH = Math.round(h * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
            bitmap.recycle();
            bitmap = scaled;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, AppConstants.JPEG_QUALITY, baos);
        bitmap.recycle();
        return baos.toByteArray();
    }
}
