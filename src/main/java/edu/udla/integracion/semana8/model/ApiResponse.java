package edu.udla.integracion.semana8.model;

public class ApiResponse {

    private String status;
    private String message;
    private String referenceId;

    public ApiResponse() {
    }

    public ApiResponse(String status, String message, String referenceId) {
        this.status = status;
        this.message = message;
        this.referenceId = referenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
