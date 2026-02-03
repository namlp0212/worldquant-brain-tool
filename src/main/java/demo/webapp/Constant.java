package demo.webapp;

public class Constant {
    // Cookie is loaded dynamically from config.properties via ConfigLoader
    // Use getCookie() method to always get the latest value
    public static String getCookie() {
        return ConfigLoader.getCookie();
    }

    // For backward compatibility - but prefer using getCookie() method
    // Note: This will be updated when ConfigLoader.reload() is called
    public static String COOKIE = ConfigLoader.getCookie();

    // Call this after updating cookie to refresh the cached value
    public static void refreshCookie() {
        COOKIE = ConfigLoader.getCookie();
    }
}
