package game.shootergame.Renderer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.ShooterGame;

public class Renderer {

    ShaderProgram shader;
    Mesh mesh;

    int screenX;

    float[] rayData;

    
    float camX = 0.0f;
    float camY = 0.0f;

    
    float yaw = 0.0f;

    
    float fov = 45.0f;

    float aspect;

    int numWorkers;
    Semaphore semStart;
    Semaphore semEnd;
    Thread[] workers;
    AtomicBoolean running;

    Texture tex;


    ArrayList<Wall> walls = new ArrayList<>();

    public Renderer(int screnX) {
        walls.add(new Wall(1.5177f, 3.4569f, 3.9219f, 3.5477f));
        walls.add(new Wall(3.9219f, 3.5477f, 3.8766f, 0.9279f));
        walls.add(new Wall(3.8766f, 0.9279f, 4.0353f, -1.5330f));
        walls.add(new Wall(1.3929f, 1.0187f, 1.5177f, 3.4569f));
        walls.add(new Wall(-1.0000f, 1.0527f, 1.3929f, 1.0187f));
        walls.add(new Wall(-1.0227f, -1.0113f, -1.0000f, 1.0527f));
        walls.add(new Wall(4.0353f, -1.5330f, 1.2908f, -1.2608f));
        walls.add(new Wall(1.2908f, -1.2608f, 1.3702f, -4.4703f));
        walls.add(new Wall(1.3702f, -4.4703f, -5.9900f, -3.8465f));
        walls.add(new Wall(-5.9900f, -3.8465f, -5.8539f, -1.3175f));
        walls.add(new Wall(-5.8539f, -1.3175f, -3.1434f, 2.2662f));
        walls.add(new Wall(-3.1434f, 2.2662f, -3.4836f, -1.7712f));
        walls.add(new Wall(-3.4836f, -1.7712f, -2.4630f, -1.8959f));
        walls.add(new Wall(-2.4630f, -1.8959f, -2.4403f, 3.4683f));
        walls.add(new Wall(-2.4403f, 3.4683f, 0.3949f, 3.5023f));
        walls.add(new Wall(0.3949f, 3.5023f, 0.2475f, 2.2095f));
        walls.add(new Wall(0.2475f, 2.2095f, -1.6805f, 2.2321f));
        walls.add(new Wall(-1.6805f, 2.2321f, -1.0227f, -1.0113f));
        walls.get(0).height = 2.0f;

        ShooterGame.getInstance().am.load("brick.png", Texture.class);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("brick.png", Texture.class);
        tex.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        resize(screnX, 0);

        mesh = new Mesh(true, 4, 6,
         new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
         new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        mesh.setVertices(new float[] {
            -1, -1, 0, 0,
            1, 1, 1, 1,
            -1, 1, 0, 1,
            1, -1, 1, 0
        });
        mesh.setIndices(new short[] {
            0, 1, 2, 1, 0, 3
        });

        running = new AtomicBoolean(true);
        numWorkers = Runtime.getRuntime().availableProcessors() - 1;
        semStart = new Semaphore(0);
        semEnd = new Semaphore(0);
        workers = new Thread[numWorkers];

        for (int i = 0; i < numWorkers; i++) {
            final int idx = i;
            workers[i] = new Thread(() -> {
                try {
                    while (true) {
                        int numPerThread = screenX / numWorkers;
                        int numTasks = screenX % numWorkers;
                        int start = idx * numPerThread + Math.min(idx, numTasks);
                        int end = (idx + 1) * numPerThread + Math.min(idx + 1, numTasks);
                        semStart.acquire();

                        if(!running.get()) break;

                        for (int j = start; j < end; j++) {
                            rayCast(j);
                        }

                        semEnd.release();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers[i].start();
        }
    }

    public class Wall {
        public Vector2 a;
        public Vector2 b;
        public float widthScaler;
        public float yOffset;
        public float height;
        Wall(float xa, float ya, float xb, float yb) {
             a = new Vector2(xa, ya); b = new Vector2(xb, yb);
             widthScaler = a.dst(b);
             yOffset = 0.0f;
             height = 1.0f;
        }
    }


    private void rayCast(int index) {
        float anglePerRay = fov / (screenX - 1);
        float angle = (float)Math.toRadians(yaw - (fov / 2.0f) + index * anglePerRay);

        float dX = (float)Math.cos(angle);
        float dY = (float)Math.sin(angle);
        Vector2 rayDir = new Vector2(dX, dY);
        Vector2 rayOrigin = new Vector2(camX, camY);

        float minDistance = Float.MAX_VALUE;
        float deltaX = 0.0f;
        float yOffset = 0.0f;
        float height = 1.0f;
        for (Wall wall : walls) {
            Vector2 segDir = new Vector2(wall.a).sub(wall.b);
            Vector2 segToRay = new Vector2(wall.b).sub(rayOrigin);
            float crossDir = rayDir.crs(segDir);
            float t = segToRay.crs(segDir) / crossDir;
            float u = segToRay.crs(rayDir) / crossDir;
            if(crossDir != 0 && t >= 0 && u >= 0 && u <= 1) {
                float dst = t * rayDir.len();
                if(dst < minDistance) {
                    minDistance = dst;
                    deltaX = u * wall.widthScaler;
                    yOffset = wall.yOffset;
                    height = wall.height;
                }
            }
        }
        rayData[index * 4] = 1.0f / minDistance;
        rayData[index * 4 + 1] = deltaX;
        rayData[index * 4 + 2] = yOffset;
        rayData[index * 4 + 3] = height;
    }
    float wx = 0.0f;
    public void render() {
        wx += 1.0f / 144.0f;

        walls.get(0).yOffset = (float)Math.sin(wx) + 1.0f;

        if(Gdx.input.isKeyPressed(Keys.RIGHT)) {
            yaw += 60.0 / 144.0;
        }
        if(Gdx.input.isKeyPressed(Keys.LEFT)) {
            yaw -= 60.0 / 144.0;
        }

        float speed = 3.0f / 144.0f;

        float yawR = (float)Math.toRadians(yaw);

        if(Gdx.input.isKeyPressed(Keys.W)) {
            camX += Math.cos(yawR) * speed;
            camY += Math.sin(yawR) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.S)) {
            camX -= Math.cos(yawR) * speed;
            camY -= Math.sin(yawR) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.A)) {
            camX -= Math.cos(yawR + Math.PI / 2.0f) * speed;
            camY -= Math.sin(yawR + Math.PI / 2.0f) * speed;
        }
        if(Gdx.input.isKeyPressed(Keys.D)) {
            camX += Math.cos(yawR + Math.PI / 2.0f) * speed;
            camY += Math.sin(yawR + Math.PI / 2.0f) * speed;
        }
        
        for (int i = 0; i < numWorkers; i++) {
            semStart.release();
        }
        for (int i = 0; i < numWorkers; i++) {
            try {
                semEnd.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        shader.bind();
        tex.bind(0);

        shader.setUniformi("texture", 0);

        shader.setUniform4fv("rayData", rayData, 0, rayData.length);
        shader.setUniformf("numRays", rayData.length / 4);
        shader.setUniformf("aspect", aspect);

        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    public void resize(int x, int y) {
        aspect = (float)x / (float)y;
        screenX = x;
        if(screenX > 1021) {
            screenX = 1021;
        }

        rayData = new float[screenX * 4];

        if(shader != null) {
            shader.dispose();
        }

        String vertexShader = 
              "attribute vec2 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n"
            + "varying vec2 v_texCoords;\n"
            + "void main()\n"
            + "{\n"
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n"
            + "   gl_Position = vec4(" + ShaderProgram.POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
            + "}\n";
        String fragmentShader = 
              "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform vec4 rayData[" + screenX + "];\n"
            + "uniform float numRays;\n"
            + "uniform float aspect;\n"
            + "uniform sampler2D texture;\n"
            + "void main()\n"
            + "{\n"
            + "  vec4 dat = rayData[int(v_texCoords.x * numRays)];\n"
            + "  float wallTop = (dat.w * 2.0 + dat.z - 1.0) * dat.x;\n"
            + "  float wallBottom = (dat.z - 1.0) * dat.x;\n"
            + "  float current = v_texCoords.y - 0.5;"
            + "  bool isWall = current > wallBottom && current < wallTop;\n"
            + "  float color = isWall ? max(min(1.0 * dat.x, 1.0), 0.3) : 0.0;\n"
            + "  float texY = dat.w * (current - wallBottom) / (wallTop - wallBottom);\n"
            + "  gl_FragColor = vec4(texture2D(texture, vec2(aspect * dat.y, texY)).rgb * color, 1.0);\n"
            + "}";
        shader = new ShaderProgram(vertexShader, fragmentShader);

        if (!shader.isCompiled()) {
            System.err.println("Shader compilation failed:\n" + shader.getLog());
        }
    }
}
