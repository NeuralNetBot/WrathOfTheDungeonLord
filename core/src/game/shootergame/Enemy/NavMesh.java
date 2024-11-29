package game.shootergame.Enemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.badlogic.gdx.math.Vector2;

import game.shootergame.Enemy.NavMesh.Triangle.Edge;

public class NavMesh {
    static public class Triangle {
        public class Edge {
            public Vector2 a;
            public Vector2 b;
            public Edge(Vector2 a, Vector2 b) { this.a = a; this.b = b; }
        }
        public Vector2[] verts;
        public int[] neighbors;//1 neighbor for each edge of triangle
        public Triangle(float ax, float ay, float bx, float by, float cx, float cy) {
            verts = new Vector2[3];
            verts[0] = new Vector2(ax, ay);
            verts[1] = new Vector2(bx, by);
            verts[2] = new Vector2(cx, cy);
            neighbors = new int[3];
            neighbors[0] = Integer.MAX_VALUE;
            neighbors[1] = Integer.MAX_VALUE;
            neighbors[2] = Integer.MAX_VALUE;
        }

        Edge[] getEdges() {
            return new Edge[] { new Edge(verts[0], verts[1]), new Edge(verts[1], verts[2]), new Edge(verts[2], verts[0]) };
        }

        public boolean containsPoint(Vector2 p) {
            Vector2 v0 = verts[1].cpy().sub(verts[0]);
            Vector2 v1 = verts[2].cpy().sub(verts[1]);
            Vector2 v2 = verts[0].cpy().sub(verts[2]);
            
            Vector2 p0 = p.cpy().sub(verts[0]);
            Vector2 p1 = p.cpy().sub(verts[1]);
            Vector2 p2 = p.cpy().sub(verts[2]);

            float c1 = v0.crs(p0);
            float c2 = v1.crs(p1);
            float c3 = v2.crs(p2);

            return (c1 >= 0 && c2 >= 0 && c3 >=0) || (c1 <= 0 && c2 <= 0 && c3 <= 0);            
        }

        public Vector2 getCenter() {
            return new Vector2(verts[0]).add(verts[1]).add(verts[2]).scl(1.0f / 3.0f);
        }
    }

    ArrayList<Triangle> triangles;
    public NavMesh() {
        triangles = new ArrayList<>();
    }

    public ArrayList<Triangle> getTriangles() {
        return triangles;
    }

    public void addTriangle(Triangle triangle) {
        Edge[] ea = triangle.getEdges();
        for (int i = 0; i < triangles.size(); i++) {
            Edge[] eb = triangles.get(i).getEdges();
            for (int j = 0; j < ea.length; j++) { //go through edges and see if they share any
                for (int k = 0; k < eb.length; k++) {
                    if((ea[j].a.equals(eb[k].a) && ea[j].b.equals(eb[k].b)) || (ea[j].a.equals(eb[k].b) && ea[j].b.equals(eb[k].a))) {
                        triangle.neighbors[j] = i;
                        triangles.get(i).neighbors[k] = triangles.size();
                    }
                }
            }
        }
        triangles.add(triangle);
    }

    private boolean linesIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        float det = (c.x - d.x) * (b.y - a.y) - (c.y - d.y) * (b.x - a.x);
        float t = ((a.x - d.x) * (b.y - a.y) - (a.y - d.y) * (b.x - a.x)) / det;
        float u = ((a.x - d.x) * (c.y - d.y) - (a.y - d.y) * (c.x - d.x)) / det;
        return t > 0 && t < 1 && u > 0 && u < 1;
    }

    private ArrayList<Vector2> reconstructPath(Node node, Vector2 start, Vector2 end) {
        ArrayList<Vector2> path = new ArrayList<>();
        path.add(end);
        while(node != null) {
            Triangle tri = triangles.get(node.idx);
            Vector2 center = tri.getCenter();

            if(node.parent != null) {
                Triangle tri2 = triangles.get(node.parent.idx);
                Vector2 center2 = tri2.getCenter();
                Edge edge = tri2.getEdges()[node.parentEdgeIndex];
                if(linesIntersect(center, center2, edge.a, edge.b)) {
                    node = node.parent;
                }
            }

            path.add(center);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private class Node {
        public int idx;
        public float gCost;
        public float fCost;
        public Node parent;
        public int parentEdgeIndex;

        public Node(int idx) { this.idx = idx; parent = null; parentEdgeIndex = -1; gCost = Float.MAX_VALUE; fCost = Float.MAX_VALUE; }
    }
    
    private float heuristic(Node a, Node b) {
        return triangles.get(a.idx).getCenter().dst(triangles.get(b.idx).getCenter());
    }

    private float cost(Node a, Node b) {
        return triangles.get(a.idx).getCenter().dst(triangles.get(b.idx).getCenter());
    }

    //returns path from start to end including start and end nodes
    public ArrayList<Vector2> pathFind(Vector2 start, Vector2 end) {
        int startI = -1;
        int endI = -1;
        for (int i = 0; i < triangles.size(); i++) {
            if(triangles.get(i).containsPoint(start)) { startI = i; }
            if(triangles.get(i).containsPoint(end)) { endI = i; }
        }
        if(startI == -1) { return null; }
        if(endI == -1) { return null; }

        if(startI == endI) {
            return reconstructPath(null, start, end);
        }

        ArrayList<Node> storedNodes = new ArrayList<>(triangles.size());
        for (int i = 0; i < triangles.size(); i++) {
            storedNodes.add(new Node(i));
        }
        Node startN = storedNodes.get(startI);
        Node endN = storedNodes.get(endI);

        PriorityQueue<Node> openSet = new PriorityQueue<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                return (int)Float.compare(n1.fCost, n2.fCost);
            }
        });
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ArrayList<Node> closedSet = new ArrayList();

        startN.gCost = 0;
        startN.fCost = heuristic(startN, endN);
        startN.parent = null;
        openSet.add(startN);

        while(!openSet.isEmpty()) {
            Node current = openSet.poll();
            if(current.equals(endN)) {
                return reconstructPath(current, start, end);
            }

            closedSet.add(current);

            int[] neighbors = triangles.get(current.idx).neighbors;
            for (int i = 0; i < neighbors.length; i++) {
                if(neighbors[i] == Integer.MAX_VALUE) continue;
                Node neighbor = storedNodes.get(neighbors[i]);

                float tG = current.gCost + cost(current, neighbor);
                if(closedSet.contains(neighbor) || tG >= neighbor.gCost) {
                    continue;
                }

                if(tG < neighbor.gCost || !openSet.contains(neighbor)) {
                    neighbor.gCost = tG;
                    neighbor.fCost = tG + heuristic(neighbor, endN);
                    neighbor.parent = current;
                    neighbor.parentEdgeIndex = i;

                    if(!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return null;
    }
}
