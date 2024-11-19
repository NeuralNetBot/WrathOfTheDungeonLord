package game.shootergame.Renderer;

import java.util.ArrayList;
import java.util.Arrays;
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
    Mesh meshFull;

    int screenX;

    float[] rayData;
    float[] rayDoorData;
    float[] rayWallTex;
    float[] rayDoorTex;

    final int MAX_SHADER_TORCHES = 64;
    float[] torchData;
    float[] torchWallData;

    
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
    ArrayList<Torch> torches;

    private class TorchAndIndex {
        public Torch torch;
        public int index;
        public TorchAndIndex(Torch torch, int index) { this.torch = torch; this.index = index; }
    }

    ArrayList<ArrayList<TorchAndIndex>> wallToTorchMap;

    private class OcclusionWall {
        public float ax, ay, bx, by;
        public float dst;
        public float getDst() { return dst; }
        public OcclusionWall(float dst) { this.dst = dst; }
    }
    final int MAX_OCCLUSION_WALS = 6;
    ArrayList<OcclusionWall[]> torchToWallMap;
    
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

        meshFull = new Mesh(true, 4, 6,
        new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
        new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
        meshFull.setVertices(new float[] {
           -1, -1, 1, -1,
           1, 1, -1, 1,
           -1, 1, 1, 1,
           1, -1, -1, -1
        });
        meshFull.setIndices(new short[] {
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
        int hitWallIdx = 0;
        float dU = 0.0f;

        float minDistanceDoor = Float.MAX_VALUE;
        Wall hitWallDoor = null;
        int hitWallDoorIdx = 0;
        float dUDoor = 0.0f;

        int i = 0;
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
                        hitWallDoorIdx = i;
                        minDistanceDoor = dst;
                        dUDoor = u;
                    }
                } else {
                    if(dst < minDistance) {
                        hitWall = wall;
                        hitWallIdx = i;
                        minDistance = dst;
                        dU = u;
                    }  
                }
            }
            i++;
        }
        float doorBottom = Float.MAX_VALUE;
        if(hitWallDoor != null && minDistanceDoor < minDistance) {
            float yOffset = hitWallDoor.yOffset;
            Vector2 hitPos = hitWallDoor.b.cpy().lerp(hitWallDoor.a, dUDoor);
            float dst = hitPos.cpy().sub(rayOrigin).dot(forward);
            float idst = 1.0f / dst;
            float wallTop = (hitWallDoor.height * 2.0f + yOffset - 1.0f) * idst;
            float wallBottom = (yOffset - 1.0f) * idst;
    
            rayDoorData[index * 4] = hitWallDoor.height;
            rayDoorData[index * 4 + 1] = dUDoor * hitWallDoor.widthScaler / 4.0f;
            rayDoorData[index * 4 + 2] = wallTop;
            rayDoorData[index * 4 + 3] = wallBottom;
            if(hitWallDoor.yOffset >= 2.0f) {
                doorBottom = wallBottom;
            }
    
            rayDoorTex[index * 4] = hitWallDoor.textureID;
            rayDoorTex[index * 4 + 1] = idst;

            //calculate light info
            ArrayList<TorchAndIndex> wallTorches = wallToTorchMap.get(hitWallDoorIdx);
            float closestDst = Float.MAX_VALUE;
            TorchAndIndex closestTorch = new TorchAndIndex(new Torch(0, 0, 0), 0);
            if(wallTorches != null) {
                for (TorchAndIndex torch : wallTorches) {
                    float torchDst = hitPos.dst(torch.torch.x, torch.torch.y);
                    
                    OcclusionWall[] oWalls = torchToWallMap.get(torch.index);
                    boolean intersects = false;    
                    for (OcclusionWall occlusionWall : oWalls) {
                        if(occlusionWall.ax == hitWallDoor.a.x && occlusionWall.ay == hitWallDoor.a.y && occlusionWall.bx == hitWallDoor.b.x && occlusionWall.by == hitWallDoor.b.y)
                            continue;
                        Vector2 wa = new Vector2(occlusionWall.ax, occlusionWall.ay);
                        Vector2 wb = new Vector2(occlusionWall.bx, occlusionWall.by);
                        Vector2 torchPos = new Vector2(torch.torch.x, torch.torch.y);
                        int o1 = orientation(wa, wb, hitPos);
                        int o2 = orientation(wa, wb, torchPos);
                        int o3 = orientation(hitPos, torchPos, wa);
                        int o4 = orientation(hitPos, torchPos, wb);
                        if(o1 != o2 && o3 != o4) { intersects = true; break; }   
                    }
                    if(intersects)
                        continue;

                    if(torchDst < closestDst) {
                        closestDst = torchDst;
                        closestTorch = torch;
                    }
                }
            }
            rayDoorTex[index * 4 + 2] = wallTop;//unmodified wall top for lighting calc
            rayDoorTex[index * 4 + 3] = closestDst * closestDst / (closestTorch.torch.radius * closestTorch.torch.radius);
        } else {
            rayDoorData[index * 4 + 2] = 0;
            rayDoorData[index * 4 + 3] = 0;
        }


        if(hitWall != null) {
            float yOffset = hitWall.yOffset;
            Vector2 hitPos = hitWall.b.cpy().lerp(hitWall.a, dU);
            float dst = hitPos.cpy().sub(rayOrigin).dot(forward);
            float idst = 1.0f / dst;
            float wallTop = (hitWall.height * 2.0f + yOffset - 1.0f) * idst;
            float wallBottom = (yOffset - 1.0f) * idst;
    
            rayData[index * 4] = hitWall.height;
            rayData[index * 4 + 1] = dU * hitWall.widthScaler / 4.0f;
            float wallTopWithDoor = Math.min(wallTop, doorBottom);
            rayData[index * 4 + 2] = wallTopWithDoor;
            rayData[index * 4 + 3] = wallBottom;
    
            rayWallTex[index * 4] = hitWall.textureID;
            rayWallTex[index * 4 + 1] = idst;

            //calculate light info
            ArrayList<TorchAndIndex> wallTorches = wallToTorchMap.get(hitWallIdx);   
            float closestDst = Float.MAX_VALUE;
            TorchAndIndex closestTorch = new TorchAndIndex(new Torch(0, 0, 0), 0);
            if(wallTorches != null) {
                for (TorchAndIndex torch : wallTorches) {
                    float torchDst = hitPos.dst(torch.torch.x, torch.torch.y);
                    
                    if(torchDst < closestDst) {

                        OcclusionWall[] oWalls = torchToWallMap.get(torch.index);
                        boolean intersects = false;    
                        for (OcclusionWall occlusionWall : oWalls) {
                            if(occlusionWall.ax == hitWall.a.x && occlusionWall.ay == hitWall.a.y && occlusionWall.bx == hitWall.b.x && occlusionWall.by == hitWall.b.y)
                                continue;
                            Vector2 wa = new Vector2(occlusionWall.ax, occlusionWall.ay);
                            Vector2 wb = new Vector2(occlusionWall.bx, occlusionWall.by);
                            Vector2 torchPos = new Vector2(torch.torch.x, torch.torch.y);
                            int o1 = orientation(wa, wb, hitPos);
                            int o2 = orientation(wa, wb, torchPos);
                            int o3 = orientation(hitPos, torchPos, wa);
                            int o4 = orientation(hitPos, torchPos, wb);
                            if(o1 != o2 && o3 != o4) { intersects = true; break; }   
                        }
                        if(intersects)
                            continue;

                        closestDst = torchDst;
                        closestTorch = torch;
                    }
                }
            }

            rayWallTex[index * 4 + 2] = wallTop;//unmodified wall top for lighting calc
            rayWallTex[index * 4 + 3] = closestDst * closestDst / (closestTorch.torch.radius * closestTorch.torch.radius);
        } else {
            rayData[index * 4 + 2] = 0;
            rayData[index * 4 + 3] = 0;
        }
    }

    private int orientation(Vector2 a, Vector2 b, Vector2 c) {
        float d = (b.y - a.y) * (c.x - b.x) - (b.x - a.x) * (c.y - b.y);
        if(d == 0.0) return 0;
        return d > 0.0 ? 1 : 2;
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

        int torchCount = buildLightFrustums();

        tex.bind(0);
        floor.bind(1);
        door.bind(2);

        floorShader.bind();

        floorShader.setUniform4fv("torchData", torchData, 0, torchData.length);
        floorShader.setUniform4fv("torchWallData", torchWallData, 0, torchWallData.length);
        floorShader.setUniformi("torchCount", torchCount);

        floorShader.setUniformi("texture", 1);

        floorShader.setUniformf("cameraInfo", camX, camY, yawR, (float)Math.tan(Math.toRadians(fov * 0.5f)));
        Vector2 forward = new Vector2((float)Math.cos(yawR), (float)Math.sin(yawR));
        floorShader.setUniformf("cameraInfo2", forward.x, forward.y);
        
        meshFull.render(floorShader, GL20.GL_TRIANGLES);

        wallShader.bind();

        wallShader.setUniformi("texture0", 0);
        wallShader.setUniformi("texture1", 2);

        wallShader.setUniformf("numRays", rayData.length / 4 / 2);
        wallShader.setUniformf("cameraInfo", camX, camY, aspect, 0);

        //render the screen one half at a time so we can have more uniform slots
        wallShader.setUniform4fv("rayData", rayData, 0, rayData.length / 2);
        wallShader.setUniform4fv("rayTex", rayWallTex, 0, rayWallTex.length / 2);
        meshLeft.render(wallShader, GL20.GL_TRIANGLES);
        wallShader.setUniform4fv("rayData", rayData, rayData.length / 2, rayData.length / 2);
        wallShader.setUniform4fv("rayTex", rayWallTex, rayWallTex.length / 2, rayWallTex.length / 2);
        meshRight.render(wallShader, GL20.GL_TRIANGLES);

        wallShader.setUniform4fv("rayData", rayDoorData, 0, rayDoorData.length / 2);
        wallShader.setUniform4fv("rayTex", rayDoorTex, 0, rayDoorTex.length / 2);
        meshLeft.render(wallShader, GL20.GL_TRIANGLES);
        wallShader.setUniform4fv("rayData", rayDoorData, rayDoorData.length / 2, rayDoorData.length / 2);
        wallShader.setUniform4fv("rayTex", rayDoorTex, rayDoorTex.length / 2, rayDoorTex.length / 2);
        meshRight.render(wallShader, GL20.GL_TRIANGLES);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
   }

    public void renderMinimap() {
        float yawR = (float)Math.toRadians(yaw);

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
            /*
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
            }*/
        }
        //Cohen-Sutherland box fitting algorithm
        //https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm
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
            int codea = findPointRegion(ax, ay, -wx, -wy, wx, wy);
            int codeb = findPointRegion(bx, by, -wx, -wy, wx, wy);
            while(true) {
                if(codea == 0 && codeb == 0) {
                    sr.line(ax + offsetX, ay + offsetY, bx + offsetX, by + offsetY);
                    break;
                } else if((codea & codeb) != 0) {
                    break;
                } else {
                    int codeOut;
                    if(codea != 0) {
                        codeOut = codea;
                    } else {
                        codeOut = codeb;
                    }
                    float x = 0, y = 0;
                    if((codeOut & RegionCode.TOP.bits) > 1) {
                        x = ax + (bx - ax) * (wy - ay) / (by - ay);
                        y = wy;
                    } else if((codeOut & RegionCode.BOTTOM.bits) > 1) {  
                        x = ax + (bx - ax) * (-wy - ay) / (by - ay);
                        y = -wy;
                    } else if((codeOut & RegionCode.RIGHT.bits) > 1) {
                        y = ay + (by - ay) * (wx - ax) / (bx - ax);
                        x = wx;
                    } else if((codeOut & RegionCode.LEFT.bits) > 1) {
                        y = ay + (by - ay) * (-wx - ax) / (bx - ax);
                        x = -wx;
                    }
                    if(codeOut == codea) {
                        ax = x; ay = y;
                        codea = findPointRegion(ax, ay, -wx, -wy, wx, wy);
                    } else {
                        bx = x; by = y;
                        codeb = findPointRegion(bx, by, -wx, -wy, wx, wy);
                    }
                }
            }
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

    private enum RegionCode {
        TOP(1 << 0),
        BOTTOM(1 << 1),
        LEFT(1 << 2),
        RIGHT(1 << 3);

        public final int bits;

        RegionCode(int id) {
            this.bits = 1 << id;
        }
    }
    private int findPointRegion(float x, float y, float xmin, float ymin, float xmax, float ymax) {
        int code = 0;
        if(y > ymax) code |= RegionCode.TOP.bits;
        if(y < ymin) code |= RegionCode.BOTTOM.bits;
        if(x > xmax) code |= RegionCode.RIGHT.bits;
        if(x < xmin) code |= RegionCode.LEFT.bits;
        return code;
    }

    public void processSpriteDraws() {

        float yawR = (float)Math.toRadians(yaw);
        Vector2 forward = new Vector2((float)Math.cos(yawR), (float)Math.sin(yawR)).cpy().scl(0.01f);//scaling so our "near" plane is closer
        Vector2 rightV = new Vector2(forward.y, -forward.x);
        float halfWidth = (float)Math.tan(Math.toRadians(fov * 0.5f));

        Vector2 leftPoint = new Vector2(
            forward.x + -1.0f * rightV.x * halfWidth,
            forward.y + -1.0f * rightV.y * halfWidth
        );
        leftPoint.add(camX, camY);

        Vector2 rightPoint = new Vector2(
            forward.x + 1.0f * rightV.x * halfWidth,
            forward.y + 1.0f * rightV.y * halfWidth
        );
        rightPoint.add(camX, camY);

        Vector2 lineVec = rightPoint.cpy().sub(leftPoint);

        for (Sprite2_5D sprite : sprites2_5d) {

            //ray intersect sprite back to cameras projected "plane", i.e. the same plane we used to cast the rays for the walls
            Vector2 fromSprite = new Vector2(camX - sprite.x, camY - sprite.y).nor();
            float denom = lineVec.x * fromSprite.y - lineVec.y * fromSprite.x;
            float t = ((sprite.y - leftPoint.y) * fromSprite.x - (sprite.x - leftPoint.x) * fromSprite.y) / denom;
            Vector2 intersectPoint = leftPoint.cpy().lerp(rightPoint, t);
            float visualDistance = -intersectPoint.sub(sprite.x, sprite.y).dot(forward) * 100.0f;//rescale distance from our near "plane" scaling

            if(visualDistance <= 0.0f) {
                sprite.isVis = false;
                continue;
            }

            sprite.dst = Vector2.dst(sprite.x, sprite.y, camX, camY);
            sprite.visHeight = sprite.height / visualDistance;
            sprite.visWidth = sprite.width / visualDistance;
            
            sprite.scrX = aspect * 2.0f * (t + 0.5f);
            sprite.scrY = sprite.z / visualDistance;

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
                    float idst = 1.0f / rayWallTex[Math.max(Math.min(i, (screenX - 1)), 0) * 4 + 1];
                    float sdst = visualDistance;
                    if(idst > sdst) {
                        stopIndexLeft = i;
                        break;
                    }
                }
                //and from right
                int stopIndexRight = rightRayIndex;
                for (int i = rightRayIndex; i >= stopIndexLeft; i--) {
                    float idst = 1.0f / rayWallTex[Math.max(Math.min(i, (screenX - 1)), 0) * 4 + 1];
                    float sdst = visualDistance;
                    if(i == stopIndexLeft) { sprite.isVis = false; break; }//ray stopped at left stop so object is completely behind wall
                    if(idst > sdst) {
                        stopIndexRight = i;
                        break;
                    }
                }
                if(!sprite.isVis) {
                    continue;
                }
                
                sprite.textureCalc.setU(sprite.texture.getU() + (stopIndexLeft - leftRayIndex) * raysPerU);
                left = left + (stopIndexLeft - leftRayIndex) * raysPerWidth;
                sprite.textureCalc.setU2(sprite.texture.getU2() - (rightRayIndex - stopIndexRight) * raysPerU);   
                right = right - (rightRayIndex - stopIndexRight) * raysPerWidth;


                sprite.scrX = (left + right) / 2.0f;
                sprite.visWidth = (right - left);
            }
        }
        Collections.sort(sprites2_5d, Comparator.comparingDouble(Sprite2_5D::getDst).reversed());

        for (Sprite2_5D sprite : sprites2_5d) {
            if(sprite.isVis) {
                //float idst = 1.0f / sprite.dst;
                float dstColor = 1.0f;//idst;
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

    public void buildLightmap(ArrayList<Torch> torches) {
        this.torches = torches;
        System.out.printf("Building lightmap. Walls: %d Torches: %d\n", walls.size(), torches.size());
        long start = System.nanoTime();

        wallToTorchMap = new ArrayList<>(Collections.nCopies(walls.size(), null));
        torchToWallMap = new ArrayList<>(Collections.nCopies(torches.size(), null));

        //find which torches "belong" to which walls, and also which walls to torch for floor occlusion
        for (int i = 0; i < walls.size(); i++) {
            Wall wall = walls.get(i);
            for (int j = 0; j < torches.size(); j++) {
                Vector2 P = new Vector2(torches.get(j).x, torches.get(j).y);
                //find distance from the torch to the point
                Vector2 AP = P.cpy().sub(wall.a);
                Vector2 AB = wall.b.cpy().sub(wall.a);
                float len2 = AB.len2();
                float t = AP.dot(AB) / len2;
                if(t < 0) t = 0;
                else if (t > 1) t = 1;

                Vector2 closest = wall.a.cpy().add(AB.cpy().scl(t));
                float dst = P.dst(closest);

                //if our torch is within range to cast light on the wall, then add it to the map
                if(dst <= torches.get(j).radius) {
                    if(wallToTorchMap.get(i) == null) {
                        wallToTorchMap.set(i, new ArrayList<>());
                    }
                    wallToTorchMap.get(i).add(new TorchAndIndex(torches.get(j), j));
                }

                OcclusionWall[] closestWallsToLight = torchToWallMap.get(j);
                if(closestWallsToLight == null) {
                    closestWallsToLight = new OcclusionWall[MAX_OCCLUSION_WALS];
                    for (int k = 0; k < closestWallsToLight.length; k++) {
                        closestWallsToLight[k] = new OcclusionWall(Float.MAX_VALUE);
                    }
                    torchToWallMap.set(j, closestWallsToLight);
                }

                //find the closest 4 walls to the light
                for (int k = 0; k < closestWallsToLight.length; k++) {
                    OcclusionWall oWall = closestWallsToLight[k];
                    if(dst < oWall.dst) {
                        oWall.dst = dst;
                        oWall.ax = wall.a.x;
                        oWall.ay = wall.a.y;
                        oWall.bx = wall.b.x;
                        oWall.by = wall.b.y;
                        Arrays.sort(closestWallsToLight, (o1, o2) -> Float.compare(o1.getDst(), o2.getDst()));
                        break;
                    }
                }
                
            }
        }

        Torch.loadTexture();
        for (Torch torch : torches) {
            addSprite(new Sprite2_5D(Torch.getTextureRegion(), torch.x, torch.y, 0.0f, 0.8f, 0.4f));
        }

        long end = System.nanoTime();
        float durationInMs = (end - start) / 1000000.0f;
        System.out.printf("Lightmap built: %fms\n", durationInMs);
    }

    //returns torch count filled
    private int buildLightFrustums() {
        float yawR = (float)Math.toRadians(yaw);
        Vector2 forward = new Vector2((float)Math.cos(yawR), (float)Math.sin(yawR));
        Vector2 rightV = new Vector2(forward.y, -forward.x);
        float halfWidth = (float)Math.tan(Math.toRadians(fov * 0.5f));

        Vector2 leftPoint = new Vector2(
            forward.x + -1.0f * rightV.x * halfWidth,
            forward.y + -1.0f * rightV.y * halfWidth
        ).nor();

        Vector2 rightPoint = new Vector2(
            forward.x + 1.0f * rightV.x * halfWidth,
            forward.y + 1.0f * rightV.y * halfWidth
        ).nor();

        int torchCounter = 0;

        for (int i = 0; i < torches.size(); i++) {

            //max torches achieved per view
            if(torchCounter >= MAX_SHADER_TORCHES) {
                break;
            }

            Torch torch = torches.get(i);
            Vector2 torchVec = new Vector2(torch.x - camX, torch.y - camY);
            if(torchVec.len() > 55) { //magic light culling distance
                continue;
            }
            float crsL = torchVec.crs(leftPoint);
            float crsR = torchVec.crs(rightPoint);
            //quick check to see if center is in view or if light radius is within camera pos
            if((crsL > 0 && crsR < 0) || torchVec.len() < torch.radius) {
                torchData[torchCounter * 4] = torch.x;
                torchData[torchCounter * 4 + 1] = torch.y;
                torchData[torchCounter * 4 + 2] = torch.radius;
                OcclusionWall[] oWalls = torchToWallMap.get(i);
                for (int index = 0; index < MAX_OCCLUSION_WALS; index++) {
                    torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4] = oWalls[index].ax;
                    torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 1] = oWalls[index].ay;
                    torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 2] = oWalls[index].bx;
                    torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 3] = oWalls[index].by;
                }
                torchCounter++;
                continue;
            }

            //check edges by circle line intersection
            float tL = torchVec.dot(leftPoint);
            float tR = torchVec.dot(rightPoint);

            //both miss
            if(tL < 0 && tR < 0) {
                continue;
            }

            float L2 = torchVec.len2();
            float dL = L2 - (tL * tL);
            float dR = L2 - (tR * tR);
            if(dL > torch.radius * torch.radius && dR > torch.radius * torch.radius) {
                continue; //ray miss, skipping torch
            }

            torchData[torchCounter * 4] = torch.x;
            torchData[torchCounter * 4 + 1] = torch.y;
            torchData[torchCounter * 4 + 2] = torch.radius;
            OcclusionWall[] oWalls = torchToWallMap.get(i);
            for (int index = 0; index < MAX_OCCLUSION_WALS; index++) {
                torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4] = oWalls[index].ax;
                torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 1] = oWalls[index].ay;
                torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 2] = oWalls[index].bx;
                torchWallData[((index * MAX_SHADER_TORCHES) + torchCounter) * 4 + 3] = oWalls[index].by;
            }

            torchCounter++;
        }
        
        return torchCounter;
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
        rayWallTex = new float[screenX * 4];
        rayDoorTex = new float[screenX * 4];

        torchData = new float[MAX_SHADER_TORCHES * 4];
        torchWallData = new float[MAX_SHADER_TORCHES * MAX_OCCLUSION_WALS * 4];

        if(wallShader != null) {
            wallShader.dispose();
            floorShader.dispose();
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
            + "uniform vec4 rayTex[" + numRayData + "];\n"
            + "uniform float numRays;\n"
            + "uniform vec4 cameraInfo;\n" //x y pos, z aspect, w tanhalffov
            + "uniform sampler2D texture0;\n"
            + "uniform sampler2D texture1;\n"
            + "void main()\n"
            + "{\n"
            + "  int index = int(v_texCoords.x * numRays);\n"
            + "  vec4 dat = rayData[index];\n"
            + "  vec4 rayTexDat = rayTex[index];\n"
            + "  float wallTop = dat.z;\n"
            + "  float wallBottom = dat.w;\n"
            + "  float current = v_texCoords.y - 0.5;\n"
            + "  bool isWall = current > wallBottom && current < wallTop;\n"
            + "  float texY = dat.x * (current - wallBottom) / (rayTexDat.z - wallBottom);\n"
            + "  vec2 texCoords = vec2(cameraInfo.z * dat.y, texY);\n"
            + "  vec4 texColor;\n"
            + "  float texID = rayTexDat.x;\n"
            + "  if(texID == 0.0) { texColor = texture2D(texture0, texCoords); }\n"
            + "  else if(texID == 1.0) { texColor = texture2D(texture1, texCoords); }\n"
            //+ "  float dst = max(1.0 - rayTex[index].y, 0.0);\n"
            + "  float lightInfluence = min(length(vec2(rayTexDat.w, texY - 0.5)), 0.85);\n"
            + "  if(isWall && texColor.a > 0.01) {\n"
            + "      gl_FragColor = vec4(mix(texColor.rgb, vec3(0.0, 0.0, 0.0), lightInfluence), 1.0);\n"
            + "  } else { discard; }\n"
            + "}\n";

        String floorFragmentShader = 
            "#ifdef GL_ES\n"
          + "precision mediump float;\n"
          + "#endif\n"
          + "varying vec2 v_texCoords;\n"
          + "uniform vec4 torchData[" + MAX_SHADER_TORCHES + "];\n"
          + "uniform vec4 torchWallData[" + MAX_SHADER_TORCHES * MAX_OCCLUSION_WALS + "];\n" //4 walls per torch to occlude light
          + "uniform int torchCount;\n"
          + "uniform vec4 cameraInfo;\n"
          + "uniform vec2 cameraInfo2;\n"
          + "uniform sampler2D texture;\n"
          + "int orientation(vec2 a, vec2 b, vec2 c) { float d = (b.y - a.y) * (c.x - b.x) - (b.x - a.x) * (c.y - b.y); if(d == 0.0) return 0; return d > 0.0 ? 1 : 2; }\n"
          + "bool onSegment(vec2 a, vec2 b, vec2 c) { return c.x >= min(a.x, b.x) && c.x <= max(a.x, b.x) && c.y >= min(a.y, b.y) && c.y <= max(a.y, b.y); }\n"
          + "void main()\n"
          + "{\n"
          + "  vec2 dxdy = vec2(cameraInfo2.x + (v_texCoords.x * cameraInfo2.y * cameraInfo.w), cameraInfo2.y + (v_texCoords.x * -cameraInfo2.x * cameraInfo.w));\n"
          + "  vec2 worldDir = vec2(dxdy.x, dxdy.y) * abs(1.0 / v_texCoords.y);\n"
          + "  vec2 worldPos = worldDir * 2.0 + (cameraInfo.xy);\n"
          + "  float dst = 1.0 - abs(v_texCoords.y);\n"
          + "  float lightValue = 0.15;\n"
          + "  for(int i = 0; i < torchCount; i++) {\n"
          + "      vec4 torch = torchData[i];\n"
          + "      bool intersects = false;\n"
          + "      for(int j = 0; j < " + MAX_OCCLUSION_WALS + "; j++) {\n"
          + "          vec4 wall = torchWallData[i + (j * " + MAX_SHADER_TORCHES + ")];\n"
          + "          int o1 = orientation(wall.xy, wall.zw, worldPos);\n"
          + "          int o2 = orientation(wall.xy, wall.zw, torch.xy);\n"
          + "          int o3 = orientation(worldPos, torch.xy, wall.xy);\n"
          + "          int o4 = orientation(worldPos, torch.xy, wall.zw);\n"
          + "          if(o1 != o2 && o3 != o4) { intersects = true; break; }\n"
          + "      }\n"
          + "      if(intersects) continue;\n"
          + "      float distToLight2 = dot(worldPos - torch.xy, worldPos - torch.xy);\n"
          + "      if(distToLight2 > torch.z * torch.z) continue;\n"
          + "      lightValue += 1.0 - distToLight2 / (torch.z * torch.z);\n"
          + "  }\n"
          + "  vec3 fColor = texture2D(texture, worldPos * 0.5).rgb * min(lightValue, 1.0);\n"
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
