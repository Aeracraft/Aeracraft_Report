package com.aeracraft.report.rest.dto;

import com.google.gson.annotations.SerializedName;

public class ReportRequest {

    @SerializedName("reporter")
    private String reporter;

    @SerializedName("target")
    private String target;

    @SerializedName("type")
    private String type;

    @SerializedName("reason")
    private String reason;

    @SerializedName("serverName")
    private String serverName;

    public ReportRequest() {
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
