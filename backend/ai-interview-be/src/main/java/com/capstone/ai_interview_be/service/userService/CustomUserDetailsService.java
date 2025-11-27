package com.capstone.ai_interview_be.service.userService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.UserRepository;
@Service
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username).orElse(null);
        if(userEntity == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new CustomUserDetails(userEntity);
    }

}
