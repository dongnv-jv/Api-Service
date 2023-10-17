package vn.vnpay.rabbitmqrpc.common;

public class GeneralLog {
    private String logId;
    private String clientId;
    private Long timeToProcess;
    private Object request;
    private Object response;

    public String generateLog(GeneralLog generalLog) {
        try {
            return ObjectConverter.objectToJson(generalLog);
        } catch (Exception e) {
            return null;
        }

    }


    public GeneralLog(String logId, String clientId, Long timeToProcess, Object request, Object response) {
        this.logId = logId;
        this.clientId = clientId;
        this.timeToProcess = timeToProcess;
        this.request = request;
        this.response = response;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Long getTimeToProcess() {
        return timeToProcess;
    }

    public void setTimeToProcess(Long timeToProcess) {
        this.timeToProcess = timeToProcess;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
