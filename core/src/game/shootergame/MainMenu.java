package game.shootergame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class MainMenu {
    private enum State {
        MAIN,
        SERVER_HOST,
        CLIENT_CONNECT,
        CLIENT_WAIT;
    };
    
    State currentState;
    Texture atlas;
    TextureRegion hostServerReg;
    TextureRegion joinServerReg;
    TextureRegion exitReg;
    TextureRegion startGameReg;
    TextureRegion connectReg;
    
    TextureRegion swordReg;
    TextureRegion halberdReg;
    TextureRegion maceReg;
    TextureRegion brassReg;

    Sprite sprite;

    float mouseX;
    float mouseY;

    boolean isDone = false;
    boolean mode = false; //false client, true server
    boolean launchGame = false;

    int selectedWeaponIndex = 0;

    public MainMenu() {
        currentState = State.MAIN;

        TextureParameter param = new TextureParameter();
        param.minFilter = TextureFilter.Linear;
        param.magFilter = TextureFilter.Linear;
        ShooterGame.getInstance().am.load("GUIAtlas.png", Texture.class, param);
        ShooterGame.getInstance().am.finishLoading();
        atlas = ShooterGame.getInstance().am.get("GUIAtlas.png", Texture.class);
        hostServerReg = new TextureRegion(atlas, 0, 0, 216, 108);
        joinServerReg = new TextureRegion(atlas, 0, 108, 216, 108);
        exitReg = new TextureRegion(atlas, 0, 216, 216, 108);
        startGameReg = new TextureRegion(atlas, 216, 0, 216, 108);
        connectReg = new TextureRegion(atlas, 216, 108, 324, 108);
        
        swordReg = new TextureRegion(atlas, 0, 324, 216, 108);
        halberdReg = new TextureRegion(atlas, 216, 324, 216, 108);
        maceReg = new TextureRegion(atlas, 0, 432, 216, 108);
        brassReg = new TextureRegion(atlas, 216, 432, 216, 108);

        sprite = new Sprite(atlas);
    }

    private void showPlayerList() {
    }

    private void showWeaponSelect() {
        if(getAndRenderButton(swordReg, -0.7f, -0.45f, 0.5f, 0.25f, selectedWeaponIndex == 0)) { selectedWeaponIndex = 0; }
        if(getAndRenderButton(halberdReg, -0.2f, -0.45f, 0.5f, 0.25f, selectedWeaponIndex == 1)) { selectedWeaponIndex = 1; }
        if(getAndRenderButton(maceReg, -0.7f, -0.7f, 0.5f, 0.25f, selectedWeaponIndex == 2)) { selectedWeaponIndex = 2; }
        if(getAndRenderButton(brassReg, -0.2f, -0.7f, 0.5f, 0.25f, selectedWeaponIndex == 3)) { selectedWeaponIndex = 3; }
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height) {
        return getAndRenderButton(reg, x, y, width, height, false);
    }

    private boolean getAndRenderButton(TextureRegion reg, float x, float y, float width, float height, boolean overrideSelection) {
        sprite.setRegion(reg);
        sprite.setSize(width, height);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(x, y);

        Rectangle rect = sprite.getBoundingRectangle();
        boolean pressed = false;
        if(rect.contains(mouseX, mouseY)) {
            sprite.setColor(0.8f, 0.8f, 0.8f, 0.8f);
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

    private void renderTextBox(TextureRegion reg) {
    }

    public boolean isDone() {
        return isDone;
    }

    public boolean getSelectedMode() {
        return mode;
    }

    public boolean shouldLaunchGame() {
        return launchGame;
    }

    public int getPort() {
        return 42069;
    }

    public String getIP() {
        return "localhost";
    }

    public int getSelectedWeapon() {
        System.out.println(selectedWeaponIndex);
        return selectedWeaponIndex;
    }

    public void update() {
        float aspect = (float)Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth();
        mouseX = (float)Gdx.input.getX() / (float)Gdx.graphics.getWidth() * (2f / aspect) - (1f / aspect);
        mouseY = -((float)Gdx.input.getY() / (float)Gdx.graphics.getHeight() * 2f - 1f);

        switch (currentState) {
        case MAIN:

            if(getAndRenderButton(hostServerReg, 0, 0.27f, 0.5f, 0.25f)) { currentState = State.SERVER_HOST; isDone = true; mode = true; }
            if(getAndRenderButton(joinServerReg, 0, 0, 0.5f, 0.25f)) { currentState = State.CLIENT_CONNECT; isDone = true; mode = false; }
            if(getAndRenderButton(exitReg, 0, -0.27f, 0.5f, 0.25f)) { /* EXIT */}
            
            break;
        case SERVER_HOST:

            if(getAndRenderButton(startGameReg, -0.7f, 0.7f, 0.5f, 0.25f)) { launchGame = true; }
            showPlayerList();
            showWeaponSelect();

            break;
        case CLIENT_CONNECT:

            if(getAndRenderButton(connectReg, 0, 0, 0.5f, 0.25f)) { launchGame = true; }

            break;
        case CLIENT_WAIT:

            renderTextBox(null);
            renderTextBox(null);

            showPlayerList();
            showWeaponSelect();

            break;
        default:
            break;
        }
    }
}
