package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.TimeUtils;
import game.shootergame.Item.RangedWeapons.CrossbowWeapon;
import game.shootergame.Item.RangedWeapons.MusketWeapon;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;


/**
 * A Simple HUD and Command Console.
 *
 * The HUD contains a Console that can be opened/closed with the
 * backtick/tilde key. When the console is open, it receives keystrokes and
 * acts as a command line interface.  When the console is closed, it ignores
 * keystrokes.
 *
 * WARNING: the HUD can interact with your game's input:
 *
 * - if your game polls the keyboard using Gdx.input.isKeyPressed() it will
 * be able to observe the keyboard state regardless of whether the HUD is open
 * or not.  In this case, you most likely want to ignore keypressed data
 * when the HUD's isOpen() method returns true.
 * - if your game relies on key event data (keyTyped events, etc),
 * you need to be aware that the HUD registers a global InputAdapter
 * (via Gdx.input.setInputAdapter()) upon creation. You must either: (1)
 * register your own input adapter prior to creating the HUD; or (2) take
 * care of multiplexing on your own.
 *
 * Two types of Command objects can be registered with the HUD:
 * HUDActionCommand instances tell the HUD how to process text entered into the console.
 * HUDViewCommand instances tell the HUD how/when to access data for visualization.
 *
 * In each case, Command objects are registered with a keyword.
 * HUDActionCommand keyword indicates the word that will invoke this command at the console
 * HUDViewCommand keyword indicates the text label associated with the data being visualized
 *
 * By default, the HUD has a few ActionCommands registered:
 * ? - ask for help at the console prompt
 * fps - toggle visibility of the fps data
 *
 * and one ViewCommand:
 * FPS - shows current frame rate
 */
public class HUD {
    final static String PROMPT = "> ";
    final static int DATA_REFRESH_INTERVAL = 100; // 100ms
    private int linesbuffered;
    private int xMargin;
    private int yMargin;
    private int rColumn;
    private boolean open;
    private BitmapFont font;
    private Deque<String> consoleLines;
    private StringBuilder currentLine;
    private Texture background;
    private HashMap<String, HUDActionCommand> knownCommands;
    private HashMap<String, HUDViewCommand> hudData;
    private long lastDataRefresh;
    private StringBuilder hudDataBuffer;


    /**
     * Make a HUD with sane defaults.
     */
    public HUD(BitmapFont fnt) {
        this(10, 13, 10, 500, fnt);
    }

    /**
     * Make a HUD
     *
     * @param linesbuffd - number of lines of data buffered
     * @param xmargin    - xmargin from left of window (in pixels)
     * @param ymargin    - ymargin from top of window (in pixels)
     * @param rcol       - the location of the right column (in pixels) where
     *                   hud data is shown when the console is open.
     * @param fnt        - the font to use for display
     */
    public HUD(int linesbuffd, int xmargin, int ymargin, int rcol, BitmapFont fnt) {
        linesbuffered = linesbuffd;
        xMargin = xmargin;
        yMargin = ymargin;
        rColumn = rcol;
        currentLine = new StringBuilder(60);
        consoleLines = new ArrayDeque<>();
        knownCommands = new HashMap<>(10);
        hudData = new HashMap<>(10);
        hudDataBuffer = new StringBuilder(20);
        font = fnt;

        // make a background for the console...bigger than needed!
        Pixmap pixmap = new Pixmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() / 2, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, .6f);
        pixmap.fill();
        background = new Texture(pixmap);
        pixmap.dispose();

        // register built-in action commands...
        registerAction("fps", new HUDActionCommand() {
            @Override
            public String execute(String[] cmd) {
                HUDViewCommand.Visibility v = hudData.get("FPS:").nextVisiblityState();
                return "fps visibility: " + v;
            }

            public String help(String[] cmd) {
                return "toggle fps visibility always <-> in console";
            }
        });
        registerAction("?", new HUDActionCommand() {
            @Override
            public String execute(String[] cmd) {

                StringBuilder sb = new StringBuilder(100);
                sb.append("Known Commands:\n");
                for(String k : knownCommands.keySet()) {
                    sb.append(k);
                    sb.append(" - ");
                    sb.append(knownCommands.get(k).help(null));
                    sb.append('\n');
                }
                return sb.toString();
            }

            public String help(String[] cmd) {
                return "list all commands";
            }
        });

        registerAction("musket", new HUDActionCommand() {
            @Override
            public String execute(String[] cmd) {
                World.getPlayer().setRangedWeapon(new MusketWeapon());
                return "musket equipped";
            }

            public String help(String[] cmd) {
                return "equip musket";
            }
        });

        registerAction("crossbow", new HUDActionCommand() {
            @Override
            public String execute(String[] cmd) {
                World.getPlayer().setRangedWeapon(new CrossbowWeapon());
                return "crossbow equipped";
            }

            public String help(String[] cmd) {
                return "equip crossbow";
            }
        });

        registerAction("god", new HUDActionCommand() {
            @Override
            public String execute(String[] cmd) {
                World.getPlayer().setInvincibility();
                if (World.getPlayer().getInvincibility()) return "god mode activated";
                else { return "god mode deactivated"; }
            }

            public String help(String[] cmd) {
                return "equip crossbow";
            }
        });

