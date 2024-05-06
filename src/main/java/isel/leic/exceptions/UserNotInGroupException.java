package isel.leic.exceptions;

public class UserNotInGroupException extends RuntimeException {
    public UserNotInGroupException(String message) {
        super(message);
    }
}