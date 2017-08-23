package ogl_samples.tests.es200

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL20.glDeleteProgram
import uno.buffer.destroyBuffers
import uno.caps.Caps.Profile
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

/**
 * Created by GBarbieri on 27.03.2017.
 */

fun main(args: Array<String>) {
    es_200_draw_elements().run()
}

private class es_200_draw_elements : TestA("es-200-draw-elements", Profile.ES, 2, 0) {

    val SHADER_SOURCE = "/es-200/flat-color"

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
            programName = compiler.create("$SHADER_SOURCE.vert", "$SHADER_SOURCE.frag")

            withProgram(programName) {
                "Position".location = semantic.attr.POSITION
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
        // Set uniform value
            usingProgram(programName) {
                Vec4(1.0f, 0.5f, 0.0f, 1.0f) to uniformDiffuse
            }

        return validated && checkError("initProgram")
    }

    override fun initBuffer() = initBuffers(floatArrayOf(
            -1f, -1f,
            +1f, -1f,
            +1f, +1f,
            -1f, +1f),
            shortArrayOf(
                    0, 1, 2,
                    0, 2, 3))

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
            mvp to uniformMVP

            withVertexLayout(bufferName[Buffer.VERTEX], bufferName[Buffer.ELEMENT], glf.pos2) {
                glDrawElements(elementCount, GL_UNSIGNED_SHORT)
            }
        }
        return true
    }
}