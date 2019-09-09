package testUI;

import com.codeborne.selenide.WebDriverRunner;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileBrowserType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import testUI.elements.TestUI;
import testUI.elements.Element;
import testUI.elements.UIElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static testUI.ADBUtils.*;
import static testUI.Configuration.*;
import static testUI.UIUtils.*;
import static testUI.iOSCommands.*;

public class TestUIDriver {
    private static ThreadLocal<List<AppiumDriver>> driver = new ThreadLocal<>();
    private static ThreadLocal<List<AndroidDriver>> AndroidTestUIDriver = new ThreadLocal<>();
    private static ThreadLocal<List<IOSDriver>> IOSTestUIDriver = new ThreadLocal<>();
    private static Map<String, AppiumDriver> driverNames = new HashMap<>();

    public synchronized static UIElement setDriver(AndroidDriver driver) {
        List<AppiumDriver> appiumDrivers = new ArrayList<>(getDrivers());
        appiumDrivers.add(driver);
        TestUIDriver.driver.set(appiumDrivers);
        List<AndroidDriver> androidDrivers = new ArrayList<>(getAndroidDrivers());
        androidDrivers.add(driver);
        TestUIDriver.AndroidTestUIDriver.set(androidDrivers);
        return TestUI.E("");
    }

    public synchronized static UIElement setDriver(IOSDriver driver) {
        List<AppiumDriver> appiumDrivers = new ArrayList<>(getDrivers());
        appiumDrivers.add(driver);
        TestUIDriver.driver.set(appiumDrivers);
        List<IOSDriver> iOSDrivers = new ArrayList<>(getIOSDrivers());
        iOSDrivers.add(driver);
        TestUIDriver.IOSTestUIDriver.set(iOSDrivers);
        return TestUI.E("");
    }

    public static UIElement setDriver(WebDriver driver) {
        WebDriverRunner.setWebDriver(driver);
        return TestUI.E("");
    }

    public  static void setDriver(AppiumDriver driver, String deviceName) {
        driverNames.put(deviceName, driver);
    }

    public static Map<String, AppiumDriver> getDriverNames() {
        return driverNames;
    }

    public synchronized static void setDriver(IOSDriver driver, int driverNumber) {
        List<IOSDriver> iOSDrivers = new ArrayList<>(getIOSDrivers());
        iOSDrivers.set(driverNumber, driver);
        TestUIDriver.IOSTestUIDriver.set(iOSDrivers);
        List<AppiumDriver> appiumDrivers = new ArrayList<>(getDrivers());
        appiumDrivers.set(driverNumber, driver);
        TestUIDriver.driver.set(appiumDrivers);
    }

    public synchronized static void setDriver(AndroidDriver driver, int driverNumber) {
        List<AndroidDriver> androidDrivers = new ArrayList<>(getAndroidDrivers());
        androidDrivers.set(driverNumber, driver);
        TestUIDriver.AndroidTestUIDriver.set(androidDrivers);
        List<AppiumDriver> appiumDrivers = new ArrayList<>(getDrivers());
        appiumDrivers.set(driverNumber, driver);
        TestUIDriver.driver.set(appiumDrivers);
    }

    public static AndroidDriver getAndroidTestUIDriver() {
        if (getAndroidDrivers().isEmpty() || getAndroidDrivers().size() < Configuration.driver) {
            throw new NullPointerException("There is no driver bound to the automation, start driver before running test cases! \n" +
                    "Configuration.driver is set to " + Configuration.driver + " and the number of drivers is only " + getAndroidDrivers().size());
        }
        return getAndroidDrivers().get(Configuration.driver - 1);
    }

    public static IOSDriver getIOSTestUIDriver() {
        if (getIOSDrivers().isEmpty() || getIOSDrivers().size() < Configuration.driver) {
            throw new NullPointerException("There is no driver bound to the automation, start driver before running test cases! \n" +
                    "Configuration.driver is set to " + Configuration.driver + " and the number of drivers is only " + getIOSDrivers().size());
        }
        return getIOSDrivers().get(Configuration.driver - 1);
    }

    public static AppiumDriver getDriver() {
        if (getDrivers().isEmpty() || getDrivers().size() < Configuration.driver) {
            throw new NullPointerException("There is no driver bound to the automation, start driver before running test cases! \n" +
                    "Configuration.driver is set to " + Configuration.driver + " and the number of drivers is only " + getDrivers().size());
        }
        return getDrivers().get(Configuration.driver - 1);
    }

    public static List<AppiumDriver> getDrivers() {
        if (driver.get() == null)
            return new ArrayList<>();
        return driver.get();
    }

    public static List<AndroidDriver> getAndroidDrivers() {
        if (AndroidTestUIDriver.get() == null)
            return new ArrayList<>();
        return AndroidTestUIDriver.get();
    }

