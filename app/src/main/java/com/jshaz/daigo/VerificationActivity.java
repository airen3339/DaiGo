package com.jshaz.daigo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jshaz.daigo.intents.UserIntent;
import com.jshaz.daigo.serverutil.ServerUtil;
import com.jshaz.daigo.ui.BaseActivity;
import com.jshaz.daigo.ui.PopUtil;
import com.jshaz.daigo.ui.ToolBarView;
import com.jshaz.daigo.util.Utility;
import com.yalantis.ucrop.UCrop;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class VerificationActivity extends BaseActivity {

    private ToolBarView toolBarView;

    private EditText ETRealName;
    private EditText ETIDCode;
    private EditText ETCampusIDCode;

    private ImageView[] IVs = new ImageView[3];

    private PopUtil pop;
    private LinearLayout layoutPopUp;

    private Uri imageUri;
    private Uri imageCroppedUri;
    private File outputImage;
    private File croppedImage;

    public static final int CAMERA = 0;
    public static final int GALLARY = 1;

    private int selectImage = 0;
    private Bitmap[] BMPs = new Bitmap[3];

    private Button submit;

    private Thread uploadThread;

    private ProgressDialog uploadDialog;

    private UserIntent userIntent;

    private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        userIntent = (UserIntent) getIntent().getSerializableExtra("user");

        initView();

        mHandler = new MyHandler(this);

        initImageFile();
        initPop();
    }

    /**
     * ?????????View
     */
    private void initView() {
        toolBarView = (ToolBarView) findViewById(R.id.verify_toolbar);
        ETRealName = (EditText) findViewById(R.id.verify_real_name);
        ETIDCode = (EditText) findViewById(R.id.verify_idcode);
        ETCampusIDCode = (EditText) findViewById(R.id.verify_campus_idcode);
        IVs[0] = (ImageView) findViewById(R.id.verify_idcard_up);
        IVs[1] = (ImageView) findViewById(R.id.verify_idcard_down);
        IVs[2] = (ImageView) findViewById(R.id.verify_schoolcard);
        submit = (Button) findViewById(R.id.verify_submit);

        toolBarView.setBackButtonVisible(true);
        toolBarView.setBackButtonImage(R.mipmap.icon_back);
        toolBarView.setBackButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolBarView.setTitleCampusVisible(false);
        toolBarView.setTitleText("????????????");

        IVs[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage = 0;
                pop.showAtLocation(view, Gravity.BOTTOM, 0, 0);
            }
        });

        IVs[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage = 1;
                pop.showAtLocation(view, Gravity.BOTTOM, 0, 0);
            }
        });

        IVs[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage = 2;
                pop.showAtLocation(view, Gravity.BOTTOM, 0, 0);
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ETRealName.getText().toString().equals("")) {
                    Toast.makeText(VerificationActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                } else if (ETIDCode.getText().toString().equals("") ||
                        ETIDCode.getText().length() < 18) {
                    Toast.makeText(VerificationActivity.this, "??????????????????????????????", Toast.LENGTH_SHORT).show();
                } else if (ETCampusIDCode.getText().toString().equals("")) {
                    Toast.makeText(VerificationActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
                } else if (BMPs[0] == null) {
                    Toast.makeText(VerificationActivity.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
                } else if (BMPs[1] == null) {
                    Toast.makeText(VerificationActivity.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
                } else if (BMPs[2] == null) {
                    Toast.makeText(VerificationActivity.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
                } else {
                    //??????????????????
                    String realName = ETRealName.getText().toString();
                    String idCode = ETIDCode.getText().toString();
                    String schoolCode = ETCampusIDCode.getText().toString();
                    String idUp = Utility.convertBitmapToString(BMPs[0]);
                    String idDown = Utility.convertBitmapToString(BMPs[1]);
                    String schCard = Utility.convertBitmapToString(BMPs[2]);
                    upLoadInfo(realName, idCode, schoolCode, idUp, idDown, schCard);
                    //Toast.makeText(VerificationActivity.this, "???????????????????????????", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * ???????????????
     */
    private void initPop() {
        pop = new PopUtil(VerificationActivity.this, R.layout.clip_pop_windows,false);
        layoutPopUp = pop.getPopup();

        TextView tvCamera = (TextView) layoutPopUp.findViewById(R.id.clip_item_popupwindows_camera);
        TextView tvAlbum = (TextView) layoutPopUp.findViewById(R.id.clip_item_popupwindows_Photo);
        TextView tvCancel = (TextView) layoutPopUp.findViewById(R.id.clip_item_popupwindows_cancel);

        /*??????*/
        tvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
                overridePendingTransition(R.anim.translate_bottom_in, R.anim.translate_bottom_out);
                pop.dismiss();
                layoutPopUp.clearAnimation();
            }
        });

        /*???????????????*/
        tvAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent, GALLARY);

                overridePendingTransition(R.anim.translate_bottom_in, R.anim.translate_normal);
                pop.dismiss();
                layoutPopUp.clearAnimation();
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop.dismiss();
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CAMERA:
                if (resultCode == RESULT_OK) {
                    openCrop(imageUri, imageCroppedUri);
                } else {
                    Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                }
                break;
            case GALLARY:
                if (resultCode == RESULT_OK) {
                    String filePath = null;
                    if (Build.VERSION.SDK_INT >= 19) {
                        filePath = handleImageOnKitKat(data);
                    } else {
                        filePath = handleImageBeforeKitKat(data);
                    }
                    //?????????????????????URI
                    if (Build.VERSION.SDK_INT >= 24) {
                        imageUri = FileProvider.getUriForFile(VerificationActivity.this,
                                "com.jshaz.daigo.fileprovider", new File(filePath));
                        imageCroppedUri = FileProvider.getUriForFile(VerificationActivity.this,
                                "com.jshaz.daigo.fileprovider", croppedImage);
                    } else {
                        imageUri = Uri.fromFile(new File(filePath));
                        imageCroppedUri = Uri.fromFile(croppedImage);
                    }
                    openCrop(imageUri, imageCroppedUri);
                } else {
                    Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                }
                break;
            case UCrop.REQUEST_CROP:
                final Uri resultUri = UCrop.getOutput(data);
                BMPs[selectImage] = BitmapFactory.decodeFile(croppedImage.getAbsolutePath());
                IVs[selectImage].setImageBitmap(BMPs[selectImage]);
                break;
            case UCrop.RESULT_ERROR:
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * ???????????????????????????
     */
    private void initImageFile() {
        outputImage = new File(getExternalCacheDir(), "cache.jpg");
        croppedImage = new File(getExternalCacheDir(), "crop.jpg");
    }

    /**
     * ??????????????????
     */
    private void takePhoto() {

        try{
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
            if (croppedImage.exists()) {
                croppedImage.delete();
            }
            croppedImage.createNewFile();
        } catch (IOException e) {
            Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(VerificationActivity.this,
                    "com.jshaz.daigo.fileprovider", outputImage);
            imageCroppedUri = FileProvider.getUriForFile(VerificationActivity.this,
                    "com.jshaz.daigo.fileprovider", croppedImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
            imageCroppedUri = Uri.fromFile(croppedImage);
        }
        //??????????????????
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA);
    }

    /**
     * ????????????????????????
     */
    @TargetApi(19)
    private String handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        return imagePath;
    }

    private String handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        return getImagePath(uri, null);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }


    /**
     * ????????????API
     * @param sourceUri
     * @param destinationUri
     */
    private void openCrop(Uri sourceUri, Uri destinationUri) {
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(16, 9)
                .withMaxResultSize(320, 320)
                .start(this);
    }

    /**
     * ????????????
     * @param realName ????????????
     * @param idCode ????????????
     * @param schoolCode ??????
     * @param idUp ???????????????
     * @param idDown ???????????????
     * @param schCard ???????????????
     */
    private void upLoadInfo(final String realName, final String idCode, final String schoolCode, final String idUp,
                            final String idDown, final String schCard) {
        startUploadDialog();
        uploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                String response = "";
                try {
                    BasicHttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                    HttpConnectionParams.setSoTimeout(httpParams, 5000);

                    HttpClient httpclient = new DefaultHttpClient(httpParams);

                    //????????????????????????Servlet
                    HttpPost httpPost = new HttpPost(ServerUtil.SLVerification);

                    List<NameValuePair> params = new ArrayList<NameValuePair>();//???????????????list
                    params.add(new BasicNameValuePair("realname", realName));
                    params.add(new BasicNameValuePair("idcode", idCode));
                    params.add(new BasicNameValuePair("schoolcode", schoolCode));
                    params.add(new BasicNameValuePair("idup", idUp));
                    params.add(new BasicNameValuePair("iddown", idDown));
                    params.add(new BasicNameValuePair("schoolcard", schCard));
                    params.add(new BasicNameValuePair("userid", userIntent.getUserId()));

                    final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "utf-8");//???UTF-8????????????
                    httpPost.setEntity(entity);
                    //???????????????????????????
                    HttpResponse httpResponse = httpclient.execute(httpPost);
                    if (httpResponse.getStatusLine().getStatusCode() == 200)//???5000??????????????????????????????
                    {
                        HttpEntity entity1 = httpResponse.getEntity();
                        response = EntityUtils.toString(entity1, "utf-8");//???UTF-8????????????
                        Message message = mHandler.obtainMessage();
                            message.what = 0;
                            message.obj = response;
                            mHandler.handleMessage(message);
                    } else {
                        Message message = mHandler.obtainMessage();
                        message.what = 1;
                        mHandler.handleMessage(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message message = mHandler.obtainMessage();
                    message.what = 1;
                    mHandler.handleMessage(message);
                }
                Looper.loop();
            }
        });
        uploadThread.start();
    }

    /**
     * ???????????????????????????
     */
    private void startUploadDialog() {
        if (uploadDialog == null) {
            uploadDialog = new ProgressDialog(this);
        }
        uploadDialog.setTitle("????????????");
        uploadDialog.setMessage("??????????????????...");
        uploadDialog.setCancelable(false);
        uploadDialog.setCanceledOnTouchOutside(false);
        uploadDialog.show();
    }

    /**
     * ???????????????????????????
     */
    private void stopUploadDialog() {
        if (uploadDialog != null) {
            uploadDialog.dismiss();
        }
    }

    private static class MyHandler extends Handler {

        WeakReference<VerificationActivity> activityWeakReference;

        public MyHandler(VerificationActivity activity) {
            this.activityWeakReference = new WeakReference<VerificationActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            activityWeakReference.get().stopUploadDialog();
            switch (msg.what) {
                case 0:
                    String response = (String) msg.obj;
                    if (response.equals("false")) {
                        Toast.makeText(activityWeakReference.get(), "???????????????????????????\n??????????????????", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activityWeakReference.get(), "????????????", Toast.LENGTH_SHORT).show();
                        activityWeakReference.get().finish();
                    }
                    break;
                case 1:
                    Toast.makeText(activityWeakReference.get(), "????????????", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
