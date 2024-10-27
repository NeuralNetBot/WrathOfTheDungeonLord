package game.shootergame;

/**
 * A HUDViewCommand represents something the HUD can display
 * and the logic to update the display. (Follows the
 * Command pattern.)
 */
public abstract class HUDViewCommand {
    public enum Visibility {WHEN_CLOSED, WHEN_OPEN, ALWAYS};
    public Visibility vis;

    public HUDViewCommand() {
        vis = Visibility.WHEN_OPEN;
    }

    public HUDViewCommand(Visibility desiredVisiblity) {
        vis = desiredVisiblity;
    }

    public abstract String execute(boolean consoleIsOpen);

    public Visibility nextVisiblityState() {
        if (vis == Visibility.WHEN_CLOSED) vis = Visibility.WHEN_OPEN;
        else if (vis == Visibility.WHEN_OPEN) vis = Visibility.WHEN_CLOSED;
        return vis;
    }

    public boolean isVisible(boolean consoleIsOpen) {
        if (consoleIsOpen) return (vis != Visibility.WHEN_CLOSED);
        else return (vis != Visibility.WHEN_OPEN);
    }
}
