package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Example test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BookingFormE2ETest {
    
    @LocalServerPort
    private int port;
    
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
    }
    
    @AfterEach
    void tearDown() {
        driver.quit();
    }
    
    @Test
    void testBookingFormSubmission() {
        driver.get("http://localhost:" + port + "/booking_form.html");
        
        driver.findElement(By.id("customerName")).sendKeys("John Doe");
        driver.findElement(By.id("email")).sendKeys("john@example.com");
        // ... fill other fields
        driver.findElement(By.id("bookingForm")).submit();
        
        // Assert success message appears
        WebElement success = driver.findElement(By.id("successMessage"));
        assertTrue(success.isDisplayed());
    }
}
