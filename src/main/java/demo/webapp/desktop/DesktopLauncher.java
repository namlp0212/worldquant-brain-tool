package demo.webapp.desktop;

/**
 * Launcher entry point for the desktop application.
 * This class is needed as the Main-Class in the fat JAR manifest
 * because JavaFX Application subclasses cannot be used directly as main class
 * when JavaFX is not on the module path.
 *
 * Run: mvn javafx:run
 */
public class DesktopLauncher {
    public static void main(String[] args) {
        DesktopApp.main(args);
    }
}
