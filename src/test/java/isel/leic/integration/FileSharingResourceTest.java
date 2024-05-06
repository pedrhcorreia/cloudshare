package isel.leic.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.google.common.base.Predicates.equalTo;
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

        token = response.getBody().asString();

        //Create second user
        token2= given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"newUser\",\"password\":\"newPassword\"}")
                .when()
                .post("/auth/signup").getBody().asString();
        token3=  given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\":\"newUser2\",\"password\":\"newPassword2\"}")
                .when()
                .post("/auth/signup").getBody().asString();

        //Obtain IDs of users
        Response secondResponse = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user");

        secondResponse.then().statusCode(200);

        user1Id = secondResponse.jsonPath().getList("findAll { it.username == 'testUser' }.id", Long.class).get(0);
        user2Id = secondResponse.jsonPath().getList("findAll { it.username == 'newUser' }.id", Long.class).get(0);
        user3Id = secondResponse.jsonPath().getList("findAll { it.username == 'newUser2' }.id", Long.class).get(0);

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

        Response temp = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + user1Id)
                .then().statusCode(200).extract().response();

        Response temp2 = given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .delete("/user/" + user2Id)
                .then().statusCode(200).extract().response();

        Response temp3 = given()
                .header("Authorization", "Bearer " + token3)
                .when()
                .delete("/user/" + user3Id)
                .then().statusCode(200).extract().response();



    }

}