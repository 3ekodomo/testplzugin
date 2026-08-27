package com.github.ekodomo3.imgbb;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePreviewActivity extends AppCompatActivity {
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Preview");
        }

        ZoomImageView imageView = findViewById(R.id.zoom_image_view);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("image_uri")) {
            String uriString = intent.getStringExtra("image_uri");
            if (uriString != null) {
                Uri uri = Uri.parse(uriString);
                boolean compress = intent.getBooleanExtra("compress", false);
                int quality = intent.getIntExtra("quality", 100);

                if (compress) {
                    executor.execute(() -> {
                        try {
                            Bitmap bitmap = decodeSampledBitmapFromUri(uri, 2048, 2048);
                            if (bitmap != null) {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                                byte[] compressedData = out.toByteArray();
                                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedData, 0, compressedData.length);
                                runOnUiThread(() -> imageView.setImageBitmap(compressedBitmap));
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error loading compressed image", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            });
                        }
                    });
                } else {
                    try {
                        imageView.setImageURI(uri);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
