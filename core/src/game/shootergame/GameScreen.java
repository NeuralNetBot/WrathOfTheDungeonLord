package game.shootergame;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import game.shootergame.Item.MeleeWeapons.BrassKnucklesWeapon;
import game.shootergame.Item.MeleeWeapons.HalberdWeapon;
import game.shootergame.Item.MeleeWeapons.MaceWeapon;
import game.shootergame.Item.MeleeWeapons.SwordWeapon;
import game.shootergame.Renderer.Renderer;


public class GameScreen extends ScreenAdapter {
    
    private HUD hud;

    private Renderer renderer;

    private MainMenu menu;

    public GameScreen() {

        menu = new MainMenu();

        renderer = Renderer.createInstance(Gdx.graphics.getWidth());
        World.createInstance();
        renderer.setWalls(World.getWalls());

        hud = new HUD(ShooterGame.getInstance().am.get(ShooterGame.RSC_MONO_FONT));

        // the HUD will show FPS always, by default.  Here's how
        // to use the HUD interface to silence it (and other HUD Data)
        hud.setDataVisibility(HUDViewCommand.Visibility.WHEN_OPEN);

        hud.registerAction("debug", new HUDActionCommand() {
            static final String help = "Usage: debug [true|false]";

            @Override
            public String execute(String[] cmd) {
                try {
                    boolean b = Boolean.parseBoolean(cmd[1]);
                    renderer.setDebugRayDraw(b);
                    return "debug: " + b;
                } catch (Exception e) {
                    return help;
                }
            }

            public String help(String[] cmd) {
                return help;
            }
        });



        // we're adding an input processor AFTER the HUD has been created,
        // so we need to be a bit careful here and make sure not to clobber
        // the HUD's input controls. Do that by using an InputMultiplexer
        InputMultiplexer multiplexer = new InputMultiplexer();
        // let the HUD's input processor handle things first....
        multiplexer.addProcessor(Gdx.input.getInputProcessor());
        // then pass input to our new handler...
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {
        
    }

    boolean doOnce = true;
    boolean doOnceLaunch = true;

    @Override
    public void render(float delta) {
        if(menu.isDone() && doOnce) {
            if(menu.getSelectedMode()) {
                World.startAsServer();
            } else {
                World.startAsClient();
            }
            doOnce = false;
        }
        if(menu.shouldLaunchGame() && doOnceLaunch) {
            switch (menu.getSelectedWeapon()) {
                case 0: World.getPlayer().setMeleeWeapon(new SwordWeapon()); break;
                case 1: World.getPlayer().setMeleeWeapon(new HalberdWeapon()); break;
                case 2: World.getPlayer().setMeleeWeapon(new MaceWeapon()); break;
                case 3: World.getPlayer().setMeleeWeapon(new BrassKnucklesWeapon()); break;
            }
            World.loadFromFile("assets/map0.data");
            renderer.buildLightmap(World.getTorches());
            renderer.setTorchRegionIndexCuller(World.getTorchRegionIndexCuller());
            doOnceLaunch = false;
        }

        ScreenUtils.clear(0, 0, 0, 1);

        if(menu.shouldLaunchGame()) {
            if(Gdx.input.isKeyJustPressed(Keys.T)) {
                Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
            }

            if (!hud.isOpen() && Gdx.input.isCursorCatched()) {
                World.processInput();
            }

            World.update(delta);
            renderer.update(World.getPlayer().x(), World.getPlayer().y(), World.getPlayer().rotation(), delta);

            renderer.render();
        }

        SpriteBatch coreBatch = ShooterGame.getInstance().coreBatch;
        ShooterGame.getInstance().coreCamera.update();
        coreBatch.setProjectionMatrix(ShooterGame.getInstance().coreCamera.combined);
        coreBatch.begin();

        if(menu.shouldLaunchGame()) {
            renderer.processSpriteDraws();
            World.render();
        } else {
            menu.update();
        }

        coreBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        
        if(menu.shouldLaunchGame()) {
            World.renderHud();
        }

        //we always want the hud to be visible
        hud.draw(coreBatch);
        coreBatch.end();

        if(menu.shouldLaunchGame()) {
            renderer.renderMinimap();
        }
    }

	@Override
	public void resize (int width, int height) {
        float aspect = (float)height / (float)width;
        Vector3 pos = ShooterGame.getInstance().coreCamera.position;
        ShooterGame.getInstance().coreCamera = new OrthographicCamera(2f / aspect, 2f);
        OrthographicCamera camera = ShooterGame.getInstance().coreCamera;
        camera.position.set(pos);

        renderer.resize(width, height);
	}

    @Override
    public void dispose() {
        renderer.shutdown();
    }
}
