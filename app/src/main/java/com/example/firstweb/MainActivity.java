package com.example.firstweb;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewRenderProcessClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity  {


    String url="https://www.ust.cl";
    SwipeRefreshLayout mySwipeRefreshLayout;
    WebView myWebview;
    final Context context = this;

    private final int MY_PERMISSIONS_REQUEST_PHONE_STATE = 0;
    private final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 0;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int  INPUT_FILE_REQUEST_CODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();


    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RequestPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myWebview = (WebView) findViewById(R.id.webview);
        assert myWebview != null;

        WebSettings webSettings = myWebview.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        myWebview.getSettings().setAppCacheEnabled(true);
        myWebview.getSettings().setDatabaseEnabled(true);
        myWebview.getSettings().setDomStorageEnabled(true);

        if(Build.VERSION.SDK_INT >= 29) {
            webSettings.setMixedContentMode(0);
            myWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT >= 19) {
            myWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT < 19) {
            myWebview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        myWebview.setWebViewClient(new WebViewClient());
        myWebview.setWebChromeClient(new WebChromeClient());
        //myWebview.setWebChromeClient((new myWebChromeClient()));
        myWebview.setWebViewClient(new MyWebViewClient());
        myWebview.setVerticalScrollBarEnabled(false);

        myWebview.loadUrl(url);

        mySwipeRefreshLayout = this.findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                findViewById(R.id.loaderwebview).setVisibility(View.VISIBLE);
                myWebview.reload();
                mySwipeRefreshLayout.setRefreshing(false);
            }
        });

        myWebview.setWebChromeClient(new WebChromeClient() {
            // for Lollipop, all in one
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;



                //AAA
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {

                    // create the file where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    // continue only if the file was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                //AA

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Seleccione una imagen");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });
    }


    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){
            super.onPageStarted(view,url,favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            if(url.indexOf("tel:") > -1){
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                return true;
            }else if(url.indexOf("out:") > -1){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("out:",""))));
                return true;
            }else if(url.indexOf("mailto:") > -1){
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            }else if(url.startsWith("https://wwww.ust.cl")){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }else{
                view.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url){
            findViewById(R.id.loaderwebview).setVisibility(View.GONE);
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed(){
        if(myWebview != null && myWebview.canGoBack()){
            myWebview.goBack();
        }else{
            super.onBackPressed();
        }
    }

    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "-";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName,".jpg",storageDir);
        return imageFile;
    }

    public void OpenSettingsApp(){
        Intent ii = new Intent();
        ii.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        ii.addCategory(Intent.CATEGORY_DEFAULT);
        ii.setData(Uri.parse("package:" + this.getPackageName()));
        ii.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ii.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        ii.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(ii);
    }

    public void RequestPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_PHONE_STATE);
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                AlertDialog.Builder a_builder = new AlertDialog.Builder(MainActivity.this);
                a_builder.setMessage("La aplicacion necesita permisos de localizacion").setCancelable(false).setPositiveButton("Dar Permisos", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OpenSettingsApp();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = a_builder.create();
                alert.setTitle(R.string.app_name);
                alert.show();
            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_PHONE_STATE);
            }
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

}
























