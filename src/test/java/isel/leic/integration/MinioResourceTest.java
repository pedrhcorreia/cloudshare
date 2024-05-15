package isel.leic.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import isel.leic.model.storage.FormData;
import isel.leic.resource.FileSharingResource;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static io.smallrye.common.constraint.Assert.assertNotNull;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioResourceTest {

    private static String token;
    private static Long userId1;

    private static String token2;
    private static Long userId2;

    private static byte[] fileArr;

    private static String anonymousToken;


    @Test
    @Order(1)
    public void testUploadFile_ValidFormData() {
        //Create user and obtain id and token
        String jsonBody = "{\"username\":\"testUser\",\"password\":\"testPassword\"}";
        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/auth/signup");

        token = response.jsonPath().getString("token");
        userId1 = (long) response.jsonPath().getInt("user.id");


        //upload file for the user created
        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";


        given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", formData.data, MediaType.APPLICATION_OCTET_STREAM)
                .formParam("filename", formData.filename)
                .formParam("mimetype", formData.mimetype)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/user/"+userId1+"/object")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    public void testGenerateAnonymousLink() {
        // Generate an anonymous access link for the uploaded file
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"expiration\": 3600}") // Expiration time in seconds
                .when()
                .post("/user/" + userId1 + "/object/test-file.txt/anonymous")
                .then()
                .statusCode(200)
                .extract()
                .response();

        anonymousToken = response.jsonPath().getString("token");
    }
    @Test
    @Order(3)
    public void testDownloadFile_ValidObjectId() {
        // Assuming a valid object key is available for testing
        String objectKey = "test-file.txt";

        // Perform the download file request and get the response
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .expect()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(notNullValue())
                .when()
                .get("/user/" + userId1 + "/object/" + objectKey);

        // Get the content of the downloaded file from the response
        byte[] downloadedFileContent = response.getBody().asByteArray();

        // Load the content of the original file for comparison
        byte[] originalFileContent = null;
        try {
            originalFileContent = Files.readAllBytes(Paths.get("src/main/resources/test-file.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Assert that the content of the downloaded file matches the content of the original file
        assertTrue(Arrays.equals(downloadedFileContent, originalFileContent));
        fileArr = downloadedFileContent;
    }

    @Test
    @Order(4)
    public void testGetFileInfoFromAnonymousLink() {
        // Get file information using the anonymous access token
        Response response = given()
                .queryParam("token", anonymousToken)
                .expect()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("anonymous/info");

        // Verify JSON response properties
        response.then()
                .assertThat()
                .body("fileName", equalTo("test-file.txt"))
                .body("userId", equalTo(userId1.intValue())) // Assuming userId is an int
                .body("username", notNullValue());
    }

    @Test
    @Order(5)
    public void testDownloadFileFromAnonymousLink() {
        // Download the file using the anonymous access token
        Response response = given()
                .queryParam("token", anonymousToken)
                .expect()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(notNullValue())
                .when()
                .get("anonymous/download");


        // Assert that the file content is not empty
        byte[] downloadedFileContent = response.getBody().asByteArray();
        assertNotNull(downloadedFileContent);

        // Load the content of the original file for comparison
        byte[] originalFileContent = null;
        try {
            originalFileContent = Files.readAllBytes(Paths.get("src/main/resources/test-file.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Compare the content of the downloaded file with the original file content
        assertArrayEquals(originalFileContent, downloadedFileContent);
    }

    @Test
    @Order(6)
    public void testRenameFile() {
        // Assuming a valid object key and new name are available for testing
        String objectKey = "test-file.txt";
        String newName = "new-test-file.txt";

        // Perform the rename file request
        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("newName", newName)
                .when()
                .put("/user/" + userId1 + "/object/" + objectKey)
                .then()
                .statusCode(200);


        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/object/" + newName)
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Compare the downloaded file's byte array to the expected byte array
        byte[] downloadedContent = response.getBody().asByteArray();
        assertArrayEquals(fileArr, downloadedContent);

        // Verify that the previous file name is not in the bucket
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/object/" + objectKey)
                .then()
                .statusCode(404);  // Assuming a 404 response if the previous file is not found
    }

    @Test
    @Order(7)
    public void testShareFileBetweenUsers() {
        // Create a new user
        String newUserJsonBody = "{\"username\":\"newUser\",\"password\":\"newUserPassword\"}";
        Response response = given()
                .contentType(ContentType.JSON)
                .body(newUserJsonBody)
                .when()
                .post("/auth/signup");

        token2 = response.jsonPath().getString("token");
        userId2 = (long) response.jsonPath().getInt("user.id");




        // Share the file uploaded by user1 with the newly created user
        FileSharingResource.ShareRequest shareRequest = new FileSharingResource.ShareRequest(
                FileSharingResource.ShareRequest.RecipientType.USER,
                userId2,
                "new-test-file.txt"
        );

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(shareRequest)
                .when()
                .post("/user/" + userId1 + "/fileshare")
                .then()
                .statusCode(200);

        // Attempt to access the shared file of user1 with the token of the new user
        given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .get("/user/" + userId1 + "/object/new-test-file.txt")
                .then()
                .statusCode(200);  // Assuming the file is successfully accessed
    }

    @Test
    @Order(8)
    public void testDeleteObjectAndUser() {
        // Assuming a valid object key is available for testing
        String objectKey = "new-test-file.txt";

        // Step 1: Delete the object
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + userId1 + "/object/" + objectKey)
                .then()
                .statusCode(200);

        // Step 2: Assert the object is not present
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/object")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        // Step 3: Delete the user
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/user/" + userId1)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token2)
                .when()
                .delete("/user/" + userId2)
                .then()
                .statusCode(200);
    }
}