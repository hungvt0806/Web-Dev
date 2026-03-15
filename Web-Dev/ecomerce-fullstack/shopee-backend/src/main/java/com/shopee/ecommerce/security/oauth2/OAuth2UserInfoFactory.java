package com.shopee.ecommerce.security.oauth2;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        }
        throw new IllegalArgumentException("Sorry! Login with " + registrationId + " is not supported yet.");
    }
}