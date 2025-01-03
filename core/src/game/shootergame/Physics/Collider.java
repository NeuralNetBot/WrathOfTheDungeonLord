package game.shootergame.Physics;

public class Collider {
    public float x, y;
    public float dx, dy;
    public float height;
    public float radius;
    public boolean isStatic;
    public boolean isPlayer;

    public Collider(float x, float y, float radius, ColliderCallback callback) {
        this.x = x; this.y = y;
        this.height = 0.0f;
        this.radius = radius;
        this.callback = callback;
        this.isStatic = true;
        this.isPlayer = false;
    }

    public Collider(float x, float y, float radius, ColliderCallback callback, boolean isStatic, float height) {
        this.x = x; this.y = y;
        this.dx = 0.0f; this.dy = 0.0f;
        this.height = height;
        this.radius = radius;
        this.callback = callback;
        this.isStatic = isStatic;
        this.isPlayer = false;
    }

    public interface ColliderCallback {
        void callback(Collider collider, float newDX, float newDY, float damage);
    }

    private ColliderCallback callback;
    public void Callback(Collider collider, float newDX, float newDY, float damage) {
        if(callback != null)
            callback.callback(collider, newDX, newDY, damage);
    }
}
