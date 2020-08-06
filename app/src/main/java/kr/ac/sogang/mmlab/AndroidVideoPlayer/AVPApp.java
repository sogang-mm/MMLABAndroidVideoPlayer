package kr.ac.sogang.mmlab.AndroidVideoPlayer;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.CrashScreenActivity;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

public class AVPApp extends Application implements Thread.UncaughtExceptionHandler
{
    private static final int MAX_STACK_TRACE_SIZE = 131000;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    static final int PERMISSIONS_REQUEST_CODE = 1000;

    ICrashListener crashListener;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread crashThread, Throwable ex)
    {
        //get crash details
        String crashedThreadName = crashThread.getName();
        String crashCauseShort = ex.toString();
        String crashCauseMessage = ex.getMessage();
        String crashCauseStacktrace = getStackTrace(ex);

        //log error details
        Logging.logE("[AndroidVideoPlayerApp] unhandled exception in Thread %s: \n" +
                "Cause: %s \n" +
                "%s \n" +
                "%s \n", crashedThreadName, crashCauseShort, crashCauseMessage, crashCauseStacktrace);

        //invoke crash listener
        if (crashListener != null)
            crashListener.onCrash(ex);

        //start the crash screen activity
        Intent crashScreenIntent = new Intent(this, CrashScreenActivity.class);
        crashScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        //shorten stack trace length for intent so we dont crash
        if (crashCauseStacktrace.length() > MAX_STACK_TRACE_SIZE) {
            String dis = " [stack trace too large]";
            crashCauseStacktrace = crashCauseStacktrace.substring(0, MAX_STACK_TRACE_SIZE - dis.length()) + dis;
        }

        //put crash details into error
        crashScreenIntent.putExtra(CrashScreenActivity.INTENT_EXTRA_THREAD_NAME, crashedThreadName);
        crashScreenIntent.putExtra(CrashScreenActivity.INTENT_EXTRA_CAUSE_SHORT, crashCauseShort);
        crashScreenIntent.putExtra(CrashScreenActivity.INTENT_EXTRA_CAUSE_MESSAGE, crashCauseMessage);
        crashScreenIntent.putExtra(CrashScreenActivity.INTENT_EXTRA_CAUSE_STACKTRACE, crashCauseStacktrace);

        //start activity
        startActivity(crashScreenIntent);

        //close this process
        killCurrentProcess();
    }

    /**
     * Get the stack trace from a throwable
     *
     * @param ex the throwable whose stacktrace should be returned
     * @return the stacktrace
     */
    private String getStackTrace(Throwable ex)
    {
        StringWriter strWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strWriter);
        ex.printStackTrace(writer);

        return strWriter.toString();
    }

    /**
     * Kill the current process immediately
     */
    private void killCurrentProcess()
    {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    /**
     * Set the crash listener
     *
     * @param listener the new listener
     */
    public void setCrashListener(ICrashListener listener)
    {
        crashListener = listener;
    }

    /**
     * Handler for app crashes.
     */
    public interface ICrashListener
    {
        /**
         * Called when a app crash occurs
         *
         * @param ex the exception that caused the crash
         */
        void onCrash(Throwable ex);
    }
}
