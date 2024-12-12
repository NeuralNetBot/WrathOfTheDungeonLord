package game.shootergame;

import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import game.shootergame.Network.RemotePlayer;

public class MainMenu {
    private enum State {
        MAIN,
        SERVER_HOST,
        CLIENT_CONNECT,
        CLIENT_WAIT;
    };
    
    State currentState;
    Texture atlas;
    Texture titleScreen;
    TextureRegion hostServerReg;
    TextureRegion joinServerReg;
    TextureRegion exitReg;
    TextureRegion startGameReg;
    TextureRegion connectReg;
    
    TextureRegion swordReg;
    TextureRegion halberdReg;
    TextureRegion maceReg;
    TextureRegion brassReg;

    TextureRegion blankReg;
    TextureRegion pointReg;

    Sprite sprite;

    float mouseX;
    float mouseY;

    boolean isDone = false;
    boolean mode = false; //false client, true server
    boolean runGame = false;

    int selectedWeaponIndex = 0;

    boolean clientConnected = false;

    BitmapFont font;
    GlyphLayout layout = new GlyphLayout();
    
    String serverIP;
    String port;

    boolean selectIPText = false;
    boolean selectPortText = false;

    ConcurrentHashMap<Integer, RemotePlayer> remotePlayers;

    private class IsHover {
        public boolean hover = false;
    }

    IsHover serverIsHover = new IsHover();
    IsHover clientIsHover = new IsHover();
    IsHover exitIsHover = new IsHover();

    public MainMenu() {
        currentState = State.MAIN;

        TextureParameter param = new TextureParameter();
        param.minFilter = TextureFilter.Linear;
        param.magFilter = TextureFilter.Linear;
        ShooterGame.getInstance().am.load("GUIAtlas.png", Texture.class, param);
        ShooterGame.getInstance().am.load("title_screen.png", Texture.class, param);
        ShooterGame.getInstance().am.finishLoading();
        atlas = ShooterGame.getInstance().am.get("GUIAtlas.png", Texture.class);
        titleScreen = ShooterGame.getInstance().am.get("title_screen.png", Texture.class);
        font = ShooterGame.getInstance().am.get(ShooterGame.RSC_MONO_FONT, BitmapFont.class);
        hostServerReg = new TextureRegion(atlas, 0, 0, 216, 108);
        joinServerReg = new TextureRegion(atlas, 0, 108, 216, 108);
        exitReg = new TextureRegion(atlas, 0, 216, 216, 108);
        startGameReg = new TextureRegion(atlas, 216, 0, 216, 108);
        connectReg = new TextureRegion(atlas, 216, 108, 324, 108);
        
        swordReg = new TextureRegion(atlas, 0, 324, 216, 108);
        halberdReg = new TextureRegion(atlas, 216, 324, 216, 108);
        maceReg = new TextureRegion(atlas, 0, 432, 216, 108);
        brassReg = new TextureRegion(atlas, 216, 432, 216, 108);
        
        blankReg = new TextureRegion(atlas, 216, 216, 216, 108);
        pointReg = new TextureRegion(atlas, 433, 395, 108, 145);

        sprite = new Sprite(atlas);
    }

    private void showPlayerList() {
        int i = 0;
        float h = (float)Gdx.graphics.getHeight();
        float w = (float)Gdx.graphics.getWidth();
        if(remotePlayers != null) {
            for (ConcurrentHashMap.Entry<Integer, RemotePlayer> player : remotePlayers.entrySet()) {
                layout.setText(font, Integer.toString(player.getKey()));
                
                font.draw(ShooterGame.getInstance().coreBatch, layout, w - layout.width, h - (i * 15.0f) - 20.0f);
                i++;
            }
        }
    }

