package isel.leic;

import io.quarkus.security.Authenticated;
import isel.leic.model.User;
import isel.leic.service.UserService;
import isel.leic.utils.TokenUtils;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    @Inject
    UserService userService;
    @ConfigProperty(name = "com.cloudshare.quarkusjwt.jwt.duration")
    Long tokenDuration;

    @POST
    @PermitAll
    @Path("/login")
    public Response login(LoginRequest loginRequest) {
        // Authenticate user using UserService
        User user = userService.authenticate(loginRequest.username, loginRequest.password).orElse(null);
        if (user != null) {
            try {
                // If authentication is successful, generate token and return it
                String token = TokenUtils.generateToken(user.getUsername(), "Cloudshare", tokenDuration);
                return Response.ok().entity(token).build();
            } catch (Exception e) {
                // Handle token generation error
                return Response.serverError().build();
            }
        } else {
            // If authentication fails, return UNAUTHORIZED status
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }


    @POST
    @PermitAll
    @Path("/signup")
    public Response signup(LoginRequest signupRequest) {
        // Check if the username already exists
        if (userService.existsByUsername(signupRequest.username)) {
            // Username already exists, return a conflict response
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists")
                    .build();
        }

        // Create a new user with the provided username and password
        User newUser = new User();
        newUser.setUsername(signupRequest.username);
        newUser.setPassword(signupRequest.password);

        // Persist the new user
        userService.persist(newUser);

        // Generate a token for the newly created user
        try {
            String token = TokenUtils.generateToken(newUser.getUsername(), "Cloudshare", tokenDuration);
            // Return the token in the response
            return Response.ok().entity(token).build();
        } catch (Exception e) {
            // Handle token generation error
            return Response.serverError().build();
        }
    }
    @DELETE
    @Authenticated
    @Path("/{username}")
    public Response deleteUser(@PathParam("username") String username, @Context SecurityContext securityContext) {

        String authenticatedUsername = securityContext.getUserPrincipal().getName();

        // Check if the authenticated user is authorized to delete the specified user
        if (!authenticatedUsername.equals(username)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not authorized to delete this user").build();
        }
        User user = userService.findByUsername(username);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        // Delete the user
        userService.delete(user);

        return Response.ok().entity("User " + username + " deleted successfully.").build();
    }

    public static class LoginRequest {
        public String username;
        public String password;


    }
}
