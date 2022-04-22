package com.scenemaxeng.projector;

public interface IVirtualInputObserver {

    void observeDrag(float degrees, float offset);
    void observeRelease();
    void observePress();

    void observeState(boolean left, boolean right, boolean forward, boolean backward);

}
