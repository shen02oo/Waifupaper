/**

 * You can modify and use this source freely
 * only for the development of application related Live2D.

 * (c) Live2D Inc. All rights reserved.
 */
package com.crossbowffs.waifupaper.app

import android.content.Context
import com.crossbowffs.waifupaper.loader.Live2DExpressionWrapper
import com.crossbowffs.waifupaper.loader.Live2DModelLoader
import com.crossbowffs.waifupaper.loader.Live2DMotionWrapper
import com.crossbowffs.waifupaper.loader.Live2DUnboundModelData
import jp.live2d.android.Live2DModelAndroid
import jp.live2d.android.UtOpenGL
import jp.live2d.framework.L2DPhysics
import jp.live2d.framework.L2DPose
import jp.live2d.framework.L2DStandardID
import jp.live2d.framework.L2DTargetPoint
import jp.live2d.motion.Live2DMotion
import jp.live2d.motion.MotionQueueManager
import net.rbgrn.android.glwallpaperservice.GLWallpaperService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Live2DRenderer(private var context: Context) : GLWallpaperService.Renderer {

    private val motionMgr: MotionQueueManager
    private val dragMgr: L2DTargetPoint
    private var modelData: Live2DUnboundModelData? = null
    private var motionIndex: Int = -1
    private var subMotionIndex: Int = -1

    init {
        dragMgr = L2DTargetPoint()
        motionMgr = MotionQueueManager()
    }

    private val hasModel: Boolean
        get() = modelData != null

    private val model: Live2DModelAndroid
        get() = modelData!!.model

    private val physics: L2DPhysics?
        get() = modelData!!.physics

    private val pose: L2DPose?
        get() = modelData!!.pose

    private val expressions: Array<Live2DExpressionWrapper>?
        get() = modelData!!.expressions

    private val motions: Array<Live2DMotionWrapper>?
        get() = modelData!!.motions

    fun chooseMotion(): Live2DMotion {
        var currMotion = motions!![motionIndex]
        if (++subMotionIndex == currMotion.submotions.size) {
            subMotionIndex = 0
            motionIndex = (motionIndex + 1) % currMotion.submotions.size
            currMotion = motions!![motionIndex]
        }
        return currMotion.submotions[subMotionIndex].motion
    }

    override fun onDrawFrame(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        gl.glEnable(GL10.GL_BLEND)
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)

        if (!hasModel) {
            return
        }

        model.loadParam()

        if (motions != null) {
            if (motionMgr.isFinished) {
                motionMgr.startMotion(chooseMotion(), false)
            } else {
                motionMgr.updateParam(model)
            }
        }

        model.saveParam()

        dragMgr.update()
        val dragX = dragMgr.x
        val dragY = dragMgr.y
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, dragX * 15)
        model.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y, dragY * 15)
        model.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, dragX * 30)

        physics?.updateParam(model)

        model.update()
        model.draw()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        val modelWidth = model.canvasWidth
        gl.glOrthof(
            0f,
            modelWidth,
            modelWidth * height / width,
            0f,
            0.5f,
            -0.5f)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        setModel(gl, "Epsilon")
    }

    fun setModel(gl: GL10, name: String) {
        release()
        val newModelData = Live2DModelLoader.loadInternal(context, name)
        newModelData.model.setGL(gl)
        for (i in newModelData.textures.indices) {
            val textureNum = UtOpenGL.buildMipmap(gl, newModelData.textures[i])
            newModelData.model.setTexture(i, textureNum)
        }
        modelData = newModelData
        motionIndex = 0
        subMotionIndex = 0
    }

    fun release() {
        motionIndex = -1
        subMotionIndex = -1
        motionMgr.stopAllMotions()
        if (modelData != null) {
            val model = modelData!!.model
            model.deleteTextures()
            model.setGL(null)
            modelData = null
        }
    }

    fun resetDrag() {
        dragMgr.set(0f, 0f)
    }


    fun drag(x: Float, y: Float) {
        dragMgr.set(x, y)
    }
}