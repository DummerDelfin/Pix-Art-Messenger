package de.pixart.messenger.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.rtoshiro.view.video.FullscreenVideoLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.utils.ExifHelper;
import de.pixart.messenger.utils.MimeUtils;

import static de.pixart.messenger.persistance.FileBackend.close;

public class MediaViewerActivity extends XmppActivity {

    Integer oldOrientation;
    SubsamplingScaleImageView mImage;
    FullscreenVideoLayout mVideo;
    ImageView mFullscreenbutton;
    Uri mFileUri;
    File mFile;
    FloatingActionButton fab;
    int height = 0;
    int width = 0;
    int rotation = 0;
    boolean isImage = false;
    boolean isVideo = false;

    public static String getMimeType(String path) {
        try {
            String type = null;
            String extension = path.substring(path.lastIndexOf(".") + 1, path.length());
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return type;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mTheme = findTheme();
        setTheme(this.mTheme);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && actionBar.isShowing()) {
            actionBar.hide();
        }

        oldOrientation = getRequestedOrientation();

        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = 1;
        }
        getWindow().setAttributes(layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_media_viewer);
        mImage = findViewById(R.id.message_image_view);
        mVideo = findViewById(R.id.message_video_view);
        mFullscreenbutton = findViewById(R.id.vcv_img_fullscreen);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MediaViewerActivity.this, v);
            popup.inflate(R.menu.media_viewer);
            final Menu menu = popup.getMenu();
            MenuItem delete = menu.findItem(R.id.action_delete);
            MenuItem open = menu.findItem(R.id.action_open);
            Log.d(Config.LOGTAG, "Path = " + mFile.toString());
            if (mFile == null || !mFile.toString().startsWith("/") || mFile.toString().contains(FileBackend.getConversationsDirectory("null"))) {
                delete.setVisible(true);
            } else {
                delete.setVisible(false);
            }
            if (isVideo) {
                if (isDarkTheme()) {
                    open.setIcon(R.drawable.ic_video_white_24dp);
                } else {
                    open.setIcon(R.drawable.ic_video_black_24dp);
                }
            } else if (isImage) {
                if (isDarkTheme()) {
                    open.setIcon(R.drawable.ic_image_white_24dp);
                } else {
                    open.setIcon(R.drawable.ic_image_black_24dp);
                }
            }
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_share:
                        share();
                        break;
                    case R.id.action_open:
                        open();
                        break;
                    case R.id.action_delete:
                        deleteFile();
                        break;
                    default:
                        return false;
                }
                return true;
            });
            MenuPopupHelper menuHelper = new MenuPopupHelper(MediaViewerActivity.this, (MenuBuilder) menu, v);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        });
    }

    private void share() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(getMimeType(mFile.toString()));
        share.putExtra(Intent.EXTRA_STREAM, FileBackend.getUriForFile(this, mFile));
        try {
            startActivity(Intent.createChooser(share, getText(R.string.share_with)));
        } catch (ActivityNotFoundException e) {
            //This should happen only on faulty androids because normally chooser is always available
            Toast.makeText(this, R.string.no_application_found_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile() {
        this.xmppConnectionService.getFileBackend().deleteFile(mFile);
        finish();
    }

    private void open() {
        Uri uri;
        try {
            uri = FileBackend.getUriForFile(this, mFile);
        } catch (SecurityException e) {
            Log.d(Config.LOGTAG, "No permission to access " + mFile.getAbsolutePath(), e);
            Toast.makeText(this, this.getString(R.string.no_permission_to_access_x, mFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
            return;
        }
        String mime = MimeUtils.guessMimeTypeFromUri(this, uri);
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(uri, mime);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PackageManager manager = this.getPackageManager();
        List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
        if (info.size() == 0) {
            openIntent.setDataAndType(uri, "*/*");
        }
        try {
            this.startActivity(openIntent);
            finish();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_application_found_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("image")) {
                mFileUri = intent.getParcelableExtra("image");
                mFile = new File(mFileUri.getPath());
                if (mFileUri != null && mFile.exists() && mFile.length() > 0) {
                    try {
                        isImage = true;
                        DisplayImage(mFile, mFileUri);
                    } catch (Exception e) {
                        isImage = false;
                        Log.d(Config.LOGTAG, "Illegal exeption :" + e);
                        Toast.makeText(MediaViewerActivity.this, getString(R.string.error_file_corrupt), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(MediaViewerActivity.this, getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                }
            } else if (intent.hasExtra("video")) {
                mFileUri = intent.getParcelableExtra("video");
                mFile = new File(mFileUri.getPath());
                if (mFileUri != null && mFile.exists() && mFile.length() > 0) {
                    try {
                        isVideo = true;
                        DisplayVideo(mFileUri);
                    } catch (Exception e) {
                        isVideo = false;
                        Log.d(Config.LOGTAG, "Illegal exeption :" + e);
                        Toast.makeText(MediaViewerActivity.this, getString(R.string.error_file_corrupt), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(MediaViewerActivity.this, getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void DisplayImage(final File file, final Uri FileUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(file.getPath()).getAbsolutePath(), options);
        height = options.outHeight;
        width = options.outWidth;
        rotation = getRotation(Uri.parse("file://" + file.getAbsolutePath()));
        Log.d(Config.LOGTAG, "Image height: " + height + ", width: " + width + ", rotation: " + rotation);
        if (useAutoRotateScreen()) {
            rotateScreen(width, height, rotation);
        }
        mImage.setVisibility(View.VISIBLE);
        try {
            mImage.setImage(ImageSource.uri(FileUri));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_file_corrupt), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void DisplayVideo(final Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.getPath());
        Bitmap bitmap = null;
        try {
            bitmap = retriever.getFrameAtTime();
            height = bitmap.getHeight();
            width = bitmap.getWidth();
        } catch (Exception e) {
            height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Log.d(Config.LOGTAG, "Video height: " + height + ", width: " + width + ", rotation: " + rotation);
        if (useAutoRotateScreen()) {
            rotateScreen(width, height, rotation);
        }
        try {
            mVideo.setVisibility(View.VISIBLE);
            mVideo.setVideoURI(uri);
            mFullscreenbutton.setVisibility(View.INVISIBLE);
            mVideo.setShouldAutoplay(true);

        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_file_corrupt), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private int getRotation(Uri image) {
        InputStream is = null;
        try {
            is = this.getContentResolver().openInputStream(image);
            return ExifHelper.getOrientation(is);
        } catch (FileNotFoundException e) {
            return 0;
        } finally {
            close(is);
        }
    }

    private void rotateScreen(final int width, final int height, final int rotation) {
        if (width > height) {
            if (rotation == 0 || rotation == 180) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else if (width <= height) {
            if (rotation == 90 || rotation == 270) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = 1;
        }
        getWindow().setAttributes(layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mVideo.setShouldAutoplay(true);
        super.onResume();
    }

    @Override
    public void onPause() {
        mVideo.reset();
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = -1;
        }
        getWindow().setAttributes(layout);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(oldOrientation);
        super.onPause();
    }

    @Override
    public void onStop() {
        mVideo.reset();
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = -1;
        }
        getWindow().setAttributes(layout);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(oldOrientation);
        super.onStop();
    }

    @Override
    void onBackendConnected() {

    }

    public boolean useMaxBrightness() {
        return getPreferences().getBoolean("use_max_brightness", getResources().getBoolean(R.bool.use_max_brightness));
    }

    public boolean useAutoRotateScreen() {
        return getPreferences().getBoolean("use_auto_rotate", getResources().getBoolean(R.bool.auto_rotate));
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
}