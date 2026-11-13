package com.vmataj.travel;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "browser.tests", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:travel-browser;DB_CLOSE_DELAY=-1")
class BrowserTest {
    @LocalServerPort int port;
    private WebDriver browser;
    private WebDriverWait wait;

    @BeforeEach
    void openBrowser() {
        var options = new ChromeOptions().addArguments("--headless=new", "--window-size=1280,900");
        String version = System.getProperty("browser.version");
        if (version != null && !version.isBlank()) options.setBrowserVersion(version);
        browser = new ChromeDriver(options);
        wait = new WebDriverWait(browser, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeBrowser() {
        if (browser != null) browser.quit();
    }

    @Test
    void filtersDeparturesByStatus() {
        browser.get(home());
        new Select(browser.findElement(By.name("status"))).selectByValue("CANCELLED");
        click(By.cssSelector(".controls form button"));
        var rows = browser.findElements(By.cssSelector("tbody tr"));
        assertEquals(1, rows.size());
        assertTrue(rows.getFirst().getText().contains("TD206"));
    }

    private String home() {
        return "http://127.0.0.1:" + port + "/";
    }

    private void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }
}
