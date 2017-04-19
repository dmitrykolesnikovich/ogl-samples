package ogl_samples.framework.glNext

import glm.vec._4.Vec4
import ogl_samples.framework.mat4Buffer
import org.lwjgl.opengl.GL30.*

/**
 * Created by elect on 18/04/17.
 */


fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: Vec4) = glClearBufferfv(buffer, drawbuffer, value to mat4Buffer)