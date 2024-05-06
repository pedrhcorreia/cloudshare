package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.exceptions.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public Response getGroups(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ){

        LOGGER.info("Received get request for groups belonging to user with ID: {}", id);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            List<Group> groups = groupService.getGroupsOfUser(id);
            if (groups.isEmpty()) {
                LOGGER.info("HTTP 404 Not Found: No groups found for user with ID: {}", id);
                return Response.status(Response.Status.NOT_FOUND).entity("No groups found for this user").build();
            }
            return Response.ok(groups).build();
        } catch(ForbiddenException e) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to get groups for user with ID: {}", id);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to get this user's groups").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while fetching user's groups - {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }


    @POST
    @Authenticated
    public Response createGroup(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext,
            String name
    ){

        LOGGER.info("Received create group request for user with ID: {}", id);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            Group createdGroup = groupService.createGroup(id, name);
            return Response.ok().entity(createdGroup).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("HTTP 400 Bad Request: Invalid request or group already exists - {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to create a group for user with ID: {}", id);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to create a group for this user").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while creating group - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while creating group").build();
        }
    }
    @PUT
    @Authenticated
    @Path("/{groupId}/name")
    public Response updateGroupName(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @NotNull String newName,
            @Context SecurityContext securityContext
    ){

        LOGGER.info("Received update group name request for group with ID: {}", groupId);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            groupService.updateGroupName(groupId, newName);
            LOGGER.info("HTTP 200 OK: Group name updated successfully for group with ID: {}", groupId);
            return Response.ok().entity("Group name updated successfully for group " + groupId).build();
        } catch (GroupNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: Group not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch(ForbiddenException e) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to update group name for group with ID: {}", groupId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to update the name of this group").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while updating group name - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while updating group name").build();
        }
    }



    @DELETE
    @Authenticated
    @Path("/{groupId}")
    public Response deleteGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext
    ) {

        LOGGER.info("Received request to delete group with ID: {} for user with ID: {}", groupId, id);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            groupService.removeGroup(groupId);
            return Response.ok().build();
        } catch (GroupNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: Group not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch(ForbiddenException e ) {
            LOGGER.warn("HTTP 401 Unauthorized: Unauthorized attempt to delete group for user with ID: {}", id);
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not authorized to delete this user's group").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while deleting group - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while deleting group").build();
        }
    }


    @POST
    @Authenticated
    @Path("/{groupId}")
    public Response addUserToGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext,
            @NotNull Long userId
    ) {

        LOGGER.info("Received a request to add user with ID: {} to group with ID: {}", id, groupId);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            groupService.addUserToGroup(userId, groupId);
            LOGGER.info("HTTP 200 OK: User with ID: {} added to group with ID: {}", userId, groupId);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("HTTP 400 Bad Request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (UserNotFoundException | GroupNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (DuplicateResourceException e) {
            LOGGER.warn("HTTP 409 Conflict: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch(ForbiddenException e ) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to add a user with ID: {} to a group with ID: {}", id, groupId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to add a user to this group").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while adding user to group").build();
        }
    }

    @GET
    @Authenticated
    @Path("/{groupId}/member")
    public Response getGroupMembers(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @Context SecurityContext securityContext
    ) {

        LOGGER.info("Received a request to list members of group with ID: {}, owned by user with ID: {}", groupId, id);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            List<User> users = groupService.getGroupMembers(groupId);
            if (users.isEmpty()) {
                LOGGER.info("HTTP 404 Not Found: No members found for group with ID: {}", groupId);
                return Response.status(Response.Status.NOT_FOUND).entity("No members found for this group").build();
            }
            LOGGER.info("HTTP 200 OK: Fetched {} members for group with ID: {}", users.size(), groupId);
            return Response.ok(users).build();
        } catch (GroupNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: Group not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MembersNotFoundException e){
            LOGGER.warn("HTTP 404 Not Found: Group member not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch(ForbiddenException e) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to get members of group with ID: {} for user with ID: {}", groupId, id);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to fetch this group's members").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while fetching group members - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while fetching group members").build();
        }
    }


    @DELETE
    @Authenticated
    @Path("/{groupId}/member/{memberId}")
    public Response removeMemberFromGroup(
            @PathParam("id") @NotNull Long id,
            @PathParam("groupId") @NotNull Long groupId,
            @PathParam("memberId") @NotNull Long memberId,
            @Context SecurityContext securityContext
    ) {

        LOGGER.info("Received a request to remove member with ID: {} from group with ID: {}, owned by user with ID: {}", memberId, groupId, id);

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());

            groupService.removeUserFromGroup(memberId,groupId);
            return Response.ok().build();
        } catch (GroupNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: Group not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (UserNotInGroupException e) {
            LOGGER.warn("HTTP 404 Not Found: User not found in group - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.warn("HTTP 403 Forbidden: Unauthorized attempt to remove a user with ID: {} from a group with ID: {}", memberId, groupId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to remove this group's members").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while removing member from group - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while removing member from group").build();
        }
    }
}