    private void showWeaponSelect() {
        if(getAndRenderButton(swordReg, -0.7f, -0.45f, 0.5f, 0.25f, selectedWeaponIndex == 0)) { selectedWeaponIndex = 0; }
        if(getAndRenderButton(halberdReg, -0.2f, -0.45f, 0.5f, 0.25f, selectedWeaponIndex == 1)) { selectedWeaponIndex = 1; }
        if(getAndRenderButton(maceReg, -0.7f, -0.7f, 0.5f, 0.25f, selectedWeaponIndex == 2)) { selectedWeaponIndex = 2; }
        if(getAndRenderButton(brassReg, -0.2f, -0.7f, 0.5f, 0.25f, selectedWeaponIndex == 3)) { selectedWeaponIndex = 3; }
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height, IsHover isHover) {
        return getAndRenderButton(reg, x, y, width, height, false, isHover);
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height) {
        return getAndRenderButton(reg, x, y, width, height, false, null);
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height, boolean overrideSelection) {
        return getAndRenderButton(reg, x, y, width, height, overrideSelection, null);
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height, boolean overrideSelection, IsHover isHover) {
        sprite.setRegion(reg);
        sprite.setSize(width, height);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(x, y);

        Rectangle rect = sprite.getBoundingRectangle();
        boolean pressed = false;
        if(isHover != null) {
            isHover.hover = false;
        }
        if(rect.contains(mouseX, mouseY)) {
            sprite.setColor(0.8f, 0.8f, 0.8f, 0.8f);
            if(isHover != null) {
                isHover.hover = true;
            }
            pressed =  Gdx.input.isButtonPressed(Buttons.LEFT);
        } else {
            sprite.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if(overrideSelection) {
            sprite.setColor(0.7f, 0.7f, 0.7f, 0.7f);
        }

        sprite.draw(ShooterGame.getInstance().coreBatch);

        return pressed;
    }

    public boolean isDone() {
        return isDone;
    }

    public boolean getSelectedMode() {
        return mode;
    }

    public boolean shouldRunGame() {
        return runGame;
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public String getIP() {
        return serverIP;
    }

    public void setPort(int port) {
        this.port = Integer.toString(port);
    }

    public void setIP(String ip) {
        serverIP = ip;
    }

    public int getSelectedWeapon() {
        return selectedWeaponIndex;
    }

    public void setClientConnected(boolean connected) {
        this.clientConnected = connected;
    }

    public void setRemotePlayers(ConcurrentHashMap<Integer, RemotePlayer> remotePlayers) {
        this.remotePlayers = remotePlayers;
    }

    private void appendToTextInputs(String append) {
        if(selectIPText) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(serverIP);
            stringBuilder.append(append);
            serverIP = stringBuilder.toString();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(port);
            stringBuilder.append(append);
            port = stringBuilder.toString();
        }
    }
    
    public void update() {
        float aspect = (float)Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth();
        mouseX = (float)Gdx.input.getX() / (float)Gdx.graphics.getWidth() * (2f / aspect) - (1f / aspect);
        mouseY = -((float)Gdx.input.getY() / (float)Gdx.graphics.getHeight() * 2f - 1f);

        switch (currentState) {
        case MAIN:

            ShooterGame.getInstance().coreBatch.draw(titleScreen, -400.f, -300.f, 200, 100);

            if(getAndRenderButton(hostServerReg, 0, 0.27f, 0.5f, 0.25f, serverIsHover)) { currentState = State.SERVER_HOST; isDone = true; mode = true; }
            if(getAndRenderButton(joinServerReg, 0, 0, 0.5f, 0.25f, clientIsHover)) { currentState = State.CLIENT_CONNECT; mode = false; }
            if(getAndRenderButton(exitReg, 0, -0.27f, 0.5f, 0.25f, exitIsHover)) { Gdx.app.exit(); }
            if(serverIsHover.hover)
                getAndRenderButton(pointReg, -0.5f, 0.27f - 0.2f, 0.7448f, 1.0f);
            if(clientIsHover.hover)
                getAndRenderButton(pointReg, -0.5f, 0 - 0.2f, 0.7448f, 1.0f);
            if(exitIsHover.hover)
                getAndRenderButton(pointReg, -0.5f, -0.27f - 0.2f, 0.7448f, 1.0f);

            break;
        case SERVER_HOST:

            if(getAndRenderButton(startGameReg, -0.7f, 0.7f, 0.5f, 0.25f)) { runGame = true; }
            showWeaponSelect();

            break;
        case CLIENT_CONNECT:
            if(getAndRenderButton(blankReg, -0.25f, 0.5f, 0.5f, 0.25f, selectIPText))
            {
                 selectIPText = true; selectPortText = false;
                 if(serverIP == null) {
                    serverIP = new String();
                 }
            }
            if(getAndRenderButton(blankReg, 0.25f, 0.5f, 0.5f, 0.25f, selectPortText))
            {
                selectIPText = false; selectPortText = true;
                if(port == null) {
                    port = new String();
                }
            }
            if(selectIPText || selectPortText) {
                int k0 = Keys.NUM_0;
                int k9 = Keys.NUM_9;
                for (int i = k0; i <= k9; i++) {
                    if(Gdx.input.isKeyJustPressed(i)) {
                        appendToTextInputs(Integer.toString(i - k0));
                    }
                }
                if(Gdx.input.isKeyJustPressed(Keys.PERIOD)) {
                    appendToTextInputs(".");
                }
            }

            if(getAndRenderButton(connectReg, 0, 0, 0.5f, 0.25f)) { if(serverIP == null || port == null) {return;} isDone = true; currentState = State.CLIENT_WAIT; }

            break;
        case CLIENT_WAIT:

            showWeaponSelect();

            if(clientConnected) {
                runGame = true;
            }

            break;
        default:
            break;
        }
    }

    public void updateText() {
        float h = (float)Gdx.graphics.getHeight();
        float w = (float)Gdx.graphics.getWidth();
        float aspect = h / w;

        switch (currentState) {
            case MAIN:
                break;
            case SERVER_HOST:
                layout.setText(font, "IP: " + serverIP + "  Port: " + port);
                font.draw(ShooterGame.getInstance().coreBatch, layout, (w / 2.0f) - (layout.width / 2.0f), h - 20.0f);
                showPlayerList();
                break;
            case CLIENT_CONNECT:
                float buttonsY = h - (h / 2.0f) * 0.5f;
                float buttonsX = aspect * (w / 2.0f) * 0.25f;
                if(serverIP == null || serverIP.equals("")) {
                    layout.setText(font, "ENTER IP");
                } else {
                    layout.setText(font, serverIP);
                }
                font.draw(ShooterGame.getInstance().coreBatch, layout, (w/2.0f) - buttonsX - (layout.width / 2.0f), buttonsY);
                if(port == null || port.equals("")) {
                    layout.setText(font, "ENTER PORT");
                } else {
                    layout.setText(font, port);
                }
                font.draw(ShooterGame.getInstance().coreBatch, layout, (w/2.0f) + buttonsX - (layout.width / 2.0f), buttonsY);
                break;
            case CLIENT_WAIT:
                showPlayerList();
                break;
            default:
                break;
            }
    }
}
