package isel.leic.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import isel.leic.model.storage.FormData;
import isel.leic.resource.FileSharingResource;
import isel.leic.resource.MinioResource;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioResourceTest {

    private static String token;
    private static Long userId1;

    private static String token2;
    private static Long userId2;



    @Test
    @Order(1)
    public void testUploadFile_ValidFormData() {
        // User signup and token retrieval code remains unchanged
        Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"testUser\",\"password\":\"testPassword\"}")
                .when()
                .post("/auth/signup");

        token = response.jsonPath().getString("token");
        userId1 = response.jsonPath().getLong("user.id");

        // File upload
        File file = new File("src/main/resources/test-file.txt");
        FormData formData = new FormData();
        formData.data = file;
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";

        given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", file, "text/plain")
                .formParam("filename", formData.filename)
                .formParam("mimetype", formData.mimetype)
                .when()
                .post("/user/" + userId1 + "/object")
                .then()
                .statusCode(201);
    }


    @Test
    @Order(3)
    public void testDownloadFile_ValidObjectId() {
        String objectKey = "test-file.txt";

        // Download file and check response
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/object/download?objectKey=" + objectKey);

        // Verify response status code and content type
        response.then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        // Get the size of the downloaded file
        byte[] fileBytes = response.asByteArray();
        int downloadedFileSize = fileBytes.length;

        // Verify the size of the downloaded file
        assertEquals(14694, downloadedFileSize, "Downloaded file size doesn't match expected size");
    }

    @Test
    @Order(4)
    public void testGetPresignedUploadUrl() {
        MinioResource.UploadRequest uploadRequest = new MinioResource.UploadRequest("filename.txt", "text/plain");

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(uploadRequest)
                .when()
                .post("/user/" + userId1 + "/object/presign/upload");

        response.then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);

        String presignedUrl= response.jsonPath().getString("presignedUrl");


        assertNotNull(presignedUrl);
        // Define the file to upload
        File fileToUpload = new File("src/main/resources/test-file2.txt");

        given()
                .urlEncodingEnabled(false) // Ensure RestAssured doesn't URL encode the presigned URL
                .body(fileToUpload)
                .when()
                .put(presignedUrl)
                .then()
                .statusCode(200); // Assuming HTTP 200 indicates a successful upload
    }



    @Test
    @Order(6)
    public void testRenameFile() {
        String objectKey = "test-file.txt";
        String newName = "new-test-file.txt";

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("objectKey", objectKey)
                .queryParam("newName", newName)
                .when()
                .put("/user/" + userId1 + "/object")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("objectKey", newName)
                .when()
                .get("/user/" + userId1 + "/object/" )
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    public void testShareFileBetweenUsers() {


        // User signup and token retrieval for the second user
        Response response2 = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"newUser\",\"password\":\"newUserPassword\"}")
                .when()
                .post("/auth/signup");

         token2 = response2.jsonPath().getString("token");
         userId2 = response2.jsonPath().getLong("user.id");

        // Share the file between users
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

        // Assuming the file is successfully accessed by the second user
        given()
                .header("Authorization", "Bearer " + token2)
                .queryParam("objectKey", "new-test-file.txt")
                .when()
                .get("/user/" + userId1 + "/object/download")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(8)
    public void testDeleteObjectAndUser() {
        String objectKey = "new-test-file.txt";

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("objectKey", objectKey)
                .when()
                .delete("/user/" + userId1 + "/object/" )
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/" + userId1 + "/object")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

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