package in.bushansirgur.foodiesapi.service;

import in.bushansirgur.foodiesapi.entity.UserEntity;
import in.bushansirgur.foodiesapi.io.UserRequest;
import in.bushansirgur.foodiesapi.io.UserResponse;
import in.bushansirgur.foodiesapi.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private  final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;
    @Override
    public UserResponse registerUser(UserRequest request) {
        UserEntity newUser = convertToEntity(request);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public String findByUserId() {
        String loggedIdUserEmail  = authenticationFacade.getAuthentication().getName();
          UserEntity loogedInUser = userRepository.findByEmail(loggedIdUserEmail).orElseThrow(()-> new UsernameNotFoundException("User not found"));
          return loogedInUser.getId();
    }

    private UserEntity convertToEntity(UserRequest request){
       return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
    }

    private  UserResponse convertToResponse(UserEntity registerUser){
        return UserResponse.builder()
                .id(registerUser.getId())
                .name(registerUser.getName())
                .email(registerUser.getEmail())
                .build();
    }

}
