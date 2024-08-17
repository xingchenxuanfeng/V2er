package me.ghui.v2er.network;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URLEncoder;

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

        if (request.tag() == NO_MODIFY_URL_TAG) {
            Log.d("ModifyUrlInterceptor", "has NO_MODIFY_URL_TAG, direct call originUrl: " + originUrl);
            return chain.proceed(request);
        }

        Response response = netImprCall(chain, url, originUrl, request);
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
                Log.e("ModifyUrlInterceptor", "error and retry", e);
            }
            Response failRetryResponse = netImprCall(chain, url, originUrl, request);
            if (failRetryResponse != null) {
                return failRetryResponse;
            }
        }
        return chain.proceed(request);
    }

    private static @Nullable Response netImprCall(Chain chain, HttpUrl url, String originUrl, Request request) throws IOException {

        if (!APIService.getImprBaseUrl().equals(Constants.BASE_URL)) {
            if (url.host().equals("www.v2ex.com")) {
                Log.d("ModifyUrlInterceptor", "change host to:" + APIService.getImprBaseUrl() + " ,originUrl: " + originUrl);
                String replaceHost = APIService.getImprBaseUrl().replace("https://", "").replace("http://", "");
                request = request.newBuilder()
                        .url(url.newBuilder().host(replaceHost).build())
                        .build();
                return chain.proceed(request);
            }
            if (url.host().contains("v2ex.com")) {
                Log.d("ModifyUrlInterceptor", "change to corsproxy, originUrl: " + originUrl);
                request = request.newBuilder()
                        .url(APIService.getImprBaseUrl() + "/corsproxy/?apiurl=" + URLEncoder.encode(originUrl))
                        .build();
                return chain.proceed(request);
            }
        }

        return null;
    }
}
