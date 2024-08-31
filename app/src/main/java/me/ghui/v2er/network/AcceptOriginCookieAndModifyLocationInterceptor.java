package me.ghui.v2er.network;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpHeaders;

public class AcceptOriginCookieAndModifyLocationInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Response response = chain.proceed(request);

        String imprBaseUrl = APIService.getImprBaseUrl();
        if (request.url().toString().contains(imprBaseUrl)) {
            HttpHeaders.receiveHeaders(APIService.cookieJar(), HttpUrl.parse(Constants.BASE_URL), response.headers());
        }

        try {
//        Location	https://www.v2ex.com/mission/daily
            String locationUrl = response.header("Location");
            if (TextUtils.isEmpty(locationUrl)) {
                return response;
            }
            HttpUrl httpUrl = HttpUrl.parse(locationUrl);
            if (httpUrl == null) {
                return response;
            }
            String host = httpUrl.host();
            if ("www.v2ex.com".equals(host)) {
                if (!imprBaseUrl.equals(Constants.BASE_URL)) {
                    String replaceHost = imprBaseUrl.replace("https://", "").replace("http://", "");

                    return response.newBuilder().header("Location", httpUrl.newBuilder().host(replaceHost).build().toString()).build();
                }
            }
        } catch (Exception e) {
            Log.e("AcceptOriginCookieAndModifyLocationInterceptor", "error", e);
        }
        return response;
    }
}
