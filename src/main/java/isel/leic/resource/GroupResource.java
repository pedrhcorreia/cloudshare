package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.mutiny.Uni;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.service.GroupService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

@Path("/user/{id}/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupResource.class);

    @Inject
    GroupService groupService;

    @GET
    @Authenticated
    public Uni<Response> getGroups(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ){
        LOGGER.info("Received get request for groups belonging to user with ID: {}", id);
        return groupService.getGroupsOfUser(id)
                .map(groups -> Response.ok(groups).build());
    }

    @POST
    @Authenticated
    public Uni<Response> createGroup(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext,
            @RequestBody createGroupRequest createGroupRequest
    ){
        LOGGER.info("Received create group request for user with ID: {}", id);
        return groupService.createGroup(id, createGroupRequest.groupName)
                .map(createdGroup -> Response.created(URI.create("/groups/" + createdGroup.getId())).entity(createdGroup).build());
    }

    @PUT
    @Authenticated
    @Path("/{groupId}/name")
    public Uni<Response> updateGroupName(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @NotNull String newName,
            @Context SecurityContext securityContext
    ){
        LOGGER.info("Received update group name request for group with ID: {}", groupId);
        return groupService.updateGroupName(groupId, newName)
                .map(updatedGroup -> {
                    LOGGER.info("HTTP 200 OK: Group name updated successfully for group with ID: {}", groupId);
                    return Response.ok().entity(updatedGroup).build();
                });
    }

    @DELETE
    @Authenticated
    @Path("/{groupId}")
    public Uni<Response> deleteGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to delete group with ID: {} for user with ID: {}", groupId, id);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        return groupService.removeGroup(groupId)
                .map(deleted -> Response.ok().build());
    }

    @POST
    @Authenticated
    @Path("/{groupId}")
    public Uni<Response> addUserToGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext,
            @NotNull Long userId
    ) {
        LOGGER.info("Received a request to add user with ID: {} to group with ID: {}", id, groupId);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        return groupService.addUserToGroup(userId, groupId)
                .map(result -> {
                    LOGGER.info("HTTP 200 OK: User with ID: {} added to group with ID: {}", userId, groupId);
                    return Response.ok().build();
                });
    }


    @GET
    @Authenticated
    @Path("/{groupId}/member")
    public Uni<Response> getGroupMembers(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received a request to list members of group with ID: {}, owned by user with ID: {}", groupId, id);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        return groupService.getGroupMembers(groupId)
                .map(users -> {
                    LOGGER.info("HTTP 200 OK: Fetched {} members for group with ID: {}", users.size(), groupId);
                    return Response.ok(users).build();
                });
    }

    @DELETE
    @Authenticated
    @Path("/{groupId}/member/{memberId}")
    public Uni<Response> removeMemberFromGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @PathParam("memberId") @NotNull Long memberId,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received a request to remove member with ID: {} from group with ID: {}, owned by user with ID: {}", memberId, groupId, id);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        return groupService.removeUserFromGroup(memberId, groupId)
                .map(result -> Response.ok().build());
    }


    public record createGroupRequest(String groupName) {

    }
}
