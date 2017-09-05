package ogl_samples.framework

import gli.wasInit
import glm_.BYTES
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glIsProgram
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.*
import uno.buffer.*
import uno.caps.Caps
import uno.glf.VertexLayout
import uno.glf.glf
import uno.gln.checkError
import uno.gln.initVertexArray
import uno.kotlin.buffers.filter
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer


abstract class TestB(title: String, profile: Caps.Profile, major: Int, minor: Int) : Test(title, profile, major, minor) {

    var elementCount = 0
    open lateinit var elementData: ShortBuffer

    open var vertexCount = 0
    open lateinit var positionData: ByteBuffer

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val TRANSFORM = 2
        val MAX = 3
    }

    val bufferName = intBufferBig(Buffer.MAX).also { glGenBuffers(it) }

    object Texture {

        val COLORBUFFER = 0
        val RENDERBUFFER = 1

        val RGBA4 = COLORBUFFER
        val RGBA4_REV = RENDERBUFFER
        val BGRA4 = 2
        val BGRA4_REV = 3

        val DIFFUSE = COLORBUFFER
        val COLOR = RENDERBUFFER

        val MAX = 4
    }

    val textureName = intBufferBig(Texture.MAX)

    object Program {
        val RENDER = 0
        val SPLASH = 1
        val MAX = 2

    }

    var programName = IntArray(Program.MAX)
    val vertexArrayName = intBufferBig(Program.MAX)

    object Shader {
        val VERT_RENDER = 0
        val FRAG_RENDER = 1
        val VERT_SPLASH = 2
        val FRAG_SPLASH = 3
        val MAX = 4

    }

    object Renderbuffer {
        val COLOR = 0
        val MAX = 1
    }

    val renderbufferName = intBufferBig(Renderbuffer.MAX)

    object Framebuffer {
        val RENDER = 0
        val RESOLVE = 1
        val MAX = 2

    }

    val framebufferName = intBufferBig(Framebuffer.MAX)
    val framebufferScale = 2

    object Uniform {
        var mvp = -1
        var diffuse = -1
        var transform = -1
    }

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
            validated = initRenderbuffer()

        if (validated)
            validated = initFramebuffer()

        return validated
    }

    open fun initProgram() = true

    open fun initBuffer() = initBuffers(positionData, elementData)

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
        if (glIsFramebuffer(framebufferName[0])) glDeleteFramebuffers(framebufferName)

        if (wasInit { positionData }) positionData.destroy()
        if (wasInit { elementData }) elementData.destroy()

        destroyBuf(bufferName, vertexArrayName, textureName, framebufferName)

        return checkError("TestA.initVertexArray")
    }
}