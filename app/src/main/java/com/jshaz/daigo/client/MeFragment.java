package com.jshaz.daigo.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.jshaz.daigo.LoginActivity;
import com.jshaz.daigo.ModifyInfoActivity;
import com.jshaz.daigo.R;
import com.jshaz.daigo.intents.UserIntent;
import com.jshaz.daigo.interfaces.BaseClassImpl;
import com.jshaz.daigo.serverutil.ServerUtil;
import com.jshaz.daigo.service.DownloadService;
import com.jshaz.daigo.ui.ComplexButton;
import com.jshaz.daigo.ui.ToolBarView;
import com.jshaz.daigo.util.AppInfo;
import com.jshaz.daigo.util.NetThread;
import com.jshaz.daigo.util.Setting;
import com.jshaz.daigo.util.User;
import com.jshaz.daigo.util.UserDatabaseHelper;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by jshaz on 2017/11/21.
 */

public class MeFragment extends Fragment {

    View view = null;

    private boolean isUpdate = false;

    private ComplexButton CBModifyInfo;
    private ComplexButton CBMyOrder;
    private ComplexButton CBSettings;
    private ComplexButton CBAboutUs;
    private ComplexButton CBReport;
    private ComplexButton CBOpenDrawer;
    private ComplexButton CBUpdate;

    private Button logout;

    private Setting setting;

    private User user;

    private ClientMainActivity parentActivity;
    private ToolBarView toolBarView;

    private ProgressDialog progressDialog;

    private String logContent;

