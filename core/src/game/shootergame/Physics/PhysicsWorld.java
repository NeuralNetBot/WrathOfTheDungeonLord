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

    boolean isIntersecting(Collider col, Wall w) {
        Vector2 d = w.a.cpy().sub(w.b);
        float a = d.x * d.x + d.y * d.y;
        float b = 2.0f * (d.x * (w.b.x - col.x) + d.y * (w.b.y - col.y));
        float c = (w.b.x - col.x) * (w.b.x - col.x) + (w.b.y - col.y) * (w.b.y - col.y) - col.radius * col.radius;

        float dsc = b * b - 4.0f * a * c;

        if(dsc > 0) {

            float t1 = (-b + (float)Math.sqrt(dsc)) / (2.0f * a);
            float t2 = (-b - (float)Math.sqrt(dsc)) / (2.0f * a);

            if(t1 <= 1 && t1 >= 0) {
                return true;
            }
            if(t2 <= 1 && t2 >= 0) {
                return true;
            }
        } else if(dsc == 0) {
            float t = -b / (2.0f * a);
            if(t <= 1 && t >= 0) {
                return true;
            }
        }

        return false;
    }

    public void update() {

        for (int i = 0; i < colliders.size(); i++) {
            for (int j = i + 1; j < colliders.size(); j++) {
                Collider colliderA = colliders.get(i);
                Collider colliderB = colliders.get(j);
                if(colliderA.isStatic && colliderB.isStatic) continue;
                if(isOverlap(colliderA, colliderB)) {
                    colliderA.Callback(colliderB);
                    colliderB.Callback(colliderA);
                }
            }
        }

        for (Collider collider : colliders) {
            if(collider.isStatic) { continue; }
            for (Wall wall : walls) {
                if(collider.height > wall.yOffset) {
                    if(isIntersecting(collider, wall)) {
                        collider.Callback(null);
                    }
                }
            }
        }
    }
}
