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
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    void navigateToBookingForm() {
        driver.get("http://localhost:" + port + "/booking_form.html");
    }

    void enterCustomerName(String name) {
        driver.findElement(By.id("customerName")).sendKeys(name);
    }

    void enterEmail(String email) {
        driver.findElement(By.id("email")).sendKeys(email);
    }

    void enterPhone(String phone) {
        driver.findElement(By.id("phone")).sendKeys(phone);
    }

    void selectFutureDate(LocalDate date) {
        WebElement dateInput = driver.findElement(By.id("reservationDate"));

        // Set the date value using JavaScript instead of sendKeys
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", dateInput, date.toString());

        // Trigger the change event to ensure it fires
        js.executeScript("arguments[0].dispatchEvent(new Event('change'));", dateInput);

    }

    void selectTimeSlot(int index) {
        Select timeSelect = new Select(driver.findElement(By.id("reservationTime")));
        timeSelect.selectByIndex(index);
    }

    void selectNumberOfGuests(int numberOfGuests) {
        driver.findElement(By.id("numberOfGuests")).sendKeys(Integer.toString(numberOfGuests));
    }

    void submitBookingForm() {
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    void submitFirstBooking(WebDriverWait wait) {
        navigateToBookingForm();

        // Fill in all required fields
        enterCustomerName("John Doe");
        enterEmail("john@example.com");
        enterPhone("555-1234");
        selectFutureDate(LocalDate.now().plusDays(7));

        // Wait for time slots to load (more than just the default option)
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("#reservationTime option"), 1));

        // Select a time slot (skip index 0 which is the placeholder)
        selectTimeSlot(1);

        selectNumberOfGuests(4);

        submitBookingForm();
    }
    @Test
    void testBookingFormSubmission() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        submitFirstBooking(wait);
        // Wait for and assert success message appears
        WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("successMessage")));
        assertTrue(success.isDisplayed());
        assertTrue(success.getText().contains("Booking confirmed"));
        
        // Optionally verify error message is NOT displayed
        WebElement error = driver.findElement(By.id("errorMessage"));
        assertFalse(error.isDisplayed());
    }

    @Test
    void testAvailableTimeSlots() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        submitFirstBooking(wait);
        // Wait for and assert success message appears
        WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("successMessage")));
        assertTrue(success.isDisplayed());
        assertTrue(success.getText().contains("Booking confirmed"));

        // submit another booking that overlaps with the first booking
        navigateToBookingForm();
        enterCustomerName("Jane Doe");
        enterEmail("jane@gmail.com");
        enterPhone("555-1234");
        selectFutureDate(LocalDate.now().plusDays(7));

        // Verify available time slots start from 11:00
        Select timeSelect = new Select(driver.findElement(By.id("reservationTime")));
        List<WebElement> options = timeSelect.getOptions();

        // Remove the first option which is the placeholder "-- Select a time --"
        List<WebElement> actualTimeSlots = options.subList(1, options.size());

        // Verify the first actual time slot is 11:00
        assertFalse(actualTimeSlots.isEmpty(), "Time slots should be available");
        assertEquals("11:00:00", actualTimeSlots.get(0).getAttribute("value"),
                "First available time slot should be 11:00:00");

        //Verify that 10:00 and earlier slots (that should be blocked) are NOT present
        List<String> blockedTimes = Arrays.asList("10:00:00", "10:15:00", "10:30:00", "10:45:00");
        for (WebElement option : actualTimeSlots) {
            String value = option.getAttribute("value");
            assertFalse(blockedTimes.contains(value),
                    "Time slot " + value + " should be blocked but is available");
        }
        selectTimeSlot(1); //select same time slot as first booking
        selectNumberOfGuests(4);
        submitBookingForm();

        success = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("successMessage")));
        assertTrue(success.isDisplayed());
        assertTrue(success.getText().contains("Booking confirmed"));

    }

    @Test
    void testBookingTooLateForClosing() {
        driver.get("http://localhost:" + port + "/booking_form.html");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Fill in all required fields
        enterCustomerName("John Doe");
        enterEmail("john@example.com");
        enterPhone("555-1234");
        selectFutureDate(LocalDate.now().plusDays(7));

        // Wait for time slots to load
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("#reservationTime option"), 1));

        // Select the last available time slot (which should be safe for 1 guest)
        Select timeSelect = new Select(driver.findElement(By.id("reservationTime")));
        List<WebElement> options = timeSelect.getOptions();
        timeSelect.selectByIndex(options.size() - 1); // Last slot

        // Try to book with 3 guests (45 minutes needed)
        // This should fail if the last slot doesn't have enough time
        selectNumberOfGuests(3);
        submitBookingForm();

        // Wait for error message
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("errorMessage")));

        // Verify error message is displayed and mentions closing time
        assertTrue(error.isDisplayed());
        assertTrue(error.getText().toLowerCase().contains("closing time") ||
                        error.getText().toLowerCase().contains("cannot be completed"),
                "Error should mention closing time constraint");
    }

    @Test
    void testBookingValidTimeBeforeClosing() {
        driver.get("http://localhost:" + port + "/booking_form.html");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Fill in all required fields
        enterCustomerName("Jane Smith");
        enterEmail("jane@example.com");
        enterPhone("555-5678");
        selectFutureDate(LocalDate.now().plusDays(7));

        // Wait for time slots to load
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("#reservationTime option"), 1));

        // Select an early time slot that should definitely work
        selectTimeSlot(1); // First available slot (should be 09:00)

        // Book with 9 guests (max party, 135 minutes)
        selectNumberOfGuests(9);
        submitBookingForm();

        // Wait for success message
        WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("successMessage")));

        assertTrue(success.isDisplayed());
        assertTrue(success.getText().contains("Booking confirmed"));
    }
}
