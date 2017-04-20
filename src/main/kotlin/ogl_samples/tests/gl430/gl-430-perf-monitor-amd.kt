package ogl_samples.tests.gl430

import glm.BYTES
import ogl_samples.framework.Test
import ogl_samples.framework.glf
import uno.buffer.floatBufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps

/**
 * Created by elect on 17/04/17.
 */

fun main(args: Array<String>) {
    gl_430_perf_monitor_amd().run()
}

class gl_430_perf_monitor_amd : Test("gl-430-perf-monitor-amd", Caps.Profile.CORE, 4, 3) {


    val SHADER_SOURCE_TEXTURE = "gl-430/fbo-texture-2d"
    val SHADER_SOURCE_SPLASH = "es-300/fbo-splash"
    val TEXTURE = "kueken7_rgb_dxt1_unorm.dds"

    val vertexCount = 4
    val vertexSize = vertexCount * glf.v2fv2f.SIZE
    val vertexData = floatBufferOf(
            -1f, -1f, 0f, 1f,
            +1f, -1f, 1f, 1f,
            +1f, +1f, 1f, 0f,
            -1f, +1f, 0f, 0f)

    val elementCount = 6
    val elementSize = elementCount * Short.BYTES
    val elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val TRANSFORM = 2
        val MAX = 3
    }

    object Texture {
        val DIFFUSE = 0
        val COLORBUFFER = 1
        val RENDERBUFFER = 2
        val MAX = 3
    }

    override fun begin(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    override fun render(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun end(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}