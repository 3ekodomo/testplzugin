package com.github.ekodomo3.imgbb;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.eu.thedoc.zettelnotes.plugins.base.BaseActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity
    extends BaseActivity {

  public static final String INTENT_EXTRA_INSERT_TEXT = "intent-extra-insert-text";
  public static final String ERROR_STRING = "intent-error";

  private static final long MAX_UPLOAD_BYTES = 32L * 1024L * 1024L; // ImgBB's stated limit

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private ProgressBar progress;
  private TextView status;
  private ActivityResultLauncher<String> picker;
  private Api api;
  private List<Uri> selectedUris = new ArrayList<>();
  private LinearLayout imagesListContainer;
  private LinearLayout compressSettingsContainer;
  private TextView tvQuality;
  private SeekBar sbQuality;
  private CheckBox cbCompress;
  private CheckBox cbShortenName;
  private Button btnUpload;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    status = findViewById(R.id.status_text);
    Button choose = findViewById(R.id.btn_choose);
    Button paste = findViewById(R.id.btn_paste);
    progress = findViewById(R.id.progress_bar);
    imagesListContainer = findViewById(R.id.images_list_container);
    compressSettingsContainer = findViewById(R.id.compress_settings_container);
    tvQuality = findViewById(R.id.tv_quality);
    sbQuality = findViewById(R.id.sb_quality);
    cbCompress = findViewById(R.id.cb_compress);
    cbShortenName = findViewById(R.id.cb_shorten_name);
    btnUpload = findViewById(R.id.btn_upload);

    api = new Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api.class);

    picker = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
      if (uris != null && !uris.isEmpty()) {
        selectedUris.addAll(uris);
        updateImagesList();
      }
    });

    choose.setOnClickListener(v -> picker.launch("image/*"));

    paste.setOnClickListener(v -> {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    if (uri != null) {
                        selectedUris.add(uri);
                    }
                }
                if (!selectedUris.isEmpty()) {
                    updateImagesList();
                } else {
                    Toast.makeText(this, "No image found in clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
        }
    });

    cbCompress.setOnCheckedChangeListener((buttonView, isChecked) -> {
      compressSettingsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      updateImagesList();
    });

    btnUpload.setOnClickListener(v -> uploadImages());

    sbQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        tvQuality.setText("Quality: " + progress + "%");
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        updateImagesList();
      }
    });
  }

  private void updateImagesList() {
    imagesListContainer.removeAllViews();
    if (selectedUris.isEmpty()) {
      btnUpload.setVisibility(View.GONE);
      status.setText("Choose images from your device or paste.");
      return;
    }

    btnUpload.setVisibility(View.VISIBLE);
    status.setText(selectedUris.size() + " image(s) selected.");

    for (Uri uri : selectedUris) {
      LinearLayout itemView = new LinearLayout(this);
      itemView.setOrientation(LinearLayout.HORIZONTAL);
      itemView.setPadding(0, dp(10), 0, dp(10));

      ImageView imageView = new ImageView(this);
      LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(64), dp(64));
      imageParams.setMarginEnd(dp(10));
      imageView.setLayoutParams(imageParams);
      imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

      int thumbnailSize = dp(64);
      executor.execute(() -> {
          Bitmap thumbnail = decodeSampledBitmapFromUri(uri, thumbnailSize, thumbnailSize);
          if (thumbnail != null) {
              runOnUiThread(() -> imageView.setImageBitmap(thumbnail));
          }
      });

      LinearLayout textContainer = new LinearLayout(this);
      textContainer.setOrientation(LinearLayout.VERTICAL);

      TextView text1 = new TextView(this);
      text1.setTextSize(16);
      TextView text2 = new TextView(this);
      text2.setTextSize(14);

      textContainer.addView(text1);
      textContainer.addView(text2);

      itemView.addView(imageView);
      itemView.addView(textContainer);

      itemView.setOnClickListener(v -> {
          Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);
          intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_URI, uri);
          startActivity(intent);
      });

      String filename = getFileName(uri);
      if (filename == null || filename.trim().isEmpty()) {
        filename = "image";
      }

      long originalSize = getFileSize(uri);

      if (cbCompress.isChecked()) {
        int quality = sbQuality.getProgress();
        text1.setText(filename);
        text2.setText("Calculating...");

        executor.execute(() -> {
            File compressedFile = compressImageToCache(uri, quality);
            if (compressedFile != null) {
               long compressedSize = compressedFile.length();
               runOnUiThread(() -> text2.setText(String.format("Original: %s | Compressed (%d%%): %s", formatSize(originalSize), quality, formatSize(compressedSize))));
               compressedFile.delete(); // Clean up temporary preview file
            } else {
               runOnUiThread(() -> text2.setText("Size: " + formatSize(originalSize) + " (Failed to compress)"));
            }
        });
      } else {
        text1.setText(filename);
        text2.setText("Size: " + formatSize(originalSize));
      }
      imagesListContainer.addView(itemView);
    }
  }

  private long getFileSize(Uri uri) {
    try {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
            cursor.close();
        }
    } catch (Exception ignored) {}
    return 0;
  }

  private String formatSize(long size) {
      if (size <= 0) return "0 B";
      final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
      int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
      return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
  }

  private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
      try {
          InputStream in = getContentResolver().openInputStream(uri);
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inJustDecodeBounds = true;
          BitmapFactory.decodeStream(in, null, options);
          if (in != null) in.close();

          options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
          options.inJustDecodeBounds = false;

          in = getContentResolver().openInputStream(uri);
          Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
          if (in != null) in.close();
          return bitmap;
      } catch (Exception e) {
          e.printStackTrace();
          return null;
      }
  }

  private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
      final int height = options.outHeight;
      final int width = options.outWidth;
      int inSampleSize = 1;

      if (height > reqHeight || width > reqWidth) {
          final int halfHeight = height / 2;
          final int halfWidth = width / 2;
          while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
              inSampleSize *= 2;
          }
      }
      return inSampleSize;
  }

  private File compressImageToCache(Uri uri, int quality) {
      try {
          Bitmap bitmap = decodeSampledBitmapFromUri(uri, 2048, 2048);
          if (bitmap == null) return null;

          File temp = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
          FileOutputStream out = new FileOutputStream(temp);
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
          out.flush();
          out.close();
          return temp;
      } catch (Exception e) {
          e.printStackTrace();
          return null;
      }
  }

  private void uploadImages() {
    String key = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE).getString(SettingsActivity.PREF_API_KEY, "");

    if (key == null || key.trim().isEmpty()) {
      Toast.makeText(this, "Set the ImgBB API key first", Toast.LENGTH_LONG).show();
      startActivity(new Intent(this, SettingsActivity.class));
      finish();
      return;
    }

    if (selectedUris.isEmpty()) {
       finishWithError("No images selected");
       return;
    }

    setLoading(true);

    executor.execute(() -> {
      StringBuilder markdownResult = new StringBuilder();

      for (int i = 0; i < selectedUris.size(); i++) {
          Uri uri = selectedUris.get(i);
          File temp = null;
          try {
            String originalFilename = getFileName(uri);
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
              originalFilename = "image";
            }

            String uploadFilename = originalFilename;
            if (cbShortenName.isChecked()) {
                uploadFilename = UUID.randomUUID().toString().substring(0, 8);
                // preserve extension if possible
                int dotIndex = originalFilename.lastIndexOf('.');
                if (dotIndex > 0) {
                    uploadFilename += originalFilename.substring(dotIndex);
                }
            }

            String mime = getContentResolver().getType(uri);
            if (mime == null) {
              mime = URLConnection.guessContentTypeFromName(originalFilename);
            }
            if (mime == null) {
              mime = "application/octet-stream";
            }

            if (cbCompress.isChecked()) {
                temp = compressImageToCache(uri, sbQuality.getProgress());
                if (temp == null) {
                     throw new IllegalStateException("Failed to compress image");
                }
                mime = "image/jpeg";
            } else {
                temp = new File(getCacheDir(), "imgbb_" + System.currentTimeMillis() + "_" + i);
                try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(temp)) {
                  if (in == null) {
                    throw new IllegalStateException("Cannot read selected image");
                  }
                  byte[] buffer = new byte[8192];
                  int n;
                  long total = 0;
                  while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > MAX_UPLOAD_BYTES) {
                      throw new IllegalArgumentException("ImgBB allows images up to 32 MB");
                    }
                    out.write(buffer, 0, n);
                  }
                }
            }

            RequestBody body = RequestBody.create(MediaType.parse(mime), temp);
            MultipartBody.Part part = MultipartBody.Part.createFormData("image", uploadFilename, body);
            RequestBody name = RequestBody.create(MediaType.parse("text/plain"), uploadFilename);

            Response<ApiResponse> response = api.upload(key.trim(), part, name).execute();

            if (temp.exists()) {
              temp.delete();
            }

            ApiResponse apiBody = response.body();
            if (!response.isSuccessful() || apiBody == null || !apiBody.success || apiBody.data == null || apiBody.data.url == null) {
               final String errMsg = extractErrorMessage(response);
               runOnUiThread(() -> finishWithError(errMsg));
               return; // abort further uploads
            }

            markdownResult.append("![image](").append(apiBody.data.url).append(")\n");

          } catch (Exception e) {
            if (temp != null && temp.exists()) {
              temp.delete();
            }
            final String errMsg = e.getMessage() == null ? "Unable to read/upload image" : e.getMessage();
            runOnUiThread(() -> finishWithError(errMsg));
            return;
          }
      }

      runOnUiThread(() -> {
          setResult(RESULT_OK, new Intent().putExtra(INTENT_EXTRA_INSERT_TEXT, markdownResult.toString().trim()));
          finish();
      });
    });
  }

  private String extractErrorMessage(Response<ApiResponse> response) {
    ApiResponse body = response.body();
    if (body != null && body.error != null && body.error.message != null && !body.error.message.isEmpty()) {
      return "ImgBB upload failed: " + body.error.message;
    }
    return "ImgBB upload failed (" + response.code() + ")";
  }

  private void setLoading(boolean loading) {
    progress.setVisibility(loading ? ProgressBar.VISIBLE : ProgressBar.GONE);
    status.setText(loading ? "Uploading..." : "Choose an image from your device.");
  }

  private void finishWithError(String message) {
    runOnUiThread(() -> {
      setResult(RESULT_CANCELED, new Intent().putExtra(ERROR_STRING, message));
      Toast
          .makeText(this, message, Toast.LENGTH_LONG)
          .show();
      finish();
    });
  }

  private String getFileName(Uri uri) {
    Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

    try (cursor) {
      if (cursor == null) {
        return null;
      }
      int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
      return index >= 0 && cursor.moveToFirst() ? cursor.getString(index) : null;
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    executor.shutdown();
  }
}



