package ogl_samples.tests.es200

import glm.BYTES
import glm.glm
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._4.Vec4
import ogl_samples.framework.*
import org.lwjgl.opengl.ARBES2Compatibility.glClearDepthf
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.buffer.shortBufferOf
import uno.caps.Caps.Profile

/**
 * Created by GBarbieri on 27.03.2017.
 */

fun main(args: Array<String>) {
    es_200_draw_elements().setup()
}

class es_200_draw_elements : Test("es-200-draw-elements", Profile.ES, 2, 0) {

    val SHADER_SOURCE = "/es-200/flat-color"

    val elementCount = 6
    val elementSize = elementCount * Short.BYTES
    val elementData = shortBufferOf(
            0, 1, 2,
            0, 2, 3)

    val vertexCount = 4
    val positionSize = vertexCount * Vec2.SIZE
    val positionData = floatBufferOf(
            -1.0f, -1.0f,
            +1.0f, -1.0f,
            +1.0f, +1.0f,
            -1.0f, +1.0f)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(Buffer.MAX)
    var programName = 0
    var uniformMVP = 0
    var uniformDiffuse = 0

    override fun begin(): Boolean {

        var validated = true

        println(gl.VENDOR)
        println(gl.RENDERER)
        println(gl.VERSION)
        gl.EXTENSIONS.forEach(::println)

        if (validated)
            validated = initProgram()

        if (validated)
            validated = initBuffer()

        return validated
    }

    fun initProgram(): Boolean {

        var validated = true

        // Create program
        if (validated) {

            val compiler = Compiler()
            programName = compiler.create(this::class, "$SHADER_SOURCE.vert", "$SHADER_SOURCE.frag")

            withProgram(programName) {
                "Position" to semantic.attr.POSITION
                link()
            }

            validated = validated && compiler.check()
            validated = validated && compiler.checkProgram(programName)
        }

        // Get variables locations
        if (validated)
            withProgram(programName) {
                uniformMVP = "MVP".location
                uniformDiffuse = "Diffuse".location
            }

        // Set some variables
        if (validated)
            usingProgram(programName) {
                // Set uniform value
                Vec4(1.0f, 0.5f, 0.0f, 1.0f) to uniformDiffuse
            }

        return validated && checkError("initProgram")
    }

    fun initBuffer(): Boolean {

        glGenBuffers(bufferName)

        bindingBuffer(bufferName[Buffer.VERTEX] to GL_ARRAY_BUFFER) { data(positionData, GL_STATIC_DRAW) }
        bindingBuffer(bufferName[Buffer.ELEMENT] to GL_ELEMENT_ARRAY_BUFFER) { data(elementData, GL_STATIC_DRAW) }

        return checkError("initBuffer")
    }

    override fun render(): Boolean {

        // Compute the MVP (Model View Projection matrix)
        val projection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 100.0f)
        val model = Mat4()
        val mvp = projection * view() * model

        // Set the display viewport
        glViewport(windowSize)

        // Clear color buffer with black
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClearDepthf(1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Bind program
        usingProgram(programName) {

            // Set the value of MVP uniform.
            mvp to uniformMVP

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
            glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, Vec2.SIZE, 0)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])

            glEnableVertexAttribArray(semantic.attr.POSITION)
            glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0)
            glDisableVertexAttribArray(semantic.attr.POSITION)
        }

        return true
    }

    override fun end(): Boolean {

        glDeleteBuffers(bufferName)
        glDeleteProgram(programName)

        destroyBuffers(bufferName, positionData)

        return true
    }
}