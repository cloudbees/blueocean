package io.blueocean.ath;

import com.google.common.base.Function;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;


@Singleton
public class WaitUtil {
    public static int DEFAULT_TIMEOUT = Integer.getInteger("webDriverDefaultTimeout", 20000);
    public static final int RETRY_COUNT = 2;

    private Logger logger = Logger.getLogger(WaitUtil.class);

    private WebDriver driver;

    @Inject
    public WaitUtil(WebDriver driver) {
        this.driver = driver;
    }

    public <T> T until(Function<WebDriver, T> function, long timeoutInMS, String errorMessage) {
        try {
            return new FluentWait<WebDriver>(driver)
                .pollingEvery(100, TimeUnit.MILLISECONDS)
                .withTimeout(timeoutInMS, TimeUnit.MILLISECONDS)
                .ignoring(NoSuchElementException.class)
                .until((WebDriver driver) -> function.apply(driver));
        } catch(Throwable t) {
            throw new AcceptanceTestException(errorMessage, t);
        }
    }

    public <T> T until(Function<WebDriver, T> function, long timeoutInMS) {
        return until(function, timeoutInMS, "Error while waiting for something");
    }
    public <T> T until(Function<WebDriver, T> function) {
        return until(function, DEFAULT_TIMEOUT);
    }

    public <T> T until(Function<WebDriver, T> function, String errorMessage) {
        return until(function, DEFAULT_TIMEOUT);
    }

    public WebElement until(WebElement element) {
        return until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement until(By by) {
        return until(ExpectedConditions.visibilityOfElementLocated(by), "Could not find " + by.toString());
    }


    public WebElement until(WebElement element, long timeoutInMS) {
        return until(ExpectedConditions.visibilityOf(element), timeoutInMS);
    }

    public WebElement until(By by, long timeoutInMS) {
        return until(ExpectedConditions.visibilityOfElementLocated(by), timeoutInMS);
    }
    public <T> Function<WebDriver, Integer> orVisible(Function<WebDriver, WebElement> trueCase, Function<WebDriver, WebElement> falseCase) {
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

    /**
     * Send a clear to the element specified by the locator.
     * Used for wiping out default entries in text fields.
     * Will retry through a number of 'element not clickable'
     * exceptions as defined by RETRY_COUNT.
     * @param by The `By` identifier we are targeting
     */
    public void clear(By by) {
        for (int i = 0; i < RETRY_COUNT + 1; i++) {
            try {
                until(ExpectedConditions.elementToBeClickable(by)).clear();
                if (i > 0) {
                    logger.info(String.format("Retry of clear successful on attempt " + i + " for %s", by.toString()));
                }
                return;
            } catch (WebDriverException ex) {
                if (ex.getMessage().contains("is not clickable at point")) {
                    logger.warn(String.format("%s not clickable: will retry clear", by.toString()));
                    logger.debug("exception: " + ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
    }


    /**
     * Click the element specified by the locator.
     * Will retry click for 'element not clickable' exceptions
     * @param by
     */
    public void click(By by) {
        for (int i = 0; i < RETRY_COUNT + 1; i++) {
            try {
                until(ExpectedConditions.elementToBeClickable(by)).click();
                if (i > 0) {
                    logger.info(String.format("Retry click successful on attempt " + i + " for %s", by.toString()));
                }
                return;
            } catch (WebDriverException ex) {
                if (ex.getMessage().contains("is not clickable at point")) {
                    logger.warn(String.format("%s not clickable: will retry click", by.toString()));
                    logger.debug("exception: " + ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * Send a key sequence to the element specified by the locator.
     * Will retry through a number of  'element not clickable'
     * exceptions as defined by RETRY_COUNT
     * @param by The `By` identifier we are targeting
     * @param keySequence a String of characters to enter
     */
    public void sendKeys(By by, String keySequence) {
        for (int i = 0; i < RETRY_COUNT + 1; i++) {
            try {
                until(ExpectedConditions.elementToBeClickable(by)).sendKeys(keySequence);
                if (i > 0) {
                    logger.info(String.format("Retry of sendKeys successful on attempt " + i + " for %s", by.toString()));
                }
                return;
            } catch (WebDriverException ex) {
                if (ex.getMessage().contains("is not clickable at point")) {
                    logger.warn(String.format("%s not clickable: will retry click", by.toString()));
                    logger.debug("exception: " + ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * Try to perform the specified function up to the specified count.
     *
     * @param desc textual description of action
     * @param tryCount number of times to try
     * @param function action to perform
     * @return result of action
     */
    public <T> T retryAction(String desc, int tryCount, Function<WebDriver, T> function) {
        for (int i = 1; i <= tryCount; i++) {
            try {
                T result = until(function);
                if (i > 1) {
                    logger.info(String.format("retry was successful for action '%s' ", desc));
                }
                return result;
            } catch (Exception ex) {
                if (i < tryCount) {
                    logger.warn(String.format("action failed for %s; will retry again", desc));
                } else {
                    throw ex;
                }
            }
        }
        return null;
    }
}
