package game.shootergame.Renderer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
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

    ShaderProgram wallShader;
    ShaderProgram floorShader;
    Mesh mesh;

    int screenX;

    float[] rayData;

    
    float camX = 0.0f;
    float camY = 0.0f;

    
    float yaw = 0.0f;

    
    float fov = 75.0f;

    float aspect;

    int numWorkers;
    Semaphore semStart;
    Semaphore semEnd;
    Thread[] workers;
    AtomicBoolean running;

    Texture tex;
    Texture floor;


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

        TextureParameter param = new TextureParameter();
        param.minFilter = TextureFilter.Linear;
        param.magFilter = TextureFilter.Linear;

        ShooterGame.getInstance().am.load("brickwall.jpg", Texture.class, param);
        ShooterGame.getInstance().am.load("brick.png", Texture.class, param);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("brickwall.jpg", Texture.class);
        floor = ShooterGame.getInstance().am.get("brick.png", Texture.class);
        tex.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        floor.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

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
    int lastX = 0;
    public void render() {

        if(Gdx.input.isKeyJustPressed(Keys.T)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }

        int currentX = Gdx.input.getX();
        int delta = lastX - currentX;
        lastX = currentX;
        yaw -= (float)delta / 20.0f;

        wx += 1.0f / 144.0f;

        walls.get(0).yOffset = (float)Math.sin(wx) + 1.0f;

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

        wallShader.bind();
        tex.bind(0);

        wallShader.setUniformi("texture", 0);

        wallShader.setUniform4fv("rayData", rayData, 0, rayData.length);
        wallShader.setUniformf("numRays", rayData.length / 4);
        wallShader.setUniformf("cameraInfo", camX, camY, aspect, 0);

        mesh.render(wallShader, GL20.GL_TRIANGLES);

        floorShader.bind();
        floor.bind(0);

        floorShader.setUniformi("texture", 0);

        floorShader.setUniform4fv("rayData", rayData, 0, rayData.length);
        floorShader.setUniformf("numRays", rayData.length / 4);
        floorShader.setUniformf("cameraInfo", camX, camY, aspect, (float)Math.toRadians(fov) * 0.5f);
        floorShader.setUniformf("cameraDir", yawR);
        
        mesh.render(floorShader, GL20.GL_TRIANGLES);
    }

    public void resize(int x, int y) {
        aspect = (float)x / (float)y;
        screenX = x;
        if(screenX > 1020) {
            screenX = 1020;
        }

        rayData = new float[screenX * 4];

        if(wallShader != null) {
            wallShader.dispose();
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
        String wallFragmentShader = 
              "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform vec4 rayData[" + screenX + "];\n"
            + "uniform float numRays;\n"
            + "uniform vec4 cameraInfo;\n" //x y pos, z aspect, w tanhalffov
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
            + "  gl_FragColor = vec4(texture2D(texture, vec2(cameraInfo.z * dat.y, texY)).rgb * color, 1.0);\n"
            + "}\n";

        String floorFragmentShader = 
            "#ifdef GL_ES\n"
          + "precision mediump float;\n"
          + "#endif\n"
          + "varying vec2 v_texCoords;\n"
          + "uniform vec4 rayData[" + screenX + "];\n"
          + "uniform float numRays;\n"
          + "uniform vec4 cameraInfo;\n"
          + "uniform float cameraDir;\n"
          + "uniform sampler2D texture;\n"
          + "void main()\n"
          + "{\n"
          + "  vec2 screenPos = v_texCoords * 2.0 - 1.0;"
          + "  vec4 dat = rayData[int(v_texCoords.x * numRays)];\n"
          + "  float wallTop = (dat.w * 2.0 + dat.z - 1.0) * dat.x;\n"
          + "  float wallBottom = (dat.z - 1.0) * dat.x;\n"
          + "  float current = v_texCoords.y - 0.5;\n"
          + "  bool isWall = current > wallBottom && current < wallTop;\n"
          + "  float rayAngle = cameraDir + (screenPos.x * cameraInfo.w);\n"
          + "  vec2 worldDir = vec2(cos(rayAngle), sin(rayAngle)) * abs(1.0 / screenPos.y);\n"
          + "  vec2 worldPos = worldDir + (cameraInfo.xy * 0.5);\n"
          + "  vec3 fColor = texture2D(texture, worldPos * 2.0).rgb * 0.3;\n"
          + "  if(!isWall) {\n"
          + "      gl_FragColor = vec4(fColor, 1.0);\n"
          + "   } else { discard; }\n"
          + "}\n";

        wallShader = new ShaderProgram(vertexShader, wallFragmentShader);
        floorShader = new ShaderProgram(vertexShader, floorFragmentShader);

        if (!wallShader.isCompiled()) {
            System.err.println("Shader compilation failed:\n" + wallShader.getLog());
        }

        if (!floorShader.isCompiled()) {
            System.err.println("Shader compilation failed:\n" + floorShader.getLog());
        }
    }
}
