package com.jasonjiagdx.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.jasonjiagdx.game.MyGdxGame;
import com.jasonjiagdx.game.b2dJson.Jb2dJson;


public class TruckScreen implements Screen, InputProcessor {
    private static final String TAG = "TruckScreen";

    public Game mGame;
    private Stage stage;
    private OrthographicCamera mCamera;
    private ExtendViewport mViewport;

    private World mWord;
    private Body mGround;
    private Body mTruck;
    /**
     * our mouse joint
     **/
    protected MouseJoint mouseJoint = null;
    /**
     * a hit body
     **/
    protected Body hitBody = null;
    private Box2DDebugRenderer mDebugRender;

    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private float mAccumulator = 0;

    public TruckScreen(Game game) {
        this.mGame = game;
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Box2D.init();

        Jb2dJson jb2dJson = new Jb2dJson();
        StringBuilder errMsg = new StringBuilder();
        mWord = jb2dJson.readFromFile(Gdx.files.internal("rube/truck.json"), errMsg, null);
//        mWord = new World(new Vector2(0, -10), true);
        mGround = jb2dJson.getBodyByName("ground");
        mTruck = jb2dJson.getBodyByName("truckBody");
        mDebugRender = new Box2DDebugRenderer();

        mViewport = new ExtendViewport(33, 20);
        stage = new Stage(mViewport);
        mCamera = (OrthographicCamera) stage.getCamera();
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
        MyGdxGame.batch.end();

        float truckPositionX = mTruck.getPosition().x;
        float cameraPositionX = mCamera.position.x;
        Gdx.app.log(TAG, "truckPositionX:" + truckPositionX + ", cameraPositionX:" + cameraPositionX);
        if (truckPositionX < 355) {
            mCamera.position.x = truckPositionX;
            mCamera.update();
        }

        stepWorld();

        mDebugRender.render(mWord, mCamera.combined);
    }

    @Override
    public void resize(int width, int height) {
        mViewport.update(width, height, true);
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
        mWord.dispose();
        mDebugRender.dispose();

        mDebugRender = null;
        mWord = null;
        mouseJoint = null;
        hitBody = null;
    }


    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();
        mAccumulator += Math.min(delta, 0.25f);
        if (mAccumulator >= STEP_TIME) {
            mAccumulator -= STEP_TIME;
            mWord.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
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
