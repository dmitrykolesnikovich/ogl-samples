package ogl_samples.tests.es300

/**
 * Created by GBarbieri on 21.04.2017.
 */

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import ogl_samples.framework.Test
import org.lwjgl.opengl.ARBFramebufferObject.*
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_INVALIDATE_BUFFER_BIT
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.ARBUniformBufferObject.*
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.ARBVertexArrayObject.glDeleteVertexArrays
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL21.GL_SRGB
import org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8
import uno.buffer.bufferBig
import uno.buffer.destroy
import uno.buffer.destroyBuf
import uno.buffer.intBufferBig
import uno.caps.Caps.Profile
import uno.glf.generateIcosahedron
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

/**
 * Created by GBarbieri on 30.03.2017.
 */

fun main(args: Array<String>) {
    es_300_fbo_srgb().loop()
}

private class es_300_fbo_srgb : Test("es-300-fbo-srgb", Profile.ES, 3, 0) {

    val SHADER_SOURCE_RENDER = "es-300/fbo-srgb"
    val SHADER_SOURCE_SPLASH = "es-300/fbo-srgb-blit"

    object Buffer {
        val VERTEX = 0
        val TRANSFORM = 1
        val MAX = 2
    }

    object Texture {
        val COLORBUFFER = 0
        val RENDERBUFFER = 1
        val MAX = 2
    }

    object Program {
        val RENDER = 0
        val SPLASH = 1
        val MAX = 2
    }

    object Shader {
        val VERT_RENDER = 0
        val FRAG_RENDER = 1
        val VERT_SPLASH = 2
        val FRAG_SPLASH = 3
        val MAX = 4
    }

    val programName = IntArray(Program.MAX)
    val vertexArrayName = intBufferBig(Program.MAX)
    val bufferName = intBufferBig(Buffer.MAX)
    val textureName = intBufferBig(Texture.MAX)
    val framebufferName = intBufferBig(1)
    var framebufferScale = 2
    var vertexCount = 0

    override fun begin(): Boolean {

        var validated = true

        glEnable(GL_CULL_FACE)

        if (validated)
            validated = initProgram()
        if (validated)
            validated = initBuffer()
        if (validated)
            validated = initVertexArray()
        if (validated)
            validated = initTexture()
        if (validated)
            validated = initFramebuffer()

        return validated
    }

    fun initProgram(): Boolean {

        var validated = true

        val compiler = ogl_samples.framework.Compiler()

        val shaderName = mutableListOf<Int>()

        initPrograms(programName) {

            if (validated) {

                shaderName += compiler.create("$SHADER_SOURCE_RENDER.vert")
                shaderName += compiler.create("$SHADER_SOURCE_RENDER.frag")

                with(Program.RENDER) {
                    this += shaderName[Shader.VERT_RENDER]
                    this += shaderName[Shader.FRAG_RENDER]
                    link()
                }
            }

            if (validated) {

                shaderName += compiler.create("$SHADER_SOURCE_SPLASH.vert")
                shaderName += compiler.create("$SHADER_SOURCE_SPLASH.frag")

                with(Program.SPLASH) {
                    attach(shaderName[Shader.VERT_SPLASH])
                    attach(shaderName[Shader.FRAG_SPLASH])
                    link()
                }
            }

            if (validated) {

                validated = validated && compiler.check()
                validated = validated && compiler.checkProgram(programName[Program.RENDER])
                validated = validated && compiler.checkProgram(programName[Program.SPLASH])
            }

            if (validated) {

                using(Program.RENDER) { "transform".blockIndex = semantic.uniform.TRANSFORM0 }
                using(Program.SPLASH) { "Diffuse".unit = semantic.sampler.DIFFUSE }
            }
        }
        return validated && checkError("initProgram")
    }

