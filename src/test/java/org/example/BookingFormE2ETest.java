package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.time.Duration;
import java.time.LocalDate;
import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Fill in all required fields
        driver.findElement(By.id("customerName")).sendKeys("John Doe");
        driver.findElement(By.id("email")).sendKeys("john@example.com");
        driver.findElement(By.id("phone")).sendKeys("555-1234");
        
        // Select a future date and set it using JavaScript
        LocalDate futureDate = LocalDate.now().plusDays(7);
        WebElement dateInput = driver.findElement(By.id("reservationDate"));
        
        // Set the date value using JavaScript instead of sendKeys
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", dateInput, futureDate.toString());
        
        // Trigger the change event to ensure it fires
        js.executeScript("arguments[0].dispatchEvent(new Event('change'));", dateInput);
        
        // Wait for time slots to load (more than just the default option)
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
            By.cssSelector("#reservationTime option"), 1));
        
        // Select a time slot (skip index 0 which is the placeholder)
        Select timeSelect = new Select(driver.findElement(By.id("reservationTime")));
        timeSelect.selectByIndex(1);
        
        driver.findElement(By.id("numberOfGuests")).sendKeys("4");
        
        // Submit form
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Wait for and assert success message appears
        WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("successMessage")));
        assertTrue(success.isDisplayed());
        assertTrue(success.getText().contains("Booking confirmed"));
        
        // Optionally verify error message is NOT displayed
        WebElement error = driver.findElement(By.id("errorMessage"));
        assertFalse(error.isDisplayed());
    }
}
