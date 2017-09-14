package ogl_samples.tests.gl320

import glm_.L
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL31.*
import org.lwjgl.system.libc.LibCString.memcpy
import uno.buffer.bufferOf
import uno.caps.Caps
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    gl_320_buffer_update().loop()
}

private class gl_320_buffer_update : TestA("es-320-buffer-update", Caps.Profile.CORE, 3, 2) {

    val SHADER_SOURCE = "gl-320/buffer-update"

    override var vertexCount = 6
    val positionSize = vertexCount * Vec2.size
    override var vertexData = bufferOf(
            Vec2(-1f, -1f),
            Vec2(+1f, -1f),
            Vec2(+1f, +1f),
            Vec2(+1f, +1f),
            Vec2(-1f, +1f),
            Vec2(-1f, -1f))

    override fun initProgram(): Boolean {

        var validated = true

        val compiler = Compiler()

        // Create program
        if (validated) {

            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {
                attach(vertShaderName, fragShaderName)
                "Position".attrib = semantic.attr.POSITION
                "Color".attrib = semantic.frag.COLOR
                link()

                validated = validated && compiler.check()
                validated = validated && compiler checkProgram name
            }
        }

        // Get variables locations
        if (validated) withProgram(programName) {
            Uniform.transform = "transform".blockIndex
            Uniform.material = "material".blockIndex
            glUniformBlockBinding(name, Uniform.transform, semantic.uniform.TRANSFORM0)
            glUniformBlockBinding(name, Uniform.material, semantic.uniform.MATERIAL)
        }

        return validated && checkError("initProgram")
    }

    /** Buffer update using glMapBufferRange    */
    override fun initBuffer(): Boolean {

        // Bind the buffer for use
        withArrayBuffer(Buffer.ARRAY) {

            // Reserve buffer memory but don't copy the values
            data(size = positionSize, usage = GL_STATIC_DRAW)

            /*  Copy the vertex data in the buffer, in this sample for the whole range of data.
            It doesn't required to be the buffer size but pointers require no memory overlapping.   */
            val data = mapRange(
                    length = positionSize,
                    access = GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT or GL_MAP_UNSYNCHRONIZED_BIT or GL_MAP_FLUSH_EXPLICIT_BIT)
            memcpy(data, vertexData)

            // Explicitly send the data to the graphic card.
            flushRange(positionSize)

            unmap()

        }   // Unbind the buffer

        // Copy buffer
        withArrayBuffer(Buffer.COPY) { data(positionSize, GL_STATIC_DRAW) }

        glBindBuffer(GL_COPY_READ_BUFFER, Buffer.ARRAY)
        glBindBuffer(GL_COPY_WRITE_BUFFER, Buffer.COPY)

        glCopyBufferSubData(
                GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER,
                0, 0,
                positionSize.L)

        glBindBuffer(GL_COPY_READ_BUFFER)
        glBindBuffer(GL_COPY_WRITE_BUFFER)

        var uniformBlockSize = 0

        run {
            withUniformBlock(programName, Uniform.transform) { uniformBlockSize = dataSize }
            withUniformBuffer(Buffer.TRANSFORM) { data(uniformBlockSize, GL_DYNAMIC_DRAW) }
        }

        run {
            val diffuse = Vec4(1f, 0.5f, 0f, 1f)
            withUniformBlock(programName, Uniform.material) { uniformBlockSize = dataSize }
            withUniformBuffer(Buffer.MATERIAL) { data(uniformBlockSize, diffuse, GL_DYNAMIC_DRAW) }
        }

        return checkError("initBuffer")
    }

    override fun initVertexArray(): Boolean {

        withVertexArray(vertexArrayName) {
            withArrayBuffer(Buffer.COPY) {
                glBindBuffer(GL_ARRAY_BUFFER, Buffer.COPY)
                glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, Vec2.size, 0)
                glBindBuffer(GL_ARRAY_BUFFER)
            }
            glEnableVertexAttribArray(semantic.attr.POSITION)
        }

        return checkError("initVertexArray")
    }

    override fun render(): Boolean {

        mappingUniformBufferRange(Buffer.TRANSFORM, Mat4.size, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT) {

        val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100f)
        val model = Mat4()
        val mvp = projection * view * model

            pointer = mvp
        }// Make sure the uniform buffer is uploaded

        glViewport(windowSize)
        glClearColorBuffer()

        glUseProgram(programName)

        // Attach the buffer to UBO binding point semantic::uniform::TRANSFORM0
        glBindUniformBufferBase(semantic.uniform.TRANSFORM0, Buffer.TRANSFORM)
        // Attach the buffer to UBO binding point semantic::uniform::MATERIAL
        glBindUniformBufferBase(semantic.uniform.MATERIAL, Buffer.MATERIAL)

        glBindVertexArray(vertexArrayName)
        glDrawArraysInstanced(vertexCount, 1)

        return true
    }
}