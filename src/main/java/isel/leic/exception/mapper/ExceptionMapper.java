package isel.leic.exception.mapper;


import isel.leic.exception.*;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @ServerExceptionMapper
    public RestResponse<String> mapUserNotFoundException(UserNotFoundException e) {
        LOGGER.error("User not found: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND,  e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapForbiddenException(ForbiddenException e) {
        LOGGER.error("Unauthorized action: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.FORBIDDEN, "Unauthorized action: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(Exception e) {
        LOGGER.error("Internal server error: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage());
    }
    @ServerExceptionMapper
    public RestResponse<String> mapIllegalArgumentException(IllegalArgumentException e) {
        LOGGER.error("Invalid argument: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.BAD_REQUEST, "Invalid argument: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapGroupNotFoundException(GroupNotFoundException e) {
        LOGGER.error("Group not found: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, "Group not found: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapUserNotInGroupException(UserNotInGroupException e) {
        LOGGER.error("User not found in group: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, "User not found in group: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapDuplicateResourceException(DuplicateResourceException e) {
        LOGGER.error("Duplicate resource: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.CONFLICT, "Duplicate resource: " + e.getMessage());
    }
    @ServerExceptionMapper
    public RestResponse<String> mapMembersNotFoundException(MembersNotFoundException e) {
        LOGGER.error("Empty group exception: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, "Empty group: " + e.getMessage());
    }
    @ServerExceptionMapper
    public RestResponse<String> mapFileSharingNotFoundException(FileSharingNotFoundException e) {
        LOGGER.error("File sharing not found: {}", e.getMessage(), e);
        return RestResponse.status(Response.Status.NOT_FOUND, "File sharing not found: " + e.getMessage());
    }


}
