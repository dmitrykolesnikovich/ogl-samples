package ogl_samples.tests.gl300

/**
 * Created by GBarbieri on 25.04.2017.
 */


import gli.Texture2d
import gli.gl
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import ogl_samples.framework.Compiler
import ogl_samples.framework.Test
import org.lwjgl.opengl.ARBFramebufferObject.*
import org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.caps.Caps.Profile
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*
import java.nio.IntBuffer

fun main(args: Array<String>) {
    gl_300_test_alpha().run()
}

private class gl_300_test_alpha : Test("gl-300-test-alpha", Profile.COMPATIBILITY, 3, 0) {

    val SHADER_SOURCE = "gl-300/image-2d"
    val TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds"
    val FRAMEBUFFER_SIZE = Vec2i(160, 120)

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    val vertexCount = 6
    val vertexSize = vertexCount * glf.pos2_tc2.stride
    val vertexData = floatBufferOf(
            -2.0f, -1.5f, /**/ 0.0f, 0.0f,
            +2.0f, -1.5f, /**/ 1.0f, 0.0f,
            +2.0f, +1.5f, /**/ 1.0f, 1.0f,
            +2.0f, +1.5f, /**/ 1.0f, 1.0f,
            -2.0f, +1.5f, /**/ 0.0f, 1.0f,
            -2.0f, -1.5f, /**/ 0.0f, 0.0f)

    object Texture {
        val DIFFUSE = 0
        val COLOR = 1
        val MAX = 2
    }

    var programName = 0
    val vertexArrayName = intBufferBig(1)
    val bufferName = intBufferBig(1)
    val textureName = intBufferBig(1)
    val colorRenderbufferName = intBufferBig(1)
    val colorTextureName = intBufferBig(1)
    val framebufferRenderName = intBufferBig(1)
    val framebufferResolveName = intBufferBig(1)
    var uniformMVP = -1
    var uniformDiffuse = -1

    override fun begin(): Boolean {

        var validated = true

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

        return validated && checkError("begin")
    }

    fun initProgram(): Boolean {

        var validated = true

        val compiler = Compiler()

        if (validated) {

            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")

            programName = glCreateProgram()
            glAttachShader(programName, vertShaderName)
            glAttachShader(programName, fragShaderName)

            glBindAttribLocation(programName, semantic.attr.POSITION, "Position")
            glBindAttribLocation(programName, semantic.attr.TEX_COORD, "Texcoord")
            glLinkProgram(programName)

            validated = validated && compiler.check()
            validated = validated && compiler checkProgram programName
        }

        if (validated) {
            uniformMVP = glGetUniformLocation(programName, "MVP")
            uniformDiffuse = glGetUniformLocation(programName, "Diffuse")
        }

        return validated && checkError("initProgram")
    }

    fun initBuffer(): Boolean {

        glGenBuffers(bufferName)
        glBindBuffer(GL_ARRAY_BUFFER, bufferName)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)

        return checkError("initBuffer")
    }

    fun initTexture(): Boolean {

        val texture = Texture2d(gli.loadDDS(javaClass.getResource("/$data/$TEXTURE_DIFFUSE").toURI()))
        gl.profile = gl.Profile.GL32

        glGenTextures(textureName)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        val format = gl.translate(texture.format, texture.swizzles)
        for (level in 0 until texture.levels())
            glTexImage2D(level, format, texture)

        texture.dispose()

        return checkError("initTexture")
    }

    fun initFramebuffer(): Boolean {

        glGenRenderbuffers(colorRenderbufferName)
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbufferName)
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_RGBA8, FRAMEBUFFER_SIZE)
        // The second parameter is the number of samples.

        glGenFramebuffers(framebufferRenderName)
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferRenderName)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbufferName)
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            return false
        glBindFramebuffer(GL_FRAMEBUFFER)

        glGenTextures(colorTextureName)
        glBindTexture(GL_TEXTURE_2D, colorTextureName)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)

        glGenFramebuffers(framebufferResolveName)
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferResolveName)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureName[0], 0)
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            return false
        glBindFramebuffer(GL_FRAMEBUFFER)

        return checkError("initFramebuffer")
    }

    fun initVertexArray(): Boolean {

        glGenVertexArrays(vertexArrayName)
        glBindVertexArray(vertexArrayName)
        glBindBuffer(GL_ARRAY_BUFFER, bufferName)
        glVertexAttribPointer(glf.pos2_tc2[0])
        glVertexAttribPointer(glf.pos2_tc2[1])
        glBindBuffer(GL_ARRAY_BUFFER)

        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEX_COORD)
        glBindVertexArray()

        return checkError("initVertexArray")
    }

    override fun render(): Boolean {

        // Clear the framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glClearBuffer(GL_COLOR, Vec4(1.0f, 0.5f, 0.0f, 1.0f))

        glUseProgram(programName)
        glUniform1i(uniformDiffuse, semantic.sampler.DIFFUSE)

        // Pass 1
        // Render the scene in a multisampled framebuffer
        glEnable(GL_MULTISAMPLE)
        renderFBO(framebufferRenderName)
        glDisable(GL_MULTISAMPLE)

        // Resolved multisampling
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferRenderName)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferResolveName)
        glBlitFramebuffer(FRAMEBUFFER_SIZE)
        glBindFramebuffer(GL_FRAMEBUFFER)

        // Pass 2
        // Render the colorbuffer from the multisampled framebuffer
        glViewport(windowSize)
        renderFB(colorTextureName)

        return true
    }

    fun renderFBO(framebuffer: IntBuffer): Boolean {

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
        glClearColor(0.0f, 0.5f, 1.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)

        val perspective = glm.perspective(glm.PIf * 0.25f, FRAMEBUFFER_SIZE, 0.1f, 100.0f)
        val model = Mat4()
        val mvp = perspective * view * model
        glUniform(uniformMVP, mvp)

        glViewport(FRAMEBUFFER_SIZE)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureName)

        glBindVertexArray(vertexArrayName)
        glDrawArrays(vertexCount)

        return checkError("renderFBO")
    }

    fun renderFB(texture2DName: IntBuffer) {

        val perspective = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100.0f)
        val model = Mat4()
        val mvp = perspective * view * model
        glUniform(uniformMVP, mvp)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture2DName)

        glBindVertexArray(vertexArrayName)
        glDrawArrays(vertexCount)

        checkError("renderFB")
    }

    override fun end(): Boolean {

        glDeleteBuffers(bufferName)
        glDeleteProgram(programName)
        glDeleteTextures(textureName)
        glDeleteTextures(colorTextureName)
        glDeleteRenderbuffers(colorRenderbufferName)
        glDeleteFramebuffers(framebufferRenderName)
        glDeleteFramebuffers(framebufferResolveName)
        glDeleteVertexArrays(vertexArrayName)

        return true
    }
}