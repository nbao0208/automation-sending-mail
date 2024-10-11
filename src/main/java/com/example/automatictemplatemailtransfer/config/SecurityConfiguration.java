package com.example.automatictemplatemailtransfer.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfiguration {
  public static final String[] WHITE_LIST={
          "/api/v1/**"
  };

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            requestRegistry->{
              requestRegistry.requestMatchers(WHITE_LIST).permitAll()
                      .anyRequest().authenticated();
            }
    ).build();
  }
}
