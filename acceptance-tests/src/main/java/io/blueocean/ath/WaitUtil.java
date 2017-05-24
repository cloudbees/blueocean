package io.blueocean.ath;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
public class WaitUtil {


    private WebDriver driver;

    @Inject
    public WaitUtil(WebDriver driver) {
        this.driver = driver;
    }

    public <T> T until(Function<WebDriver, T> function, long timeoutInMS) {
        return new FluentWait<WebDriver>(driver)
            .pollingEvery(100, TimeUnit.MILLISECONDS)
            .withTimeout(timeoutInMS, TimeUnit.MILLISECONDS)
            .ignoring(NoSuchElementException.class)
            .until((WebDriver driver) -> function.apply(driver));
    }

    public <T> T until(Function<WebDriver, T> function) {
        return new FluentWait<WebDriver>(driver)
            .pollingEvery(100, TimeUnit.MILLISECONDS)
            .withTimeout(10000, TimeUnit.MILLISECONDS)
            .ignoring(NoSuchElementException.class)
            .until((WebDriver driver) -> function.apply(driver));

    }

    public <T> Function<WebDriver, Integer> orVisisble(Function<WebDriver, WebElement> trueCase, Function<WebDriver, WebElement> falseCase) {
        return driver -> {
            try {
                if(trueCase.apply(driver).isDisplayed()) {
                    return 1;
                }
            } catch (NotFoundException e) {
                if(falseCase.apply(driver).isDisplayed()) {
                    return 2;
                }
            }

            throw new NotFoundException();
        };
    }

}
