package com.bloodconnect.util;

public class ApiException extends RuntimeException {
    private final int status;
    private final transient Object extra; // optional extra JSON fields (e.g. eligibilityIssues)

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
        this.extra = null;
    }

    public ApiException(int status, String message, Object extra) {
        super(message);
        this.status = status;
        this.extra = extra;
    }

    public int getStatus() { return status; }
    public Object getExtra() { return extra; }
}
