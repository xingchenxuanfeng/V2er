package me.ghui.v2er.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import me.ghui.v2er.general.App;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ModifyUrlInterceptor implements Interceptor {
    public static final Object NO_MODIFY_URL_TAG = new Object();

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        HttpUrl url = request.url();
        String originUrl = url.toString();

        if (!isNetworkAvailable()) {
            Log.e("ModifyUrlInterceptor", "network not available, direct call originUrl: " + originUrl);
            return chain.proceed(request);
        }

        if (request.tag() == NO_MODIFY_URL_TAG) {
            Log.d("ModifyUrlInterceptor", "has NO_MODIFY_URL_TAG, direct call originUrl: " + originUrl);
            return chain.proceed(request);
        }

        Response response = null;
        try {
            response = netImprCall(chain, url, originUrl, request, false);
        } catch (Exception e) {
            Log.e("ModifyUrlInterceptor", "netImprCall error", e);
            if (e instanceof UnknownHostException) {
                Log.e("ModifyUrlInterceptor", "netImprCall UnknownHostException, retry");
                APIService.updateCurImprBaseUrlSync();
                response = netImprCall(chain, url, originUrl, request, false);
            }
        }
        if (response != null) {
            return response;
        }

        try {
            Log.d("ModifyUrlInterceptor", "direct call originUrl: " + originUrl);
            return chain.proceed(request);
        } catch (Exception e) {
            if ("Canceled".equals(e.getMessage())) {
                //ignore
            } else {
                if (e instanceof UnknownHostException) {
                    APIService.updateCurImprBaseUrlSync();
                } else if (url.host().contains("v2ex.com") && APIService.getImprBaseUrl().equals(Constants.BASE_URL)) {
                    APIService.updateCurImprBaseUrlSync();
                } else {
                    APIService.updateCurImprBaseUrlAsync();
                }
                Log.e("ModifyUrlInterceptor", "error and retry", e);
                Response failRetryResponse = netImprCall(chain, url, originUrl, request, true);
                if (failRetryResponse != null) {
                    return failRetryResponse;
                }
            }
        }
        return chain.proceed(request);
    }

    private static @Nullable Response netImprCall(Chain chain, HttpUrl url, String originUrl, Request request, boolean fallBack) throws IOException {

        String imprBaseUrl = APIService.getImprBaseUrl();
        Log.d("ModifyUrlInterceptor", "netImprCall,imprBaseUrl:" + imprBaseUrl + " ,originUrl: " + originUrl);

        if (!imprBaseUrl.equals(Constants.BASE_URL)) {
            if (url.host().equals("www.v2ex.com")) {
                Log.d("ModifyUrlInterceptor", "change host to:" + imprBaseUrl + " ,originUrl: " + originUrl);
                String replaceHost = imprBaseUrl.replace("https://", "").replace("http://", "");
                request = request.newBuilder()
                        .url(url.newBuilder().host(replaceHost).build())
                        .build();
                return chain.proceed(request);
            }
            if (url.host().contains("v2ex.com")) {
                Log.d("ModifyUrlInterceptor", "change to corsproxy, originUrl: " + originUrl);
                request = request.newBuilder()
                        .url(imprBaseUrl + "/corsproxy/?apiurl=" + URLEncoder.encode(originUrl))
                        .build();
                return chain.proceed(request);
            }
            if (url.host().contains("imgur.com") || fallBack) {
                Log.d("ModifyUrlInterceptor", "change to corsproxy, originUrl: " + originUrl);
                request = request.newBuilder()
                        .url(imprBaseUrl + "/corsproxy/?apiurl=" + URLEncoder.encode(originUrl))
                        .build();
                return chain.proceed(request);
            }
        }
        return null;
    }

    private static boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) App.get().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e("ModifyUrlInterceptor", "isNetworkAvailable error", e);
        }
        return false;
    }
}
