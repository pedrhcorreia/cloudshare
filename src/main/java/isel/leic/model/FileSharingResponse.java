package isel.leic.model;

import isel.leic.model.storage.FileObject;

public class FileSharingResponse {
    private FileSharing fileSharing;
    private FileObject fileObject;

    public FileSharingResponse(FileSharing fileSharing, FileObject fileObject) {
        this.fileSharing = fileSharing;
        this.fileObject = fileObject;
    }

    public FileSharing getFileSharing() {
        return fileSharing;
    }

    public void setFileSharing(FileSharing fileSharing) {
        this.fileSharing = fileSharing;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public void setFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }
}
