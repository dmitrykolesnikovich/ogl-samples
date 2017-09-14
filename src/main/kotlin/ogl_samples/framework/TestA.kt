package ogl_samples.framework

import gli.wasInit
import glm_.vec2.Vec2
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import uno.buffer.destroy
import uno.buffer.destroyBuf
import uno.buffer.intBufferBig
import uno.caps.Caps
import uno.glf.VertexLayout
import uno.gln.*
import uno.kotlin.buffers.filter
import java.nio.ByteBuffer
import java.nio.ShortBuffer


abstract class TestA(title: String, profile: Caps.Profile, major: Int, minor: Int, orientation: Vec2 = Vec2()) :
        Test(title, profile, major, minor, orientation) {


    var elementCount = 0
    open lateinit var elementData: ShortBuffer

    open var vertexCount = 0
    open lateinit var vertexData: ByteBuffer


    enum class Buffer { VERTEX, ELEMENT, TRANSFORM, PER_SCENE, PER_PASS, PER_DRAW, UNIFORM }
    enum class Texture { COLORBUFFER, RENDERBUFFER, DEPTHBUFFER, RGBA4, RGBA4_REV, BGRA4, BGRA4_REV, DIFFUSE, COLOR, DEPTH, RENDER,
        SHADOWMAP
    }

    enum class Program { RENDER, SPLASH, DEPTH }
    enum class VertexArray { RENDER, SPLASH }   // >= 2 you can rename them
    enum class Renderbuffer { COLOR, COLORBUFFER, DEPTHBUFFER, DEPTH, RENDER }
    enum class Framebuffer { FRAMEBUFFER, RENDER, RESOLVE, DEPTH, SHADOW }

    init {
        bufferName = intBufferBig<Buffer>()
        glGenBuffers(bufferName)
        textureName = intBufferBig<Texture>()
        programName = intArrayBig<Program>()
        vertexArrayName = intBufferBig<VertexArray>()
        renderbufferName = intBufferBig<Renderbuffer>()
        framebufferName = intBufferBig<Framebuffer>()
    }

    enum class Shader { VERT, FRAG, VERT_RENDER, FRAG_RENDER, VERT_SPLASH, FRAG_SPLASH, VERT_DEPTH, FRAG_DEPTH }

    val framebufferScale = 2

    object Uniform {

        var mvp = -1
        var diffuse = -1
        var transform = -1
        var material = -1

        var depthMVP = -1

        var perDraw = -1
        var perPass = -1
        var perScene = -1

        object Light {
            var proj = -1
            var view = -1
            var world = -1
            var pointLightPosition = -1
            var clipNearFar = -1
        }

        object Render {
            var p = -1
            var v = -1
            var w = -1
            var shadow = -1
            var pointLightPosition = -1
            var clipNearFar = -1
            var bias = -1
            var mvp = -1
            var depthBiasMVP = -1
        }
    }

    override fun begin(): Boolean {

        var validated = true

        if (validated)
            validated = initTest()

        if (validated)
            validated = initProgram()

        if (validated)
            validated = initBuffer()

        if (validated)
            validated = initVertexArray()

        if (validated)
            validated = initTexture()

        if (validated)
            validated = initRenderbuffer()

        if (validated)
            validated = initFramebuffer()

        return validated
    }

    open fun initTest() = true
    open fun initProgram() = true

    open fun initBuffer() = initBuffers(vertexData, elementData)

    open fun initBuffers(vertices: ByteBuffer, elements: ShortBuffer): Boolean {

        initArrayBuffer(vertices)

        initElementeBuffer(elements)

        return checkError("TestA.initBuffers")
    }

    open fun initArrayBuffer(vertices: ByteBuffer): Boolean {

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        return checkError("TestA.initArrayBuffer")
    }

    open fun initElementeBuffer(elements: ShortBuffer) {

        elementCount = elements.capacity()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    open fun initVertexArray() = true

    open fun initVertexArray(vertexLayout: VertexLayout): Boolean {

        initVertexArray(vertexArrayName) {
            array(bufferName[Buffer.VERTEX], vertexLayout)
            element(bufferName[Buffer.ELEMENT])
        }
        return checkError("TestA.initVertexArray")
    }

    open fun initTexture() = true
    open fun initRenderbuffer() = true
    open fun initFramebuffer() = true

    override abstract fun render(): Boolean

    override fun end(): Boolean {

        programName.filter(GL20::glIsProgram).map(GL20::glDeleteProgram)

        bufferName.filter(GL15::glIsBuffer).map(GL15::glDeleteBuffers)
        vertexArrayName.filter(GL30::glIsVertexArray).map(GL30::glDeleteVertexArrays)
        textureName.filter(GL11::glIsTexture).map(GL11::glDeleteTextures)
        framebufferName.filter(GL30::glIsFramebuffer).map(GL30::glDeleteFramebuffers)
        renderbufferName.filter(GL30::glIsRenderbuffer).map(GL30::glDeleteRenderbuffers)

        if (wasInit { vertexData }) vertexData.destroy()
        if (wasInit { elementData }) elementData.destroy()

        destroyBuf(bufferName, vertexArrayName, textureName, framebufferName, renderbufferName)

        return checkError("TestA.initVertexArray")
    }
}