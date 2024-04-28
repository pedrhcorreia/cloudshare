package isel.leic;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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

    @Test
    @Order(1)
    public void testSignupEndpoint_ValidCredentials() {
        String username = "new_user";
        String password = "new_password";

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
    public void testLoginEndpoint_ValidCredentials() {
        String username = "new_user";
        String password = "new_password";

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
    @Order(3)
    public void testLoginEndpoint_InvalidCredentials() {
        String username = "wrong_username";
        String password = "wrong_password";

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
    @Order(4)
    public void testDeleteUser_Unauthenticated() {
        String username = "new_user";

        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/auth/" + username)
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    public void testDeleteUser_Unauthorized() {

        String usernameToDelete = "username1"; // User to be deleted

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/auth/" + usernameToDelete)
                .then()
                .statusCode(401)
                .body(equalTo("You are not authorized to delete this user"));
    }

    @Test
    @Order(7)
    public void testDeleteUser_ValidCredentials() {
        assertNotNull(token);

        String username = "new_user";

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/auth/" + username)
                .then()
                .statusCode(200)
                .body(equalTo("User " + username + " deleted successfully."));
    }

    @Test
    @Order(6)
    public void testSignupEndpoint_InvalidCredentials() {
        String username = "new_user";
        String password = "existing_password";

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
