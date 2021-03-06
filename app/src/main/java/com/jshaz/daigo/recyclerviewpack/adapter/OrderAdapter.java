package com.jshaz.daigo.recyclerviewpack.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jshaz.daigo.R;
import com.jshaz.daigo.client.ClientMainActivity;
import com.jshaz.daigo.client.OrderDetailActivity;
import com.jshaz.daigo.client.OrderFragment;
import com.jshaz.daigo.gson.OrderDAO;
import com.jshaz.daigo.interfaces.BaseClassImpl;
import com.jshaz.daigo.serverutil.ServerUtil;
import com.jshaz.daigo.util.Order;
import com.jshaz.daigo.util.Setting;
import com.jshaz.daigo.util.Utility;

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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by jshaz on 2017/11/21.
 */

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<OrderDAO> orderList = new ArrayList<>();

    private Context mContext;

    private OrderDAO order;

    private String userId;

    private ClientMainActivity parentActivity;

    private OrderFragment parentFragment;

    private MyHandler handler = new MyHandler(this);

    public OrderFragment getParentFragment() {
        return parentFragment;
    }

    public void setParentFragment(OrderFragment parentFragment) {
        this.parentFragment = parentFragment;
        handler.setFragmentWeakReference(parentFragment);
    }

    public ClientMainActivity getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(ClientMainActivity parentActivity) {
        this.parentActivity = parentActivity;
        handler.setActivityWeakReference(parentActivity);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OrderAdapter(List<OrderDAO> orderList) {

        this.orderList = orderList;

    }

    public OrderAdapter(List<OrderDAO> orderList, Context mContext) {

        this.orderList = orderList;
        this.mContext = mContext;
    }

    public void setOrderList(List<OrderDAO> orderList) {
        this.orderList = orderList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.info_item,
                parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        try {
            order = orderList.get(position);
        }  catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }


        holder.infoTitle.setText(order.getTitle());
        holder.infoContent.setText(order.getPublicDetails());
        holder.infoTime.setText(Utility.getOrderReleaseTime(order.getOrderId()));
        holder.btnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //parentActivity.setDetailActivityReturned(true);
                order = orderList.get(position);
                Intent intent = new Intent(mContext, OrderDetailActivity.class);
                intent.putExtra("order_id", order.getOrderId());
                intent.putExtra("user_id", userId);
                getParentActivity().startActivityForResult(intent, 0);
            }
        });

        holder.btnDetail.setLongClickable(true);
        holder.btnDetail.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });

        holder.senderHead.setImageBitmap(
                Utility.convertStringToBitmap(order.getOrderSender().getHeadIcon()));
        holder.senderNickName.setText(order.getOrderSender().getNickName());
        holder.reqeustTime.setText(order.getRequestTime() / 60000 + " ???????????????");
        holder.campusName.setText(Utility.getCampusName(order.getOrderId()));
        holder.itemBg.setImageResource(
                Setting.CAMPUS_BANNER_RID[Utility.getCampusCode(order.getOrderId())]);

        if (order.getOrderSender().getUserId().equals(userId)) {
            //??????????????????????????????????????????
            holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(mContext, "?????????????????????????????????:)", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    order = orderList.get(position);
                    parentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setMessage("?????????????????????");
                            builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    checkIsVerified(position);
                                }
                            });
                            builder.setNegativeButton("??????", null);
                            builder.show();
                        }
                    });
                }
            });
        }

        holder.btnAccept.setLongClickable(true);
        holder.btnAccept.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        
    }


    /**
     * ?????????????????????????????????
     */
    private void checkIsVerified(final int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                Looper.prepare();
                try {
                    BasicHttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                    HttpConnectionParams.setSoTimeout(httpParams, 5000);

                    HttpClient httpclient = new DefaultHttpClient(httpParams);

                    //????????????????????????Servlet
                    HttpPost httpPost = new HttpPost(ServerUtil.SLCheckVerification);

                    List<NameValuePair> params = new ArrayList<NameValuePair>();//???????????????list
                    params.add(new BasicNameValuePair("userid", userId));


                    final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "utf-8");//???UTF-8????????????
                    httpPost.setEntity(entity);
                    //???????????????????????????
                    HttpResponse httpResponse = httpclient.execute(httpPost);
                    if (httpResponse.getStatusLine().getStatusCode() == 200)//???5000??????????????????????????????
                    {
                        HttpEntity entity1 = httpResponse.getEntity();
                        response = EntityUtils.toString(entity1, "utf-8");//???UTF-8????????????
                        Message message = new Message();
                        message.what = 2;
                        message.obj = response;
                        message.arg1 = position;
                        handler.handleMessage(message);
                    } else {
                        Message message = new Message();
                        message.what = BaseClassImpl.NET_ERROR;
                        handler.handleMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = new Message();
                    message.what = BaseClassImpl.NET_ERROR;
                    handler.handleMessage(message);
                }
                Looper.loop();
            }
        }).start();
    }

    /**
     * ??????
     */
    private void acceptOrder(final String orderId, final int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                Looper.prepare();
                long date = 0;
                /*??????????????????*/
                try {
                    URL url = new URL("https://www.baidu.com");
                    URLConnection uc = url.openConnection();
                    uc.connect();
                    date = uc.getDate();
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = BaseClassImpl.NET_ERROR;
                    handler.handleMessage(msg);
                }
                /*??????????????????*/
                try {
                    BasicHttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                    HttpConnectionParams.setSoTimeout(httpParams, 5000);

                    HttpClient httpclient = new DefaultHttpClient(httpParams);

                    //????????????????????????Servlet
                    HttpPost httpPost = new HttpPost(ServerUtil.SLOrderStateModify);

                    List<NameValuePair> params = new ArrayList<NameValuePair>();//???????????????list
                    /**
                     * ??????????????????????????????
                     */
                    params.add(new BasicNameValuePair("type", Order.RECEIVED + ""));
                    params.add(new BasicNameValuePair("orderid", order.getOrderId()));
                    params.add(new BasicNameValuePair("receiverid", userId));
                    params.add(new BasicNameValuePair("accepttime", "" + date));


                    final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "utf-8");//???UTF-8????????????
                    httpPost.setEntity(entity);
                    //???????????????????????????
                    HttpResponse httpResponse = httpclient.execute(httpPost);
                    if (httpResponse.getStatusLine().getStatusCode() == 200)//???5000??????????????????????????????
                    {
                        HttpEntity entity1 = httpResponse.getEntity();
                        response = EntityUtils.toString(entity1, "utf-8");//???UTF-8????????????
                        Message message = handler.obtainMessage();
                        message.what = 1;
                        message.obj = response;
                        message.arg1 = position;
                        handler.handleMessage(message);
                    } else {
                        Message message = handler.obtainMessage();
                        message.what = BaseClassImpl.NET_ERROR;
                        handler.handleMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = handler.obtainMessage();
                    message.what = BaseClassImpl.NET_ERROR;
                    handler.handleMessage(message);
                }
                Looper.loop();
            }
        }).start();
    }


    @Override
    public int getItemCount() {
        return orderList.size();
    }

    private static class MyHandler extends Handler {
        WeakReference<ClientMainActivity> activityWeakReference;
        WeakReference<OrderFragment> fragmentWeakReference;
        WeakReference<OrderAdapter> adapterWeakReference;

        public MyHandler(OrderAdapter adapter) {
            this.adapterWeakReference = new WeakReference<OrderAdapter>(adapter);
        }

        public void setActivityWeakReference(ClientMainActivity activity) {
            this.activityWeakReference = new WeakReference<ClientMainActivity>(activity);
        }

        public void setFragmentWeakReference(OrderFragment fragment) {
            this.fragmentWeakReference = new WeakReference<OrderFragment>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final ClientMainActivity activity = activityWeakReference.get();
            final OrderFragment fragment = fragmentWeakReference.get();
            final OrderAdapter adapter = adapterWeakReference.get();
            String response = "";
            switch (msg.what) {
                case BaseClassImpl.NET_ERROR:

                    Toast.makeText(activity, "????????????", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    response = (String) msg.obj;
                    if (response.equals("true")) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder info = new AlertDialog.Builder(activity);
                                info.setMessage("???????????????????????????????????????????????????");
                                info.setPositiveButton("??????", null);
                                info.show();
                                fragment.removeFromOrderList(msg.arg1);
                            }
                        });
                    } else {
                        Toast.makeText(activity, "????????????", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    response = (String) msg.obj;
                    if (response.equals("true")) {
                        adapter.acceptOrder(adapter.order.getOrderId(), msg.arg1);
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new
                                        AlertDialog.Builder(activity);
                                builder.setMessage("???????????????????????????-??????????????????-??????????????????????????????????????????????????????");
                                builder.setPositiveButton("??????", null);
                                builder.show();
                            }
                        });

                    }
                    break;
            }
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private Button btnDetail; //????????????

        private Button btnAccept; //????????????

        private CircleImageView senderHead; //???????????????

        private TextView senderNickName; //???????????????

        private TextView infoTitle; //????????????

        private TextView infoContent; //????????????

        private TextView infoTime; //????????????

        private TextView reqeustTime; //??????????????????

        private TextView campusName;

        private ImageView itemBg;


        public ViewHolder(View view) {
            super(view);
            btnDetail = (Button) view.findViewById(R.id.info_btn_details);
            btnAccept = (Button) view.findViewById(R.id.info_btn_accpet);
            senderHead = (CircleImageView) view.findViewById(R.id.info_ognz_head);
            senderNickName = (TextView) view.findViewById(R.id.info_ognz_nickname);
            infoTitle = (TextView) view.findViewById(R.id.info_content_title);
            infoContent = (TextView) view.findViewById(R.id.info_details);
            infoTime = (TextView) view.findViewById(R.id.info_time);
            reqeustTime = (TextView) view.findViewById(R.id.info_request_time);
            campusName = (TextView) view.findViewById(R.id.info_campus_name);
            itemBg = (ImageView) view.findViewById(R.id.info_itembg);
        }
    }
}
