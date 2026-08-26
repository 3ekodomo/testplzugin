package org.eu.thedoc.zettelnotes.interfaces;

import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import androidx.activity.result.ActivityResult;

public abstract class ButtonInterface {
  protected Callback mCallback;

  public abstract String getName();
  public abstract Listener getListener();

  public void registerCallback(Callback callback) {
    mCallback = callback;
  }

  public interface Listener {
    void onClick();
    boolean onLongClick();
  }

  public interface ActivityResultListener {
    void getActivityResult(ActivityResult result);
  }

  public interface Callback {
    void startActivityForResult(Intent intent);
    void setActivityResultListener(ActivityResultListener result);
    void insertText(String text);
    void replaceTextSelected(String text);
    Pair<Integer, String> getCurrentLine();
    void replaceCurrentLine(Pair<Integer, String> pair);
    String getTextSelected(boolean returnAllIfEmpty);
    void insertUri(Uri uri);
  }
}
