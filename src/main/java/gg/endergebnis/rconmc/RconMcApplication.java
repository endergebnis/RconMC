package gg.endergebnis.rconmc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RconMcApplication {

    public static void main(String[] args) {
        SpringApplication.run(RconMcApplication.class, args);
    }
}
