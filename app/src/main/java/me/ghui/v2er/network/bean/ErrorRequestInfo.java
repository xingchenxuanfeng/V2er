package me.ghui.v2er.network.bean;

public class ErrorRequestInfo extends BaseInfo {
    private final String msg;

    public ErrorRequestInfo(String msg) {
        this.msg = msg;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String getResponse() {
        return msg;
    }
}
