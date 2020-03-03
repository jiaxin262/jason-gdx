package com.jasonjiagdx.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.jasonjiagdx.game.MyGdxGame;
import com.jasonjiagdx.game.b2dJson.Jb2dJson;


public class TruckScreen implements Screen, InputProcessor {
    private static final String TAG = "TruckScreen";

    public Game mGame;
    private Stage stage;
    private Stage uiStage;
    private OrthographicCamera mCamera;
    private ExtendViewport mViewport;
    private InputMultiplexer multiplexer;
    private TextureAtlas mTextureAtlas, mFruitAtlas;
    private Sprite carBodySprite, frontWheelSprite, rearWheelSprite, boxSprite, stoneSprite,
            boardSprite, orangeSprite;
    private Image groudImage, skyImage;

    private World mWord;
    private Body mGround, mBoard;
    private Body mTruck, mFrontWheel, mRearWheel;
    private RevoluteJoint frontWheelJoint, rearWheelJoint;
    private Body[] boxBodies, stoneBodies;
    /**
     * our mouse joint
     **/
    protected MouseJoint mouseJoint = null;
    /**
     * a hit body
     **/
    protected Body hitBody = null;
    private Box2DDebugRenderer mDebugRender;
    private Jb2dJson jb2dJson;

    private static final float STEP_TIME = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private float mAccumulator = 0;
    private float carBodyWidth = 8.4f;
    private float carBodyHeight = 4.6f;
    private float wheelRadius = 1.06f;
    private float boxRadius = 0.5f;
    private float boardWidth = 20;
    private float boardHeight = 0.5f;
    private Vector2 boardBodyCenter = new Vector2(10, 0.25f);
    private Vector2 carBodyCenter = new Vector2(3.5f, 2.3f);
    private Vector2 stoneCenter = new Vector2(0.24f, 0.14f);

    public TruckScreen(Game game) {
        this.mGame = game;
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Box2D.init();
        // load rube word
        jb2dJson = new Jb2dJson();
        StringBuilder errMsg = new StringBuilder();
        mWord = jb2dJson.readFromFile(Gdx.files.internal("rube/car.json"), errMsg, null);
        mGround = jb2dJson.getBodyByName("ground");
        mTruck = jb2dJson.getBodyByName("truckBody");
        mFrontWheel = jb2dJson.getBodyByName("frontwheel");
        mRearWheel = jb2dJson.getBodyByName("rearwheel");
        mBoard = jb2dJson.getBodyByName("board");
        boxBodies = jb2dJson.getBodiesByName("box");
        stoneBodies = jb2dJson.getBodiesByName("stone");
        frontWheelJoint = (RevoluteJoint) jb2dJson.getJointByName("frontWheelJoint");
        rearWheelJoint = (RevoluteJoint) jb2dJson.getJointByName("rearWheelJoint");

        mFruitAtlas = new TextureAtlas("fruits/sprites.txt");
        boxSprite = mFruitAtlas.createSprite("crate");
        boxSprite.setSize(1, 1);
        boxSprite.setOrigin(boxRadius, boxRadius);
        orangeSprite = mFruitAtlas.createSprite("orange");
        orangeSprite.setSize(1, 1);
        mTextureAtlas = new TextureAtlas("car/car.txt");
        stoneSprite = mTextureAtlas.createSprite("stone");
        stoneSprite.setSize(0.48f, 0.28f);
        stoneSprite.setOrigin(stoneCenter.x, stoneCenter.y);
        // 汽车UI
        carBodySprite = mTextureAtlas.createSprite("carBody");
        carBodySprite.setSize(carBodyWidth, carBodyHeight);
        carBodySprite.setOrigin(carBodyCenter.x, carBodyCenter.y);
        frontWheelSprite = mTextureAtlas.createSprite("tire");
        frontWheelSprite.setSize(wheelRadius * 2, wheelRadius * 2);
        frontWheelSprite.setOrigin(wheelRadius, wheelRadius);
        rearWheelSprite = mTextureAtlas.createSprite("tire");
        rearWheelSprite.setSize(wheelRadius * 2, wheelRadius * 2);
        rearWheelSprite.setOrigin(wheelRadius, wheelRadius);
        // 天空UI
        Texture sky = new Texture("car/sky.jpeg");
        sky.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        TextureRegion skyRegion = new TextureRegion(sky, 0, 0, 3700, 375);
        skyImage = new Image(skyRegion);
        skyImage.setSize(370, 13);
        skyImage.setPosition(0, 7);
        // 地面UI
        Texture grassland = new Texture("car/grassland.png");
        grassland.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        TextureRegion region = new TextureRegion(grassland, 0, 0, 3700, 87);
        groudImage = new Image(region);
        groudImage.setSize(370, 10);
        groudImage.setPosition(0, 0);
        // 翘板UI
        boardSprite = mTextureAtlas.createSprite("board");
        boardSprite.setSize(boardWidth, boardHeight);
        boardSprite.setOrigin(boardBodyCenter.x, boardBodyCenter.y);

        mDebugRender = new Box2DDebugRenderer();
        mViewport = new ExtendViewport(33, 20);
        stage = new Stage(mViewport);
        uiStage = new Stage(new ScreenViewport());
        // add 加速、减速按钮
        Skin skin = new Skin();
        skin.add("accBtnUp", new Texture("car/acc.png"));
        skin.add("accBtnDown", new Texture("car/acc2.png"));
        skin.add("decBtnUp", new Texture("car/dec.png"));
        skin.add("decBtnDown", new Texture("car/dec2.png"));

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        Gdx.app.log(TAG, "screenWidth:" + screenWidth + ", screenHeight:" + screenHeight);

        ImageButton accelerateBtn = new ImageButton(skin.getDrawable("accBtnUp"), skin.getDrawable("accBtnDown"));
        accelerateBtn.setSize(300, 400);
        accelerateBtn.setPosition(screenWidth - 400, 20);
        accelerateBtn.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                frontWheelJoint.setMotorSpeed(0);
                rearWheelJoint.setMotorSpeed(0);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                frontWheelJoint.setMotorSpeed(-1000);
                rearWheelJoint.setMotorSpeed(-1000);
                return true;
            }
        });

        ImageButton decelerateBtn = new ImageButton(skin.getDrawable("decBtnUp"), skin.getDrawable("decBtnDown"));
        decelerateBtn.setSize(300, 400);
        decelerateBtn.setPosition(100, 20);
        decelerateBtn.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                frontWheelJoint.setMotorSpeed(0);
                rearWheelJoint.setMotorSpeed(0);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                frontWheelJoint.setMotorSpeed(500);
                rearWheelJoint.setMotorSpeed(500);
                return true;
            }
        });

        uiStage.addActor(accelerateBtn);
        uiStage.addActor(decelerateBtn);
