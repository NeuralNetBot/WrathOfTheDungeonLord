package game.shootergame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;

import java.util.Random;

public class ShooterGame extends Game {
    public static final String RSC_MONO_FONT_FILE = "JetBrainsMono-Regular.ttf";
    public static final String RSC_MONO_FONT = "JBM.ttf";
    static ShooterGame gameInstance;

    public AssetManager am;  // AssetManager provides a single source for loaded resources
    public SpriteBatch coreBatch;
    public OrthographicCamera coreCamera;

    public Random random = new Random();

    public static ShooterGame getInstance() {
        return gameInstance;
    }

    @Override
    public void create() {
        gameInstance = this;
        am = new AssetManager();

		/* True Type Fonts are a bit of a pain. We need to tell the AssetManager
           a bit more than simply the file name in order to get them into an
           easily usable (BitMap) form...
		 */
        FileHandleResolver resolver = new InternalFileHandleResolver();
        am.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        am.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
        FreetypeFontLoader.FreeTypeFontLoaderParameter myFont = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        myFont.fontFileName = RSC_MONO_FONT_FILE;
        myFont.fontParameters.size = 14;
        am.load(RSC_MONO_FONT, BitmapFont.class, myFont);

        am.finishLoading();

        coreCamera = new OrthographicCamera();
        coreBatch = new SpriteBatch();
        setScreen(new GameScreen());
    }

    @Override
    public void dispose() {
        coreBatch.dispose();
        am.dispose();
    }
}