package isel.leic.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import isel.leic.model.Group;
import isel.leic.model.User;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.*;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAndGroupResourceTest {

    private static String token;
    private static String token2;
    private static Long userId1;
    private static Long userId2;
    private static Long groupId;


    @Test
    @Order(1)
    public void testGetUsersEndpoint() {
        String jsonBody = "{\"username\":\"testUser\",\"password\":\"testPassword\"}";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/auth/signup");


        token = response.jsonPath().getString("token");
        userId1 = (long) response.jsonPath().getInt("user.id");
        String newUserJsonBody = "{\"username\":\"newUser\",\"password\":\"newPassword\"}";
        Response response2 = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUserJsonBody)
                .when()
                .post("/auth/signup");

        token2 = response2.jsonPath().getString("token");
        userId2 = (long) response.jsonPath().getInt("user.id");



        Response secondResponse = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user");

        secondResponse.then().statusCode(200);

        userId1 = secondResponse.jsonPath().getList("findAll { it.username == 'testUser' }.id", Long.class).get(0);
        userId2 = secondResponse.jsonPath().getList("findAll { it.username == 'newUser' }.id", Long.class).get(0);
    }

    @Test
    @Order(2)
    public void testUpdateUserPassword() {
        String newPassword = "newPassword123";

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(newPassword)
                .when()
                .put("/user/" + userId1);

        response.then().statusCode(200);

        Response secondResponse = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user");


    }

    @Test
    @Order(3)
    public void testCreateGroupEndpoint() {
        String groupName = "Test Group";

        String jsonBody = "{\"name\":\"" + groupName + "\"}";

        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(jsonBody)
                .when()
                .post("/user/" + userId1 + "/group");
        response.then().statusCode(200);
    }



    @Test
    @Order(4)
    public void testGetGroupsEndpoint() {
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/group");

        response.then().statusCode(200);

        List<Group> groups = response.jsonPath().getList("$", Group.class);
        assertEquals(1, groups.size());
        groupId = groups.get(0).getId();
    }

    @Test
    @Order(5)
    public void testUpdateGroupName() {
        String newName = "Updated Group Name";


        Response updateResponse = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body( newName)
                .when()
                .put("/user/" + userId1 + "/group/" + groupId+ "/name" );

        updateResponse.then().statusCode(200);


        Response fetchResponse = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/group/" );

        fetchResponse.then().statusCode(200);

        List<Group> groups = fetchResponse.jsonPath().getList("$", Group.class);
        assertEquals(1, groups.size());
        assertEquals(newName, groups.get(0).getName());
    }


    @Test
    @Order(6)
    public void testAddUserToGroup() {
        Long userId = userId2;

        Response addUserToGroupResponse = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(userId)
                .when()
                .post("/user/" + userId1 + "/group/" + groupId);


        addUserToGroupResponse.then().statusCode(200);
    }

    @Test
    @Order(7)
    public void testGetGroupMembers() {

        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/group/" + groupId + "/member");

        response.then().statusCode(200);

        List<User> groupMembers = response.jsonPath().getList("$", User.class);

        assertEquals(1, groupMembers.size());
        assertEquals(userId2, groupMembers.get(0).getId());
    }

    @Test
    @Order(8)
    public void testRemoveMemberFromGroup() {
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + userId1 + "/group/" + groupId + "/member/" + userId2);

        response.then().statusCode(200);
    }
    @Test
    @Order(9)
    public void testGetEmptyGroupMembers() {

        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/group/" + groupId + "/member");

        response.then().statusCode(200);

    }


    @Test
    @Order(10)
    public void testDeleteUserEndpoint() {
        Response temp = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + userId1);

        temp.then().statusCode(200);

        Response response = given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .delete("/user/" + userId2);


        response.then().statusCode(200);

    }





}
