package ogl_samples.framework

import glm_.BYTES
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glIsProgram
import uno.buffer.*
import uno.caps.Caps
import uno.gln.checkError
import java.nio.FloatBuffer
import java.nio.ShortBuffer

abstract class TestA(title: String, profile: Caps.Profile, major: Int, minor: Int) : Test(title, profile, major, minor) {

    var elementCount = 0
    var elementSize = 0
    lateinit var elementData: ShortBuffer

    var vertexCount = 0
    var positionSize = 0
    lateinit var positionData: FloatBuffer

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(Buffer.MAX).also { glGenBuffers(it) }
    var programName = 0
    var uniformMVP = 0
    var uniformDiffuse = 0
    val vertexArrayName = intBufferBig(1)

    override fun begin(): Boolean {

        var validated = true

        if (validated)
            validated = initProgram()

        if (validated)
            validated = initBuffer()

        return validated
    }

    open fun initProgram() = true
    open fun initBuffer() = true

    open fun initBuffers(vertices : FloatArray, elements: ShortArray): Boolean {

        initArrayBuffer(*vertices)

        initElementeBuffer(*elements)

        return checkError("initBuffers")
    }

    open fun initArrayBuffer(vararg args: Float) {

        positionData = floatBufferOf(*args)

        vertexCount = args.size
        positionSize = vertexCount * Float.BYTES

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, positionData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    open fun initElementeBuffer(vararg args: Short) {

        elementData = shortBufferOf(*args)

        elementCount = args.size
        elementSize = elementCount * Short.BYTES

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementData, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override abstract fun render():Boolean

    override fun end(): Boolean {

        for (i in 0 until Buffer.MAX)
            if (glIsBuffer(bufferName[i])) {
                glDeleteBuffers(bufferName[i])
                when (i) {
                    Buffer.VERTEX -> positionData.destroy()
                    Buffer.ELEMENT -> elementData.destroy()
                }
            }
        if (glIsProgram(programName))
            glDeleteProgram(programName)

        destroyBuf(bufferName)

        return true
    }
}