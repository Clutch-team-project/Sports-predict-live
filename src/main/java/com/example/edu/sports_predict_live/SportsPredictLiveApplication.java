package com.example.edu.sports_predict_live;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SportsPredictLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsPredictLiveApplication.class, args);
    }

}
