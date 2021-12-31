package com.mrz.austock;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.mrz.austock.activity.Notification;
import com.mrz.austock.utils.StringXORer;
import com.pusher.rest.Pusher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

public class AUStockAPP extends Application {

    static {
        System.loadLibrary("MRZGuard");
    }

    public void onCreate() {
        super.onCreate();
        SignatureCheck();
        startnotification();
    }

    private void startnotification() {
        if (!isServiceRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startService(new Intent(this, Notification.class));
            }
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (Notification.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
    }

    private void SignatureCheck() {
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (null != info && info.signatures.length > 0) {
            byte[] rawCertJava = info.signatures[0].toByteArray();
            byte[] rawCertNative = bytesFromJNI();
            Log.d("FIRMA", Arrays.toString(rawCertJava));
            String java = StringXORer.encode(getInfoFromBytes(rawCertJava));
            String natve = StringXORer.encode(getInfoFromBytes(rawCertNative));
            if (!java.equals(natve) && !natve.isEmpty() && !natve.equals("null")){
                Log.d("JAVA",java);
                Log.d("NATIVO", natve);
            } else if (java.equals(natve)){
                Toast.makeText(this,"Firma correcta",Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getInfoFromBytes(byte[] bytes) {
        if(null == bytes) {
            return "null";
        }

        /*
         * Get the X.509 certificate.
         */
        InputStream certStream = new ByteArrayInputStream(bytes);
        StringBuilder sb = new StringBuilder();
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);

            sb.append("Certificate subject: ").append(x509Cert.getSubjectDN()).append("\n");
            sb.append("Certificate issuer: ").append(x509Cert.getIssuerDN()).append("\n");
            sb.append("Certificate serial number: ").append(x509Cert.getSerialNumber()).append("\n");
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(bytes);
                byte[] byteArray = md.digest();
                //String hash_key = new String(Base64.encode(md.digest(), 0));
                sb.append("MD5: ").append(bytesToString(byteArray)).append("\n");
                md.reset();
                md = MessageDigest.getInstance("SHA");
                md.update(bytes);
                byteArray = md.digest();
                //String hash_key = new String(Base64.encode(md.digest(), 0));
                sb.append("SHA1: ").append(bytesToString(byteArray)).append("\n");
                md.reset();
                md = MessageDigest.getInstance("SHA256");
                md.update(bytes);
                byteArray = md.digest();
                sb.append("SHA256: ").append(bytesToString(byteArray)).append("\n");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


            sb.append("\n");
        } catch (CertificateException e) {
            // e.printStackTrace();
        }
        return sb.toString();
    }


    private String bytesToString(byte[] bytes) {
        StringBuilder md5StrBuff = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (Integer.toHexString(0xFF & bytes[i]).length() == 1) {
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & bytes[i]));
            } else {
                md5StrBuff.append(Integer.toHexString(0xFF & bytes[i]));
            }
            if (bytes.length - 1 != i) {
                md5StrBuff.append(":");
            }
        }
        return md5StrBuff.toString();
    }

    private native byte[] bytesFromJNI();

}
