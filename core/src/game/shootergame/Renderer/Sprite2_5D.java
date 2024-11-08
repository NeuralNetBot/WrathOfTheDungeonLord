package game.shootergame.Renderer;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Sprite2_5D {
    public TextureRegion texture;
    public float x, y;
    public float height;
    public float width;

    //internal do not change values
    public TextureRegion textureCalc;
    public float dst;
    public float visHeight;
    public float visWidth;
    public float scrX, scrY;
    public boolean isVis;
    public float getDst() { return dst; }
    
    public Sprite2_5D(TextureRegion texture, float x, float y, float height, float width) {
        this.texture = texture;
        this.textureCalc = new TextureRegion(texture);
        this.x = x; this.y = y;
        this.height = height;
        this.width = width;
    }

    public Sprite2_5D(TextureRegion texture, float x, float y, float height) {
        this.texture = texture;
        this.textureCalc = new TextureRegion(texture);
        this.x = x;
        this.y = y;
        this.height = height;
        float aspect = Math.abs(texture.getU2() - texture.getU()) / Math.abs(texture.getV2() - texture.getV());
        this.width = height * aspect;
    }
}