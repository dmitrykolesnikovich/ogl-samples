package ogl_samples.tests.es300

import glm.BYTES
import glm.glm
import glm.mat.Mat4
import glm.set
import glm.vec._2.Vec2
import glm.vec._4.Vec4
import ogl_samples.framework.*
import org.lwjgl.opengl.ARBES2Compatibility.glClearDepthf
import org.lwjgl.opengl.ARBVertexArrayObject.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.buffer.intBufferOf
import uno.caps.Caps.Profile

/**
 * Created by GBarbieri on 30.03.2017.
 */

fun main(args: Array<String>) {
    es_300_draw_elements().setup()
}

class es_300_draw_elements : Test("es-300-draw-elements", Profile.ES, 3, 0) {

    val SHADER_SOURCE = "es-300/flat-color"
    val FRAGMENT_SHADER_SOURCE_FAIL = "es-300/flat-color-fail.frag"

    val elementCount = 6
    val elementSize = elementCount * Int.BYTES
    val elementData = intBufferOf(
            0, 1, 2,
            0, 2, 3)

    val vertexCount = 4
    val positionSize = vertexCount * Vec2.SIZE
    val positionData = floatBufferOf(
            -1.0f, -1.0f,
            +1.0f, -1.0f,
            +1.0f, +1.0f,
            -1.0f, +1.0f)

    object buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(buffer.MAX)
    val vertexArrayName = intBufferBig(1)
    var programName = 0
    var uniformMVP = 0
    var uniformDiffuse = 0

    val projection = Mat4()

    val buffers = intBufferBig(1)

    override fun begin(): Boolean {

        var validated = true

        println(glGetString(GL_VENDOR))
        println(glGetString(GL_RENDERER))
        println(glGetString(GL_VERSION))
        glGetString(GL_EXTENSIONS).split("\\s+".toRegex()).forEach(::println)

        if (validated)
            validated = initProgram()
        if (validated)
            validated = initBuffer()
        if (validated)
            validated = initVertexArray()

        return validated
    }

    fun initProgram(): Boolean {

        var validated = true

        // Check fail positive
        if (validated) {

            val compiler = Compiler()
            val FragShaderName = compiler.create(this::class, FRAGMENT_SHADER_SOURCE_FAIL)

//            validated = validated && !compiler.check()  TODO
        }

        // Create program
        if (validated) {

            val compiler = Compiler()
            val vertShaderName = compiler.create(this::class, SHADER_SOURCE + ".vert")
            val fragShaderName = compiler.create(this::class, SHADER_SOURCE + ".frag")

            programName = glCreateProgram()
            glAttachShader(programName, vertShaderName)
            glAttachShader(programName, fragShaderName)

            glBindAttribLocation(programName, semantic.attr.POSITION, "Position")
            glLinkProgram(programName)

            validated = validated && compiler.check()
            validated = validated && compiler.checkProgram(programName)
        }

        // Get variables locations
        if (validated) {
            uniformMVP = glGetUniformLocation(programName, "MVP")
            uniformDiffuse = glGetUniformLocation(programName, "Diffuse")
        }

        // Set some variables
        if (validated) {

            // Bind the program for use
            glUseProgram(programName)

            // Set uniform value
            glUniform4fv(uniformDiffuse, Vec4(1.0f, 0.5f, 0.0f, 1.0f))

            // Unbind the program
            glUseProgram(0)
        }

        return validated && checkError("initProgram")
    }

    fun initBuffer(): Boolean {

        glGenBuffers(bufferName)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, positionData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[buffer.ELEMENT])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementData, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        return checkError("initBuffer")
    }

    fun initVertexArray(): Boolean {

        glGenVertexArrays(vertexArrayName)
        glBindVertexArray(vertexArrayName[0])
        glBindBuffer(GL_ARRAY_BUFFER, bufferName[buffer.VERTEX])
        glVertexAttribPointer(semantic.attr.POSITION, Vec2.length, GL_FLOAT, false, Vec2.SIZE, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[buffer.ELEMENT])

        glEnableVertexAttribArray(semantic.attr.POSITION)
        glBindVertexArray(0)

        return checkError("initVertexArray")
    }

    override fun render(): Boolean {

        buffers[0] = GL_BACK
        glDrawBuffers(buffers)

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
        glUseProgram(programName)

        // Set the value of MVP uniform.
        glUniformMatrix4fv(uniformMVP, mvp)

        glBindVertexArray(vertexArrayName[0])

        glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, 0)

        return true
    }

    override fun end(): Boolean {

        glDeleteBuffers(bufferName)
        glDeleteVertexArrays(vertexArrayName)
        glDeleteProgram(programName)

        destroyBuffers(bufferName, vertexArrayName, buffers)

        return true
    }
}