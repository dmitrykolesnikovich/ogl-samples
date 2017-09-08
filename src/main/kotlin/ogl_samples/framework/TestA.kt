package ogl_samples.framework

import gli.wasInit
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
import uno.gln.checkError
import uno.gln.initVertexArray
import uno.kotlin.buffers.filter
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer


abstract class TestA(title: String, profile: Caps.Profile, major: Int, minor: Int) : Test(title, profile, major, minor) {

    var elementCount = 0
    open lateinit var elementData: ShortBuffer

    open var vertexCount = 0
    open lateinit var vertexData: ByteBuffer


    operator fun IntBuffer.get(e: Enum<*>) = get(e.ordinal)
    operator fun IntArray.get(e: Enum<*>) = get(e.ordinal)
    operator fun IntArray.set(e: Enum<*>, int: Int) = set(e.ordinal, int)


    enum class Buffer { VERTEX, ELEMENT, TRANSFORM }

    val bufferName = intBufferBig<Buffer>().also { glGenBuffers(it) }


    enum class Texture { COLORBUFFER, RENDERBUFFER, RGBA4, RGBA4_REV, BGRA4, BGRA4_REV, DIFFUSE, COLOR }

    val textureName = intBufferBig<Texture>()


    enum class Program { RENDER, SPLASH }

    var programName = IntArray(Program.values().size)


    enum class VertexArray { RENDER, SPLASH }   // >= 2 you can rename them

    val vertexArrayName = intBufferBig<VertexArray>()


    enum class Shader { VERT_RENDER, FRAG_RENDER, VERT_SPLASH, FRAG_SPLASH }


    enum class Renderbuffer { COLOR }

    val renderbufferName = intBufferBig<Renderbuffer>()


    enum class Framebuffer { RENDER, RESOLVE }

    val framebufferName = intBufferBig<Framebuffer>()
    val framebufferScale = 2

    object Uniform {
        var mvp = -1
        var diffuse = -1
        var transform = -1
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