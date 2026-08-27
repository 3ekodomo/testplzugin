package com.github.ekodomo3.imgbb;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import android.content.Intent;

public class ImagePreviewActivity extends AppCompatActivity {

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
                try {
                    imageView.setImageURI(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
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
