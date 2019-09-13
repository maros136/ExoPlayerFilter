package com.daasuu.epf;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.CallSuper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/**
 * https://gist.github.com/ilya-t/c54bd715edd495c07677
 *
 * <p>TextureView implementation for displaying openGL rendering.</p>
 * based on Romain Guy answer in
 * <a href="https://groups.google.com/forum/#!topic/android-developers/U5RXFGpAHPE">"Re: How to replace GLSurfaceView with TextureView in Android Ice Cream Sandwich?"</a>
 * also on this <a href="https://github.com/dalinaum/TextureViewDemo/blob/master/src/kr/gdg/android/textureview/GLTriangleActivity.java">TextureViewDemo</a> github project
 * and of course on original {@link android.opengl.GLSurfaceView}'s GLThread
 */
public class GLTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private final static String TAG = "GLTextureView";

    private GLSurfaceView.Renderer renderer;
    private GLThread glThread;
    private List<Runnable> mEventQueue = Collections.synchronizedList(new ArrayList<Runnable>());
    private GLSurfaceView.EGLConfigChooser mEGLConfigChooser;
    private GLSurfaceView.EGLContextFactory mEGLContextFactory;

    public GLTextureView(Context context) {
        this(context, null);
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(this);
    }

    /**
     * Install a custom EGLContextFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(GLSurfaceView.Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    public void setEGLContextFactory(GLSurfaceView.EGLContextFactory factory) {
        mEGLContextFactory = factory;
        /*
        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        }
        */
    }


    /**
     * Install a custom EGLConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(GLSurfaceView.Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an EGLConfig that is compatible with the current
     * android.view.Surface, with a depth buffer depth of
     * at least 16 bits.
     * @param configChooser
     */
    public void setEGLConfigChooser(GLSurfaceView.EGLConfigChooser configChooser) {
        mEGLConfigChooser = configChooser;

        /*
        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            int[] configSpec = getConfig();
            if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            }
            return null;
        }

        private int[] getConfig() {
            return new int[] {
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
            };
        }
        */
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        glThread = new GLThread(surface);
        glThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        glThread.onWindowResize(width, height);
    }

    @CallSuper
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        glThread.finish();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        this.renderer = renderer;
    }


    private class GLThread extends Thread {
        static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        static final int EGL_OPENGL_ES2_BIT = 4;

        private volatile boolean finished;

        private final SurfaceTexture surface;

        private EGL10 egl;
        private EGLDisplay eglDisplay;
        private EGLConfig eglConfig;
        private EGLContext eglContext;
        private EGLSurface eglSurface;
        private GL gl;
        private int width = getWidth();
        private int height = getHeight();
        private volatile boolean sizeChanged = true;

        GLThread(SurfaceTexture surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            initGL();
            GL10 gl10 = (GL10) gl;
            renderer.onSurfaceCreated(gl10, eglConfig);
            while (!finished) {
                checkCurrent();
                if (sizeChanged) {
                    createSurface();
                    renderer.onSurfaceChanged(gl10, width, height);
                    sizeChanged = false;
                }

                if (!mEventQueue.isEmpty()) {
                    Runnable event = mEventQueue.remove(0);
                    event.run();
                }

                renderer.onDrawFrame(gl10);
                if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                    throw new RuntimeException("Cannot swap buffers");
                }

/*
            //TODO: https://medium.com/rosberryapps/make-your-custom-view-60fps-in-android-4587bbffa557
                try {
                    float framerate = 55f;
                    Thread.sleep((long) (1000 / framerate));
                } catch (InterruptedException e) {
                    // Ignore
                }
*/
            }
            finishGL();
        }

        private void destroySurface() {
            if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                egl.eglDestroySurface(eglDisplay, eglSurface);
                eglSurface = null;
            }
        }

        /**
         * Create an egl surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        public boolean createSurface() {
            /*
             * Check preconditions.
             */
            if (egl == null) {
                throw new RuntimeException("egl not initialized");
            }
            if (eglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (eglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurface();

            /*
             * Create an EGL surface we can render into.
             */

            try {
                eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null);
            } catch (IllegalArgumentException e) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e(TAG, "eglCreateWindowSurface", e);
                return false;
            }

            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                int error = egl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
                Log.e(TAG, "eglMakeCurrent failed " + GLUtils.getEGLErrorString(egl.eglGetError()));
                return false;
            }

            return true;
        }


        private void checkCurrent() {
            if (!eglContext.equals(egl.eglGetCurrentContext())
                    || !eglSurface.equals(egl
                    .eglGetCurrentSurface(EGL10.EGL_DRAW))) {
                checkEglError();
                if (!egl.eglMakeCurrent(eglDisplay, eglSurface,
                        eglSurface, eglContext)) {
                    throw new RuntimeException(
                            "eglMakeCurrent failed "
                                    + GLUtils.getEGLErrorString(egl
                                    .eglGetError()));
                }
                checkEglError();
            }
        }

        private void checkEglError() {
            final int error = egl.eglGetError();
            if (error != EGL10.EGL_SUCCESS) {
                Log.e("PanTextureView", "EGL error = 0x" + Integer.toHexString(error));
            }
        }
        private void finishGL() {
            cleanupFinishingGL();
            egl.eglDestroyContext(eglDisplay, eglContext);
            egl.eglTerminate(eglDisplay);
            egl.eglDestroySurface(eglDisplay, eglSurface);
        }

        private void initGL() {
            egl = (EGL10) EGLContext.getEGL();

            eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            int[] version = new int[2];
            if (!egl.eglInitialize(eglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            eglConfig = mEGLConfigChooser.chooseConfig(egl, eglDisplay);
            if (eglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }

            eglContext = mEGLContextFactory.createContext(egl, eglDisplay, eglConfig);

            createSurface();

            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            gl = eglContext.getGL();
        }

        void finish() {
            finished = true;
        }

        public synchronized void onWindowResize(int w, int h) {
            width = w;
            height = h;
            sizeChanged = true;
        }
    }

    /**
     * For executing event in {@link GLThread}.
     */
    synchronized public void queueEvent(Runnable event) {
        mEventQueue.add(event);
    }

    public void requestRender() {
        postInvalidate();//TODO: valid?
    }

    /**
     * Cleanup before {@link GLThread} is shutdown.
     */
    protected void cleanupFinishingGL() {

    }
}