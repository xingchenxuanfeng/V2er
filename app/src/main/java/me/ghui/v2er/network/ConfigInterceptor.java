package me.ghui.v2er.network;

import static me.ghui.v2er.network.APIService.UA_KEY;
import static me.ghui.v2er.network.APIService.WAP_USER_AGENT;

import android.util.Log;

import java.io.IOException;

import me.ghui.v2er.util.Check;
import me.ghui.v2er.util.UserUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ConfigInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        long startTime = System.currentTimeMillis();
        Request request = chain.request();
        String ua = request.header(UA_KEY);
        Request.Builder builder = request.newBuilder();
        if (Check.isEmpty(ua)) {
            builder.addHeader("user-agent", WAP_USER_AGENT);
        }
        String secUserAgent = request.header("v-sec-device-id");
        if (Check.isEmpty(secUserAgent)) {

            String deviceId = UserUtils.getDeviceId();
            builder.addHeader("v-sec-device-id", deviceId);
        }
        request = builder.build();
        Response response = chain.proceed(request);

        long endTime = System.currentTimeMillis();

        Log.d("APIService", "ConfigInterceptor, url:" + request.url() +
                ", networkResponse:" + (response.networkResponse() != null) +
                ", cacheResponse:" + (response.cacheResponse() != null) +
                ", code:" + (response.networkResponse() != null ? response.networkResponse().code() : -1) +
                ", duration:" + (endTime - startTime)
        );

        return response;

//            try {
//                return chain.proceed(request);
//            } catch (Exception e) {
//                if (!(e instanceof SocketException || e instanceof SocketTimeoutException)) {
//                    Log.e("ConfigInterceptor", "error", e);
//                }
//                return new Response.Builder()
//                        .protocol(Protocol.HTTP_1_1)
//                        .code(404)
//                        .message("Exeception when execute chain.proceed request")
//                        .body(new ResponseBody() {
//                            @Nullable
//                            @Override
//                            public MediaType contentType() {
//                                return null;
//                            }
//
//                            @Override
//                            public long contentLength() {
//                                return 0;
//                            }
//
//                            @Override
//                            public BufferedSource source() {
//                                return null;
//                            }
//                        })
//                        .request(request).build();
//            }
    }
}
