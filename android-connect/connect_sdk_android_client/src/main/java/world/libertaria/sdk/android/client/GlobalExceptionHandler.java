package world.libertaria.sdk.android.client;

import android.content.Context;
import android.util.Log;

import com.google.protobuf.compiler.PluginProtos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 22/11/2017.
 */

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final static String TAG = GlobalExceptionHandler.class.getSimpleName();

    public GlobalExceptionHandler() {
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            Log.e(TAG, "Uncaught exception! Oh, oh!: " + ex.getClass());
        } catch (Exception e) {
            Log.e(TAG, "Exception on the exception handler! Oh no!!", e);
        }

    }
}
