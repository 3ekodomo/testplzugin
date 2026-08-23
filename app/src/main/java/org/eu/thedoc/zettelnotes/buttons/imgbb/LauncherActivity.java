package org.eu.thedoc.zettelnotes.buttons.imgbb;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Discovery/info activity. Zettel Notes discovers this app through the
 * org.eu.thedoc.zettelnotes.intent.buttons intent action.
 */
public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
