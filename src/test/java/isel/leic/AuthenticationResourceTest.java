package isel.leic;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import isel.leic.model.User;
import isel.leic.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationResourceTest {
    private static String token;

    @Inject
    UserService userService;

    @Test
    @Order(1)
    public void testSignupEndpoint_ValidCredentials() {
        String username = "new-user";
        String password = "new-password";

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .when()
                .post("/auth/signup")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().response();
    }

    @Test
    @Order(2)
    public void testSignupEndpoint_ExistingUsername() {
        String existingUsername = "new-user"; // Assume this username already exists

        // Construct the request body with an existing username
        String jsonBody = "{\"username\":\"" + existingUsername + "\",\"password\":\"new_password\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .when()
                .post("/auth/signup")
                .then()
                .statusCode(409) // Conflict status code
                .body(equalTo("Username already exists")); // Verify the response message
    }

    @Test
    @Order(3)
    public void testLoginEndpoint_ValidCredentials() {
        String username = "new-user";
        String password = "new-password";

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        Response signupResponse = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().response();
        token = signupResponse.getBody().asString();
    }

    @Test
    @Order(4)
    public void testLoginEndpoint_InvalidCredentials() {
        String username = "wrong-username";
        String password = "wrong-password";

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body(equalTo("Invalid credentials"));
    }

    @Test
    @Order(5)
    public void testDeleteUser_Unauthenticated() {
        String username = "new-user";

        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/auth/" + username)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(6)
    public void testDeleteUser_Unauthorized() {

        Long usernameToDelete = 6000L; // User to be deleted

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/auth/" + usernameToDelete)
                .then()
                .statusCode(401)
                .body(equalTo("You are not authorized to delete this user"));
    }

    @Test
    @Order(8)
    public void testDeleteUser_ValidCredentials() {
        assertNotNull(token);


        User user = userService.findByUsername("new-user");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/auth/" + user.getId())
                .then()
                .statusCode(200)
                .body(equalTo("User " + user.getId() + " deleted successfully."));
    }

    @Test
    @Order(7)
    public void testSignupEndpoint_InvalidCredentials() {
        String username = "new-user";
        String password = "existing-password";

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .when()
                .post("/auth/signup")
                .then()
                .statusCode(409)
                .body(equalTo("Username already exists"));
    }
}
