package com.daasuu.epf;

import com.daasuu.epf.chooser.EConfigChooser;
import com.daasuu.epf.contextfactory.EContextFactory;
import com.daasuu.epf.filter.GlFilter;
import com.google.android.exoplayer2.SimpleExoPlayer;

class EPlayerDelegate {

    private final IPlayerView playerView;

    private final EPlayerRenderer renderer;
    private SimpleExoPlayer player;

    private float videoAspect = 1f;
    private PlayerScaleType playerScaleType = PlayerScaleType.RESIZE_FIT_HEIGHT;
    private boolean mIsRotated = false;

    EPlayerDelegate(IPlayerView playerView) {
        this.playerView = playerView;
        this.playerView.setEGLContextFactory(new EContextFactory());
        this.playerView.setEGLConfigChooser(new EConfigChooser());
        renderer = new EPlayerRenderer(playerView);
        this.playerView.setRenderer(renderer);
    }

    void setSimpleExoPlayer(SimpleExoPlayer player) {
        if (this.player != null) {
            //Why release??? this.player.release();
            this.player.removeVideoListener(playerView);
            this.player = null;
        }
        this.player = player;
        if (this.player != null) {
            this.player.addVideoListener(playerView);
        }
        this.renderer.setSimpleExoPlayer(player);
    }

    void setGlFilter(GlFilter glFilter) {
        renderer.setGlFilter(glFilter);
    }

    void setPlayerScaleType(PlayerScaleType playerScaleType) {
        this.playerScaleType = playerScaleType;
        playerView.requestLayout();
    }

    public void setRotated(boolean isRotated) {
        mIsRotated = isRotated;
    }

    void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = playerView.getMeasuredWidth();
        int measuredHeight = playerView.getMeasuredHeight();

        int viewWidth = measuredWidth;
        int viewHeight = measuredHeight;

        switch (playerScaleType) {
            case RESIZE_FIT_WIDTH:
                viewHeight = (int) (measuredWidth / videoAspect);
                break;
            case RESIZE_FIT_HEIGHT:
                viewWidth = (int) (measuredHeight * videoAspect);
                break;
        }

        // Log.d(TAG, "onMeasure viewWidth = " + viewWidth + " viewHeight = " + viewHeight);

        playerView.setMeasuredDimensionImpl(viewWidth, viewHeight);
    }

    void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        // Log.d(TAG, "width = " + width + " height = " + height + " unappliedRotationDegrees = " + unappliedRotationDegrees + " pixelWidthHeightRatio = " + pixelWidthHeightRatio);
        videoAspect = ((float) width / height) * pixelWidthHeightRatio;

        if (mIsRotated) {
            videoAspect = 1.0f / videoAspect;
        }

        // Log.d(TAG, "videoAspect = " + videoAspect);
        playerView.requestLayout();
    }

    void rendererRelease() {
        renderer.release();
    }
}
