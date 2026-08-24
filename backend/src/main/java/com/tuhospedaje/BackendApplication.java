package com.tuhospedaje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.tuhospedaje.configuration.CookieProperties;
import com.tuhospedaje.configuration.CanonicalAssetsProperties;
import com.tuhospedaje.configuration.CorsProperties;
import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.configuration.SessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CookieProperties.class, CanonicalAssetsProperties.class, CorsProperties.class, SessionProperties.class, EmailOutboxProperties.class})
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
