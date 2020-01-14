package com.daasuu.epf;

import android.opengl.GLSurfaceView;

import com.daasuu.epf.filter.GlFilter;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.video.VideoListener;

/**
 * Interface provide
 */
public interface IPlayerView extends VideoListener {
    //Custom
    void setGlFilter(GlFilter glFilter);
    IPlayerView setSimpleExoPlayer(SimpleExoPlayer player);
    void setPlayerScaleType(PlayerScaleType playerScaleType);
    void setRotated(boolean isRotated);

    //GL
    void setRenderer(GLSurfaceView.Renderer renderer);
    void setEGLConfigChooser(GLSurfaceView.EGLConfigChooser eConfigChooser);
    void setEGLContextFactory(GLSurfaceView.EGLContextFactory eContextFactory);

    //GL Thread
    void queueEvent(Runnable r);
    void requestRender();

    //View
    void requestLayout();
    int getMeasuredWidth();
    int getMeasuredHeight();
    void setMeasuredDimensionImpl(int viewWidth, int viewHeight);
}
