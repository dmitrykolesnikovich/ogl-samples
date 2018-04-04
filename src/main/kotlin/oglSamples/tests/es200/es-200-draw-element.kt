package oglSamples.tests.es200

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import gln.checkError
import gln.draw.glDrawElements
import gln.glClearColor
import gln.glClearDepthf
import gln.glViewport
import oglSamples.framework.Compiler
import oglSamples.framework.TestA
import org.lwjgl.opengl.GL11.*
import uno.buffer.bufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps.Profile
import gln.glf.glf
import gln.glf.semantic
import gln.program.programName
import gln.program.usingProgram
import gln.program.withProgram
import gln.vertexArray.withVertexLayout

/**
 * Created by GBarbieri on 27.03.2017.
 */

fun main(args: Array<String>) {
    es_200_draw_elements().loop()
}

class es_200_draw_elements : TestA("es-200-draw-elements", Profile.ES, 2, 0) {

    val SHADER_SOURCE = "/es-200/flat-color"

    override var vertexCount = 4
    override var vertexData = bufferOf(
            Vec2(-1f, -1f),
            Vec2(+1f, -1f),
            Vec2(+1f, +1f),
            Vec2(-1f, +1f))

    override var elementData = shortBufferOf(
            0, 1, 2,
            0, 2, 3)

    override fun begin(): Boolean {

        println(caps.version.VENDOR)
        println(caps.version.RENDERER)
        println(caps.version.VERSION)
        caps.extensions.list.forEach(::println)

        return super.begin()
    }

    override fun initProgram(): Boolean {

        var validated = true

        // Create program
        if (validated) {

            val compiler = Compiler()
            programName[0] = compiler.create("$SHADER_SOURCE.vert", "$SHADER_SOURCE.frag")

            withProgram(programName) {
                "Position".attrib = semantic.attr.POSITION
                link()
            }

            validated = validated && compiler.check()
            validated = validated && compiler checkProgram programName
        }

        // Get variables locations
        if (validated)
            withProgram(programName) {
                Uniform.mvp = "MVP".uniform
                Uniform.diffuse = "Diffuse".uniform
            }

        // Set some variables
        if (validated)
        // Set uniform value
            usingProgram(programName) {
                Vec4(1f, 0.5f, 0f, 1f) to Uniform.diffuse
            }

        return validated && checkError("initProgram")
    }

    override fun render(): Boolean {

        // Compute the MVP (Model View Projection matrix)
        val projection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 100f)
        val model = Mat4()
        val mvp = projection * view * model

        // Set the display viewport
        glViewport(windowSize)

        // Clear color buffer with black
        glClearColor()
        glClearDepthf()
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Bind program
        usingProgram(programName) {

            // Set the value of MVP uniform.
            mvp to Uniform.mvp

            withVertexLayout(Buffer.VERTEX, Buffer.ELEMENT, glf.pos2) {
                glDrawElements(elementCount, GL_UNSIGNED_SHORT)
            }
        }
        return true
    }
}