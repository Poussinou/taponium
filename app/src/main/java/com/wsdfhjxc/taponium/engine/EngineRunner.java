package com.wsdfhjxc.taponium.engine;

import android.app.*;
import android.os.*;

import com.wsdfhjxc.taponium.scenes.*;

public class EngineRunner implements Runnable {
    private boolean running = false;
    private boolean paused = false;
    private final Thread runnerThread = new Thread(this);

    private final SceneKeeper sceneKeeper;
    private final ResourceKeeper resourceKeeper;

    private final InputHandler inputHandler;
    private final UpdateHandler updateHandler;
    private final RenderHandler renderHandler;

    public EngineRunner(Activity activity) {
        sceneKeeper = new SceneKeeper();
        resourceKeeper = new ResourceKeeper(activity);

        ApplicationView applicationView = new ApplicationView(activity, sceneKeeper);
        activity.setContentView(applicationView);

        inputHandler = new InputHandler(applicationView, sceneKeeper);
        updateHandler = new UpdateHandler(sceneKeeper, 15); // 15 updates per second
        renderHandler = new RenderHandler(applicationView, updateHandler, 30); // 30 renders per second

        // flex config for resolution independence calculations
        FlexConfig flexConfig = new FlexConfig(activity, 1080); // base resolution is 1080p width

        // add initial default scene to processing list
        sceneKeeper.addScene(new DefaultScene(sceneKeeper, resourceKeeper, renderHandler,
                                              flexConfig));
    }

    @Override
    public void run() {
        while (running && sceneKeeper.hasScenes()) {
            if (!paused) {
                sceneKeeper.poll();
                inputHandler.poll();
                updateHandler.poll();
                renderHandler.poll();
            }

            SystemClock.sleep(1); // prevent CPU from hogging and draining battery
        }

        System.exit(0); // destroy app process at engine stop
    }

    public void start() {
        if (!runnerThread.isAlive()) {
            running = true;
            runnerThread.start();
        } else {
            resume();
        }
    }

    public void stop() {
        running = false;
        sceneKeeper.removeAllScenes();
        resourceKeeper.unloadEverything();
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        updateHandler.resetDelay(); // ignore a big time difference after coming back from pause
    }

    public void backPressed() {
        for (Scene scene : sceneKeeper.scenes) {
            scene.backPressed();
        }
    }
}