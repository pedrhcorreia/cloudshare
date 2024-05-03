package isel.leic;

import isel.leic.model.User;
import isel.leic.repository.UserRepository;
import isel.leic.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock behavior of userRepository
        User mockUser = new User();
        mockUser.setUsername("Stuart");
        when(userService.findByUsername("Stuart")).thenReturn(mockUser);
        when(userService.findByUsername("John")).thenReturn(null);
    }
    //TODO test the userService with mocking
}