    public static List<IOSDriver> getIOSDrivers() {
        if (IOSTestUIDriver.get() == null)
            return new ArrayList<>();
        return IOSTestUIDriver.get();
    }

    public static void removeDriver(int driver) {
        List<AppiumDriver> appiumDrivers = new ArrayList<>(getDrivers());
        appiumDrivers.remove(driver);
        TestUIDriver.driver.set(appiumDrivers);
    }

    private static DesiredCapabilities desiredCapabilities;

    public static WebDriver getSelenideDriver() {
        return WebDriverRunner.getWebDriver();
    }

    public static byte[] takeScreenshot() {
        if (Configuration.deviceTests) {
            if (getDrivers().size() != 0) {
                Configuration.driver = Configuration.driver > getDrivers().size() ? getDrivers().size() : Configuration.driver;
                return ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
            } else {
                return new byte[1];
            }
        }
        return ((TakesScreenshot) getSelenideDriver()).getScreenshotAs(OutputType.BYTES);
    }

    public static List<byte[]> takeScreenshotAllDevicesList() {
        List<byte[]> screenshots = new ArrayList<>();
        boolean test = Configuration.deviceTests;
        Configuration.deviceTests = true;
        for (int index = 0; index < getDrivers().size(); index++) {
            screenshots.add(takeScreenshot(index));
        }
        Configuration.deviceTests = false;
        if (WebDriverRunner.driver().hasWebDriverStarted()) {
            try {
                screenshots.add(takeScreenshot());
            } catch (Exception e) {
                System.err.println("Could not take a screenshot in the laptop browser...");
            }
        }
        Configuration.deviceTests = test;
        return screenshots;
    }

    public static Map<String, byte[]> takeScreenshotAllDevicesMap(boolean includeAllure) {
        Map<String, byte[]> screenshots = new HashMap<>();
        boolean test = Configuration.deviceTests;
        Configuration.deviceTests = true;
        for (int index = 0; index < getDrivers().size(); index++) {
            screenshots.put(getDevicesNames().get(index), takeScreenshot(index));
        }
        Configuration.deviceTests = false;
        if (WebDriverRunner.driver().hasWebDriverStarted()) {
            try {
                screenshots.put("browser", takeScreenshot());
            } catch (Exception e) {
                System.err.println("Could not take a screenshot in the laptop browser...");
            }
        }
        if (includeAllure) {
            screenshots.forEach((k, v) -> Allure.getLifecycle().addAttachment(k, "image/png", "png", v));
        }
        Configuration.deviceTests = test;
        return screenshots;
    }

    public static byte[] takeScreenshot(int index) {
        if (Configuration.deviceTests) {
            return ((TakesScreenshot) getDrivers().get(index)).getScreenshotAs(OutputType.BYTES);
        }
        try {
            return ((TakesScreenshot) getSelenideDriver()).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.err.println("Could not take a screenshot in the laptop browser...");
        }
        return new byte[1];
    }

    public static byte[] takeScreenshot(AppiumDriver driver) {
        if (Configuration.deviceTests) {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        }
        try {
            return ((TakesScreenshot) getSelenideDriver()).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.err.println("Could not take a screenshot in the laptop browser...");
        }
        return new byte[1];
    }

    public static void setDesiredCapabilities(DesiredCapabilities desiredCapabilities) {
        TestUIDriver.desiredCapabilities = desiredCapabilities;
    }

    public static DesiredCapabilities getDesiredCapabilities() {
        return TestUIDriver.desiredCapabilities;
    }

