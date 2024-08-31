package me.ghui.v2er.network;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static me.ghui.v2er.network.APIService.UA_KEY;
import static me.ghui.v2er.network.APIService.WAP_USER_AGENT;

import android.util.Log;

import java.io.IOException;

import me.ghui.v2er.general.App;
import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.module.login.LoginActivity;
import me.ghui.v2er.util.Check;
import me.ghui.v2er.util.UserUtils;
import me.ghui.v2er.util.Voast;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoginInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        HttpUrl originUrl = request.url();
        HttpUrl newUrl = response.request().url();
        if (!originUrl.equals(newUrl)) {
            if (newUrl.encodedPath().contains("signin")) {
                Voast.show("需要登录");
                LoginActivity.goLogin();
            }
        }
        return response;
    }
}
