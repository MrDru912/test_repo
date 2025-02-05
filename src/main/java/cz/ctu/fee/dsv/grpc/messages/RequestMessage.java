package cz.ctu.fee.dsv.grpc.messages;

public class RequestMessage {
    private String resourceId;
    private int time;

    public RequestMessage(String resourceId, int time) {
        this.resourceId = resourceId;
        this.time = time;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
