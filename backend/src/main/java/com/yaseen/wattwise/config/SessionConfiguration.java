package com.yaseen.wattwise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
public class SessionConfiguration {

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${server.servlet.session.cookie.secure:false}")
            boolean secure) {

        DefaultCookieSerializer serializer =
                new DefaultCookieSerializer();

        serializer.setCookieName("WATTWISE_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secure);

        return serializer;
    }
}