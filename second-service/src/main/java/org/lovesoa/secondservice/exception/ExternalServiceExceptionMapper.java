package org.lovesoa.secondservice.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.lovesoa.secondservice.exception.responses.ErrorResponse;

@Provider
public class ExternalServiceExceptionMapper implements ExceptionMapper<ExternalServiceException> {

    @Override
    public Response toResponse(ExternalServiceException ex) {
        ErrorResponse error = new ErrorResponse(
                mapStatusToErrorCode(ex.getStatus()),
                ex.getMessage(),
                ex.getDetails()
        );

        return Response.status(ex.getStatus())
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String mapStatusToErrorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "VALIDATION_FAILED";
            default -> "INTERNAL_ERROR";
        };
    }
}
