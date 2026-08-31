package com.terramap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI terramapOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TerraMap API")
                        .description("Map-first marketplace API for buying and selling land parcels with PostGIS spatial validation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TerraMap Engineering Team")
                                .email("engineering@terramap.dev"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