//        Label distanceTextLabel = new Label("distance:0", MyGdxGame.skin, "big");
//        uiStage.addActor(distanceTextLabel);

        mCamera = (OrthographicCamera) stage.getCamera();

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        multiplexer.addProcessor(uiStage);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(multiplexer);
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
        skyImage.draw(MyGdxGame.batch, 1);
        groudImage.draw(MyGdxGame.batch, 1);
        drawSprites();
        MyGdxGame.batch.end();

        uiStage.act();
        uiStage.draw();

        float truckPositionX = mTruck.getPosition().x;
        if (truckPositionX < 355) {
//            Gdx.app.log(TAG, "update camera pos, truckPositionX:" + truckPositionX + ", cameraPositionX:" + cameraPositionX);
            mCamera.position.x = truckPositionX + carBodyWidth / 2;
            mCamera.update();
        }

        stepWorld();

//        mDebugRender.render(mWord, mCamera.combined);
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
        mTextureAtlas.dispose();
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

    private void drawSprites() {
        // 车身
        Vector2 carBodyPosition = mTruck.getPosition();
        float carBodyDegrees = (float) Math.toDegrees(mTruck.getAngle());
        drawSprite(carBodySprite, carBodyPosition.x - carBodyCenter.x, carBodyPosition.y - carBodyCenter.y, carBodyDegrees);
        // 前轮
        Vector2 frontWheelPosition = mFrontWheel.getPosition();
        float frontWheelDegrees = (float) Math.toDegrees(mFrontWheel.getAngle());
        drawSprite(frontWheelSprite, frontWheelPosition.x - wheelRadius, frontWheelPosition.y - wheelRadius, frontWheelDegrees);
        // 后轮
        Vector2 rearWheelPosition = mRearWheel.getPosition();
        float rearWheelDegrees = (float) Math.toDegrees(mRearWheel.getAngle());
        drawSprite(rearWheelSprite, rearWheelPosition.x - wheelRadius, rearWheelPosition.y - wheelRadius, rearWheelDegrees);
        // 箱子
        if (boxBodies != null && boxBodies.length > 0) {
            drawBoxes();
        }
        // 石头
        if (stoneBodies != null && stoneBodies.length > 0) {
            drawStones();
        }
        // 翘板
        Vector2 boardPosition = mBoard.getPosition();
        float boardDegrees = (float) Math.toDegrees(mBoard.getAngle());
        drawSprite(boardSprite, boardPosition.x - boardBodyCenter.x, boardPosition.y - boardBodyCenter.y, boardDegrees);
        // 翘板支撑
        drawSprite(orangeSprite, 44.5f, 6.5f, 0);
    }

    private void drawStones() {
        for (Body body : stoneBodies) {
            Vector2 stonePosition = body.getPosition();
            float stoneDegrees = (float) Math.toDegrees(body.getAngle());
            drawSprite(stoneSprite, stonePosition.x - stoneCenter.x, stonePosition.y - stoneCenter.y, stoneDegrees);
        }
    }

    private void drawBoxes() {
        for (Body body : boxBodies) {
            Vector2 boxPosition = body.getPosition();
            float boxDegrees = (float) Math.toDegrees(body.getAngle());
            drawSprite(boxSprite, boxPosition.x - boxRadius, boxPosition.y - boxRadius, boxDegrees);
        }
    }

    private void drawSprite(Sprite sprite, float posX, float posY, float degrees) {
        sprite.setPosition(posX, posY);
        sprite.setRotation(degrees);
        sprite.draw(MyGdxGame.batch);
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
            def.maxForce = 50.0f * hitBody.getMass();

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

    /**
     * another temporary vector
     **/
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
