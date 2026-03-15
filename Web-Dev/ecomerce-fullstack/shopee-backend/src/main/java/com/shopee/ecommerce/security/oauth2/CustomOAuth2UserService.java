package com.shopee.ecommerce.security.oauth2;

import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import com.shopee.ecommerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (info.getEmail() == null || info.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = upsertUser(registrationId, info);
        return UserPrincipal.fromUser(user, oAuth2User.getAttributes());
    }

    private User upsertUser(String registrationId, OAuth2UserInfo info) {
        Optional<User> existing = userRepository.findByEmail(info.getEmail());

        if (existing.isPresent()) {
            User user = existing.get();
            // Update avatar if changed
            if (info.getImageUrl() != null) user.setAvatarUrl(info.getImageUrl());
            return userRepository.save(user);
        }

        // Create new user
        User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
        User user = User.builder()
                .email(info.getEmail())
                .fullName(info.getName())
                .avatarUrl(info.getImageUrl())
                .provider(provider)
                .providerId(info.getId())
                .role(User.Role.USER)
                .active(true)
                .verified(true) // OAuth2 emails are verified
                .build();
        return userRepository.save(user);
    }
}