package ogl_samples.tests.es300

import glm.glm
import glm.mat.Mat4
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import ogl_samples.framework.Test
import ogl_samples.framework.semantic
import org.lwjgl.opengl.ARBFramebufferObject.*
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_INVALIDATE_BUFFER_BIT
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.ARBUniformBufferObject.*
import org.lwjgl.opengl.ARBVertexArrayObject.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL21.GL_SRGB
import org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8
import uno.buffer.byteBufferBig
import uno.buffer.destroy
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.caps.Caps.Profile
import uno.glf.generateIcosahedron
import uno.glf.glf
import uno.gln.*

/**
 * Created by GBarbieri on 30.03.2017.
 */

fun main(args: Array<String>) {
    es_300_fbo_srgb().run()
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
    var uniformDiffuse = 0
    var uniformTransform = -1
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

        if (validated) {

            shaderName += compiler.create(this::class, SHADER_SOURCE_RENDER + ".vert")
            shaderName += compiler.create(this::class, SHADER_SOURCE_RENDER + ".frag")

            programName[Program.RENDER] = glCreateProgram()
            glAttachShader(programName[Program.RENDER], shaderName[Shader.VERT_RENDER])
            glAttachShader(programName[Program.RENDER], shaderName[Shader.FRAG_RENDER])
            glLinkProgram(programName[Program.RENDER])
        }

        if (validated) {

            shaderName += compiler.create(this::class, SHADER_SOURCE_SPLASH + ".vert")
            shaderName += compiler.create(this::class, SHADER_SOURCE_SPLASH + ".frag")

            programName[Program.SPLASH] = glCreateProgram()
            glAttachShader(programName[Program.SPLASH], shaderName[Shader.VERT_SPLASH])
            glAttachShader(programName[Program.SPLASH], shaderName[Shader.FRAG_SPLASH])
            glLinkProgram(programName[Program.SPLASH])
        }

        if (validated) {

            validated = validated && compiler.check()
            validated = validated && compiler checkProgram programName[Program.RENDER]
            validated = validated && compiler checkProgram programName[Program.SPLASH]
        }

        if (validated) {

            uniformTransform = glGetUniformBlockIndex(programName[Program.RENDER], "transform")
            uniformDiffuse = glGetUniformLocation(programName[Program.SPLASH], "Diffuse")

            glUseProgram(programName[Program.RENDER])
            glUniformBlockBinding(programName[Program.RENDER], uniformTransform, semantic.uniform.TRANSFORM0)

            glUseProgram(programName[Program.SPLASH])
            glUniform1i(uniformDiffuse, semantic.sampler.DIFFUSE)
        }

        return validated && checkError("initProgram")
    }

    fun initBuffer(): Boolean {

        val vertices = generateIcosahedron(4)
        vertexCount = vertices.size * Vec3.length
        val vertexData = byteBufferBig(vertexCount * Vec3.SIZE)
        vertices.forEachIndexed { i, it -> it.to(vertexData, i * Vec3.SIZE) }

        glGenBuffers(bufferName)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)

        val uniformBufferOffset = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT)
        val uniformBlockSize = glm.max(Mat4.SIZE, uniformBufferOffset)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM])
        glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, GL_DYNAMIC_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER)

        vertexData.destroy()

        return true
    }

    fun initTexture(): Boolean {

        val windowSize = windowSize * framebufferScale

        glGenTextures(textureName)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureName[Texture.COLORBUFFER])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexStorage2D(GL_TEXTURE_2D, GL_SRGB8_ALPHA8, windowSize)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureName[Texture.RENDERBUFFER])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexStorage2D(GL_TEXTURE_2D, GL_DEPTH_COMPONENT24, windowSize)

        return true
    }

    fun initVertexArray(): Boolean {

        glGenVertexArrays(vertexArrayName)
        glBindVertexArray(vertexArrayName[Program.RENDER])
        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glVertexAttribPointer(glf.pos3)
        glBindBuffer(GL_ARRAY_BUFFER)

        glEnableVertexAttribArray(semantic.attr.POSITION)
        glBindVertexArray()

        glBindVertexArray(vertexArrayName[Program.SPLASH])
        glBindVertexArray()

        return true
    }

    fun initFramebuffer(): Boolean {

        glGenFramebuffers(framebufferName)
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferName)
        glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureName[Texture.COLORBUFFER])
        glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, textureName[Texture.RENDERBUFFER])

        val framebufferEncoding = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING)
        if (framebufferEncoding != GL_SRGB)
            return false

        if (!checkFramebuffer(framebufferName))
            return false

        glBindFramebuffer(GL_FRAMEBUFFER)
/*
		GLint Encoding = -1;
		glGetFramebufferAttachmentParameteriv(GL_DRAW_FRAMEBUFFER, GL_BACK_LEFT, GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, &Encoding);
		GLint Capable = 0;
		glGetIntegerv(GL_FRAMEBUFFER_SRGB_CAPABLE_EXT, &Capable);
*/

        return true
    }

    override fun render(): Boolean {

        run {
            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM])
            val pointer = glMapBufferRange(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT)

            //glm::mat4 Projection = glm::perspectiveFov(glm::pi<float>() * 0.25f, 640.f, 480.f, 0.1f, 100.0f);
            val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100.0f)

            projection * view() to pointer

            // Make sure the uniform buffer is uploaded
            glUnmapBuffer(GL_UNIFORM_BUFFER)
        }

        // Render to a sRGB framebuffer object.
        run {
            glEnable(GL_DEPTH_TEST)
            glDepthFunc(GL_LEQUAL)

            glViewport(windowSize * framebufferScale)
            glBindFramebuffer(GL_FRAMEBUFFER, framebufferName)

            val depth = 1.0f
            glClearBuffer(GL_DEPTH, depth)
            glClearBuffer(GL_COLOR, Vec4(1.0f, 0.5f, 0.0f, 1.0f))

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

        destroyBuffers(framebufferName, bufferName, textureName, vertexArrayName)

        return true
    }
}