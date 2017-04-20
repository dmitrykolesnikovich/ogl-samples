package ogl_samples.framework

import glm.mat.Mat4
import glm.vec._4.Vec4
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.glLinkProgram
import java.nio.*

/**
 * Created by elect on 09/04/17.
 */
object gl {

    val VENDOR: String
        get() = GL11.glGetString(GL11.GL_VENDOR)
    val RENDERER: String
        get() = GL11.glGetString(GL11.GL_RENDERER)
    val VERSION: String
        get() = GL11.glGetString(GL11.GL_VERSION)
    val EXTENSIONS: List<String>
        get() = GL11.glGetString(GL11.GL_EXTENSIONS).split("\\s+".toRegex())
}

inline fun <R> withProgram(program: Int, block: Program.() -> R): R {
    Program.name = program
    return Program.block()
}

inline fun <R> usingProgram(program: Int, block: Program.() -> R) {
    GL20.glUseProgram(program)
    withProgram(program) { block() }
    GL20.glUseProgram(0)
}

object Program {

    var name = 0

    infix fun String.to(location: Int) = GL20.glBindAttribLocation(name, location, this)
    val String.location
        get() = GL20.glGetUniformLocation(name, this)

    fun link() = glLinkProgram(name)

    fun uniform4fv(location: Int, vec4: Vec4) = GL20.glUniform4fv(location, vec4 to vec4Buffer)
    infix fun Vec4.to(location: Int) = GL20.glUniform4fv(location, this to vec4Buffer)
    infix fun Mat4.to(location: Int) = GL20.glUniformMatrix4fv(location, false, this to mat4Buffer)
}


inline fun <R> bindingBuffer(pair: Pair<Int, Int>, block: Buffer.() -> R) {
    glBindBuffer(pair.second, pair.first)
    Buffer.target = pair.second
    Buffer.block()
    glBindBuffer(pair.second, 0)
}

object Buffer {

    var target = 0

    fun data(buffer: ByteBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: ShortBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: IntBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    //    fun data(buffer: LongBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: FloatBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)

    fun data(buffer: DoubleBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
}

inline fun <R> bindingVertexArray(vertexArray: Int, block: VertexArray.() -> R) {
    ARBVertexArrayObject.glBindVertexArray(vertexArray)
    VertexArray.block()
    ARBVertexArrayObject.glBindVertexArray(0)
}

object VertexArray {

    var target = 0

    fun data(buffer: ByteBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: ShortBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: IntBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    //    fun data(buffer: LongBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
    fun data(buffer: FloatBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)

    fun data(buffer: DoubleBuffer, usage: Int) = GL15.glBufferData(target, buffer, usage)
}