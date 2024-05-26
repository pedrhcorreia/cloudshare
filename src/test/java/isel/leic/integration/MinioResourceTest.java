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
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static io.smallrye.common.constraint.Assert.assertNotNull;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        // Create user and obtain id and token
        String jsonBody = "{\"username\":\"testUser\",\"password\":\"testPassword\"}";

        CompletableFuture<Response> signupFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return given()
                        .contentType(ContentType.JSON)
                        .body(jsonBody)
                        .when()
                        .post("/auth/signup");
            } catch (Exception e) {
                throw new RuntimeException("Failed to sign up", e);
            }
        });

        signupFuture.thenAccept(signupResponse -> {
            try {
                assertEquals(200, signupResponse.getStatusCode());

                token = signupResponse.jsonPath().getString("token");
                userId1 = Long.valueOf(signupResponse.jsonPath().getString("user.id")); // Assign userId1 properly

                // Upload file for the user created
                FormData formData = new FormData();
                formData.data = new File("src/main/resources/test-file.txt");
                formData.filename = "test-file.txt";
                formData.mimetype = "text/plain";

                CompletableFuture<Response> uploadFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return given()
                                .header("Authorization", "Bearer " + token)
                                .multiPart("file", formData.data, MediaType.APPLICATION_OCTET_STREAM)
                                .formParam("filename", formData.filename)
                                .formParam("mimetype", formData.mimetype)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .when()
                                .post("/user/"+userId1+"/object");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload file", e);
                    }
                });

                uploadFuture.thenAccept(uploadResponse -> {
                    try {
                        assertEquals(201, uploadResponse.getStatusCode());
                        // Add additional assertions as needed
                    } catch (Throwable t) {
                        throw new RuntimeException("Assertion failed in upload response", t);
                    }
                }).join(); // Wait for uploadFuture to complete
            } catch (Throwable t) {
                throw new RuntimeException("Failed to process sign up response", t);
            }
        }).join(); // Wait for signupFuture to complete
    }


    @Test
    @Order(2)
    public void testGenerateAnonymousLink() {
        // Generate an anonymous access link for the uploaded file
        CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body("{\"expiration\": 3600}") // Expiration time in seconds
                    .when()
                    .post("/user/" + userId1 + "/object/test-file.txt/anonymous");
        });

        responseFuture.thenAccept(response -> {
            assertEquals(200, response.getStatusCode());
            // Extract the anonymous token and add additional assertions as needed
        });
    }

    @Test
    @Order(3)
    public void testDownloadFile_ValidObjectId() {
        // Assuming a valid object key is available for testing
        String objectKey = "test-file.txt";

        // Perform the download file request and get the response
        CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .header("Authorization", "Bearer " + token)
                    .expect()
                    .statusCode(200)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(notNullValue())
                    .when()
                    .get("/user/" + userId1 + "/object/" + objectKey);
        });

        responseFuture.thenAccept(response -> {
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
        });
    }

    @Test
    @Order(4)
    public void testGetFileInfoFromAnonymousLink() {
        // Get file information using the anonymous access token
        CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .queryParam("token", anonymousToken)
                    .expect()
                    .statusCode(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .when()
                    .get("anonymous/info");
        });

        responseFuture.thenAccept(response -> {
            // Verify JSON response properties
            response.then()
                    .assertThat()
                    .body("fileName", equalTo("test-file.txt"))
                    .body("userId", equalTo(userId1.intValue())) // Assuming userId is an int
                    .body("username", notNullValue());
        });
    }


    @Test
    @Order(5)
    public void testDownloadFileFromAnonymousLink() {
        // Download the file using the anonymous access token
        CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .queryParam("token", anonymousToken)
                    .expect()
                    .statusCode(200)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(notNullValue())
                    .when()
                    .get("anonymous/download");
        });

        responseFuture.thenAccept(response -> {
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
        });
    }

    @Test
    @Order(6)
    public void testRenameFile() {
        // Assuming a valid object key and new name are available for testing
        String objectKey = "test-file.txt";
        String newName = "new-test-file.txt";

        // Perform the rename file request
        CompletableFuture<Response> renameResponseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .header("Authorization", "Bearer " + token)
                    .queryParam("newName", newName)
                    .when()
                    .put("/user/" + userId1 + "/object/" + objectKey);
        });

        renameResponseFuture.thenAccept(response -> {
            assertEquals(200, response.getStatusCode());

            // Verify that the renamed file can be accessed
            CompletableFuture<Response> accessResponseFuture = CompletableFuture.supplyAsync(() -> {
                return given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/user/" + userId1 + "/object/" + newName);
            });

            accessResponseFuture.thenAccept(accessResponse -> {
                byte[] downloadedContent = accessResponse.getBody().asByteArray();
                assertArrayEquals(fileArr, downloadedContent);

                // Verify that the previous file name is not in the bucket
                CompletableFuture<Response> notFoundResponseFuture = CompletableFuture.supplyAsync(() -> {
                    return given()
                            .header("Authorization", "Bearer " + token)
                            .when()
                            .get("/user/" + userId1 + "/object/" + objectKey);
                });

                notFoundResponseFuture.thenAccept(notFoundResponse -> {
                    assertEquals(404, notFoundResponse.getStatusCode());
                });
            });
        });
    }


    @Test
    @Order(7)
    public void testShareFileBetweenUsers() {
        // Create a new user
        String newUserJsonBody = "{\"username\":\"newUser\",\"password\":\"newUserPassword\"}";
        Response createUserResponse = given()
                .contentType(ContentType.JSON)
                .body(newUserJsonBody)
                .when()
                .post("/auth/signup");

        token2 = createUserResponse.jsonPath().getString("token");
        userId2 = (long) createUserResponse.jsonPath().getInt("user.id");

        // Share the file uploaded by user1 with the newly created user
        FileSharingResource.ShareRequest shareRequest = new FileSharingResource.ShareRequest(
                FileSharingResource.ShareRequest.RecipientType.USER,
                userId2,
                "new-test-file.txt"
        );

        CompletableFuture<Response> shareResponseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(shareRequest)
                    .when()
                    .post("/user/" + userId1 + "/fileshare");
        });

        shareResponseFuture.thenAccept(shareResponse -> {
            assertEquals(200, shareResponse.getStatusCode());

            // Attempt to access the shared file of user1 with the token of the new user
            CompletableFuture<Response> accessResponseFuture = CompletableFuture.supplyAsync(() -> {
                return given()
                        .header("Authorization", "Bearer " + token2)
                        .when()
                        .get("/user/" + userId1 + "/object/new-test-file.txt");
            });

            accessResponseFuture.thenAccept(accessResponse -> {
                assertEquals(200, accessResponse.getStatusCode());
            });
        });
    }

    @Test
    @Order(8)
    public void testDeleteObjectAndUser() {
        // Assuming a valid object key is available for testing
        String objectKey = "new-test-file.txt";

        // Step 1: Delete the object
        CompletableFuture<Response> deleteObjectResponseFuture = CompletableFuture.supplyAsync(() -> {
            return given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete("/user/" + userId1 + "/object/" + objectKey);
        });

        deleteObjectResponseFuture.thenAccept(deleteObjectResponse -> {
            assertEquals(200, deleteObjectResponse.getStatusCode());

            // Step 2: Assert the object is not present
            CompletableFuture<Response> verifyObjectNotPresentFuture = CompletableFuture.supplyAsync(() -> {
                return given()
                        .header("Authorization", "Bearer " + token)
                        .when()
                        .get("/user/" + userId1 + "/object");
            });

            verifyObjectNotPresentFuture.thenAccept(verifyObjectNotPresentResponse -> {
                assertEquals(200, verifyObjectNotPresentResponse.getStatusCode());
                assertEquals(0, verifyObjectNotPresentResponse.getBody().jsonPath().getList("$").size());

                // Step 3: Delete the user
                CompletableFuture<Response> deleteUserResponseFuture = CompletableFuture.supplyAsync(() -> {
                    return given()
                            .header("Authorization", "Bearer " + token)
                            .when()
                            .delete("/user/" + userId1);
                });

                deleteUserResponseFuture.thenAccept(deleteUserResponse -> {
                    assertEquals(200, deleteUserResponse.getStatusCode());
                });
            });
        });
    }

}