package game.shootergame.Renderer;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
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
    Mesh meshLeft;
    Mesh meshRight;

    int screenX;

    float[] rayData;
    float[] rayWallTex;
    float[] rayData2;

    
    float camX = 0.0f;
    float camY = 0.0f;

    
    float yaw = 0.0f;

    
    float fov = 90.0f;

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
        walls.get(0).textureID = 1.0f;

        TextureParameter param = new TextureParameter();
        param.minFilter = TextureFilter.Nearest;
        param.magFilter = TextureFilter.Nearest;

        ShooterGame.getInstance().am.load("brickwall.jpg", Texture.class, param);
        ShooterGame.getInstance().am.load("brick.png", Texture.class, param);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("brickwall.jpg", Texture.class);
        floor = ShooterGame.getInstance().am.get("brick.png", Texture.class);
        tex.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        floor.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

        resize(screnX, 0);

        //splitting the mesh into left and right halfs for more scene data
        meshLeft = new Mesh(true, 4, 6,
         new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
         new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
         meshLeft.setVertices(new float[] {
            -1, -1, 0, 0,
            0, 1, 1, 1,
            -1, 1, 0, 1,
            0, -1, 1, 0
        });
        meshLeft.setIndices(new short[] {
            0, 1, 2, 1, 0, 3
        });

        meshRight = new Mesh(true, 4, 6,
        new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
        new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        meshRight.setVertices(new float[] {
           0, -1, 0, 0,
           1, 1, 1, 1,
           0, 1, 0, 1,
           1, -1, 1, 0
        });
        meshRight.setIndices(new short[] {
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
        public float textureID;
        Wall(float xa, float ya, float xb, float yb) {
             a = new Vector2(xa, ya); b = new Vector2(xb, yb);
             widthScaler = a.dst(b);
             yOffset = 0.0f;
             height = 1.0f;
             textureID = 0.0f;
        }
    }


    private void rayCast(int index) {
        float yawR = (float)Math.toRadians(yaw);
        Vector2 forward = new Vector2((float)Math.cos(yawR), (float)Math.sin(yawR));
        Vector2 right = new Vector2(forward.y, -forward.x);
        float halfWidth = (float)Math.tan(Math.toRadians(fov * 0.5f));
        float offset = ((screenX - index) * 2.0f / (screenX - 1.0f)) - 1.0f;

        float dX = forward.x + offset * right.x * halfWidth;
        float dY = forward.y + offset * right.y * halfWidth;
        Vector2 rayDir = new Vector2(dX, dY).nor();
        Vector2 rayOrigin = new Vector2(camX, camY);

        float minDistance = Float.MAX_VALUE;
        float deltaX = 0.0f;
        float yOffset = 0.0f;
        float height = 1.0f;
        float textureID = 0.0f;
        Vector2 hitPos = new Vector2(Float.MAX_VALUE, Float.MAX_VALUE);
        for (Wall wall : walls) {
            Vector2 segDir = new Vector2(wall.a).sub(wall.b);
            Vector2 segToRay = new Vector2(wall.b).sub(rayOrigin);
            float crossDir = rayDir.crs(segDir);
            float t = segToRay.crs(segDir) / crossDir;
            float u = segToRay.crs(rayDir) / crossDir;
            if(crossDir != 0 && t >= 0 && u >= 0 && u <= 1) {
                float dst = t * rayDir.len();
                if(dst < minDistance) {
                    hitPos = wall.b.cpy().lerp(wall.a, u);
                    minDistance = dst;
                    deltaX = u * wall.widthScaler / 4.0f;
                    yOffset = wall.yOffset;
                    height = wall.height;
                    textureID = wall.textureID;
                }
            }
        }
        
        float dst = hitPos.sub(rayOrigin).dot(forward);
        float idst = 1.0f / dst;
        float wallTop = (height * 2.0f + yOffset - 1.0f) * idst;
        float wallBottom = (yOffset - 1.0f) * idst;

        rayData[index * 4] = height;
        rayData[index * 4 + 1] = deltaX;
        rayData[index * 4 + 2] = wallTop;
        rayData[index * 4 + 3] = wallBottom;

        rayWallTex[index * 2] = textureID;
        rayWallTex[index * 2 + 1] = idst;

        rayData2[index * 4] = dX; //specifically using the non normalized rays for distance stretching
        rayData2[index * 4 + 1] = dY;
        rayData2[index * 4 + 2] = wallTop;
        rayData2[index * 4 + 3] = wallBottom;
    }
    float wx = 0.0f;
    
    public void update(float x, float y, float rotation) {
        camX = x; camY = y; yaw = rotation;
    }

    public void render() {
        wx += 1.0f / 144.0f;

        walls.get(0).yOffset = (float)Math.sin(wx) + 1.0f;

        walls.get(0).height = 1.0f - walls.get(0).yOffset / 2.0f;

        float yawR = (float)Math.toRadians(yaw);
        
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


        tex.bind(0);
        floor.bind(1);

        wallShader.bind();

        wallShader.setUniformi("texture0", 0);
        wallShader.setUniformi("texture1", 1);

        wallShader.setUniformf("numRays", rayData.length / 4 / 2);
        wallShader.setUniformf("cameraInfo", camX, camY, aspect, 0);

        //render the screen one half at a time so we can have more uniform slots
        wallShader.setUniform4fv("rayData", rayData, 0, rayData.length / 2);
        wallShader.setUniform2fv("rayTex", rayWallTex, 0, rayWallTex.length / 2);
        meshLeft.render(wallShader, GL20.GL_TRIANGLES);
        wallShader.setUniform4fv("rayData", rayData, rayData.length / 2, rayData.length / 2);
        wallShader.setUniform2fv("rayTex", rayWallTex, rayWallTex.length / 2, rayWallTex.length / 2);
        meshRight.render(wallShader, GL20.GL_TRIANGLES);

        floorShader.bind();

        floorShader.setUniformi("texture", 1);

        floorShader.setUniformf("numRays", rayData.length / 4 / 2);
        floorShader.setUniformf("cameraInfo", camX, camY, yawR, 0.0f);
        
        floorShader.setUniform4fv("rayData", rayData2, 0, rayData.length / 2);
        meshLeft.render(floorShader, GL20.GL_TRIANGLES);
        floorShader.setUniform4fv("rayData", rayData2, rayData.length / 2, rayData.length / 2);
        meshRight.render(floorShader, GL20.GL_TRIANGLES);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
    }

    public void resize(int x, int y) {
        aspect = (float)x / (float)y;
        screenX = x;
        if(screenX > 2040) {
            screenX = 2040;
        }

        int numRayData = screenX / 2;

        rayData = new float[screenX * 4];
        rayWallTex = new float[screenX * 2];
        rayData2 = new float[screenX * 4];

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
            + "uniform vec4 rayData[" + numRayData + "];\n"
            + "uniform vec2 rayTex[" + numRayData + "];\n"
            + "uniform float numRays;\n"
            + "uniform vec4 cameraInfo;\n" //x y pos, z aspect, w tanhalffov
            + "uniform sampler2D texture0;\n"
            + "uniform sampler2D texture1;\n"
            + "void main()\n"
            + "{\n"
            + "  int index = int(v_texCoords.x * numRays);\n"
            + "  vec4 dat = rayData[index];\n"
            + "  float wallTop = dat.z;\n"
            + "  float wallBottom = dat.w;\n"
            + "  float current = v_texCoords.y - 0.5;"
            + "  bool isWall = current > wallBottom && current < wallTop;\n"
            + "  float color = isWall ? 1.0 : 0.0;\n"
            + "  float texY = dat.x * (current - wallBottom) / (wallTop - wallBottom);\n"
            + "  vec2 texCoords = vec2(cameraInfo.z * dat.y, texY);\n"
            + "  vec3 texColor;\n"
            + "  switch(rayTex[index].x) {\n"
            + "  case 0.0: texColor = texture2D(texture0, texCoords).rgb; break;\n"
            + "  case 1.0: texColor = texture2D(texture1, texCoords).rgb; break;\n"
            + "  }\n"
            + "  float dst = max(1.0 - rayTex[index].y, 0.0);\n"
            + "  gl_FragColor = vec4(mix(texColor, vec3(0.0, 0.0, 0.0), dst * dst) * color, 1.0);\n"
            + "}\n";

        String floorFragmentShader = 
            "#ifdef GL_ES\n"
          + "precision mediump float;\n"
          + "#endif\n"
          + "varying vec2 v_texCoords;\n"
          + "uniform vec4 rayData[" + numRayData + "];\n"
          + "uniform float numRays;\n"
          + "uniform vec4 cameraInfo;\n"
          + "uniform sampler2D texture;\n"
          + "void main()\n"
          + "{\n"
          + "  vec2 screenPos = v_texCoords * 2.0 - 1.0;"
          + "  vec4 dat = rayData[int(v_texCoords.x * numRays)];\n"
          + "  float wallTop = dat.z;\n"
          + "  float wallBottom = dat.w;\n"
          + "  float current = v_texCoords.y - 0.5;\n"
          + "  bool isWall = current > wallBottom && current < wallTop;\n"
          + "  vec2 worldDir = vec2(dat.x, dat.y) * abs(1.0 / screenPos.y);\n"
          + "  vec2 worldPos = worldDir + (cameraInfo.xy * 0.5);\n"
          + "  float dst = 1.0 - abs(screenPos.y);\n"
          + "  vec3 fColor = mix(texture2D(texture, worldPos).rgb, vec3(0.0, 0.0, 0.0), dst * dst);\n"
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
