package game.shootergame.Physics;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

import game.shootergame.Wall;

public class PhysicsWorld {

    ArrayList<Collider> colliders = new ArrayList<>();
    ArrayList<Wall> walls;

    public PhysicsWorld(ArrayList<Wall> walls) {
        this.walls = walls;
    }

    public void addCollider(Collider collider) {
        colliders.add(collider);
    }

    boolean isOverlap(Collider a, Collider b) {
        return Vector2.dst(a.x, a.y, b.x, b.y) < a.radius + b.radius;
    }

    boolean isIntersecting(Collider c, Wall w) {
        return false;
    }

    public void update() {
        for (Collider colliderA : colliders) {
            for (Collider colliderB : colliders) {
                if(colliderA == colliderB) continue;
                if(isOverlap(colliderA, colliderB)) {
                    colliderA.Callback(colliderB);
                    colliderB.Callback(colliderA);
                }
            }
        }


        for (Collider collider : colliders) {
            for (Wall wall : walls) {
                if(isIntersecting(collider, wall)) {
                    collider.Callback(null);
                }
            }
        }
    }
}