        // register build-in view commands...
        registerView("FPS:", new HUDViewCommand(HUDViewCommand.Visibility.WHEN_CLOSED) {
            @Override
            public String execute(boolean consoleIsOpen) {
                return Integer.toString(Gdx.graphics.getFramesPerSecond());
            }
        });

        // create a new InputAdapter to allow the HUD to get commands etc...
        InputAdapter inputAdapter = new InputAdapter() {
            public boolean keyTyped(char character) {
                String cmd, result;
                if (character == '`') {
                    toggleConsole();
                    return true;
                }
                if (open) {
                    if (character == '\n') {
                        // when the line is ended, see if a valid command was issued...
                        cmd = currentLine.toString();
                        String[] words = cmd.split("[ \t]+");
                        HUDActionCommand callback = knownCommands.get(words[0]);
                        result = (callback == null) ? "?" : callback.execute(words);
                        consoleLines.add(PROMPT + cmd);
                        Collections.addAll(consoleLines, result.split("\n"));
                        while (consoleLines.size() >= linesbuffered) {
                            consoleLines.removeFirst();
                        }
                        currentLine.setLength(0);
                    } else if (character == '\b') {
                        currentLine.setLength(currentLine.length() - 1);
                    } else {
                        currentLine.append(character);
                    }
                    return true;
                }
                return false;
            }
        };

        lastDataRefresh = TimeUtils.millis() - DATA_REFRESH_INTERVAL;
        if (Gdx.input.getInputProcessor() != null) {
            Gdx.app.log("HUD", "InputProcessor detected...installing multiplexer (see HUD docs if you have problems)");
            // if there's already an input processor,
            // create a multiplexer that runs input first through the hud's input adapter
            // then through the old one...
            InputProcessor old = Gdx.input.getInputProcessor();
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(inputAdapter);
            multiplexer.addProcessor(old);
            Gdx.input.setInputProcessor(multiplexer);
        } else {
            Gdx.app.log("HUD", "No InputProcessor detected...initializing; (see HUD docs if you call setInputProcessor() later)");
            Gdx.input.setInputProcessor(inputAdapter);
        }
    }

    /**
     * @return true iff the HUD console is open (and accepting input)
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * This method should be called to render the HUD the specified batch
     * should have previously been opened (and not yet closed). Generally,
     * you'll want to call this method after all other items on the screen
     * have been drawn.
     *
     * @param batch an open Batch in which to do the rendering
     */
    public void draw(Batch batch) {
        String console;
        int xlocation;

        if (open) {
            xlocation = rColumn;
        } else {
            xlocation = xMargin;
        }

        // draw based on the open/closed status
        if (open) {
            batch.draw(background, 0, Gdx.graphics.getHeight() - ((font.getLineHeight()) * linesbuffered) - yMargin);
            console = String.join("\n", consoleLines);
            if (console.equals("")) {
                console = PROMPT + currentLine.toString();

            } else {
                console = console + '\n' + PROMPT + currentLine.toString();
            }
            font.draw(batch, console, xMargin, Gdx.graphics.getHeight() - yMargin);
        }
        // refresh HUD Data every second....
        if (TimeUtils.millis() - lastDataRefresh > DATA_REFRESH_INTERVAL) {
            lastDataRefresh = TimeUtils.millis();
            hudDataBuffer.setLength(0); // reset, since we're about to rewrite the data

            for(String k : hudData.keySet()) {
                HUDViewCommand vc = hudData.get(k);
                if (vc.isVisible(open)) {
                    hudDataBuffer.append(k);
                    hudDataBuffer.append(' ');
                    hudDataBuffer.append(vc.execute(open));
                    hudDataBuffer.append('\n');
                }
            }
        }
        // draw HUD Data every frame...
        font.draw(batch, hudDataBuffer.toString(), xlocation, Gdx.graphics.getHeight() - yMargin);
    }

    /**
     * Register a new command executed from the HUD Console
     *
     * @param cmd         - name (1 word) of command
     * @param cmdcallback - The HUDActionCommand to execute
     * @return true if registration succeeds
     */
    public boolean registerAction(String cmd, HUDActionCommand cmdcallback) {
        if (knownCommands.containsKey(cmd)) {
            return false;
        }
        knownCommands.put(cmd, cmdcallback);
        return true;
    }

    /**
     * Register a new visual item in the HUD (something you can see)
     *
     * @param key          - text label/name
     * @param viewcallback - The HUDViewCommand executed to obtain the visual
     * @return true if registration succeeds
     */
    public boolean registerView(String key, HUDViewCommand viewcallback) {
        if (hudData.containsKey(key)) {
            return false;
        }
        hudData.put(key, viewcallback);
        return true;
    }

    /**
     * Toggle the console open/closed and force HUDData to refresh
     *
     * @return true iff console is open
     */
    public boolean toggleConsole() {
        open = !open;
        hudDataBuffer.setLength(0);
        lastDataRefresh -= DATA_REFRESH_INTERVAL;
        return open;
    }

    /**
     * For all registered HUDViewCommands, set the visibility as specified
     */
    public void setDataVisibility(HUDViewCommand.Visibility v) {
        for(HUDViewCommand c : hudData.values()) {
            c.vis = v;
        }
    }

    public HashMap<String, HUDViewCommand> getHudData() {
        return hudData;
    }
}
