package isel.leic.model.storage;

import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;

public class FileObject {
    private String objectKey;
    private Long size;
    private Instant lastModified;
    private String eTag;
    private String storageClass;

    public FileObject() {
    }

    public static FileObject from(S3Object s3Object) {
        FileObject file = new FileObject();
        if (s3Object != null) {
            file.setObjectKey(s3Object.key());
            file.setSize(s3Object.size());
            file.setLastModified(s3Object.lastModified());
            file.setETag(s3Object.eTag());
            file.setStorageClass(s3Object.storageClassAsString());
        }
        return file;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public Long getSize() {
        return size;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public String getETag() {
        return eTag;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public FileObject setObjectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }

    public FileObject setSize(Long size) {
        this.size = size;
        return this;
    }

    public FileObject setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public FileObject setETag(String eTag) {
        this.eTag = eTag;
        return this;
    }

    public FileObject setStorageClass(String storageClass) {
        this.storageClass = storageClass;
        return this;
    }
}
