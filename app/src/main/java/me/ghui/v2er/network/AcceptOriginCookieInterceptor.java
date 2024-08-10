package me.ghui.v2er.network;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpHeaders;

public class AcceptOriginCookieInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Response response = chain.proceed(request);

        if (request.url().toString().contains(APIService.getImprBaseUrl())) {
            HttpHeaders.receiveHeaders(APIService.cookieJar(), HttpUrl.parse(Constants.BASE_URL), response.headers());
        }
        return response;
    }
}
