package com.jasonjiagdx.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.codeandweb.physicseditor.PhysicsShapeCache;
import com.jasonjiagdx.game.MyGdxGame;

import java.util.HashMap;
import java.util.Random;

public class FruitGameScreen implements Screen, InputProcessor {
    private static final String TAG = "FruitGameScreen";

    public Game mGame;
    private Stage stage;
    private TextureAtlas mTextureAtlas;
    private Sprite mBananaSprite;
    private OrthographicCamera mCamera;
    private ExtendViewport mViewport;
    private HashMap<String, Sprite> mSpritesMap = new HashMap<String, Sprite>();

    private World mWord;
    private Body mGround;
    /**
     * our mouse joint
     **/
    protected MouseJoint mouseJoint = null;
    /**
     * a hit body
     **/
    protected Body hitBody = null;
    private Box2DDebugRenderer mDebugRender;

    private PhysicsShapeCache mPhysicsBodies;

    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final float SCALE = 0.05f;
    static final int COUNT = 16;
    Body[] fruitBodies = new Body[COUNT];
    String[] names = new String[COUNT];

    private float mAccumulator = 0;

    public FruitGameScreen(Game game) {
        this.mGame = game;
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Box2D.init();
        mWord = new World(new Vector2(0, -10), true);
        mDebugRender = new Box2DDebugRenderer();

        mViewport = new ExtendViewport(50, 50);
        stage = new Stage(mViewport);
        mCamera = (OrthographicCamera) stage.getCamera();
        mTextureAtlas = new TextureAtlas("fruits/sprites.txt");
        mBananaSprite = mTextureAtlas.createSprite("banana");
        addSprites();

        mPhysicsBodies = new PhysicsShapeCache("fruits/pgysics.xml");
        generateFruit();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            mGame.setScreen(new IndexScreen(mGame));
        }
        MyGdxGame.batch.setProjectionMatrix(mCamera.combined);
        stage.act();
        stage.draw();
        MyGdxGame.batch.begin();
        for (int i = 0; i < fruitBodies.length; i++) {
            Body body = fruitBodies[i];
            String name = names[i];

            Vector2 position = body.getPosition();
            float degrees = (float) Math.toDegrees(body.getAngle());
            drawSprite(name, position.x, position.y, degrees);
        }
        MyGdxGame.batch.end();

        stepWorld();

        mDebugRender.render(mWord, mCamera.combined);
    }

    @Override
    public void resize(int width, int height) {
        mViewport.update(width, height, true);
        createGround();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        mTextureAtlas.dispose();
        mSpritesMap.clear();
        mWord.dispose();
        mDebugRender.dispose();

        mDebugRender = null;
        mWord = null;
        mouseJoint = null;
        hitBody = null;
    }

    private void addSprites() {
        Array<TextureAtlas.AtlasRegion> regions = mTextureAtlas.getRegions();
        for (TextureAtlas.AtlasRegion region : regions) {
            Sprite sprite = mTextureAtlas.createSprite(region.name);
            float width = sprite.getWidth() * SCALE;
            float height = sprite.getHeight() * SCALE;
            sprite.setSize(width, height);
            sprite.setOrigin(0, 0);
            mSpritesMap.put(region.name, sprite);
        }
    }

    private void drawSprite(String name, float posX, float posY, float degrees) {
        if (name == null || "".equals(name)) {
            return;
        }
        Sprite sprite = mSpritesMap.get(name);
        sprite.setPosition(posX, posY);
        sprite.setRotation(degrees);
        sprite.draw(MyGdxGame.batch);
    }

    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();
        mAccumulator += Math.min(delta, 0.25f);
        if (mAccumulator >= STEP_TIME) {
            mAccumulator -= STEP_TIME;
            mWord.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
        }
    }

    private Body createBody(String name, float x, float y, float rotation) {
        Body body = mPhysicsBodies.createBody(name, mWord, SCALE, SCALE);
        body.setTransform(x, y, rotation);
        return body;
    }

    private void createGround() {
        if (mGround != null) {
            mWord.destroyBody(mGround);
        }
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = 1;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(mCamera.viewportWidth / 2, 1);

        fixtureDef.shape = shape;

        mGround = mWord.createBody(bodyDef);
        mGround.createFixture(fixtureDef);
        mGround.setTransform(mCamera.viewportWidth / 2, 1, 0);

        shape.dispose();
    }

    private void generateFruit() {
        String[] fruitNames = new String[]{"banana", "cherries", "orange", "crate"};

        Random random = new Random();

        for (int i = 0; i < fruitBodies.length; i++) {
            String name = fruitNames[random.nextInt(fruitNames.length)];

            float x = random.nextFloat() * 50;
            float y = random.nextFloat() * 50 + 50;

            names[i] = name;
            fruitBodies[i] = createBody(name, x, y, 0);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    /**
     * we instantiate this vector and the callback here so we don't irritate the GC
     **/
    Vector3 testPoint = new Vector3();
    QueryCallback callback = new QueryCallback() {
        @Override
        public boolean reportFixture(Fixture fixture) {
            // if the hit point is inside the fixture of the body
            // we report it
            if (fixture.testPoint(testPoint.x, testPoint.y)) {
                Gdx.app.log(TAG, "reportFixture");
                hitBody = fixture.getBody();
                return false;
            } else
                return true;
        }
    };

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Gdx.app.log(TAG, "touchDown screenX:" + screenX + ", screenY:" + screenY);
        // translate the mouse coordinates to world coordinates
        mCamera.unproject(testPoint.set(screenX, screenY, 0));
        // ask the world which bodies are within the given
        // bounding box around the mouse pointer
        hitBody = null;
        mWord.QueryAABB(callback, testPoint.x - 0.0001f, testPoint.y - 0.0001f, testPoint.x + 0.0001f, testPoint.y + 0.0001f);

        if (hitBody == mGround) hitBody = null;

        // ignore kinematic bodies, they don't work with the mouse joint
        if (hitBody != null && hitBody.getType() == BodyDef.BodyType.KinematicBody) return false;

        // if we hit something we create a new mouse joint
        // and attach it to the hit body.
        if (hitBody != null) {
            MouseJointDef def = new MouseJointDef();
            def.bodyA = mGround;
            def.bodyB = hitBody;
            def.collideConnected = true;
            def.target.set(testPoint.x, testPoint.y);
            def.maxForce = 1000.0f * hitBody.getMass();

            mouseJoint = (MouseJoint) mWord.createJoint(def);
            hitBody.setAwake(true);
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Gdx.app.log(TAG, "touchUp screenX:" + screenX + ", screenY:" + screenY + ", mouseJoint:" + mouseJoint);
        // if a mouse joint exists we simply destroy it
        if (mouseJoint != null) {
            mWord.destroyJoint(mouseJoint);
            mouseJoint = null;
        }
        return false;
    }

    /** another temporary vector **/
    Vector2 target = new Vector2();

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        Gdx.app.log(TAG, "touchDragged screenX:" + screenX + ", screenY:" + screenY + ", mouseJoint:" + mouseJoint);
        // if a mouse joint exists we simply update
        // the target of the joint based on the new
        // mouse coordinates
        if (mouseJoint != null) {
            mCamera.unproject(testPoint.set(screenX, screenY, 0));
            mouseJoint.setTarget(target.set(testPoint.x, testPoint.y));
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
