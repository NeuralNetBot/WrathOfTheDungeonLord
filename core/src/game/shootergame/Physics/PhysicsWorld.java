package game.shootergame.Physics;

import java.util.ArrayList;
import java.util.LinkedList;

import com.badlogic.gdx.math.Vector2;

import game.shootergame.Wall;

public class PhysicsWorld {

    LinkedList<Collider> colliders = new LinkedList<>();
    ArrayList<Wall> walls;

    public PhysicsWorld(ArrayList<Wall> walls) {
        this.walls = walls;
    }

    public void addCollider(Collider collider) {
        colliders.add(collider);
    }

    public void removeCollider(Collider collider) {
        colliders.remove(collider);
    }

    boolean isOverlap(Collider a, Collider b) {
        return Vector2.dst(a.x, a.y, b.x, b.y) < a.radius + b.radius;
    }

    public class WallIntersectionResult {
        public boolean intersect;
        public float x, y;
        public float nx, ny;
        
        WallIntersectionResult(float x, float y, float nx, float ny, boolean intersect) { this.x = x; this.y = y; this.nx = nx; this.ny = ny; this.intersect = intersect; }
    }

    WallIntersectionResult isIntersecting(Collider col, Wall w) {

        float cx = col.x + col.dx;
        float cy = col.y + col.dy;

        Vector2 d = w.a.cpy().sub(w.b);
        float a = d.x * d.x + d.y * d.y;
        float b = 2.0f * (d.x * (w.b.x - cx) + d.y * (w.b.y - cy));
        float c = (w.b.x - cx) * (w.b.x - cx) + (w.b.y - cy) * (w.b.y - cy) - col.radius * col.radius;

        float dsc = b * b - 4.0f * a * c;

        float iX1 = 0.0f;
        float iY1 = 0.0f;
        float iX2 = 0.0f;
        float iY2 = 0.0f;
        boolean intersect = false;
        boolean intersectTwice = false;

        if(dsc > 0) {

            float t1 = (-b + (float)Math.sqrt(dsc)) / (2.0f * a);
            float t2 = (-b - (float)Math.sqrt(dsc)) / (2.0f * a);
            boolean t1i = false;
            if(t1 <= 1 && t1 >= 0) {
                intersect = true;
                iX1 = w.b.x * t1 * d.x;
                iY1 = w.b.y * t1 * d.y;
                t1i = true;
            }
            boolean t2i = false;
            if(t2 <= 1 && t2 >= 0) {
                intersect = true;
                iX2 = w.b.x * t2 * d.x;
                iY2 = w.b.y * t2 * d.y;
                t2i = true;
            }
            if(t1i && t2i) intersectTwice = true;

        } else if(dsc == 0) {
            float t = -b / (2.0f * a);
            if(t <= 1 && t >= 0) {
                intersect = true;
                iX1 = w.b.x * t * d.x;
                iY1 = w.b.y * t * d.y;
            }
        }
        if(!intersect)
            return new WallIntersectionResult(0.0f, 0.0f, 0.0f, 0.0f, false);

        float iX;
        float iY;
        if(intersectTwice) {
            if(Vector2.dst(iX1, iY1, col.x, col.y) < Vector2.dst(iX2, iY2, col.x, col.y)) {
                iX = iX1; iY = iY1;
            } else {
                iX = iX2; iY = iY2;
            }
        } else {
            iX = iX1; iY = iY1;
        }


        Vector2 wallDir = w.a.cpy().sub(w.b).nor();
        Vector2 normal = new Vector2(wallDir.y, -wallDir.x);
        Vector2 colToContactPoint = new Vector2(iX, iY).sub(col.x, col.y);
        if(colToContactPoint.dot(normal) < 0) {
            normal.x = -normal.x;
            normal.y = -normal.y;
        }
        return new WallIntersectionResult(iX, iY, normal.x, normal.y, true);
    }

