package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AdminDashboardE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    
    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        // Clear any existing bookings
        bookingRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    private void loginAsAdmin() {
        driver.get("http://localhost:" + port + "/admin_dashboard.html");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for login form to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        
        // Enter credentials
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin");
        
        // Submit login form
        driver.findElement(By.cssSelector("#loginForm button[type='submit']")).click();
        
        // Wait for dashboard to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
    }

    private void createTestBooking(String name, String email, LocalDate date, LocalTime time, int guests) {
        Booking booking = new Booking(name, email, "555-1234", date, time, guests);
        bookingRepository.save(booking);
    }

    @Test
    void testLoginWithValidCredentials() {
        driver.get("http://localhost:" + port + "/admin_dashboard.html");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Enter valid credentials
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.cssSelector("#loginForm button[type='submit']")).click();
        
        // Verify dashboard is displayed
        WebElement dashboard = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
        assertTrue(dashboard.isDisplayed(), "Dashboard should be visible after successful login");
        
        // Verify login form is hidden
        WebElement loginContainer = driver.findElement(By.id("loginContainer"));
        assertFalse(loginContainer.isDisplayed(), "Login form should be hidden after successful login");
    }

    @Test
    void testLoginWithInvalidCredentials() {
        driver.get("http://localhost:" + port + "/admin_dashboard.html");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Enter invalid credentials
        driver.findElement(By.id("username")).sendKeys("wrong");
        driver.findElement(By.id("password")).sendKeys("wrong");
        driver.findElement(By.cssSelector("#loginForm button[type='submit']")).click();
        
        // Verify error message is displayed
        WebElement errorDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginError")));
        assertTrue(errorDiv.isDisplayed(), "Error message should be displayed");
        assertTrue(errorDiv.getText().contains("Invalid"), "Error message should mention invalid credentials");
        
        // Verify dashboard is NOT displayed
        WebElement dashboard = driver.findElement(By.id("dashboard"));
        assertFalse(dashboard.isDisplayed(), "Dashboard should not be visible with invalid credentials");
    }

    @Test
    void testDashboardDisplaysBookings() {
        // Create test bookings
        createTestBooking("John Doe", "john@example.com", LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);
        createTestBooking("Jane Smith", "jane@example.com", LocalDate.now().plusDays(2), LocalTime.of(14, 0), 4);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for bookings table to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Verify table is visible
        WebElement table = driver.findElement(By.id("bookingsTable"));
        assertTrue(table.isDisplayed(), "Bookings table should be visible");
        
        // Verify bookings are displayed
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(2, rows.size(), "Should display 2 bookings");
        
        // Verify booking details
        assertTrue(driver.getPageSource().contains("John Doe"), "Should display John Doe");
        assertTrue(driver.getPageSource().contains("Jane Smith"), "Should display Jane Smith");
    }

    @Test
    void testStatisticsDisplayCorrectly() {
        // Create test bookings
        LocalDate today = LocalDate.now();
        createTestBooking("Today Booking", "today@example.com", today, LocalTime.of(10, 0), 2);
        createTestBooking("Future Booking 1", "future1@example.com", today.plusDays(1), LocalTime.of(11, 0), 3);
        createTestBooking("Future Booking 2", "future2@example.com", today.plusDays(2), LocalTime.of(12, 0), 4);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for statistics to load
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("totalBookings"), "3"));
        
        // Verify statistics
        assertEquals("3", driver.findElement(By.id("totalBookings")).getText(), "Total bookings should be 3");
        assertEquals("1", driver.findElement(By.id("todayBookings")).getText(), "Today's bookings should be 1");
        assertEquals("2", driver.findElement(By.id("upcomingBookings")).getText(), "Upcoming bookings should be 2");
        assertEquals("9", driver.findElement(By.id("totalGuests")).getText(), "Total guests should be 9 (2+3+4)");
    }

    @Test
    void testFilterByToday() {
        // Create test bookings
        LocalDate today = LocalDate.now();
        createTestBooking("Today Booking", "today@example.com", today, LocalTime.of(10, 0), 2);
        createTestBooking("Future Booking", "future@example.com", today.plusDays(1), LocalTime.of(11, 0), 3);
        createTestBooking("Past Booking", "past@example.com", today.minusDays(1), LocalTime.of(12, 0), 4);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Apply "Today" filter
        Select filterSelect = new Select(driver.findElement(By.id("filterStatus")));
        filterSelect.selectByValue("today");
        
        // Wait for filter to apply
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#bookingsBody tr"), 1));
        
        // Verify only today's booking is shown
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(1, rows.size(), "Should display only 1 booking for today");
        assertTrue(driver.getPageSource().contains("Today Booking"), "Should display today's booking");
        assertFalse(driver.getPageSource().contains("Future Booking"), "Should not display future booking");
    }

    @Test
    void testFilterByUpcoming() {
        // Create test bookings
        LocalDate today = LocalDate.now();
        createTestBooking("Today Booking", "today@example.com", today, LocalTime.of(10, 0), 2);
        createTestBooking("Future Booking", "future@example.com", today.plusDays(1), LocalTime.of(11, 0), 3);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Apply "Upcoming" filter
        Select filterSelect = new Select(driver.findElement(By.id("filterStatus")));
        filterSelect.selectByValue("upcoming");
        
        // Wait for filter to apply
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#bookingsBody tr"), 1));
        
        // Verify only upcoming bookings are shown
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(1, rows.size(), "Should display only 1 upcoming booking");
        assertTrue(driver.getPageSource().contains("Future Booking"), "Should display future booking");
    }

    @Test
    void testSortByName() {
        // Create test bookings with different names
        createTestBooking("Zoe Last", "zoe@example.com", LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);
        createTestBooking("Alice First", "alice@example.com", LocalDate.now().plusDays(1), LocalTime.of(11, 0), 3);
        createTestBooking("Mike Middle", "mike@example.com", LocalDate.now().plusDays(1), LocalTime.of(12, 0), 4);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Apply "Name" sorting
        Select sortSelect = new Select(driver.findElement(By.id("sortBy")));
        sortSelect.selectByValue("name");
        
        // Wait a moment for sorting to apply
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Verify bookings are sorted by name
        List<WebElement> nameElements = driver.findElements(By.cssSelector("#bookingsBody tr td:nth-child(2) strong"));
        assertEquals("Alice First", nameElements.get(0).getText(), "First should be Alice");
        assertEquals("Mike Middle", nameElements.get(1).getText(), "Second should be Mike");
        assertEquals("Zoe Last", nameElements.get(2).getText(), "Third should be Zoe");
    }

    @Test
    void testDeleteBooking() {
        // Create a test booking
        Booking booking = new Booking("Test User", "test@example.com", "555-1234", 
                                     LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);
        bookingRepository.save(booking);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Verify booking exists
        List<WebElement> rowsBefore = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(1, rowsBefore.size(), "Should have 1 booking before deletion");
        
        // Click delete button
        WebElement deleteButton = driver.findElement(By.cssSelector(".btn-danger.btn-small"));
        deleteButton.click();
        
        // Handle confirmation dialog
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        
        // Wait for alert confirmation
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        
        // Wait for table to update (booking should be removed)
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#bookingsBody tr"), 0));
        
        // Verify booking is deleted
        WebElement emptyState = driver.findElement(By.id("emptyState"));
        assertTrue(emptyState.isDisplayed(), "Empty state should be displayed after deleting all bookings");
    }

    @Test
    void testLogout() {
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Verify dashboard is visible
        assertTrue(driver.findElement(By.id("dashboard")).isDisplayed(), "Dashboard should be visible");
        
        // Click logout button
        driver.findElement(By.cssSelector(".btn-danger:not(.btn-small)")).click();
        
        // Wait for login form to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginContainer")));
        
        // Verify login form is visible and dashboard is hidden
        assertTrue(driver.findElement(By.id("loginContainer")).isDisplayed(), "Login form should be visible after logout");
        assertFalse(driver.findElement(By.id("dashboard")).isDisplayed(), "Dashboard should be hidden after logout");
    }

    @Test
    void testRefreshButton() {
        // Create initial booking
        createTestBooking("Initial Booking", "initial@example.com", LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);
        
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingsTable")));
        
        // Verify 1 booking
        List<WebElement> rowsBefore = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(1, rowsBefore.size(), "Should have 1 booking initially");
        
        // Add another booking directly to database
        createTestBooking("New Booking", "new@example.com", LocalDate.now().plusDays(2), LocalTime.of(11, 0), 3);
        
        // Click refresh button
        driver.findElement(By.cssSelector(".btn-success")).click();
        
        // Wait for table to update
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#bookingsBody tr"), 2));
        
        // Verify 2 bookings are now displayed
        List<WebElement> rowsAfter = driver.findElements(By.cssSelector("#bookingsBody tr"));
        assertEquals(2, rowsAfter.size(), "Should have 2 bookings after refresh");
    }

    @Test
    void testEmptyStateWhenNoBookings() {
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Wait for empty state to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("emptyState")));
        
        // Verify empty state is displayed
        WebElement emptyState = driver.findElement(By.id("emptyState"));
        assertTrue(emptyState.isDisplayed(), "Empty state should be displayed when no bookings exist");
        assertTrue(emptyState.getText().contains("No bookings found"), "Empty state should have appropriate message");
        
        // Verify table is hidden
        WebElement table = driver.findElement(By.id("bookingsTable"));
        assertFalse(table.isDisplayed(), "Table should be hidden when no bookings exist");
    }

    @Test
    void testSessionPersistence() {
        loginAsAdmin();
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
        
        // Refresh the page
        driver.navigate().refresh();
        
        // Verify user is still logged in (dashboard should still be visible)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
        assertTrue(driver.findElement(By.id("dashboard")).isDisplayed(), 
                  "Dashboard should still be visible after page refresh (session persistence)");
    }
}
