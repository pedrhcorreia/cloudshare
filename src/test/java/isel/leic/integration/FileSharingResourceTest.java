package isel.leic.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import jakarta.json.Json;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.*;

import java.util.List;
import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileSharingResourceTest {

    private static String token;
    private static String token2;
    private static String token3;

    private static Long user1Id;

    private static Long user2Id;
    private static Long user3Id;

    private static Long fileShareId;




    @Test
    @Order(1)
    public void testShareFileToUser_ValidRequest() {
        //Create first user
        Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"testUser\",\"password\":\"testPassword\"}")
                .when()
                .post("/auth/signup");

        token = response.jsonPath().getString("token");
        user1Id = (long) response.jsonPath().getInt("user.id");


        //Create second user
        Response response2 = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"newUser\",\"password\":\"newPassword\"}")
                .when()
                .post("/auth/signup");

        user2Id = (long) response2.jsonPath().getInt("user.id");
        token2 = response2.jsonPath().getString("token");

        Response response3 =  given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"newUser2\",\"password\":\"newPassword2\"}")
                .when()
                .post("/auth/signup");

        user3Id = (long) response3.jsonPath().getInt("user.id");
        token3 = response3.jsonPath().getString("token");



        String jsonBody = "{\"recipientType\":\"USER\",\"recipientId\":" + user2Id + ",\"filename\":\"" + "example.txt" + "\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(jsonBody)
                .when()
                .post("/user/" + user1Id + "/fileshare")
                .then()
                .statusCode(200);

    }

    @Test
    @Order(2)
    public void testShareFileToGroup_ValidRequest() {
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"name\":\"Test Group\"}")
                .when()
                .post("/user/" + user1Id + "/group");
        Group createdGroup = response.as(Group.class);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(user3Id)
                .when()
                .post("/user/" + user1Id + "/group/" + createdGroup.getId());

        String jsonBody = "{\"recipientType\":\"GROUP\",\"recipientId\":" + createdGroup.getId() + ",\"filename\":\"" + "example.txt" + "\"}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token) // Include authentication token
                .body(jsonBody)
                .when()
                .post("/user/" + user1Id + "/fileshare")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    public void testGetFilesSharedByUser_ValidRequest() {
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + user1Id + "/fileshare")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        List<FileSharing> sharedFiles = response.jsonPath().getList(".", FileSharing.class);


        Assertions.assertEquals(2, sharedFiles.size());

        Assertions.assertEquals(user2Id, sharedFiles.get(0).getSharedToUserId());
        Assertions.assertEquals(user3Id, sharedFiles.get(1).getSharedToUserId());
        fileShareId = sharedFiles.get(0).getId();
    }

    @Test
    @Order(4)
    public void testGetFilesSharedToUser_ValidRequest() {
        Response response = given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .get("/user/" + user2Id + "/fileshare/received")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        List<FileSharing> sharedFiles = response.jsonPath().getList(".", FileSharing.class);


        Assertions.assertEquals(1,sharedFiles.size());

        Assertions.assertEquals(user1Id, sharedFiles.get(0).getSharedByUserId());

    }

    @Test
    @Order(5)
    public void testDeleteFileShare_ValidRequest() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(fileShareId)
                .when()
                .delete("/user/" + user1Id + "/fileshare")
                .then()
                .statusCode(200);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + user1Id + "/fileshare")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        List<FileSharing> sharedFiles = response.jsonPath().getList(".", FileSharing.class);
        Assertions.assertEquals(1,sharedFiles.size());
        Assertions.assertEquals(sharedFiles.get(0).getSharedToUserId(),user3Id);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + user1Id)
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .delete("/user/" + user2Id)
                .then().statusCode(200);

       given()
                .header("Authorization", "Bearer " + token3)
                .when()
                .delete("/user/" + user3Id)
                .then().statusCode(200);



    }

}