    public void update() {

        for (int i = 0; i < colliders.size(); i++) {
            for (int j = i + 1; j < colliders.size(); j++) {
                Collider colliderA = colliders.get(i);
                Collider colliderB = colliders.get(j);
                if(colliderA.isStatic && colliderB.isStatic) continue;
                if(isOverlap(colliderA, colliderB)) {
                    colliderA.Callback(colliderB, 0.0f, 0.0f, 0.0f);
                    colliderB.Callback(colliderA, 0.0f, 0.0f, 0.0f);
                }
            }
        }
        
        for (Collider collider : colliders) {
            if(collider.isStatic) { continue; }
            Vector2[] normals = new Vector2[3];//3 collisions should handle every case for our needs
            int count = 0;
            for (Wall wall : walls) {
                if(collider.height > wall.yOffset) {
                    WallIntersectionResult res = isIntersecting(collider, wall);
                    if(res.intersect) {
                        normals[count] = new Vector2(res.nx, res.ny);
                        count += 1;
                        if(count >= 2) break;
                    }
                }
            }

            if(count > 0) {
                Vector2 effectiveSlide = new Vector2();
                float totalWeight = 0.0f;
                for (int i = 0; i < count; i++) {
                    float weight = new Vector2(collider.dx, collider.dy).dot(normals[i]);
                    totalWeight += weight;
                    effectiveSlide.add(-normals[i].y * weight, normals[i].x * weight);
                }
                if(totalWeight == 0.0f) { totalWeight = 0.00001f; }
                effectiveSlide.scl(1.0f / totalWeight);

                float d = new Vector2(collider.dx, collider.dy).dot(effectiveSlide);

                Vector2 slide = effectiveSlide.scl(d);
                collider.Callback(null, slide.x, slide.y, 0.0f);
            } else {
                collider.Callback(null, collider.dx, collider.dy, 0.0f);
            }
        }
    }

    public ArrayList<Collider> runAngleSweep(Collider self, float x, float y, float direction, float angle, float distance, float damage) {
        ArrayList<Collider> hits = new ArrayList<>();
        for (Collider collider : colliders) {
            if(collider == self) continue;
            float dst = Vector2.dst(x, y, collider.x, collider.y);
            if(dst <= distance + collider.radius) {
                float angleBetweenCenters = (float)Math.atan2(collider.y - y, collider.x - x);
                direction = direction % (2.0f * (float)Math.PI);
                float angleMax = direction + (angle / 2.0f);
                angleMax = angleMax % (2.0f * (float)Math.PI);
                float angleMin = direction - (angle / 2.0f);
                angleMin = angleMin % (2.0f * (float)Math.PI);

                if(angleBetweenCenters > angleMin && angleBetweenCenters < angleMax) {
                    collider.Callback(collider, x, y, damage);
                }
                hits.add(collider);
            }
        }
        return hits;
    }

    //TODO: impliment
    public Collider rayCast(Collider self, float x, float y, float dx, float dy) {
        float closestDst = Float.MAX_VALUE;
        Collider hit = null;
        //check if hit collider
        for (Collider collider : colliders) {
            if(collider == self) continue;

            Vector2 L = new Vector2(collider.x - x, collider.y - y);
            float T = L.x * dx + L.y * dy;
            float d2 = (L.x * L.x + L.y * L.y) - T * T;
            if(d2 > collider.radius * collider.radius) continue;
            float t = (float)Math.sqrt(collider.radius * collider.radius - d2);
            float t1 = T - t;
            float t2 = T + t;
            float dst = 0.0f;
            if(t1 >= 0) dst = t1;
            else if(t2 >= 0) dst = t2;
            else continue;
            if(closestDst < dst) {
                closestDst = dst;
                hit = collider;
            }
        }

        //if we hit then we need to check if we hit a wall first or not
        if(hit != null) {
            for (Wall wall : walls) {
                //open door so we ignore hits
                if(wall.yOffset >= 1.0f) continue;

                Vector2 segDir = new Vector2(wall.a).sub(wall.b);
                Vector2 segToRay = new Vector2(wall.b).sub(x, y);
                float crossDir = new Vector2(dx, dy).crs(segDir);
                float t = segToRay.crs(segDir) / crossDir;
                float u = segToRay.crs(dx, dy) / crossDir;
                if(crossDir != 0 && t >= 0 && u >= 0 && u <= 1) {
                    float dst = t * new Vector2(dx, dy).len();
                    if(dst < closestDst) { //wall hit was closer than hit entity so we stop
                        hit = null;
                        break;
                    }
                }
            }
        }

        return null;
    }
}
