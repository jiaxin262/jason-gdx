package com.jasonjiagdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.jasonjiagdx.game.screens.IndexScreen;

public class MyGdxGame extends Game {

    public static Skin skin;
    public static TextureAtlas textureAtlas;
    public static SpriteBatch batch;

    @Override
    public void create() {

        skin = new Skin(Gdx.files.internal("skin/glassy-ui.json"));
        textureAtlas = new TextureAtlas();
        textureAtlas.addRegion("note", new TextureRegion(new Texture("note.png")));
        batch = new SpriteBatch();
        this.setScreen(new IndexScreen(this));

    }

    @Override
    public void render() {
        super.render();

    }

    @Override
    public void dispose() {
        skin.dispose();
        textureAtlas.dispose();
    }
}
