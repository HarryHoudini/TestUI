package testUI;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.AndroidServerFlag;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.openqa.selenium.remote.DesiredCapabilities;
import testUI.Utils.AppiumHelps;
import testUI.Utils.AppiumTimeoutException;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Selenide.close;
import static testUI.ADBUtils.*;
import static testUI.Configuration.*;
import static testUI.NetworkCalls.getProxy;
import static testUI.NetworkCalls.stopProxy;
import static testUI.TestUIDriver.*;
import static testUI.UIUtils.*;
import static testUI.Utils.AppiumHelps.sleep;
import static testUI.iOSCommands.*;

public class TestUIServer {
    private static ThreadLocal<Boolean> serviceRunning = new ThreadLocal<>();

    protected static void startServer(String port, String Bootstrap, TestUIConfiguration configuration) {
        AppiumServiceBuilder builder;
        DesiredCapabilities cap;
        //Set Capabilities
        cap = new DesiredCapabilities();
        cap.setCapability("noReset", "false");
        //Build the Appium service
        builder = new AppiumServiceBuilder();
        builder.withIPAddress("127.0.0.1");
        builder.usingPort(Integer.parseInt(port));
        builder.withCapabilities(cap);
        builder.withArgument(GeneralServerFlag.SESSION_OVERRIDE);
        builder.withArgument(GeneralServerFlag.LOG_LEVEL, "info");
        builder.withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, Bootstrap);
        //Start the server with the builder
        TestUIServer.serviceRunning.set(false);
        boolean slowResponse = false;
        setService(AppiumDriverLocalService.buildService(builder));
        getServices().get(getServices().size() - 1).start();
        long t= System.currentTimeMillis();
        long end = t+(configuration.getTimeStartAppiumServer() * 1000);
        while(System.currentTimeMillis() < end) {
            String serviceOut = getServices().get(getServices().size() - 1).getStdOut();
            if (serviceOut != null) {
                if (serviceOut.contains("Could not start REST http")) {
                    putLog("Could not start server in port: " + port + "\n Let's try a different one");
                    TestUIServer.serviceRunning.set(false);
                    slowResponse = false;
                    break;
                } else if (serviceOut.contains("Appium REST http interface listener started")) {
                    TestUIServer.serviceRunning.set(true);
                    slowResponse = false;
                    break;
                } else {
                    slowResponse = true;
                    TestUIServer.serviceRunning.set(true);
                }
            } else {
                slowResponse = true;
                TestUIServer.serviceRunning.set(false);
            }
            sleep(100);
        }
        if (slowResponse) {
            getServices().get(getServices().size() - 1).stop();
            throw new AppiumTimeoutException("Appium server took too long to start");
        }
        if (configuration.getServerLogLevel().equals("error")) {
            getServices().get(getServices().size() - 1).clearOutPutStreams();
        }
        if (!TestUIServer.serviceRunning.get()) {
            getServices().remove(getServices().size() - 1);
        }
    }

    protected static void attachShutDownHookStopEmulator(List<AppiumDriverLocalService> serv, List<String> emulators) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopEmulators(serv, emulators)));
    }

    protected static void attachShutDownHookStopEmulator(List<AppiumDriverLocalService> serv, String device) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopEmulators(serv, device)));
    }

    private static Thread closeDriverAndServerThread;

    protected static void attachShutDownHook(List<AppiumDriverLocalService> serv, List<AppiumDriver> drivers) {
        if (closeDriverAndServerThread != null) {
            Runtime.getRuntime().removeShutdownHook(closeDriverAndServerThread);
        }
        closeDriverAndServerThread = new Thread(() -> closeDriverAndServer(serv, drivers));
        Runtime.getRuntime().addShutdownHook(closeDriverAndServerThread);
    }

    private static void stopEmulators(List<AppiumDriverLocalService> serv, List<String> emulators) {
        for (int i = 0; i < 40; i++) {
            if (serv.size() != 0 && !serv.get(0).isRunning()) {
                AppiumHelps.sleep(1000);
                for (String device : emulators) {
                    stopEmulator(device);
                }
                break;
            }
            AppiumHelps.sleep(1000);
        }
    }

    private static void stopEmulators(List<AppiumDriverLocalService> serv, String device) {
        for (int i = 0; i < 40; i++) {
            if (serv.size() == 0 || !serv.get(0).isRunning()) {
                AppiumHelps.sleep(1000);
                stopEmulator(device);
                break;
            }
            AppiumHelps.sleep(1000);
        }
    }

    private static void closeDriverAndServer(List<AppiumDriverLocalService> serv, List<AppiumDriver> drivers) {
        putLog("Stopping drivers");
        for (AppiumDriver driver : drivers) {
            try {
                driver.close();
            } catch (Exception e) {
                putLog("Couldn't close the driver, probably already closed");
            }
            try {
                driver.quit();
            } catch (Exception e) {
                putLog("Couldn't quit the driver, probably already stopped");
            }
        }
        putLog("Running Shutdown Server");
        for (AppiumDriverLocalService service: serv) {
            if (service.isRunning()) {
                service.stop();
            }
        }
    }

    private static void checkIfAppPathExists() {
        if (!Configuration.androidAppPath.isEmpty()) {
            String appPath = Configuration.androidAppPath.charAt(0) == '/' ? Configuration.androidAppPath :
                    System.getProperty("user.dir") + "/" + Configuration.androidAppPath;
            File tmpDir = new File(appPath);
            if (!tmpDir.exists()) {
                Configuration.androidAppPath = "";
                throw new Error("The file for the Android app :" + appPath + " does not exists!");
            }
        }
    }

    protected static synchronized void startServerAndDevice(TestUIConfiguration configuration) {
        checkIfAppPathExists();
        int connectedDevices = getDeviceNames().size();
        int startedEmulators = 0;
        for (String devicesNames : getDeviceNames()) {
            if (devicesNames.contains("emulator")) {
                startedEmulators++;
            }
        }
        int emulators = configuration.isUseEmulators() ? getEmulatorName().size() : 0;
        int totalDevices = emulators + connectedDevices - startedEmulators;
        int ports = configuration.getBaseAppiumPort() + getUsePort().size()*100;
        int bootstrap = configuration.getBaseAppiumBootstrapPort() + getUseBootstrapPort().size()*100;
        int realDevices = totalDevices - emulators;
        String port = String.valueOf(ports);
        String Bootstrap = String.valueOf(bootstrap);
        for (int device = getUsePort().size(); device < totalDevices + iOSDevices; device++) {
            if (configuration.getAppiumUrl().isEmpty()) {
                startServer(port, Bootstrap, configuration);
                attachShutDownHook(getServices(), getDrivers());
            }
            if (serviceRunning.get() || (!configuration.getAppiumUrl().isEmpty() && getDevices().size() >= device)) {
                setRunDevice(realDevices, connectedDevices, device, configuration);
                break;
            }
            port = String.valueOf(Integer.parseInt(port) + 100);
            Bootstrap = String.valueOf(Integer.parseInt(Bootstrap) + 100);
        }
        if (configuration.getAppiumUrl().isEmpty()) {
            setUsePort(port);
            setUseBootstrapPort(Bootstrap);
            putAllureParameter("Using Appium port", getUsePort().get(getUsePort().size() - 1));
        } else {
            putAllureParameter("Using Appium url", appiumUrl);
        }
    }

    protected static void setRunDevice(int realDevices, int connectedDevices, int device, TestUIConfiguration configuration) {
        if (!configuration.isiOSTesting()) {
            if (configuration.getAndroidDeviceName().isEmpty() && configuration.getEmulatorName().isEmpty()) {
                if (connectedDevices <= device) {
                    if (!configuration.isUseEmulators()) {
                        throw new Error("There are not enough devices connected");
                    } else if (getEmulatorName().get(device - realDevices) == null || getEmulatorName().get(device - realDevices).isEmpty()) {
                        throw new Error("There are no emulators to start the automation");
                    }
                    configuration.setEmulatorName(getEmulatorName().get(device - realDevices));
                    setEmulator(configuration.getEmulatorName());
                    attachShutDownHookStopEmulator(getServices(), getEmulators());
                } else {
                    if (!getDevices().toString().contains(getDeviceNames().get(device))) {
                        setDevice(getDeviceNames().get(device), getDeviceNames().get(device));
                    }
                }
            } else {
                if (configuration.getEmulatorName().isEmpty()) {
                    setDevice(configuration.getAndroidDeviceName(), configuration.getAndroidDeviceName());
                }
            }
        } else {
            if (iOSDeviceName.isEmpty()) {
                if (UDID.isEmpty()) {
                    Map<String, String> sampleIOSDevice = getSampleDevice();
                    iOSDeviceName = sampleIOSDevice.get("name");
                    iOSVersion = sampleIOSDevice.get("version");
                    UDID = sampleIOSDevice.get("udid");
                } else {
                    iOSDeviceName = getIOSName(UDID);
                    iOSVersion = getIOSVersion(UDID);
                }
            }
            setiOSDevice(iOSDeviceName);
        }
        driver = iOSTesting ? getDevices().size() + getIOSDevices().size() : getDevices().size();
        driver = configuration.getEmulatorName().isEmpty() ? driver : driver + 1;
    }

    public static void stop(int driver) {
            if (deviceTests) {
                removeUsePort(driver - 1);
                removeUseBootstrapPort(driver - 1);
            if (iOSTesting) {
                getDrivers().get(driver - 1).close();
                sleep(500);
            }
            getDrivers().get(driver - 1).quit();
            removeDriver(driver - 1);
            getServices().get(driver - 1).stop();
            getServices().remove(driver - 1);
            if (getDevices().size() != 0) {
                iOSDevices = driver - getDevices().size();
                stopEmulator(getDevices().get(driver - iOSDevices - 1));
                removeDevice(driver - iOSDevices - 1);
            }
            Configuration.driver = getDrivers().size();
        } else {
            try {
                getSelenideDriver().close();
                getSelenideDriver().quit();
            } catch (Exception e) {
                putLog("Browser closed already");
            }
            close();
        }
        try {
            if (getProxy() != null && getProxy().isStarted()) {
                stopProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void tryStop(int driver) {
        if (deviceTests) {
            try {
                removeUsePort(driver - 1);
                removeUseBootstrapPort(driver - 1);
            } catch (Exception e) {
                putLog("could not remove ports");
            }
            try {
                getDrivers().get(driver - 1).quit();
            } catch (Exception e) {
                System.err.println("Could not quit driver, probably already stopped");
            }
            try {
                removeDriver(driver - 1);
            } catch (Exception e){
                putLog("could not remove driver");
            }
            try {
                if (getServices().size() == driver) {
                    getServices().get(driver - 1).stop();
                    getServices().remove(driver - 1);
                }
            } catch (Exception e) {
                putLog("Could not remove services");
            }
            if (getDevices().size() != 0) {
                stopEmulator(getDevices().get(driver - 1));
                removeDevice(driver - 1);
            }
            Configuration.driver = getDrivers().size();
        } else {
            try {
                getSelenideDriver().close();
                getSelenideDriver().quit();
            } catch (Exception e) {
                putLog("Browser closed already");
            }
            close();
        }
        try {
            if (getProxy() != null && getProxy().isStarted()) {
                stopProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (deviceTests) {
            removeUsePort(driver - 1);
            removeUseBootstrapPort(driver - 1);
            if (iOSTesting) {
                getDrivers().get(driver - 1).close();
                sleep(500);
            }
            getDrivers().get(driver - 1).quit();
            removeDriver(driver - 1);
            getServices().get(driver - 1).stop();
            getServices().remove(driver - 1);
            if (getDevices().size() != 0) {
                iOSDevices = driver - getDevices().size();
                stopEmulator(getDevices().get(driver - iOSDevices - 1));
                removeDevice(driver - iOSDevices - 1);
            }
            driver = getDrivers().size();
        } else {
            try {
                getSelenideDriver().close();
                getSelenideDriver().quit();
            } catch (Exception e) {
                putLog("Browser closed already");
            }
            close();
        }
        try {
            if (getProxy() != null && getProxy().isStarted()) {
                stopProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}