package ogl_samples.framework.glNext

import glm.vec._2.Vec2i
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.*
import java.nio.IntBuffer

/**
 * Created by elect on 18/04/17.
 */


fun glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, size: Vec2i) =
        GL30.glRenderbufferStorageMultisample(target, samples, internalformat, size.x, size.y)

fun glBindRenderbuffer(target: Int) = GL30.glBindRenderbuffer(target, 0)
fun glBindRenderbuffer(target: Int, buffer: IntBuffer) = GL30.glBindRenderbuffer(target, buffer[0])

fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: IntBuffer) =
        glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer[0])


fun glBindFramebuffer(target: Int, framebuffer: IntBuffer) = glBindFramebuffer(target, framebuffer[0])
fun glBindFramebuffer(target: Int) = glBindFramebuffer(target, 0)