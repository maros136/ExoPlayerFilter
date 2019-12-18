package com.daasuu.epf;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.daasuu.epf.filter.GlFilter;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * Created by sudamasayuki on 2017/05/16.
 */
public class EPlayerSurfaceView extends GLSurfaceView implements IPlayerView {

    private final static String TAG = EPlayerSurfaceView.class.getSimpleName();

    private final EPlayerDelegate playerDelegate;

    public EPlayerSurfaceView(Context context) {
        this(context, null);
    }

    public EPlayerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        playerDelegate = new EPlayerDelegate(this);
    }

    public EPlayerSurfaceView setSimpleExoPlayer(SimpleExoPlayer player) {
        playerDelegate.setSimpleExoPlayer(player);
        return this;
    }

    public void setGlFilter(GlFilter glFilter) {
        playerDelegate.setGlFilter(glFilter);
    }

    public void setPlayerScaleType(PlayerScaleType playerScaleType) {
        playerDelegate.setPlayerScaleType(playerScaleType);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        playerDelegate.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setMeasuredDimensionImpl(int viewWidth, int viewHeight) {
        setMeasuredDimension(viewWidth, viewHeight);
    }

    //////////////////////////////////////////////////////////////////////////
    // SimpleExoPlayer.VideoListener

    //TODO: Should work without overriding, but probably no compiled as Java 8 target
    @Override
    public void onSurfaceSizeChanged(int width, int height) {
        //super.onSurfaceSizeChanged(width, height);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
       playerDelegate.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }

    @Override
    public void onRenderedFirstFrame() {
        // do nothing
    }

    //////////////////////////////////////////////////////////////////////////
    //Render Releasing

    @Override
    public void onPause() {
        super.onPause();
        playerDelegate.rendererRelease();
    }
}
