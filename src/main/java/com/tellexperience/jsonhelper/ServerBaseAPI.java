package com.tellexperience.jsonhelper;

/**
 * Created by Aidin on 1/22/2017.
 */

import android.app.ProgressDialog;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.tellexperience.jsonhelper.JsonMapper;

public class ServerBaseAPI {

    private Context context;
    private Context applicationContext;
    private final List<ServerAPIEventListener> listeners;

    public ServerBaseAPI(Context applicationContext){
        this.context=context;
        this.applicationContext=applicationContext;
        this.listeners = new CopyOnWriteArrayList<ServerAPIEventListener>();
    }
    protected  <T> void Get(String url, Class<T> type,final ServerResult callBack){
        Boolean isArray=isArrayResult(callBack);
        SendRequest(Request.Method.GET, url,(JSONArray) null,type, callBack);
    }
    protected <T> void Post(String url, JSONObject jsonObject, Class<T> type, final ServerResult callBack){
        Boolean isArray=isArrayResult(callBack);
        SendRequest(Request.Method.POST, url,jsonObject,type, callBack);
    }

    private <T> void SendRequest(int method, String url, JSONArray jsonRequest,final Class<T> type, final ServerResult callBack ){
        showWait();
        final Boolean isArray=isArrayResult(callBack);
        Response.Listener listener = isArray? CreateJsonArrayResponseListener(type, callBack) :CreateJsonObjectResponseListener(type, callBack);
        Response.ErrorListener errorListener = CreateErrorResponseListener(callBack);
        if (isArray){
            MyJsonArrayRequest jsObjRequest = new MyJsonArrayRequest(method, url, jsonRequest, listener, errorListener);
            addToRequestQueue(jsObjRequest);
        }
        else{
            MyJsonObjectRequest jsObjRequest = new MyJsonObjectRequest(method, url, jsonRequest, listener,errorListener);
            addToRequestQueue(jsObjRequest);
        }
    }
    public void addServerAPIEventListener(ServerAPIEventListener listener) {
        listeners.add(listener);
    }
    public void removeServerAPIEventListener(ServerAPIEventListener listener) {
        listeners.remove(listener);
    }
    private <T> void SendRequest(int method, String url, JSONObject jsonRequest,final Class<T> type, final ServerResult callBack ){
        showWait();
        final Boolean isArray=isArrayResult(callBack);
        Response.Listener listener = isArray? CreateJsonArrayResponseListener(type, callBack) :CreateJsonObjectResponseListener(type, callBack);
        Response.ErrorListener errorListener = CreateErrorResponseListener(callBack);

        if (isArray){
            MyJsonArrayRequest jsObjRequest = new MyJsonArrayRequest(method, url, jsonRequest, listener, errorListener);
            addToRequestQueue(jsObjRequest);
        }
        else{
            MyJsonObjectRequest jsObjRequest = new MyJsonObjectRequest(method, url, jsonRequest, listener,errorListener);
            addToRequestQueue(jsObjRequest);
        }


    }


    private <T> Response.Listener<JSONArray> CreateJsonArrayResponseListener(final Class<T> type,final ServerResult callBack){
        return new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                hideWait();
                try{
                    List<T> result= JsonMapper.toObjects(response,type);
                    callBack.Always(result,null);
                }
                catch (Exception e){
                    e.printStackTrace();
                    callBack.Always(null,e);
                }
            }
        };
    }
    private <T> Response.Listener<JSONObject>  CreateJsonObjectResponseListener(final Class<T> type,final ServerResult callBack){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                hideWait();
                try{
                    T result= JsonMapper.toObject(response,type);
                    callBack.Always(result,null);
                }
                catch (Exception e){
                    e.printStackTrace();
                    callBack.Always(null,e);
                }
            }
        };
    }
    private <T> Response.ErrorListener  CreateErrorResponseListener(final ServerResult callBack){
        return  new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideWait();
                error.printStackTrace();
                String errorMessage=error.getMessage();
                if (errorMessage!=null)
                    Log.d("Error",errorMessage );
                callBack.Always(null,error);
                // TODO Auto-generated method stub

            }
        };
    }

    private boolean isArrayResult(ServerResult callBack){
        Type[] genericInterfaces = callBack.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
                for (Type genericType : genericTypes) {
                    if (genericType instanceof ParameterizedType && (((ParameterizedType) genericType).getRawType().equals(List.class) || ((ParameterizedType) genericType).getRawType().equals(ArrayList.class))){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    Integer waitCount=0;
    public void showWait(){
        waitCount++;
        ServerAPIEvent event=new ServerAPIEvent(this,waitCount);
        for(ServerAPIEventListener listener : listeners){
            listener.started(event);
        }
    }
    public void hideWait(){
        waitCount--;
        if (waitCount<0)
            waitCount=0;
        ServerAPIEvent event=new ServerAPIEvent(this,waitCount);
        for(ServerAPIEventListener listener : listeners){
            listener.finished(event);
        }
    }

    private static RequestQueue mRequestQueue;
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(applicationContext);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }


    public interface ServerResult<T>{
        public void Always(T data, Exception error);
    }

    public interface ServerAPIEventListener
    {
        void started(ServerAPIEvent event);
        void finished(ServerAPIEvent event);
    }

    public class ServerAPIEvent extends EventObject {
        public ServerAPIEvent(Object source) {super(source);}
        public ServerAPIEvent(Object source,int numberOfRequests) {
            super(source);
            this.numberOfRequests=numberOfRequests;
        }
        public int numberOfRequests;
    }
}
