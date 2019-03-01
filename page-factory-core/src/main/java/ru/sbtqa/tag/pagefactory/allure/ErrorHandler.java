package ru.sbtqa.tag.pagefactory.allure;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.pagefactory.properties.Configuration;
import ru.sbtqa.tag.pagefactory.utils.ScreenshotUtils;

public class ErrorHandler {

    private static final Configuration PROPERTIES = Configuration.create();
    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {
    }

    public static void attachScreenshot() {
        try {
            ScreenshotUtils screenshot = ScreenshotUtils.valueOf(PROPERTIES.getScreenshotStrategy().toUpperCase());
            ParamsHelper.addAttachmentToRender(screenshot.take(), "Screenshot", Type.PNG);
        } catch (Exception e) {
            LOG.error("Can't attach screenshot to allure reports", e);
        }
    }

    public static void attachError(Throwable e) {
//        Throwable cause = e.getCause() != null ? e.getCause() : e;
//
//        String title = cause.getMessage() == null || cause.getMessage().isEmpty()
//                ? e.getMessage() : cause.getMessage();

        String errorHTML = "<div style='background-color: #ffc2c2; height: auto; display: table'>" +
                "<pre style='color:#880b0b'>" + ExceptionUtils.getStackTrace(e) + "</pre></div>";
        ParamsHelper.addAttachmentToRender(errorHTML.getBytes(), e.getMessage(), Type.HTML);
    }
}

//                    ErrorHandler.attachError(message, cause);
//                    ErrorHandler.attachScreenshot();