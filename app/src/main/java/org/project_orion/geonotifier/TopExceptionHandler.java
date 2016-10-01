package org.project_orion.geonotifier;

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// http://stackoverflow.com/questions/3643395/how-to-get-android-crash-logs
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Activity activity;

    public TopExceptionHandler(Activity activity) {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.activity = activity;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        String report = ex.toString() + "\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (StackTraceElement item : stackTrace)
            report += "    " + item.toString() + "\n";
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable cause = ex.getCause();
        if (cause != null) {
            report += "--------- Cause ---------\n\n";
            report += cause.toString() + "\n\n";
            stackTrace = cause.getStackTrace();
            for (StackTraceElement item : stackTrace)
                report += "    " + item.toString() + "\n";
            report += "-------------------------------\n\n";
        }

        saveReport(report);

        defaultHandler.uncaughtException(thread, ex);
    }

    private void saveReport(String report) {
        try {
            FileOutputStream trace = activity.openFileOutput("stack.trace", 0);
            trace.write(report.getBytes());
            trace.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
