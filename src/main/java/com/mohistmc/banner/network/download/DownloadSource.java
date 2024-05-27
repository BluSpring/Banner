package com.mohistmc.banner.network.download;

import com.mohistmc.banner.BannerMCStart;
import com.mohistmc.banner.config.BannerConfigUtil;
import com.mohistmc.tools.ConnectionUtil;


public enum DownloadSource {

    MOHIST("https://maven.mohistmc.com/"),
    CHINA("http://s1.devicloud.cn:25119/"),
    GITHUB("https://mohistmc.github.io/maven/");

    public static final DownloadSource defaultSource = isCN() ? CHINA : MOHIST;
    public final String url;

    DownloadSource(String url) {
        this.url = url;
    }

    public static DownloadSource get() {
        String ds = BannerConfigUtil.defaultSource();
        DownloadSource urL;
        for (DownloadSource me : DownloadSource.values()) {
            if (me.name().equalsIgnoreCase(ds)) {
                if (ConnectionUtil.isValid(me.url)) {
                    if (ds.equals("CHINA")) {
                        urL = MOHIST;
                        if (ConnectionUtil.isValid(urL.url)) {
                            return GITHUB;
                        }
                    }
                    return GITHUB;
                }
                return me;
            }
        }
        return defaultSource;
    }

    public static boolean isCN() {
        return BannerMCStart.I18N.isCN() &&  ConnectionUtil.measureLatency(CHINA.url) <  ConnectionUtil.measureLatency(MOHIST.url);
    }
}