    private MyHandler handler;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view != null) return view;
        view = inflater.inflate(R.layout.fragment_me, container, false);

        setting = new Setting(getContext());
        setting.readFromLocalSharedPref();

        user = new User(getContext());

        CBModifyInfo = (ComplexButton) view.findViewById(R.id.me_modify_info);
        CBMyOrder = (ComplexButton) view.findViewById(R.id.me_my_order);
        CBAboutUs = (ComplexButton) view.findViewById(R.id.me_about_us);
        CBReport = (ComplexButton) view.findViewById(R.id.me_report);
        CBSettings = (ComplexButton) view.findViewById(R.id.me_settings);
        CBOpenDrawer = (ComplexButton) view.findViewById(R.id.me_open_drawer);
        CBUpdate = (ComplexButton) view.findViewById(R.id.me_about_update);
        logout = (Button) view.findViewById(R.id.me_logout);


        CBOpenDrawer.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBOpenDrawer.setItemName("????????????/????????????");
        CBOpenDrawer.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolBarView.callOnBackButtonClick();
            }
        });


        CBModifyInfo.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //??????????????????
                user.readFromLocalSharedPref();
                if (user.getUserId() == null || user.getUserId().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setNegativeButton("??????", null);
                    builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(getContext(), LoginActivity.class));
                            parentActivity.finish();
                        }
                    });
                    builder.setMessage("???????????????");
                    builder.show();
                } else {
                    Intent intent = new Intent(getContext(), ModifyInfoActivity.class);
                    startActivity(intent);
                }

            }
        });
        CBModifyInfo.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBModifyInfo.setItemName("??????????????????");

        CBMyOrder.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //????????????
                user.readFromLocalSharedPref();
                if (user.getUserId() == null || user.getUserId().equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setNegativeButton("??????", null);
                    builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(getContext(), LoginActivity.class));
                            parentActivity.finish();
                        }
                    });
                    builder.setMessage("???????????????");
                    builder.show();
                } else {
                    UserIntent userIntent = new UserIntent();
                    userIntent.setUserId(user.getUserId());
                    Intent intent = new Intent(getContext(), MyOrderActivity.class);
                    intent.putExtra("user", userIntent);
                    startActivity(intent);
                }
            }
        });
        CBMyOrder.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBMyOrder.setItemName("????????????");

        CBAboutUs.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBAboutUs.setItemName("????????????");
        CBAboutUs.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), AboutUsActivity.class);
                startActivity(intent);
            }
        });

        CBReport.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBReport.setItemName("??????");
        CBReport.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), ReportActivity.class));
            }
        });

        CBSettings.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBSettings.setItemName("??????");
        CBSettings.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), SettingActivity.class));
            }
        });

        CBUpdate.selectType(ComplexButton.TYPE_TEXT_ONLY);
        CBUpdate.setItemName("????????????");
        CBUpdate.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startProgressDialog();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("type", "vercode"));
                Thread updateThread = new NetThread(ServerUtil.SLUpdate, params, handler,
                        0, 1, true);
                updateThread.start();
            }
        });
        if (isUpdate) {
            setUpdateAble();
        }

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //????????????
                //????????????SharedPref??????
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("????????????????????????");
                builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UserDatabaseHelper dbHelper = new UserDatabaseHelper(getContext(), "usercache.db", null, 1);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("drop table if exists usercache");
                        db.execSQL("create table usercache(" +
                                "userid text)");
                        ContentValues values = new ContentValues();
                        values.put("userid", "");
                        db.insert("usercache", null, values);
                        db.close();
                        dbHelper.close();
                        /*
                        SharedPreferences.Editor editor = getContext().
                                getSharedPreferences("user_cache", Context.MODE_PRIVATE).edit();
                        editor.putString("user_id", "");
                        editor.apply();
                        */
                        startActivity(new Intent(getContext(), ClientMainActivity.class));
                        parentActivity.finish();
                    }
                });
                builder.setNegativeButton("??????", null);
                builder.show();

            }
        });

        //??????????????????
        getUserInfo();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getUserInfo();
    }

    public void setToolBarView(ToolBarView toolBarView) {
        this.toolBarView = toolBarView;
    }

    /**
     * ??????????????????
     */
    private void getUserInfo() {
        user.readFromLocalSharedPref();
        user.readFromLocalDatabase();
        if (user.getUserId().equals("")) {
            setUnLogin();
        } else {
            if (user.checkUserInfo()) {
                user.cloneData(handler); //???????????????????????????????????????
            } else {
                setUnLogin();
            }
        }
    }



    private void startDownload(String filename) {
        parentActivity.startDownload(ServerUtil.getDownloadUrl(filename));
    }

    private void startProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
        }
        progressDialog.setMessage("??????????????????");
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void stopProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * ??????????????????View
     */
    private void setUnLogin() {
        logout.setVisibility(View.GONE);
    }

    /**
     * ???????????????View
     */
    private void setLogin() {
        logout.setVisibility(View.VISIBLE);
    }

    public void setActivity(ClientMainActivity activity) {
        parentActivity = activity;
        handler = new MyHandler(parentActivity, this);
    }

    public void setUpdateAble() {
        CBUpdate.setRedDot(true);
    }

    public void setUpdate() {
        isUpdate = true;
    }

    private long getContentLength(String downloadUrl) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static class MyHandler extends Handler {

        WeakReference<ClientMainActivity> activityWeakReference;
        WeakReference<MeFragment> fragmentWeakReference;

        public MyHandler(ClientMainActivity activity, MeFragment fragment) {
            this.activityWeakReference = new WeakReference<ClientMainActivity>(activity);
            this.fragmentWeakReference = new WeakReference<MeFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                activityWeakReference.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragmentWeakReference.get().stopProgressDialog();
                    }
                });
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            String response = "";
            switch (msg.what) {
                case User.USER_RESPONSE:
                    fragmentWeakReference.get().setLogin();
                    break;
                case User.NET_ERROR:
                    break;
                case User.USER_WRONG:
                    fragmentWeakReference.get().setUnLogin();
                    break;
                case 0:
                    response = (String) msg.obj;
                    if (response.equals("" + AppInfo.getVersionCode(activityWeakReference.get()))) {
                        Toast.makeText(activityWeakReference.get(), "?????????????????????", Toast.LENGTH_SHORT).show();
                    } else {

                        activityWeakReference.get().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fragmentWeakReference.get().startProgressDialog();
                            }
                        });

                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("type", "logcontent"));
                        Thread updateThread = new NetThread(ServerUtil.SLUpdate, params, this,
                                2, 1, true);
                        updateThread.start();
                    }
                    break;
                case 1:

                    break;
                case 2:
                    fragmentWeakReference.get().logContent = (String) msg.obj;
                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("type", "filename"));
                    Thread updateThread = new NetThread(ServerUtil.SLUpdate, params, MyHandler.this,
                            4, 1, true);
                    updateThread.start();

                    break;
                case 3:
                    response = (String) msg.obj;
                    //??????????????????
                    fragmentWeakReference.get().startDownload(response);
                    break;
                case 4:
                    final String response1 = (String) msg.obj;
                    long length = fragmentWeakReference.get().getContentLength(ServerUtil.getDownloadUrl(response1));
                    Format format = new DecimalFormat(".#");
                    String fileLength = format.format((double) length / (1024 * 1024));
                    AlertDialog.Builder builder = new AlertDialog.Builder(activityWeakReference.get());
                    builder.setTitle("???????????????");
                    builder.setMessage(fragmentWeakReference.get().logContent + "\n\n???????????????" +
                            fileLength + "MB");
                    builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //???????????????

                            activityWeakReference.get().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fragmentWeakReference.get().startProgressDialog();
                                }
                            });

                            Message message = obtainMessage();
                            message.what = 3;
                            message.obj = response1;
                            handleMessage(message);

                        }
                    });
                    builder.setNegativeButton("??????", null);
                    builder.show();
                    break;
            }
        }
    }


}