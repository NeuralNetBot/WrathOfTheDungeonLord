package game.shootergame;

/**
 * A HUDActionCommand represents something the HUD can do
 * as a result of text typed into the Console/HUD. (Follows the
 * Command pattern.)
 */
public interface HUDActionCommand {
    /**
     * Perform the command
     *
     * @param cmd -  the command line used to invoke this command
     * @return - a String of user feedback
     */
    String execute(String[] cmd);

    /**
     * (Optionally) help for this command line function
     *
     * @param cmd - the command line used to invoke help
     * @return - A help string
     */
    default String help(String[] cmd) {
        return "?";
    }
}
