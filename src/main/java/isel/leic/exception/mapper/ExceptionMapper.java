package isel.leic.exception.mapper;


import isel.leic.exception.*;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @ServerExceptionMapper
    public RestResponse<String> mapUserNotFoundException(UserNotFoundException e) {
        LOGGER.error( e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND,  buildErrorMessage(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapForbiddenException(ForbiddenException e) {
        LOGGER.error("Unauthorized action: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.FORBIDDEN, buildErrorMessage("Unauthorized action: " + e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapCompletionException(CompletionException e) {
        String errorMessage = e.getMessage();
        String statusCode = "500"; // Default status code
        if (errorMessage != null) {
            // Regex pattern to match status code
            Pattern pattern = Pattern.compile("Status Code: (\\d+)");
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                statusCode = matcher.group(1);
            }
            // Extract error message
            errorMessage = errorMessage.split("\\(Service")[0].trim();
        }
        LOGGER.error("CompletionException with status {} occurred: {}", statusCode, errorMessage, e);
        return RestResponse.status(Response.Status.fromStatusCode(Integer.parseInt(statusCode)), buildErrorMessage(errorMessage));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(Exception e) {
        LOGGER.error("Internal server error: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, buildErrorMessage("Internal server error: " + e.getMessage()));

    }

    @ServerExceptionMapper
    public RestResponse<String> mapIllegalArgumentException(IllegalArgumentException e) {
        LOGGER.error("Invalid argument: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.BAD_REQUEST, buildErrorMessage("Invalid argument - " + e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapGroupNotFoundException(GroupNotFoundException e) {
        LOGGER.error("Group not found: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, buildErrorMessage("Group not found - " + e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapUserNotInGroupException(UserNotInGroupException e) {
        LOGGER.error("User not found in group: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, buildErrorMessage("User not found in group - " + e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<String> mapDuplicateResourceException(DuplicateResourceException e) {
        LOGGER.error("Duplicate resource: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.CONFLICT, buildErrorMessage("Duplicate resource - " + e.getMessage()));
    }
    @ServerExceptionMapper
    public RestResponse<String> mapMembersNotFoundException(MembersNotFoundException e) {
        LOGGER.error("Empty group exception: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, buildErrorMessage("Empty group - " + e.getMessage()));
    }
    @ServerExceptionMapper
    public RestResponse<String> mapFileSharingNotFoundException(FileSharingNotFoundException e) {
        LOGGER.error("File sharing not found: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, buildErrorMessage("File sharing not found - " + e.getMessage()));
    }

    public String buildErrorMessage(String message){
        return "{\"Error\":\" "+message+"\"}";
    }

}
