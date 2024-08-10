package me.ghui.v2er.network;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.httpdns.HTTPDNSResult;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.RequestIpType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.ghui.v2er.BuildConfig;
import me.ghui.v2er.general.App;
import okhttp3.Dns;

public class HttpDNS implements Dns {
    private static final String TAG = "HttpDNS";
    public static HttpDNS instance = new HttpDNS();

    private HttpDnsService httpDnsService;

    private HttpDNS() {
        httpDnsService = HttpDns.getService(App.get(), "194256", "a95d848a641a1818de5c8d3e107573ed");
        ArrayList<String> hostList = new ArrayList<>();
        hostList.add("www.v2ex.com");

        hostList.add(BuildConfig.imprBaseUrlConfig1.replace("https://", ""));
        hostList.add(BuildConfig.imprBaseUrlConfig2.replace("https://", ""));
        httpDnsService.setPreResolveHosts(hostList);

    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
//        if (true) {
//            InetAddress mock = InetAddress.getByName("127.154.152.107");
//            List<InetAddress> mockAddresses = new ArrayList<>();
//            mockAddresses.add(mock);
//            return mockAddresses;
//        }

//        HTTPDNSResult httpdnsResult = httpDnsService.getHttpDnsResultForHostSync(hostname, RequestIpType.auto);
        HTTPDNSResult httpdnsResult = httpDnsService.getHttpDnsResultForHostAsync(hostname, RequestIpType.auto);
        List<InetAddress> inetAddresses = new ArrayList<>();
        try {
            if (httpdnsResult.ips != null) {
                //处理IPv4地址
                for (String ipv4 : httpdnsResult.ips) {
                    InetAddress address = InetAddress.getByName(ipv4);
                    inetAddresses.add(address);
                }
            }
            if (httpdnsResult.ipv6s != null) {
                //处理IPv6地址
                for (String ipv6 : httpdnsResult.ipv6s) {
                    InetAddress address = InetAddress.getByName(ipv6);
                    inetAddresses.add(address);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }


        if (!inetAddresses.isEmpty()) {
            Log.d(TAG, "from HttpDNS, hostname:" + hostname + " ips:" + inetAddresses);
            return inetAddresses;
        }
        List<InetAddress> ips = Dns.SYSTEM.lookup(hostname);
        Log.d(TAG, "from SYSTEM, hostname:" + hostname + " ips:" + ips);
        return ips;
    }

}
