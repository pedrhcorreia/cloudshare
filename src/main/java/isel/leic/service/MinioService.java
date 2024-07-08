package isel.leic.service;


import isel.leic.model.FileSharing;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.repository.FileSharingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class MinioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioService.class);

    @Inject
    S3Client minioClient;

    @Inject
    FileSharingRepository fileSharingRepository;

    @Inject
    S3Presigner preSigner;



    public List<Bucket> listBuckets() {
        LOGGER.info("Listing buckets");
        ListBucketsResponse response = minioClient.listBuckets();
        return response.buckets();
    }

    public String createBucket(String bucketName) {
        LOGGER.info("Creating bucket: {}", bucketName);
        CreateBucketRequest request = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        minioClient.createBucket(request);
        LOGGER.info("Bucket created successfully: {}", bucketName);
        return "Bucket created successfully: " + bucketName;
    }

    public String createEmptyFolder(String bucketName, String folderName) {
        LOGGER.info("Creating empty folder: {} in bucket: {}", folderName, bucketName);

        try {
            File emptyFile = File.createTempFile(folderName, ".tmp");
            try (FileOutputStream fos = new FileOutputStream(emptyFile)) {
                // Write zero bytes
            }
            emptyFile.deleteOnExit();

            FormData formData = new FormData();
            formData.data = emptyFile;
            formData.filename = folderName;
            formData.mimetype = "application/x-directory";

            String result = uploadObject(bucketName, formData);
            if (result.startsWith("Object uploaded successfully")) {
                LOGGER.info("Empty folder created successfully: {} in bucket: {}", folderName, bucketName);
                return "Folder created successfully";
            } else {
                String errorMessage = "Failed to create folder: " + folderName + " in bucket: " + bucketName;
                LOGGER.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while preparing to create empty folder: {} in bucket: {}", folderName, bucketName, e);
            throw new RuntimeException(e);
        }
    }

    public String deleteBucket(String bucketName) {
        LOGGER.info("Deleting bucket: {}", bucketName);
        List<FileObject> objects = listObjects(bucketName, null, null);
        if (!objects.isEmpty()) {
            for (FileObject object : objects) {
                deleteObject(bucketName, object.getObjectKey());
            }
            LOGGER.info("All objects deleted successfully from bucket: {}", bucketName);
        } else {
            LOGGER.info("No objects found in bucket: {}", bucketName);
        }

        String result = deleteBucketWithoutObjects(bucketName);
        return result;
    }

    private String deleteBucketWithoutObjects(String bucketName) {
        DeleteBucketRequest request = DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build();
        minioClient.deleteBucket(request);
        LOGGER.info("Bucket deleted successfully: {}", bucketName);
        return "Bucket deleted successfully: " + bucketName;
    }

    public List<FileObject> listObjects(String bucketName, String prefix, String delimiter) {
        LOGGER.info("Listing objects in bucket: {} {} {}", bucketName, prefix == null ? "" : "with prefix '" + prefix + "'",delimiter == null ? "" : "with delimiter '" + delimiter + "'");

        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .delimiter(delimiter)
                .build();
        ListObjectsResponse response = minioClient.listObjects(request);
        return response.contents().stream()
                .map(FileObject::from)
                .toList();
    }

    public String uploadObject(String bucketName, FormData formData) {
        LOGGER.info("Uploading object '{}' to bucket: {}", formData.getFilename(), bucketName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(formData.getFilename())
                .contentType(formData.getMimetype())
                .build();
        minioClient.putObject(request, formData.getData().toPath());
        LOGGER.info("Object uploaded successfully: {}", formData.getFilename());
        return "Object uploaded successfully: " + formData.getFilename();
    }

    public URL generatePreSignedUploadUrl(String bucketName, String objectKey, String contentType) {
        LOGGER.info("Generating presigned upload URL for bucket: {}, object: {}", bucketName, objectKey);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();


        PresignedPutObjectRequest preSignedRequest = preSigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest));

        URL url = preSignedRequest.url();
        LOGGER.info("Successfully generated presigned upload URL: {}", url);
        return url;

    }

    public URL generatePresignedDownloadUrl(String bucketName, String objectKey) {
        LOGGER.info("Generating presigned download URL for bucket: {}, object: {}", bucketName, objectKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();


        PresignedGetObjectRequest presignedRequest = preSigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest));

        URL url = presignedRequest.url();
        LOGGER.info("Successfully generated presigned download URL: {}", url);
        return url;

    }

    public String startMultipartUpload(String bucketName, String filename) {
        try {
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();

            CreateMultipartUploadResponse response = minioClient.createMultipartUpload(request);

            return response.uploadId();
        } catch (Exception e) {
            LOGGER.error("Error starting multipart upload for '{}': {}", filename, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String uploadPart(String bucketName, File data, String filename, String uploadId, int partNumber) {
        try {
            UploadPartRequest uploadRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartResponse uploadResponse = minioClient.uploadPart(uploadRequest, data.toPath());

            return uploadResponse.eTag();
        } catch (Exception e) {
            LOGGER.error("Error uploading part number {}: {}", partNumber, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void abortMultipartUpload(String bucketName, String filename, String uploadId) {
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .uploadId(uploadId)
                    .build();

            minioClient.abortMultipartUpload(request);
        } catch (Exception e) {
            LOGGER.error("Error aborting multipart upload for '{}': {}", filename, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void completeMultipartUpload(String bucketName, String filename, String uploadId) {
        try {
            ListPartsResponse listPartsResponse = minioClient.listParts(
                    ListPartsRequest.builder()
                            .bucket(bucketName)
                            .key(filename)
                            .uploadId(uploadId)
                            .build());

            List<Part> parts = listPartsResponse.parts();

            List<CompletedPart> completedParts = parts.stream()
                    .map(part -> CompletedPart.builder()
                            .partNumber(part.partNumber())
                            .eTag(part.eTag())
                            .build())
                    .collect(Collectors.toList());

            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            minioClient.completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(filename)
                            .uploadId(uploadId)
                            .multipartUpload(completedUpload)
                            .build());
            LOGGER.info("Multipart upload completed for '{}'", filename);
        } catch (Exception e) {
            LOGGER.error("Error completing multipart upload for '{}': {}", filename, e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public byte[] downloadObject(String bucketName, String objectKey) throws IOException {
        LOGGER.info("Downloading object '{}' from bucket: {}", objectKey, bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseInputStream<GetObjectResponse> responseInputStream = minioClient.getObject(request);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = responseInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        LOGGER.info("Object downloaded successfully: {}", objectKey);
        return outputStream.toByteArray();
    }

    public StreamingOutput downloadObjectAsStream(String bucketName, String objectKey) throws IOException {
        LOGGER.info("Downloading object '{}' from bucket: {}", objectKey, bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseInputStream<GetObjectResponse> responseInputStream = minioClient.getObject(request);

        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = responseInputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                responseInputStream.close();
            }
        };
    }

    public String deleteObject(String bucketName, String objectKey) {
        LOGGER.info("Deleting object '{}' from bucket: {}", objectKey, bucketName);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        minioClient.deleteObject(request);
        LOGGER.info("Object deleted successfully: {}", objectKey);
        return "Object deleted successfully: " + objectKey;
    }

    @Transactional
    public String renameObject(Long userId,String bucketName, String objectKey, String newObjectKey) {
        LOGGER.info("Renaming object '{}' in bucket '{}' to '{}'", objectKey, bucketName, newObjectKey);

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            HeadObjectResponse headResponse = minioClient.headObject(headRequest);
            String contentType = headResponse.contentType();

            renameFile(userId, bucketName, objectKey, newObjectKey);

            boolean isFolder = "application/x-directory".equals(contentType) || objectKey.endsWith("/");
            if (isFolder) {
                LOGGER.info("The object '{}' is a folder.", objectKey);
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(objectKey)
                        .build();
                ListObjectsV2Response listResponse = minioClient.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    String oldKey = s3Object.key();
                    String newKey = oldKey.replaceFirst(objectKey, newObjectKey);
                    renameFile(userId, bucketName, oldKey, newKey);
                }
            }
            Optional<List<FileSharing>> fileSharing = fileSharingRepository.findBySharedByUserIdAndFilename(userId, objectKey);
            if(fileSharing.isPresent()){
                for(FileSharing fs : fileSharing.get()){
                    fs.setFilename(newObjectKey);
                    fileSharingRepository.persist(fs);
                }
            }
            return "Object renamed successfully";
        } catch (S3Exception e) {
            String errorMessage = "Failed to rename object: " + objectKey + " to " + newObjectKey + " in bucket: " + bucketName;
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public void renameFile(Long userId, String bucketName, String objectKey, String newObjectKey){
        try{
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(objectKey)
                    .destinationBucket(bucketName)
                    .destinationKey(newObjectKey)
                    .build();
            CopyObjectResponse copyResponse = minioClient.copyObject(copyRequest);
            LOGGER.info("Object renamed successfully: '{}' to '{}'", objectKey, newObjectKey);
            deleteObject(bucketName, objectKey);
        }catch (S3Exception e){
            String errorMessage = "Failed to rename object: " + objectKey + " to " + newObjectKey + " in bucket: " + bucketName;
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public boolean doesObjectExist(String bucketName, String objectKey) {
        LOGGER.info("Verifying if object '{}' exists in bucket: {}", objectKey, bucketName);
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        try {
            minioClient.headObject(request);
            LOGGER.info("Object '{}' exists in bucket: {}", objectKey, bucketName);
            return true;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchKeyException) {
                LOGGER.info("Object '{}' does not exist in bucket: {}", objectKey, bucketName);
                return false;
            } else {
                LOGGER.error("Error verifying if object '{}' exists in bucket '{}': {}", objectKey, bucketName, cause.getMessage());
                throw new RuntimeException("Error verifying if object exists", cause);
            }
        }
    }





}