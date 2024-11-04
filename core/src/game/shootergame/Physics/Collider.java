package game.shootergame.Physics;

public class Collider {
    public float x, y;
    public float height;
    public float radius;
    public boolean isStatic;
    
    public Collider(float x, float y, float radius, ColliderCallback callback) {
        this.x = x; this.y = y;
        this.height = 0.0f;
        this.radius = radius;
        this.callback = callback;
        this.isStatic = true;
    }  
    public Collider(float x, float y, float radius, ColliderCallback callback, boolean isStatic, float height) {
        this.x = x; this.y = y;
        this.height = height;
        this.radius = radius;
        this.callback = callback;
        this.isStatic = isStatic;
    }  

    public interface ColliderCallback {
        void callback(Collider collider);
    }

    private ColliderCallback callback;
    public void Callback(Collider collider) {
        if(callback != null)
            callback.callback(collider);
    }
}
