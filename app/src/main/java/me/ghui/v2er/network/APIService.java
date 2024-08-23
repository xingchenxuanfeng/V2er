package me.ghui.v2er.network;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import me.ghui.fruit.Fruit;
import me.ghui.fruit.converter.retrofit.FruitConverterFactory;
import me.ghui.retrofit.converter.GlobalConverterFactory;
import me.ghui.retrofit.converter.annotations.Html;
import me.ghui.retrofit.converter.annotations.Json;
import me.ghui.v2er.BuildConfig;
import me.ghui.v2er.general.App;
import me.ghui.v2er.util.Check;
import me.ghui.v2er.util.L;
import me.ghui.v2er.util.UserUtils;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by ghui on 25/03/2017.
 */

public class APIService {
    public static final String WAP_USER_AGENT = "Mozilla/5.0 (Linux; Android 9.0; V2er Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36";
    public static final String WEB_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4; V2er) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36";
    public static final String UA_KEY = "user-agent";

    public static final long TIMEOUT_LENGTH = 30;
    private static APIs mAPI_SERVICE;
    private static Gson sGson;
    private static Fruit sFruit;
    private static WebkitCookieManagerProxy sCookieJar;
    private static OkHttpClient sHttpClient;

    private static List<String> imprBaseUrlList = new ArrayList<>();
    private static String curImprBaseUrl;
    private final static String CUR_IMPR_BASEURL_KEY = "curImprBaseUrlKey";
    private static SharedPreferences sharePreferences;
    private static boolean checkImprBaseUrlFinish = false;

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition valueSet = lock.newCondition();

    public static void init() {
        if (mAPI_SERVICE == null) {
            sharePreferences = App.get().getSharedPreferences("APIService", Context.MODE_PRIVATE);
            getImprBaseUrlListFromServer(new CallBack() {
                @Override
                public void onResult(List<String> imprBaseUrlListFromServer) {
                    List<String> list = new ArrayList<>();
                    list.add(Constants.BASE_URL);
                    list.add(BuildConfig.imprBaseUrlConfig1);
                    list.add(BuildConfig.imprBaseUrlConfig2);

                    list.addAll(imprBaseUrlListFromServer);

                    HashSet<String> set = new HashSet<>(list);
                    imprBaseUrlList.clear();
                    imprBaseUrlList.addAll(set);
                    startUpdateImprBaseUrl();
                }
            });


            lock.lock();

            try {
                if (sharePreferences.getString(CUR_IMPR_BASEURL_KEY, null) == null) {
                    Log.d("APIService", "Main thread waiting for curImprBaseUrl...");
                    valueSet.await(1000, TimeUnit.MILLISECONDS); // 主线程等待，直到 x 被设置
                }
                Log.d("APIService", "Main thread got curImprBaseUrl: " + getImprBaseUrl());
            } catch (Exception e) {
                Log.e("APIService", "wait", e);
            } finally {
                lock.unlock();
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .client(httpClient())
                    .addConverterFactory(GlobalConverterFactory
                            .create()
                            .add(FruitConverterFactory.create(fruit()), Html.class)
                            .add(GsonConverterFactory.create(gson()), Json.class))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(Constants.BASE_URL)
                    .build();
            mAPI_SERVICE = retrofit.create(APIs.class);
        }
    }

    public static void startUpdateImprBaseUrl() {

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!appOnForeground()) {
                    return;
                }
                updateCurImprBaseUrlAsync();
            }
        }, 0, 5 * 60 * 1000);
    }

    public static void updateCurImprBaseUrlAsync() {
        checkImprBaseUrlFinish = false;
        for (String url : imprBaseUrlList) {
            new Thread(() -> {
                boolean result = canAccessNet(url);
                Log.d("APIService", "canAccessNet url:" + url + " result:" + result);
                if (result) {
                    if (!checkImprBaseUrlFinish || url.equals(Constants.BASE_URL)) {
                        lock.lock();
                        try {
                            curImprBaseUrl = url;
                            sharePreferences.edit().putString(CUR_IMPR_BASEURL_KEY, url).apply();
                            Log.d("APIService", "success set curImprBaseUrl:" + url);
                            valueSet.signalAll(); // 通知等待的线程（主线程）
                        } finally {
                            lock.unlock();
                        }
                        checkImprBaseUrlFinish = true;
                    } else {
                        Log.d("APIService", "too late,but access:" + url);
                    }
                }
            }).start();
        }
    }

    public static void updateCurImprBaseUrlSync() {
        Log.d("APIService", "updateCurImprBaseUrlSync imprBaseUrlList:" + imprBaseUrlList);

        for (String url : imprBaseUrlList) {
            boolean result = canAccessNet(url);
            Log.d("APIService", "canAccessNet url:" + url + " result:" + result);
            if (result) {
                curImprBaseUrl = url;
                sharePreferences.edit().putString(CUR_IMPR_BASEURL_KEY, url).apply();
                Log.d("APIService", "success set curImprBaseUrl:" + url);
                return;
            }
        }
        Log.e("APIService", "updateCurImprBaseUrlSync all canAccessNet fail!!!, imprBaseUrlList:" + imprBaseUrlList);
    }

    private static boolean appOnForeground() {
        if (System.currentTimeMillis() - App.get().getAppStartTime() < 60 * 1000) {
            return true;
        }
        ActivityManager activityManager = (ActivityManager) App.get().getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();

        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(App.get().getPackageName()) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }

        return false;
    }

    public static String getImprBaseUrl() {
        if (curImprBaseUrl != null) {
            return curImprBaseUrl;
        }
        String cachedUrl = sharePreferences.getString(CUR_IMPR_BASEURL_KEY, null);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        return Constants.BASE_URL;
    }

    public static boolean canAccessNet(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .tag(ModifyUrlInterceptor.NO_MODIFY_URL_TAG)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();

            Response response = httpClient().newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e("APIService", "canAccessNet url:" + url, e);
            return false;
        }
    }

    interface CallBack {
        void onResult(List<String> list);
    }

    public static void getImprBaseUrlListFromServer(CallBack callBack) {
        try {
            long start = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url(BuildConfig.getImprBaseUrlListFromServerApi)
                    .tag(ModifyUrlInterceptor.NO_MODIFY_URL_TAG)
                    .build();
            Log.d("APIService", "getImprBaseUrlListFromServer start");

            httpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("APIService", "getImprBaseUrlListFromServer onFailure", e);
                    callBack.onResult(new ArrayList<>());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("APIService", "getImprBaseUrlListFromServer onResponse cost:" + (System.currentTimeMillis() - start));
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        Type type = new TypeToken<List<String>>() {
                        }.getType();
                        List<String> list = gson().fromJson(json, type);
                        long end = System.currentTimeMillis();
                        Log.d("APIService", "getImprBaseUrlListFromServer cost:" + (end - start) + " ,result:" + String.join(",", list));
                        callBack.onResult(list);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("APIService", "getImprBaseUrlListFromServer", e);
        }
    }

    public static APIs get() {
        return mAPI_SERVICE;
    }

    public static Gson gson() {
        if (sGson == null) {
            sGson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
        }
        return sGson;
    }

    public static Fruit fruit() {
        if (sFruit == null) {
            sFruit = new Fruit();
        }
        return sFruit;
    }

    public static OkHttpClient httpClient() {
        if (sHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .cache(new Cache(new File(App.get().getCacheDir(), "okhttp"), 10 * 1024 * 1024))
                    .connectTimeout(TIMEOUT_LENGTH, TimeUnit.SECONDS)
                    .cookieJar(cookieJar())
                    .dns(HttpDNS.instance)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(new ConfigInterceptor())
                    .addNetworkInterceptor(new AcceptOriginCookieAndModifyLocationInterceptor())
                    .addInterceptor(new ModifyUrlInterceptor());
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new HttpLoggingInterceptor(L::v)
                        .setLevel(HttpLoggingInterceptor.Level.BODY));
            }
            sHttpClient = builder.build();
        }
        return sHttpClient;
    }

    public static WebkitCookieManagerProxy cookieJar() {
        if (sCookieJar == null) {
//            sCookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(App.get()));
            sCookieJar = new WebkitCookieManagerProxy();
        }
        return sCookieJar;
    }

    private static class ConfigInterceptor implements Interceptor {
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

}
