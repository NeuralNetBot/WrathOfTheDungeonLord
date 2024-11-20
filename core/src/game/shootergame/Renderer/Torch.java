package game.shootergame.Renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;

import game.shootergame.ShooterGame;

public class Torch {
    public float x;
    public float y;
    public float z;
    public float radius;
    public Torch(float x, float y, float radius) {
        this.x = x; this.y = y; this.radius = radius;
    }

    static Texture tex;
    static TextureRegion reg;
    static Animation<TextureRegion> animation;
    static float animTime = 0.0f;
    static void loadTexture() {
        ShooterGame.getInstance().am.load("torch.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("torch.png", Texture.class);
        reg = new TextureRegion(tex, 0, 0, 128, 256);

        TextureRegion[][] tempFrames = TextureRegion.split(tex, tex.getWidth() / 8, tex.getHeight() / 4);
        TextureRegion[] animFrames = new TextureRegion[(8 * 4) - 1];
        int index = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                if(index < (8 * 4) - 1)
                    animFrames[index++] = tempFrames[i][j];
            }
        }
        animation = new Animation<TextureRegion>(0.04167f, animFrames);
        animation.setPlayMode(PlayMode.LOOP);
    }

    static void updateRegion(float delta) {
        animTime += delta;
        TextureRegion animReg = animation.getKeyFrame(animTime);
        reg.setRegion(animReg);
    }

    static TextureRegion getTextureRegion() {
        return reg;
    }
}
