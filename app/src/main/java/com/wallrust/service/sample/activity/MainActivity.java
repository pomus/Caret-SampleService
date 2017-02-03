package com.wallrust.service.sample.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wallrust.service.sample.R;
import com.wallrust.service.sample.util.CaretApi;
import com.wallrust.service.sample.util.CaretApiConfiguration;

/**
 * <pre>
 * Preparation:
 *   1. Login <a href="https://portal.caret.co/">Caret Web Communicator</a>
 *   2. Create new Service
 *   3. Copy Phone Number, Service name, Service Id and Service Secret Key to {@link CaretApiConfiguration}
 *
 * Service run a device:
 *   First time:
 *     1. Enable Caret integration for your service: {@link CaretApi#appInit}
 *     2. Ask Caret users for approving your service: ({@link CaretApi#consent}, open Caret application)
 *   All time:
 *     3. Publish status update. {@link CaretApi#publishStatus}
 *     4. Publish service-off, if the user leaves the app. {@link CaretApi#serviceOff}
 *    (5.) Delete service consent, if user wants to switch off Caret service {@link CaretApi#appDelete}
 * </pre>
 * @see <a href="https://xwiki.caret.co/bin/view/Services/">Caret Services</a>
 */

public class MainActivity extends Activity {

    private String UUID;

    /* 1. Init service */
    private void initCaretService() {

        CaretApi.appInit(new CaretApi.Callback() {
            @Override
            public void ok(String UUID) {
                updateUUID(UUID);
            }

            @Override
            public void error(String error) {
                toast("Service appInit error: " + error);
            }
        });
    }

    private void updateUUID(String uuid) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        pref.edit().putString("uuid", uuid).apply();
        UUID = uuid;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uuidText.setText(UUID);
                consentButton.setEnabled(true);
                deleteButton.setEnabled(true);
            }
        });
    }

    /* 2. Consent */
    private void consent(String uuid) {
        CaretApi.consent(this, uuid);
    }

    /* Caret consent callback */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CaretApi.CARET_CONSENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                successCaretConsentCallback();
            } else {
                cancelCaretConsentCallback(data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void successCaretConsentCallback() {
        toast("Caret consent success");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        pref.edit().putBoolean("successCaretConsent", true).apply();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendStatusButton.setEnabled(true);
                sendOffButton.setEnabled(true);
            }
        });
    }

    private void cancelCaretConsentCallback(Intent data) {
        if (data != null && data.hasExtra("result")) {
            toast(data.getStringExtra("result"));
        } else {
            toast("Caret consent canceled");
        }
    }

    /* 3. Update caret status */
    private void publishStatus() {
        CaretApi.publishStatusWithText(UUID, CaretApi.GAMING, "Hello word");
    }

    /* 4. Send service-off */
    private void sendServiceOff() {
        CaretApi.serviceOff(UUID);
    }

    /* 5. Delete service consents */
    private void serviceDelete() {
        CaretApi.appDelete(UUID, new CaretApi.Callback() {
            @Override
            public void ok(String result) {
                clearData();
            }

            @Override
            public void error(String error) {
                toast("App appDelete error: " + error);
            }
        });
    }

    private void clearData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        preferences.edit().clear().commit();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setupScreen();
            }
        });
    }

    /* Sample activity screen */

    private View deleteButton;
    private View consentButton;
    private View sendStatusButton;
    private View sendOffButton;
    private TextView uuidText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        deleteButton = findViewById(R.id.delete);
        deleteButton.setOnClickListener(clickListener);

        findViewById(R.id.init).setOnClickListener(clickListener);

        uuidText = ((TextView)findViewById(R.id.uuid));

        consentButton = findViewById(R.id.consent);
        consentButton.setOnClickListener(clickListener);

        sendStatusButton = findViewById(R.id.status);
        sendStatusButton.setOnClickListener(clickListener);

        sendOffButton = findViewById(R.id.off);
        sendOffButton.setOnClickListener(clickListener);

        setupScreen();
    }

    private void setupScreen() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        UUID = preferences.getString("uuid", "");
        if (TextUtils.isEmpty(UUID)) {
            consentButton.setEnabled(false);
            deleteButton.setEnabled(false);
            uuidText.setText("");
        } else {
            uuidText.setText(UUID);
            deleteButton.setEnabled(true);
        }

        boolean consent = preferences.getBoolean("successCaretConsent", false);
        sendStatusButton.setEnabled(consent);
        sendOffButton.setEnabled(consent);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.delete: serviceDelete(); break;
                case R.id.init: initCaretService(); break;
                case R.id.consent: consent(UUID); break;
                case R.id.status: publishStatus(); break;
                case R.id.off: sendServiceOff(); break;
            }
        }
    };


    private void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

}