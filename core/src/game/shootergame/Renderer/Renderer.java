package game.shootergame.Renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;

import game.shootergame.ShooterGame;
import game.shootergame.Wall;

public class Renderer {

    ShaderProgram wallShader;
    ShaderProgram floorShader;
    Mesh meshLeft;
    Mesh meshRight;

    int screenX;

    float[] rayData;
    float[] rayDoorData;
    float[] rayWallTex;
    float[] rayDoorTex;

    float[] rayFloorData;

    
    float camX = 0.0f;
    float camY = 0.0f;

    
    float yaw = 0.0f;

    
    float fov = 90.0f;

    float aspect;

    int numWorkers;
    CyclicBarrier startBarrier;
    CyclicBarrier doneBarrier;
    Thread[] workers;
    AtomicBoolean running;

    Texture tex;
    Texture floor;
    Texture door;


    ArrayList<Wall> walls;

    
    LinkedList<Sprite2_5D> sprites2_5d = new LinkedList<>();

    boolean debugRayDraw = false;
    ShapeRenderer sr = new ShapeRenderer();

    static Renderer instance;
    public static Renderer createInstance(int screenX) {
        if(instance == null) {
            instance = new Renderer(screenX);
        } else {
            System.err.println("Renderer already has running instance!");
        }
        return instance;
    }

    public static Renderer inst() {
        return instance;
    }

    public void setWalls(ArrayList<Wall> walls) {
        this.walls = walls;
    }

