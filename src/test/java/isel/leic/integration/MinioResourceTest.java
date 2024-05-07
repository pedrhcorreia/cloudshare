package isel.leic.integration;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.service.MinioService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioResourceTest {

    @Inject
    MinioService minioService;
/*
    @Test
    @Order(1)
    public void testUploadFile_ValidFormData() {

        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";


        given()
                .multiPart("formData", formData)
                .pathParam("id", 123L) //Change for actual user id
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/user/{id}/object")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    public void testDownloadFile_ValidObjectId() {
        // Assuming a valid object key is available for testing
        String objectKey = "example.pdf";

        // Perform the download file request
        given()
                .pathParam("id", 123L) //Change for actual user id
                .pathParam("objectKey", objectKey)
                .when()
                .get("/user/{id}/object/{objectKey}")
                .then()
                .statusCode(200);
    }
*/
    // Add test methods for other endpoints
}