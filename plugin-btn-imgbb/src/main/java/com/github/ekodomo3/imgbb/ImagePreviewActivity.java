package com.github.ekodomo3.imgbb;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.github.chrisbanes.photoview.PhotoView;

public class ImagePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Preview");
        }

        PhotoView photoView = findViewById(R.id.photo_view);
        Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);

        if (imageUri != null) {
            photoView.setImageURI(imageUri);
        }
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
