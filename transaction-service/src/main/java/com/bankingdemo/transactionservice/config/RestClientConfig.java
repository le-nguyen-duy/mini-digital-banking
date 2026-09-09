package com.bankingdemo.transactionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${account-service.base-url}")
    private String accountServiceBaseUrl;

    @Bean
    public RestClient accountServiceRestClient() {
        return RestClient.builder()
                .baseUrl(accountServiceBaseUrl)
                .build();
    }
}
