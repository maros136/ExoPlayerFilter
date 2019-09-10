package com.daasuu.epf;

import com.daasuu.epf.filter.GlFilter;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * Interface provide
 */
public interface IPlayerView {
    void queueEvent(Runnable r);
    void requestRender();

    void setGlFilter(GlFilter glFilter);
    IPlayerView setSimpleExoPlayer(SimpleExoPlayer player);
}
