package com.davemorrissey.labs.subscaleview;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class used to set the source and additional attributes from a variety of sources. Supports
 * use of a bitmap, asset, resource, external file or any other URI.
 *
 * When you are using a preview image, you must set the dimensions of the full size image on the
 * ImageSource object for the full size image using the {@link #dimensions(int, int)} method.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ImageSource<T> {

    private static final String TAG = ImageSource.class.getSimpleName();

    static final String FILE_SCHEME = "file:///";
    static final String ASSET_SCHEME = "file:///android_asset/";

    /** Display the image file in its native orientation. */
    public static final int ORIENTATION_0 = 0;
    /** Rotate the image 90 degrees clockwise. */
    public static final int ORIENTATION_90 = 90;
    /** Rotate the image 180 degrees. */
    public static final int ORIENTATION_180 = 180;
    /** Rotate the image 270 degrees clockwise. */
    public static final int ORIENTATION_270 = 270;

    public  static final List<Integer> VALID_ORIENTATIONS = Arrays.asList(ORIENTATION_0, ORIENTATION_90, ORIENTATION_180, ORIENTATION_270);

    private final T source;
    private boolean tile;
    private int sWidth;
    private int sHeight;
    private Rect sRegion;
    protected boolean useOnlyRegionDecoder = false;

    public ImageSource(@NonNull T source) {
        this.source = source;
        this.tile = true;
    }

    /**
     * Enable tiling of the image. This does not apply to preview images which are always loaded as a single bitmap.,
     * and tiling cannot be disabled when displaying a region of the source image.
     * @return this instance for chaining.
     */
    @NonNull
    public ImageSource tilingEnabled() {
        return tiling(true);
    }

    /**
     * Disable tiling of the image. This does not apply to preview images which are always loaded as a single bitmap,
     * and tiling cannot be disabled when displaying a region of the source image.
     * @return this instance for chaining.
     */
    @NonNull
    public ImageSource tilingDisabled() {
        return tiling(false);
    }

    /**
     * Enable or disable tiling of the image. This does not apply to preview images which are always loaded as a single bitmap,
     * and tiling cannot be disabled when displaying a region of the source image.
     * @param tile whether tiling should be enabled.
     * @return this instance for chaining.
     */
    @NonNull
    public ImageSource tiling(boolean tile) {
        this.tile = tile;
        return this;
    }

    /**
     * Use a region of the source image. Region must be set independently for the full size image and the preview if
     * you are using one.
     * @param sRegion the region of the source image to be displayed.
     * @return this instance for chaining.
     */
    @NonNull
    public ImageSource region(Rect sRegion) {
        this.sRegion = sRegion;
        setInvariants();
        return this;
    }

    /**
     * Declare the dimensions of the image. This is only required for a full size image, when you are specifying a URI
     * and also a preview image. When displaying a bitmap object, or not using a preview, you do not need to declare
     * the image dimensions. Note if the declared dimensions are found to be incorrect, the view will reset.
     * @param sWidth width of the source image.
     * @param sHeight height of the source image.
     * @return this instance for chaining.
     */
    @NonNull
    public ImageSource dimensions(int sWidth, int sHeight) {
        this.sWidth = sWidth;
        this.sHeight = sHeight;
        setInvariants();
        return this;
    }

    private void setInvariants() {
        if (this.sRegion != null) {
            this.tile = true;
            this.sWidth = this.sRegion.width();
            this.sHeight = this.sRegion.height();
        }
    }

    public final T getSource() { return source; }

    protected final boolean getTile() {
        return tile;
    }

    protected int getWidth() {
        return sWidth;
    }

    protected int getHeight() {
        return sHeight;
    }

    protected final Rect getRegion() {
        return sRegion;
    }

    protected final boolean useOnlyRegionDecoder() { return useOnlyRegionDecoder; }

    protected int getExifOrientation(Context context) {
        int exifOrientation = ORIENTATION_0;

        String sourceUri = source.toString();
        if (sourceUri.startsWith(ContentResolver.SCHEME_CONTENT)) {
            Cursor cursor = null;
            try {
                String[] columns = { MediaStore.Images.Media.ORIENTATION };
                cursor = context.getContentResolver().query(Uri.parse(sourceUri), columns, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int orientation = cursor.getInt(0);
                        if (VALID_ORIENTATIONS.contains(orientation)) {
                            exifOrientation = orientation;
                        } else {
                            Log.w(TAG, "Unsupported orientation: " + orientation);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get orientation of image from media store");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (sourceUri.startsWith(ImageSource.FILE_SCHEME) && !sourceUri.startsWith(ImageSource.ASSET_SCHEME)) {
            try {
                ExifInterface exifInterface = new ExifInterface(sourceUri.substring(ImageSource.FILE_SCHEME.length() - 1));
                int orientationAttr = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                exifOrientation = convertExifOrientation(orientationAttr);
            } catch (Exception e) {
                Log.w(TAG, "Could not get EXIF orientation of image");
            }
        }
        return exifOrientation;
    }

    protected int convertExifOrientation(int exifOrientation) {
        int rotation = ORIENTATION_0;
        if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
            rotation = ORIENTATION_0;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            rotation = ORIENTATION_90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            rotation = ORIENTATION_180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            rotation = ORIENTATION_270;
        } else {
            Log.w(TAG, "Unsupported EXIF orientation: " + exifOrientation);
        }
        return rotation;
    }
}