    fun initBuffer(): Boolean {

        val vertices = generateIcosahedron(4)
        vertexCount = vertices.size * Vec3.length
        val vertexData = bufferBig(vertexCount * Vec3.size)
        vertices.forEachIndexed { i, it -> it.to(vertexData, i * Vec3.size) }

        initBuffers(bufferName) {

            withArrayAt(Buffer.VERTEX) { data(vertexData, GL_STATIC_DRAW) }

            val uniformBufferOffset = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT)
            val uniformBlockSize = glm.max(Mat4.size, uniformBufferOffset)

            withUniformAt(Buffer.TRANSFORM) { data(uniformBlockSize, GL_STATIC_DRAW) }
        }

        vertexData.destroy()

        return true
    }

    fun initTexture(): Boolean {

        val windowSize = windowSize * framebufferScale

        initTextures2d(textureName) {

            at(Texture.COLORBUFFER) {
                levels = 0..0
                minFilter = linear
                magFilter = linear
                storage(GL_SRGB8_ALPHA8, windowSize)
            }
            at(Texture.RENDERBUFFER) {
                levels = 0..0
                minFilter = nearest
                magFilter = nearest
                storage(GL_DEPTH_COMPONENT24, windowSize)
            }
        }
        return true
    }

    fun initVertexArray(): Boolean {

        initVertexArrays(vertexArrayName) {

            at(Program.RENDER) {
                array(bufferName[Buffer.VERTEX], glf.pos3)
            }
        }
        return true
    }

    fun initFramebuffer(): Boolean {

        var framebufferEncoding = 0
        var check = true

        initFramebuffer(framebufferName) {

            texture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName[Texture.COLORBUFFER])
            texture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, textureName[Texture.RENDERBUFFER])

            framebufferEncoding = GL_COLOR_ATTACHMENT0.colorEncoding
            check = checkFramebuffer()
        }

        if (framebufferEncoding != GL_SRGB)
            return false

        if (!check)
            return false

/*
		GLint Encoding = -1;
		glGetFramebufferAttachmentParameteriv(GL_DRAW_FRAMEBUFFER, GL_BACK_LEFT, GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, &Encoding);
		GLint Capable = 0;
		glGetIntegerv(GL_FRAMEBUFFER_SRGB_CAPABLE_EXT, &Capable);
*/

        return true
    }

    override fun render(): Boolean {

        withUniformBuffer(bufferName[Buffer.TRANSFORM]) {

            val pointer = mapRange(Mat4.size, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT)

            //glm::mat4 Projection = glm::perspectiveFov(glm::pi<float>() * 0.25f, 640.f, 480.f, 0.1f, 100.0f);
            val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100.0f)

            projection * view to pointer

            // Make sure the uniform buffer is uploaded
            glUnmapBuffer(GL_UNIFORM_BUFFER)
        }

        // Render to a sRGB framebuffer object.
        run {
            depth {
                test = true
                func = lEqual
            }

            glViewport(windowSize * framebufferScale)
            glBindFramebuffer(GL_FRAMEBUFFER, framebufferName)

            clear {
                depth(1f)
                color(1.0f, 0.5f, 0.0f, 1.0f)
            }

            glUseProgram(programName[Program.RENDER])
            glBindVertexArray(vertexArrayName[Program.RENDER])
            glBindBufferBase(GL_UNIFORM_BUFFER, semantic.uniform.TRANSFORM0, bufferName[Buffer.TRANSFORM])

            glDrawArraysInstanced(vertexCount, 1)
        }

        // Blit the sRGB framebuffer to the default framebuffer back buffer.
        run {
            glDisable(GL_DEPTH_TEST)

            glViewport(windowSize)

            glBindFramebuffer(GL_FRAMEBUFFER)
            glUseProgram(programName[Program.SPLASH])
            glActiveTexture(GL_TEXTURE0)
            glBindVertexArray(vertexArrayName[Program.SPLASH])
            glBindTexture(GL_TEXTURE_2D, textureName[Texture.COLORBUFFER])

            glDrawArraysInstanced(3, 1)
        }

        return true
    }

    override fun end(): Boolean {

        glDeleteFramebuffers(framebufferName)
        glDeletePrograms(programName)

        glDeleteBuffers(bufferName)
        glDeleteTextures(textureName)
        glDeleteVertexArrays(vertexArrayName)

        destroyBuf(framebufferName, bufferName, textureName, vertexArrayName)

        return true
    }
}