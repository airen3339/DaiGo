package com.jshaz.daigo.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import com.jshaz.daigo.ui.BaseActivity;
import com.jshaz.daigo.ui.BaseFragment;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import java.util.List;

/**
 * Created by jshaz on 2017/12/22.
 */

public class PlannedNetThread extends Thread {
    private String ipAddress;
    private List<NameValuePair> params;
    private Handler handler;
    private int successCode;
    private int failCode;
    private BaseFragment fragment;
    private BaseActivity activity;
    private long millis;

    public PlannedNetThread() {
        super();
    }

    public PlannedNetThread(String ipAddress, List<NameValuePair> params, @Nullable Handler handler,
                     int successCode, int failCode, long millis) {
        this.ipAddress = ipAddress;
        this.params = params;
        this.handler = handler;
        this.successCode = successCode;
        this.failCode = failCode;
        this.millis = millis;
    }

    public PlannedNetThread(String ipAddress, List<NameValuePair> params, @Nullable Handler handler,
                     int successCode, int failCode, BaseFragment fragment, long millis) {
        this.ipAddress = ipAddress;
        this.params = params;
        this.handler = handler;
        this.successCode = successCode;
        this.failCode = failCode;
        this.fragment = fragment;
        this.millis = millis;
    }

    public PlannedNetThread(String ipAddress, List<NameValuePair> params, @Nullable Handler handler,
                     int successCode, int failCode, BaseActivity activity, long millis) {
        this.ipAddress = ipAddress;
        this.params = params;
        this.handler = handler;
        this.successCode = successCode;
        this.failCode = failCode;
        this.activity = activity;
        this.millis = millis;
    }

    @Override
    public void run() {
        while (true) {
            try {
                BasicHttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                HttpConnectionParams.setSoTimeout(httpParams, 5000);

                HttpClient httpclient = new DefaultHttpClient(httpParams);

                //????????????????????????Servlet
                HttpPost httpPost = new HttpPost(ipAddress);

                final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "utf-8");//???UTF-8????????????
                httpPost.setEntity(entity);
                //???????????????????????????
                HttpResponse httpResponse = httpclient.execute(httpPost);

                if (handler == null) return;

                if(httpResponse.getStatusLine().getStatusCode()==200)//???5000??????????????????????????????
                {
                    String response = "";
                    HttpEntity entity1 = httpResponse.getEntity();
                    response = EntityUtils.toString(entity1, "utf-8");//???UTF-8????????????
                    Message message = handler.obtainMessage();
                    message.what = successCode;
                    message.obj = response;
                    if (fragment != null) {
                        while (true) {
                            if (!fragment.isPaused()) {
                                handler.handleMessage(message);
                                break;
                            }
                        }
                    } else if (activity != null) {
                        while (true) {
                            if (!activity.isPaused()) {
                                handler.handleMessage(message);
                                break;
                            }
                        }
                    } else {
                        handler.handleMessage(message);
                    }
                } else {
                    Message message = handler.obtainMessage();
                    message.what = failCode;
                    handler.handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (handler == null) return;
                Message message = handler.obtainMessage();
                message.what = failCode;
                handler.handleMessage(message);
            } finally {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
