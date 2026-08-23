package org.eu.thedoc.zettelnotes.buttons.imgbb;

import com.google.gson.annotations.SerializedName;

public class ImgBBResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("status")
    public int status;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("url")
        public String url;

        @SerializedName("display_url")
        public String displayUrl;

        @SerializedName("url_viewer")
        public String viewerUrl;

        @SerializedName("delete_url")
        public String deleteUrl;
    }
}
