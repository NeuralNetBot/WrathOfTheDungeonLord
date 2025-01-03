package game.shootergame;

import com.badlogic.gdx.math.Vector2;

public class Wall {
    public Vector2 a;
    public Vector2 b;
    public float widthScaler;
    public float yOffset;
    public float height;
    public float textureID;
    public boolean transparentDoor;
    public Wall(float xa, float ya, float xb, float yb) {
         a = new Vector2(xa, ya); b = new Vector2(xb, yb);
         widthScaler = a.dst(b);
         yOffset = 0.0f;
         height = 1.0f;
         textureID = 0.0f;
         transparentDoor = false;
    }

    public Wall(float xa, float ya, float xb, float yb, float height, float textureID, boolean isDoor) {
        a = new Vector2(xa, ya); b = new Vector2(xb, yb);
        widthScaler = a.dst(b);
        yOffset = 0.0f;
        this.height = height;
        this.textureID = textureID;
        transparentDoor = isDoor;
   }
}