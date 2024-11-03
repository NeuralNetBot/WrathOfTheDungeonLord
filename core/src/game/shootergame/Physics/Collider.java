package game.shootergame.Physics;

public class Collider {
    public float x, y;
    public float radius;
    
    public Collider(float x, float y, float radius, ColliderCallback callback) {
        this.x = x; this.y = y;
        this.radius = radius;
        this.callback = callback;
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
