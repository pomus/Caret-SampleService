package com.wallrust.service.sample.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Communication with Caret infrastructure
 * @see <a href="https://xwiki.caret.co/bin/view/Services/">Caret Services</a>
 */
public class CaretApi {

    private static final String TAG = "CaretApi";

    public static final String CARET_API_URL = "https://prod-api.caret.co/api/v1.0/services/";
    public static final int CARET_CONSENT_REQUEST_CODE = 185235645;

    /**
     * Enable Caret integration for your service.
     * Async method, result UUID
     */
    public static void appInit(@NonNull final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = CARET_API_URL + CaretApiConfiguration.PHONE_NUMBER + "/appinit";
                String result = trySend(url, "POST", new JSONObject());
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    callback.ok(jsonObject.getString("uuid"));
                } catch (JSONException e) {
                    callback.error(result);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * <pre>
     * We need to pass the control to the Caret app on the mobile device with the information of our service
     * so the user can decide if she allows our service to publish customized status info.
     *
     * This method open the Caret application.
     *
     * The callback is {@link android.app.Activity#onActivityResult}, please implements onActivityResult method in your activity.
     * </pre>
     */
    public static void consent(Activity activity, String uuid) {
        Intent intent = new Intent("com.wallrust.caret.intent.action.SERVICES");
        intent.putExtra("service_id", CaretApiConfiguration.SERVICE_ID);
        intent.putExtra("uuid", uuid);
        intent.putExtra("name", CaretApiConfiguration.SERVICE_NAME);
        startCaretIntent(activity, intent);
    }

    private static void startCaretIntent(Activity activity, Intent intent) {
        List<ResolveInfo> infos = activity.getPackageManager().queryIntentActivities(intent, 0);
        if (infos.size() > 0) {
            if (CaretApiConfiguration.LOG) Log.d(TAG, "Open Caret consent dialog");
            activity.startActivityForResult(intent, CARET_CONSENT_REQUEST_CODE);
        } else {
            openCaretInGooglePlay(activity);
        }
    }

    private static void openCaretInGooglePlay(Activity activity) {
        if (CaretApiConfiguration.LOG) Log.d(TAG, "Caret not found, open Google Play");
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.wallrust.caret")));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.wallrust.caret")));
        }
    }

    @StringDef({AVAILABLE, BUSY, SLEEPING, DND, IN_VEHICLE, IN_CALL, LOW_BATTERY, SILENT, GAMING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Status {}

    public static final String AVAILABLE = "available";
    public static final String BUSY = "busy";
    public static final String SLEEPING = "sleeping";
    public static final String DND = "dnd";
    public static final String IN_VEHICLE = "in-vehicle";
    public static final String IN_CALL = "in-call";
    public static final String LOW_BATTERY = "low-battery";
    public static final String SILENT = "silent";
    public static final String GAMING = "gaming";

    /**
     * Sets status for the attached numbers.
     *
     * @param uuid Id from {@link #appInit}
     * @param status One of {@link #AVAILABLE}, {@link #BUSY}, {@link #SLEEPING}, {@link #DND}, {@link #IN_VEHICLE}, {@link #IN_CALL}, {@link #LOW_BATTERY}, {@link #SILENT} or {@link #GAMING}.
     */
    public static void publishStatus(@NonNull String uuid, @Status String status) {
        publishStatusWithText(uuid, status, "");
    }

    /**
     * Sets status for the attached numbers with an optional custom text.
     *
     * @param uuid Id from {@link #appInit}
     * @param status One of {@link #AVAILABLE}, {@link #BUSY}, {@link #SLEEPING}, {@link #DND}, {@link #IN_VEHICLE}, {@link #IN_CALL}, {@link #LOW_BATTERY}, {@link #SILENT} or {@link #GAMING}.
     * @param text optional
     */
    public static void publishStatusWithText(@NonNull String uuid, @Status String status, String text) {
        String url = CARET_API_URL + CaretApiConfiguration.PHONE_NUMBER + "/status";
        trySendAsync(url, "PUT", getStatusJSON(uuid, status, null, text));
    }

    /**
     * Sets status for the attached numbers with an context text & app values.
     *
     * @param uuid Id from {@link #appInit}
     * @param status One of {@link #AVAILABLE}, {@link #BUSY}, {@link #SLEEPING}, {@link #DND}, {@link #IN_VEHICLE}, {@link #IN_CALL}, {@link #LOW_BATTERY}, {@link #SILENT} or {@link #GAMING}.
     * @param context JSON, custom status information
     * @see <a href="https://xwiki.caret.co/bin/view/Services/#HPublishcustomstatusinformation">Publish custom status information</a>
     */
    public static void publishStatusWithContextJSON(@NonNull String uuid, @Status String status, JSONObject context) {
        String url = CARET_API_URL + CaretApiConfiguration.PHONE_NUMBER + "/status";
        trySendAsync(url, "PUT", getStatusJSON(uuid, status, context, ""));
    }

    private static final String SERVICE_OFF = "service-off";

    /**
     * Stop your service
     * If the user leaves your app, you can tell Caret that you won't publish any new status recently,
     * so the users' call status can be reverted back to the one before they started to use your app.
     *
     * @param uuid Id from {@link #appInit}
     */
    public static void serviceOff(String uuid) {
        String url = CARET_API_URL + CaretApiConfiguration.PHONE_NUMBER + "/status";
        trySendAsync(url, "PUT", getStatusJSON(uuid, SERVICE_OFF, null, ""));
    }

    /**
     * Disable Caret integration for your service.
     */
    public static void appDelete(final String uuid, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = CARET_API_URL + CaretApiConfiguration.PHONE_NUMBER + "/appdelete";
                String result = trySend(url, "PUT", gutUUID_JSON(uuid));
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("appdelete")) {
                        callback.ok(result);
                    } else {
                        callback.error(result);
                    }
                } catch (JSONException e) {
                    callback.error(result);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static JSONObject gutUUID_JSON(String uuid) {
        JSONObject data = new JSONObject();
        try {
            data.put("uuid", uuid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static JSONObject getStatusJSON(String target, String status, JSONObject context, String text) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", status);
            jsonObject.put("target", target);
            if (context != null) {
                jsonObject.put("context", context.toString());
            } else {
                jsonObject.put("custom_text", text);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private static void trySendAsync(final String url, final String requestMethod, final JSONObject json) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                trySend(url, requestMethod, json);
            }
        }).start();
    }

    private static String trySend(String url, String requestMethod, JSONObject json) {
        try {
            return send(url, requestMethod, json);
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    private static String send(String url, String requestMethod, JSONObject json) throws IOException {
        if (CaretApiConfiguration.LOG) Log.d(TAG, "Send, url: " + url + " requestMethod: " + requestMethod + " data: " + json);

        URL myURL = new URL(url);
        HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
        myURLConnection.setRequestMethod(requestMethod);
        myURLConnection.setRequestProperty("Authorization", getAuthorization());
        myURLConnection.setRequestProperty("Accept", "application/json");
        myURLConnection.setRequestProperty("Content-Type", "application/json");
        myURLConnection.setUseCaches(false);
        if ("PUT".equals(requestMethod) || "POST".equals(requestMethod)) {
            myURLConnection.setDoOutput(true);
            OutputStream os = myURLConnection.getOutputStream();
            os.write(json.toString().getBytes());
            os.close();
        }

        myURLConnection.connect();

        String response;

        if (myURLConnection.getResponseCode() == 200) {
            response = readInputStream(myURLConnection.getInputStream());
        } else {
            response = myURLConnection.getResponseMessage();
        }

        myURLConnection.disconnect();

        if (CaretApiConfiguration.LOG) Log.d(TAG, "Response: " + response);

        return response;
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private static String getAuthorization() {
        String secret = CaretApiConfiguration.SERVICE_ID + ":" + CaretApiConfiguration.SERVICE_SECRET;
        return "Basic " + Base64.encodeToString(secret.getBytes(), Base64.NO_WRAP);
    }

    public interface Callback {
        void ok(String result);
        void error(String error);
    }

}
