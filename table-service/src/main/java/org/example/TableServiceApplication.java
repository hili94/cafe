package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class TableServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TableServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initTables(TableRepository tableRepository) {
        return args -> {
            // Initialize tables only if the database is empty
            if (tableRepository.count() == 0) {
                tableRepository.saveAll(Arrays.asList(
                        // Tables 1-3: up to 2 guests
                        new Table(1L, 2L),
                        new Table(2L, 2L),
                        new Table(3L, 2L),

                        // Tables 4-7: up to 6 guests
                        new Table(4L, 6L),
                        new Table(5L, 6L),
                        new Table(6L, 6L),
                        new Table(7L, 6L),

                        // Tables 8-9: up to 9 guests
                        new Table(8L, 9L),
                        new Table(9L, 9L)
                ));
                System.out.println("Table inventory initialized.");
            }
        };
    }
}