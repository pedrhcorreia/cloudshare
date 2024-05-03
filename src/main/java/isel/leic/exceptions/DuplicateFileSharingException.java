package isel.leic.exceptions;

public class DuplicateFileSharingException extends RuntimeException {

    public DuplicateFileSharingException(String message) {
        super(message);
    }

    public DuplicateFileSharingException(String message, Throwable cause) {
        super(message, cause);
    }
}