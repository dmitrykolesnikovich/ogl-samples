package ogl_samples.framework.glNext

import gli.Texture2d
import gli.gl.Format
import org.lwjgl.opengl.GL11.*
import java.nio.IntBuffer

/**
 * Created by elect on 18/04/17.
 */

fun glTexImage2D(level: Int, format: Format, texture: Texture2d)
        = glTexImage2D(GL_TEXTURE_2D, level, format.internal.i, texture[level].extent().x, texture[level].extent().y, 0,
        format.external.i, format.type.i, texture[level].data())

fun glBindTexture(target: Int, texture: IntBuffer) = glBindTexture(target, texture[0])