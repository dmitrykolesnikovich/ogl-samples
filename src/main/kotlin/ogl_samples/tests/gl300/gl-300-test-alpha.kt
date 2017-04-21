package ogl_samples.tests.gl300

import gli.Texture2d
import gli.gl
import gli.loadDDS
import ogl_samples.framework.Compiler
import ogl_samples.framework.Test
import ogl_samples.framework.glNext.*
import ogl_samples.framework.glf
import ogl_samples.framework.semantic
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL41.GL_MAX_VARYING_VECTORS
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.caps.Caps
import uno.glf.Vertex_v2fv2f
import glm.glm
import glm.mat.Mat4
import org.lwjgl.opengl.GL30.*
import uno.buffer.destroyBuffers

/**
 * Created by elect on 21/04/17.
 */

fun main(args: Array<String>) {
    gl_300_test_alpha().run()
}

class gl_300_test_alpha : Test("gl-300-test-alpha", Caps.Profile.COMPATIBILITY, 3, 0) {

    val SHADER_SOURCE = "gl-300/image-2d"
    val TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds"

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    val vertexCount = 6
    val vertexSize = vertexCount * Vertex_v2fv2f.SIZE
    val vertexData = floatBufferOf(
            -1.0f, -1.0f, /**/ 0.0f, 1.0f,
            +1.0f, -1.0f, /**/ 1.0f, 1.0f,
            +1.0f, +1.0f, /**/ 1.0f, 0.0f,
            +1.0f, +1.0f, /**/ 1.0f, 0.0f,
            -1.0f, +1.0f, /**/ 0.0f, 0.0f,
            -1.0f, -1.0f, /**/ 0.0f, 1.0f)

    object Texture {
        val DIFFUSE = 0
        val COLOR = 1
        val MAX = 2
    }

    var programName = 0
    val vertexArrayName = intBufferBig(1)
    val bufferName = intBufferBig(1)
    val texture2DName = intBufferBig(1)
    var uniformMVP = -1
    var uniformDiffuse = -1

    override fun begin(): Boolean {

        var validated = true

        val maxVaryingOutputComp = glGetInteger(GL_MAX_VARYING_COMPONENTS)
        val maxVaryingOutputVec = glGetInteger(GL_MAX_VARYING_VECTORS)

        if(validated)
            validated = initTest()
        if(validated)
            validated = initProgram()
        if(validated)
            validated = initBuffer()
        if(validated)
            validated = initVertexArray()
        if(validated)
            validated = initTexture()

        return validated && checkError("begin")
    }

    fun initProgram(): Boolean {

        var validated = true

        if (validated) {

            val compiler = Compiler()
            val vertShaderName = compiler.create(this::class, SHADER_SOURCE + ".vert")
            val fragShaderName = compiler.create(this::class, SHADER_SOURCE + ".frag")

            programName = glCreateProgram()
            glAttachShader(programName, vertShaderName)
            glAttachShader(programName, fragShaderName)

            glBindAttribLocation(programName, semantic.attr.POSITION, "Position")
            glBindAttribLocation(programName, semantic.attr.TEXCOORD, "Texcoord")
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

        glGenTextures(texture2DName)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture2DName)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        gl.profile = gl.Profile.GL32
        val texture = Texture2d(loadDDS(javaClass.getResource("/$data/$TEXTURE_DIFFUSE").toURI()))
        val format = gl.translate(texture.format, texture.swizzles)
        for (level in 0 until texture.levels())
            glTexImage2D(level, format, texture)

        texture.dispose()

        return checkError("initTexture")
    }

    fun initVertexArray(): Boolean {

        glGenVertexArrays(vertexArrayName)
        glBindVertexArray(vertexArrayName)
        glBindBuffer(GL_ARRAY_BUFFER, bufferName)
        glVertexAttribPointer(glf.pos2_tc2)
        glVertexAttribPointer(glf.pos2_tc2[1])
        glBindBuffer(GL_ARRAY_BUFFER)

        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.TEXCOORD)
        glBindVertexArray()

        return checkError("initVertexArray")
    }

    fun initTest(): Boolean {

        glEnable(GL_ALPHA_TEST)
        glAlphaFunc(GL_GREATER, 0.2f)

        //To test alpha blending:
        //glEnable(GL_BLEND);
        //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        return checkError("initTest")
    }

    override fun render():Boolean    {

        val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100.0f)
        val model = Mat4()
        val mvp = projection * view() * model

        glViewport(windowSize)
        glClearColor(1.0f, 0.5f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)

        glUseProgram(programName)
        glUniform1i(uniformDiffuse, 0)
        glUniformMatrix4f(uniformMVP, mvp)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture2DName)

        glBindVertexArray(vertexArrayName)

        glDrawArrays(vertexCount)

        return true
    }

    override fun end():Boolean    {

        glDeleteBuffers(bufferName)
        glDeleteProgram(programName)
        glDeleteTextures(texture2DName)
        glDeleteVertexArrays(vertexArrayName)

        destroyBuffers(bufferName, texture2DName, vertexArrayName)

        return true
    }
}