    public static DesiredCapabilities setAppAndroidCapabilities(TestUIConfiguration configuration) {
        if (configuration.getEmulatorName().isEmpty() && !getDeviceStatus(getDevice()).equals("device")) {
            System.err.println("The device status is " + getDeviceStatus(getDevice()) +
                    " to use usb, you must allow usb debugging for this device: " + getDevice());
            throw new Error();
        }
        getDevModel(configuration);
        String deviceVersion = Configuration.androidVersion.isEmpty() && configuration.getEmulatorName().isEmpty() ? getDeviceVersion(getDevice()) :
                Configuration.androidVersion;
        // Created object of DesiredCapabilities class.
        DesiredCapabilities cap = new DesiredCapabilities();
        if (getDesiredCapabilities() == null) {
            if (configuration.getEmulatorName().isEmpty()) {
                cap.setCapability(MobileCapabilityType.DEVICE_NAME, getDevice());
                cap.setCapability(MobileCapabilityType.PLATFORM_VERSION, deviceVersion);
            } else {
                cap.setCapability(MobileCapabilityType.DEVICE_NAME, configuration.getEmulatorName());
                cap.setCapability(AndroidMobileCapabilityType.AVD, configuration.getEmulatorName());
            }
            cap.setCapability(AndroidMobileCapabilityType.APP_WAIT_DURATION, Configuration.launchAppTimeout);
            if (Configuration.AutomationName.isEmpty()) {
                cap.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
            } else {
                cap.setCapability(MobileCapabilityType.AUTOMATION_NAME, Configuration.AutomationName);
            }
            if (!Configuration.chromeDriverPath.isEmpty()) {
                String chromePath = Configuration.chromeDriverPath.charAt(0) == '/' ? Configuration.chromeDriverPath :
                        System.getProperty("user.dir") + "/" + Configuration.chromeDriverPath;
                cap.setCapability(AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE, chromePath);
            }
            cap.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID);
            if (!Configuration.appActivity.isEmpty() && !Configuration.appPackage.isEmpty()) {
                cap.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, Configuration.appActivity);
                cap.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, Configuration.appPackage);
            }
            if (!Configuration.androidAppPath.isEmpty()){
                String appPath = Configuration.androidAppPath.charAt(0) == '/' ? Configuration.androidAppPath :
                        System.getProperty("user.dir") + "/" + Configuration.androidAppPath;
                cap.setCapability("androidInstallPath", appPath);
                cap.setCapability("app", appPath);
            }
            int systemPort = Integer.parseInt(getUsePort().get(getUsePort().size() - 1)) + 10;
            cap.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT, systemPort);
        } else {
            cap = getDesiredCapabilities();
        }
        // ADD CUSTOM CAPABILITIES
        if (!Configuration.addMobileDesiredCapabilities.asMap().isEmpty()) {
            for (String key : addMobileDesiredCapabilities.asMap().keySet()) {
                cap.setCapability(key, addMobileDesiredCapabilities.asMap().get(key));
            }
            addMobileDesiredCapabilities = new DesiredCapabilities();
        }
        Configuration.desiredCapabilities = cap;
        return cap;
    }

    public static DesiredCapabilities setAndroidBrowserCapabilities(TestUIConfiguration configuration) {
        if (configuration.getEmulatorName().isEmpty() && getDevices().size() == 0) {
            throw new Error("There is no device available to run the automation!");
        }
        if (configuration.getEmulatorName().isEmpty() && !getDeviceStatus(getDevice()).equals("device")) {
            System.err.println("The device status is " + getDeviceStatus(getDevice()) +
                    " to use usb, you must allow usb debugging for this device: " + getDevice());
            throw new Error();
        }
        getDevModel(configuration);
        String deviceVersion = Configuration.androidVersion.isEmpty() && configuration.getEmulatorName().isEmpty() ? getDeviceVersion(getDevice()) :
                Configuration.androidVersion;
        String browserFirstLetter = Configuration.browser.subSequence(0, 1).toString().toUpperCase();
        String browser = browserFirstLetter + Configuration.browser.substring(1);
        // Created object of DesiredCapabilities class.
        DesiredCapabilities cap = new DesiredCapabilities();
        if (!configuration.getChromeDriverPath().isEmpty()) {
            String chromePath = configuration.getChromeDriverPath().charAt(0) == '/' ? configuration.getChromeDriverPath() :
                    System.getProperty("user.dir") + "/" + configuration.getChromeDriverPath();
            cap.setCapability(AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE, chromePath);
        }
        if (getDesiredCapabilities() == null) {
            if (configuration.getEmulatorName().isEmpty()) {
                cap.setCapability(MobileCapabilityType.DEVICE_NAME, getDevice());
                cap.setCapability(MobileCapabilityType.PLATFORM_VERSION, deviceVersion);
            } else {
                cap.setCapability(MobileCapabilityType.DEVICE_NAME, configuration.getEmulatorName());
                cap.setCapability(AndroidMobileCapabilityType.AVD, configuration.getEmulatorName());
            }
            if (Configuration.AutomationName.isEmpty()) {
                cap.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
            } else {
                cap.setCapability(MobileCapabilityType.AUTOMATION_NAME, Configuration.AutomationName);
            }
            int systemPort = Integer.parseInt(getUsePort().get(getUsePort().size() - 1)) + 10;
            int chromeDriverPort = Integer.parseInt(getUsePort().get(getUsePort().size() - 1)) + 15;
            cap.setCapability("chromeDriverPort", chromeDriverPort);
            cap.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT, systemPort);
            cap.setCapability(MobileCapabilityType.NO_RESET, true);
            cap.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID);
            cap.setCapability(MobileCapabilityType.BROWSER_NAME, browser);
            cap.setCapability(AndroidMobileCapabilityType.NATIVE_WEB_SCREENSHOT, true);
        } else {
            cap = getDesiredCapabilities();
        }
        // ADD CUSTOM CAPABILITIES
        if (!Configuration.addMobileDesiredCapabilities.asMap().isEmpty()) {
            for (String key : addMobileDesiredCapabilities.asMap().keySet()) {
                cap.setCapability(key, addMobileDesiredCapabilities.asMap().get(key));
            }
            addMobileDesiredCapabilities = new DesiredCapabilities();
        }
        Configuration.desiredCapabilities = cap;
        return cap;
    }

    public static DesiredCapabilities setIOSCapabilities(boolean browser) {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        if (getDesiredCapabilities() == null) {
            // CHECK IF DEVICE SPECIFIED
            if (Configuration.iOSDeviceName.isEmpty()) {
                if (Configuration.UDID.isEmpty()) {
                    Map<String, String> sampleIOSDevice = getSampleDevice();
                    Configuration.iOSDeviceName = sampleIOSDevice.get("name");
                    Configuration.iOSVersion = sampleIOSDevice.get("version");
                    Configuration.UDID = sampleIOSDevice.get("udid");
                } else {
                    Configuration.iOSDeviceName = getIOSName(Configuration.UDID);
                    Configuration.iOSVersion = getIOSVersion(Configuration.UDID);
                }
                capabilities.setCapability("udid", Configuration.UDID);
            } else {
                if (Configuration.UDID.isEmpty()) {
                    capabilities.setCapability("udid", "auto");
                } else {
                    capabilities.setCapability("udid", Configuration.UDID);
                }
            }
            if (!getIOSDevices().toString().contains(iOSDeviceName)) {
                setiOSDevice(iOSDeviceName);
            }
            // BROWSER OR APP
            if (browser) {
                capabilities.setCapability(MobileCapabilityType.AUTO_WEBVIEW, true);
                capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, MobileBrowserType.SAFARI);
            } else if (!Configuration.iOSAppPath.isEmpty()) {
                String appPath = Configuration.iOSAppPath.charAt(0) == '/' ? Configuration.iOSAppPath :
                        System.getProperty("user.dir") + "/" + Configuration.iOSAppPath;
                capabilities.setCapability(MobileCapabilityType.APP, appPath);
            }
            // IN CASE OF REAL DEVICE
            if (!Configuration.xcodeOrgId.isEmpty()) {
                capabilities.setCapability(IOSMobileCapabilityType.XCODE_ORG_ID, Configuration.xcodeOrgId);
                capabilities.setCapability(IOSMobileCapabilityType.XCODE_SIGNING_ID, Configuration.xcodeSigningId);
            }
            if (!Configuration.updatedWDABundleId.isEmpty()) {
                capabilities.setCapability("updatedWDABundleId", Configuration.updatedWDABundleId);
            }
            if (!Configuration.bundleId.isEmpty()) {
                capabilities.setCapability("bundleId", Configuration.bundleId);
            }
            // DEFAULT THINGS
            capabilities.setCapability(MobileCapabilityType.NO_RESET, false);
            capabilities.setCapability(IOSMobileCapabilityType.USE_NEW_WDA, Configuration.useNewWDA);
            capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, Configuration.iOSDeviceName);
            capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, Configuration.iOSVersion);
            capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.IOS);
            capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
            capabilities.setCapability(IOSMobileCapabilityType.START_IWDP, true);
            capabilities.setCapability(IOSMobileCapabilityType.LAUNCH_TIMEOUT, Configuration.launchAppTimeout);
            capabilities.setCapability(IOSMobileCapabilityType.COMMAND_TIMEOUTS, 30000);
            // ADD CUSTOM CAPABILITIES
            if (!Configuration.addMobileDesiredCapabilities.asMap().isEmpty()) {
                for (String key : addMobileDesiredCapabilities.asMap().keySet()) {
                    capabilities.setCapability(key, addMobileDesiredCapabilities.asMap().get(key));
                }
                addMobileDesiredCapabilities = new DesiredCapabilities();
            }
        } else {
            capabilities = getDesiredCapabilities();
        }
        Configuration.desiredCapabilities = capabilities;
        putAllureParameter("Device Model", Configuration.iOSDeviceName);
        putAllureParameter("Version", Configuration.iOSVersion);
        return capabilities;
    }


    private static void getDevModel(TestUIConfiguration configuration) {
        String devModel;
        if (configuration.getEmulatorName().isEmpty()) {
            devModel = (getDeviceName().equals(getDevice()) ? getDeviceModel(getDevice()) : getDeviceName());
        } else {
            if (Configuration.driver == 1) {
                Configuration.firstEmulatorName.set(configuration.getEmulatorName());
            }
            devModel = configuration.getEmulatorName();
        }
        if (Configuration.driver == 1 && Configuration.firstEmulatorName.get() != null) {
            putAllureParameter("Device Model", Configuration.firstEmulatorName.get());
        } else {
            putAllureParameter("Device Model", devModel);
        }
    }
}