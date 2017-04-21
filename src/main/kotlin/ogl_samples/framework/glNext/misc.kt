package ogl_samples.framework.glNext

import glm.set
import glm.vec._2.Vec2i
import glm.vec._4.Vec4
import ogl_samples.framework.float
import ogl_samples.framework.mat4Buffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glClearDepth
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL30.glBlitFramebuffer
import org.lwjgl.opengl.GL30.glClearBufferfv
import org.lwjgl.opengl.GL41

/**
 * Created by elect on 18/04/17.
 */


fun glClearBuffer(buffer: Int, value: Float) = glClearBuffer(buffer, 0, value)
fun glClearBuffer(buffer: Int, drawbuffer: Int, value: Float) {
    float[0] = value
    glClearBufferfv(buffer, drawbuffer, float)
}
fun glClearBuffer(buffer: Int, value: Vec4) = glClearBuffer(buffer, 0, value)
fun glClearBuffer(buffer: Int, drawbuffer: Int, value: Vec4) = glClearBufferfv(buffer, drawbuffer, value to mat4Buffer)

fun glViewport(size: Vec2i) = glViewport(0, 0, size.x, size.y)

fun glBlitFramebuffer(size: Vec2i) = glBlitFramebuffer(
        0, 0, size.x, size.y,
        0, 0, size.x, size.y,
        GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR)


fun glClearColor() = GL11.glClearColor(0f, 0f, 0f, 1f)
fun glClearDepthf() = GL41.glClearDepthf(1f)