    private Renderer(int screnX) {
        this.walls = new ArrayList<>();
        TextureParameter param = new TextureParameter();
        param.minFilter = TextureFilter.Linear;
        param.magFilter = TextureFilter.Linear;

        ShooterGame.getInstance().am.load("brickwall.jpg", Texture.class, param);
        ShooterGame.getInstance().am.load("brick.png", Texture.class, param);
        ShooterGame.getInstance().am.load("transparent.png", Texture.class, param);
        ShooterGame.getInstance().am.finishLoading();
        tex = ShooterGame.getInstance().am.get("brickwall.jpg", Texture.class);
        floor = ShooterGame.getInstance().am.get("brick.png", Texture.class);
        door = ShooterGame.getInstance().am.get("transparent.png", Texture.class);
        tex.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        floor.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        door.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

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
        startBarrier = new CyclicBarrier(numWorkers + 1 /* +main thread */);
        doneBarrier = new CyclicBarrier(numWorkers + 1 /* +main thread */);
        workers = new Thread[numWorkers];

        for (int i = 0; i < numWorkers; i++) {
            final int idx = i;
            workers[i] = new Thread(() -> {
                try {
                    while (running.get()) {
                        int numPerThread = screenX / numWorkers;
                        int numTasks = screenX % numWorkers;
                        int start = idx * numPerThread + Math.min(idx, numTasks);
                        int end = (idx + 1) * numPerThread + Math.min(idx + 1, numTasks);
                        startBarrier.await();

                        for (int j = start; j < end; j++) {
                            rayCast(j);
                        }

                        doneBarrier.await();
                    }
                    System.out.println("Render thread: " + idx + " shutting down.");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers[i].start();
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
        Wall hitWall = null;
        float dU = 0.0f;

        float minDistanceDoor = Float.MAX_VALUE;
        Wall hitWallDoor = null;
        float dUDoor = 0.0f;

        for (Wall wall : walls) {
            Vector2 segDir = new Vector2(wall.a).sub(wall.b);
            Vector2 segToRay = new Vector2(wall.b).sub(rayOrigin);
            float crossDir = rayDir.crs(segDir);
            float t = segToRay.crs(segDir) / crossDir;
            float u = segToRay.crs(rayDir) / crossDir;
            if(crossDir != 0 && t >= 0 && u >= 0 && u <= 1) {

                float dst = t * rayDir.len();
                if(wall.transparentDoor) {
                    if(dst < minDistanceDoor) {
                        hitWallDoor = wall;
                        minDistanceDoor = dst;
                        dUDoor = u;
                    }
                } else {
                    if(dst < minDistance) {
                        hitWall = wall;
                        minDistance = dst;
                        dU = u;
                    }  
                }
            }
        }

        if(hitWallDoor != null && minDistanceDoor < minDistance) {
            float yOffset = hitWallDoor.yOffset;
            Vector2 hitPos = hitWallDoor.b.cpy().lerp(hitWallDoor.a, dUDoor);
            float dst = hitPos.sub(rayOrigin).dot(forward);
            float idst = 1.0f / dst;
            float wallTop = (hitWallDoor.height * 2.0f + yOffset - 1.0f) * idst;
            float wallBottom = (yOffset - 1.0f) * idst;
    
            rayDoorData[index * 4] = hitWallDoor.height;
            rayDoorData[index * 4 + 1] = dUDoor * hitWallDoor.widthScaler / 4.0f;
            rayDoorData[index * 4 + 2] = wallTop;
            rayDoorData[index * 4 + 3] = wallBottom;
    
            rayDoorTex[index * 2] = hitWallDoor.textureID;
            rayDoorTex[index * 2 + 1] = idst;
        } else {
            rayDoorData[index * 4 + 2] = 0;
            rayDoorData[index * 4 + 3] = 0;
        }


        if(hitWall != null) {
            float yOffset = hitWall.yOffset;
            Vector2 hitPos = hitWall.b.cpy().lerp(hitWall.a, dU);
            float dst = hitPos.sub(rayOrigin).dot(forward);
            float idst = 1.0f / dst;
            float wallTop = (hitWall.height * 2.0f + yOffset - 1.0f) * idst;
            float wallBottom = (yOffset - 1.0f) * idst;
    
            rayData[index * 4] = hitWall.height;
            rayData[index * 4 + 1] = dU * hitWall.widthScaler / 4.0f;
            rayData[index * 4 + 2] = wallTop;
            rayData[index * 4 + 3] = wallBottom;
    
            rayWallTex[index * 2] = hitWall.textureID;
            rayWallTex[index * 2 + 1] = idst;
        } else {
            rayData[index * 4 + 2] = 0;
            rayData[index * 4 + 3] = 0;
        }
        
        rayFloorData[index * 4] = dX; //specifically using the non normalized rays for distance stretching
        rayFloorData[index * 4 + 1] = dY;
        rayFloorData[index * 4 + 2] = 0;
        rayFloorData[index * 4 + 3] = 0;
    }

    public void setDebugRayDraw(boolean debugRayDraw) {
        this.debugRayDraw = debugRayDraw;
    }
    
    public void update(float x, float y, float rotation) {
        camX = x; camY = y; yaw = rotation;
    }

    public void render() {
        float yawR = (float)Math.toRadians(yaw);

        try {
            startBarrier.await();
            doneBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }

        tex.bind(0);
        floor.bind(1);
        door.bind(2);

        floorShader.bind();

        floorShader.setUniformi("texture", 1);

        floorShader.setUniformf("numRays", rayData.length / 4 / 2);
        floorShader.setUniformf("cameraInfo", camX, camY, yawR, 0.0f);
        
        floorShader.setUniform4fv("rayData", rayFloorData, 0, rayData.length / 2);
        meshLeft.render(floorShader, GL20.GL_TRIANGLES);
        floorShader.setUniform4fv("rayData", rayFloorData, rayData.length / 2, rayData.length / 2);
        meshRight.render(floorShader, GL20.GL_TRIANGLES);

        wallShader.bind();

        wallShader.setUniformi("texture0", 0);
        wallShader.setUniformi("texture1", 2);

        wallShader.setUniformf("numRays", rayData.length / 4 / 2);
        wallShader.setUniformf("cameraInfo", camX, camY, aspect, 0);

        //render the screen one half at a time so we can have more uniform slots
        wallShader.setUniform4fv("rayData", rayData, 0, rayData.length / 2);
        wallShader.setUniform2fv("rayTex", rayWallTex, 0, rayWallTex.length / 2);
        meshLeft.render(wallShader, GL20.GL_TRIANGLES);
        wallShader.setUniform4fv("rayData", rayData, rayData.length / 2, rayData.length / 2);
        wallShader.setUniform2fv("rayTex", rayWallTex, rayWallTex.length / 2, rayWallTex.length / 2);
        meshRight.render(wallShader, GL20.GL_TRIANGLES);

        wallShader.setUniform4fv("rayData", rayDoorData, 0, rayDoorData.length / 2);
        wallShader.setUniform2fv("rayTex", rayDoorTex, 0, rayDoorTex.length / 2);
        meshLeft.render(wallShader, GL20.GL_TRIANGLES);
        wallShader.setUniform4fv("rayData", rayDoorData, rayDoorData.length / 2, rayDoorData.length / 2);
        wallShader.setUniform2fv("rayTex", rayDoorTex, rayDoorTex.length / 2, rayDoorTex.length / 2);
        meshRight.render(wallShader, GL20.GL_TRIANGLES);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        sr.setProjectionMatrix(ShooterGame.getInstance().coreCamera.combined);
        float scale = 50.0f;
        float wx = 15.0f / scale;
        float wy = 15.0f / scale;
        float offsetX = ((float)Gdx.graphics.getWidth() / (float)Gdx.graphics.getHeight()) - 0.33f, offsetY = 0.67f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeType.Filled);
        sr.setColor(0.0f, 0.0f, 0.0f, 0.3f);
        sr.rect(-wx + offsetX, -wy + offsetY, wx * 2, wy * 2);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        sr.begin(ShapeType.Line);

        if(debugRayDraw) {
            for (int i = 0; i < screenX; i++) {
                Color c = new Color(0xffa500ff);
                sr.setColor(c.lerp(Color.CYAN, (float)i / (float)screenX));
                float idst = 1.0f / rayWallTex[i*2+1];
                float dx = rayFloorData[i*4];
                float dy = rayFloorData[i*4+1];


                float tx = wx / Math.abs(dx);
                float ty = wy / Math.abs(dy);
                float t = Math.min(tx, ty) * scale;
                if(t < idst) {
                    idst = t;
                }

                float x = (-dx * idst / scale);
                float y = (dy * idst / scale);

                sr.line(offsetX, offsetY, x + offsetX, y + offsetY);
            }
        }

        for (Wall wall : walls) {
            if(wall.transparentDoor) {
                sr.setColor(Color.RED);
            } else {
                sr.setColor(Color.WHITE);
            }
            float ax = (-wall.a.x - -camX) / scale;
            float ay = (wall.a.y - camY) / scale;
            float bx = (-wall.b.x - -camX) / scale;
            float by = (wall.b.y - camY) / scale;
            if(Math.abs(ax) > wx && Math.abs(bx) > wx)
                continue;
            if(Math.abs(ay) > wy && Math.abs(by) > wy)
                continue;
                
            ax = Math.min(Math.max(ax, -wx), wx);
            ay = Math.min(Math.max(ay, -wy), wy);
            bx = Math.min(Math.max(bx, -wx), wx);
            by = Math.min(Math.max(by, -wy), wy);

            sr.line(ax + offsetX, ay + offsetY, bx + offsetX, by + offsetY);
        }
        sr.setColor(Color.GRAY);
        sr.line(-wx + offsetX, -wy + offsetY, -wx + offsetX, wy + offsetY);
        sr.line(-wx + offsetX, wy + offsetY, wx + offsetX, wy + offsetY);
        sr.line(wx + offsetX, wy + offsetY, wx + offsetX, -wy + offsetY);
        sr.line(-wx + offsetX, -wy + offsetY, wx + offsetX, -wy + offsetY);
        sr.setColor(Color.GREEN);
        sr.circle(offsetX, offsetY, 0.01f, 8);
        sr.line(offsetX, offsetY, offsetX + -(float)Math.cos(yawR) / 25, offsetY + (float)Math.sin(yawR) / 25);

        sr.end();
    }

    public void processSpriteDraws() {

        float yawR = (float)Math.toRadians(yaw);
        Vector2 forward = new Vector2((float)Math.cos(yawR), (float)Math.sin(yawR)).scl(0.01f);//scaling so our "near" plane is closer
        Vector2 rightV = new Vector2(forward.y, -forward.x);
        float halfWidth = (float)Math.tan(Math.toRadians(fov * 0.5f));

        Vector2 leftPoint = new Vector2(
            forward.x + -1.0f * rightV.x * halfWidth,
            forward.y + -1.0f * rightV.y * halfWidth
        );

        Vector2 rightPoint = new Vector2(
            forward.x + 1.0f * rightV.x * halfWidth,
            forward.y + 1.0f * rightV.y * halfWidth
        );

        Vector2 lineVec = rightPoint.cpy().sub(leftPoint);

        for (Sprite2_5D sprite : sprites2_5d) {

            //ray intersect sprite back to cameras projected "plane", i.e. the same plane we used to cast the rays for the walls
            Vector2 fromSprite = new Vector2(camX - sprite.x, camY - sprite.y).nor();
            float denom = lineVec.x * fromSprite.y - lineVec.y * fromSprite.x;
            float t = ((sprite.y - leftPoint.y) * fromSprite.x - (sprite.x - leftPoint.x) * fromSprite.y) / denom;

            Vector2 intersectPoint = leftPoint.cpy().add(lineVec.cpy().scl(t));
            float visualDistance = intersectPoint.sub(camX, camY).dot(forward) * 100.0f;//rescaling distance from our "near" plane scaling
            if(visualDistance <= 0.0f) {
                sprite.isVis = false;
                continue;
            }

            sprite.dst = Vector2.dst(sprite.x, sprite.y, camX, camY);
            sprite.visHeight = sprite.height / visualDistance;
            sprite.visWidth = sprite.width / visualDistance;
            
            sprite.scrX = aspect * 2.0f * (t + 0.5f);
            sprite.scrY = -1.0f / visualDistance;

            float left = sprite.scrX - sprite.visWidth / 2.0f;
            float right = sprite.scrX + sprite.visWidth / 2.0f;

            sprite.isVis = !(Math.abs(left) > aspect && Math.abs(right) > aspect);
            
            if(sprite.isVis) {
                int leftRayIndex = (int)((left + aspect) / (aspect * 2) * (screenX - 1));
                int rightRayIndex = (int)((right + aspect) / (aspect * 2) * (screenX - 1));
                
                float deltaU = sprite.texture.getU2() - sprite.texture.getU();
                float raysPerU = (deltaU / (float)(rightRayIndex - leftRayIndex));
                float raysPerWidth = (sprite.visWidth / (float)(rightRayIndex - leftRayIndex));

                //check our rays from the left
                int stopIndexLeft = leftRayIndex;
                for (int i = leftRayIndex; i <= rightRayIndex; i++) {
                    float idst = 1.0f / rayWallTex[Math.max(Math.min(i, (screenX - 1)), 0) * 2 + 1];
                    float sdst = sprite.dst;
                    if(idst > sdst) {
                        stopIndexLeft = i;
                        break;
                    }
                }
                //and from right
                int stopIndexRight = rightRayIndex;
                for (int i = rightRayIndex; i >= stopIndexLeft; i--) {
                    float idst = 1.0f / rayWallTex[Math.max(Math.min(i, (screenX - 1)), 0) * 2 + 1];
                    float sdst = sprite.dst;
                    if(i == stopIndexLeft) { sprite.isVis = false; }//ray stopped at left stop so object is completely behind wall
                    if(idst > sdst) {
                        stopIndexRight = i;
                        break;
                    }
                }

                sprite.textureCalc.setU(sprite.texture.getU() + (stopIndexLeft - leftRayIndex) * raysPerU);
                sprite.textureCalc.setU2(sprite.texture.getU2() - (rightRayIndex - stopIndexRight) * raysPerU);
                
                left = ((float)(stopIndexLeft - (screenX / 2))) * raysPerWidth;
                right = ((float)(stopIndexRight - (screenX / 2))) * raysPerWidth;
                sprite.scrX = (left + right) / 2.0f;
                sprite.visWidth = (right - left);
            }
        }
        Collections.sort(sprites2_5d, Comparator.comparingDouble(Sprite2_5D::getDst).reversed());

        for (Sprite2_5D sprite : sprites2_5d) {
            if(sprite.isVis) {
                float idst = 1.0f / sprite.dst;
                float dstColor = idst;
                ShooterGame.getInstance().coreBatch.setColor(dstColor, dstColor, dstColor, 1.0f);
                ShooterGame.getInstance().coreBatch.draw(sprite.textureCalc, sprite.scrX - (sprite.visWidth / 2.0f), sprite.scrY - (sprite.visHeight / 2.0f), sprite.visWidth, sprite.visHeight);
            }
        }
    }

    public void addSprite(Sprite2_5D sprite) {
        sprites2_5d.add(sprite);
    }

    public void removeSprite(Sprite2_5D sprite) {
        sprites2_5d.remove(sprite);
    }

    public void resize(int x, int y) {
        aspect = (float)x / (float)y;
        screenX = x;
        if(screenX > 1020) {
            screenX = 1020;
        }

        int numRayData = screenX / 2;

        rayData = new float[screenX * 4];
        rayDoorData = new float[screenX * 4];
        rayWallTex = new float[screenX * 2];
        rayDoorTex = new float[screenX * 2];
        rayFloorData = new float[screenX * 4];

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
            + "  float texY = dat.x * (current - wallBottom) / (wallTop - wallBottom);\n"
            + "  vec2 texCoords = vec2(cameraInfo.z * dat.y, texY);\n"
            + "  vec4 texColor;\n"
            + "  float texID = rayTex[index].x;\n"
            + "  if(texID == 0.0) { texColor = texture2D(texture0, texCoords); }\n"
            + "  else if(texID == 1.0) { texColor = texture2D(texture1, texCoords); }\n"
            + "  float dst = max(1.0 - rayTex[index].y, 0.0);\n"
            + "  if(isWall && texColor.a > 0.01) {\n"
            + "      gl_FragColor = vec4(mix(texColor.rgb, vec3(0.0, 0.0, 0.0), dst * dst), 1.0);\n"
            + "  } else { discard; }\n"
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
          + "  vec2 worldDir = vec2(dat.x, dat.y) * abs(1.0 / screenPos.y);\n"
          + "  vec2 worldPos = worldDir + (cameraInfo.xy * 0.5);\n"
          + "  float dst = 1.0 - abs(screenPos.y);\n"
          + "  vec3 fColor = mix(texture2D(texture, worldPos).rgb, vec3(0.0, 0.0, 0.0), dst * dst);\n"
          + "  gl_FragColor = vec4(fColor, 1.0);\n"
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

    public void shutdown() {
        running.set(false);

        //force threads to loop to make them exit
        try {
            startBarrier.await();
            doneBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
