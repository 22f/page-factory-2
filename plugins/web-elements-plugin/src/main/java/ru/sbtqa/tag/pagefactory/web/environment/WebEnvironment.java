package ru.sbtqa.tag.pagefactory.web.environment;

import org.aeonbits.owner.ConfigFactory;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.web.properties.WebConfiguration;
import ru.sbtqa.tag.pagefactory.web.support.BrowserName;

public class WebEnvironment extends Environment {

    private static final WebConfiguration PROPERTIES = ConfigFactory.create(WebConfiguration.class);

    public static BrowserName getBrowserName() {
        return adaptBrowserName(PROPERTIES.getBrowserName());
    }

    private static BrowserName adaptBrowserName(String name) {
        BrowserName browserName = BrowserName.valueOf(name.toUpperCase());
        return isIE(browserName) ? BrowserName.IE : browserName;
    }

    private static boolean isIE(BrowserName browserName) {
        return browserName.equals(BrowserName.IE)
                || browserName.equals(BrowserName.INTERNET_EXPLORER)
                || browserName.equals(BrowserName.IEXPLORE);
    }
}
