//package ogl_samples.tests.es300
//
//import glm_.BYTES
//import glm_.glm
//import glm_.mat4x4.Mat4
//import glm_.vec2.Vec2
//import glm_.vec4.Vec4
//import ogl_samples.framework.Compiler
//import ogl_samples.framework.Test
//import org.lwjgl.opengl.ARBVertexArrayObject.glDeleteVertexArrays
//import org.lwjgl.opengl.GL11.*
//import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
//import org.lwjgl.opengl.GL15.glDeleteBuffers
//import org.lwjgl.opengl.GL20.*
//import uno.buffer.destroyBuffers
//import uno.buffer.floatBufferOf
//import uno.buffer.intBufferBig
//import uno.buffer.intBufferOf
//import uno.caps.Caps.Profile
//import uno.glf.glf
//import uno.glf.semantic
//import uno.gln.*
//
///**
// * Created by GBarbieri on 30.03.2017.
// */
//
//fun main(args: Array<String>) {
//    es_300_draw_elements().run()
//}
//
//private class es_300_draw_elements : Test("es-300-draw-elements", Profile.ES, 3, 0) {
//
//    val SHADER_SOURCE = "es-300/flat-color"
//    val FRAGMENT_SHADER_SOURCE_FAIL = "es-300/flat-color-fail.frag"
//
//    val elementCount = 6
//    val elementSize = elementCount * Int.BYTES
//    val elementData = intBufferOf(
//            0, 1, 2,
//            0, 2, 3)
//
//    val vertexCount = 4
//    val positionSize = vertexCount * Vec2.size
//    val positionData = floatBufferOf(
//            -1.0f, -1.0f,
//            +1.0f, -1.0f,
//            +1.0f, +1.0f,
//            -1.0f, +1.0f)
//
//    object buffer {
//        val VERTEX = 0
//        val ELEMENT = 1
//        val MAX = 2
//    }
//
//    val bufferName = intBufferBig(buffer.MAX)
//    val vertexArrayName = intBufferBig(1)
//    var programName = 0
//    var uniformMVP = 0
//
//    val projection = Mat4()
//
//    override fun begin(): Boolean {
//
//        var validated = true
//
//        println(caps.version.VENDOR)
//        println(caps.version.RENDERER)
//        println(caps.version.VERSION)
//        caps.extensions.list.forEach(::println)
//
//        if (validated)
//            validated = initProgram()
//        if (validated)
//            validated = initBuffer()
//        if (validated)
//            validated = initVertexArray()
//
//        return validated
//    }
//
//    fun initProgram(): Boolean {
//
//        var validated = true
//
//        // Check fail positive
//        if (validated) {
//
//            val compiler = Compiler()
//            val FragShaderName = compiler.create(FRAGMENT_SHADER_SOURCE_FAIL)
//
////            validated = validated && !compiler.check()  TODO
//        }
//
//        // Create program
//        if (validated) {
//
//            val compiler = Compiler()
//
//            programName = compiler.create("$SHADER_SOURCE.vert", "$SHADER_SOURCE.frag")
//
//            withProgram(programName) {
//                "Position".location = semantic.attr.POSITION
//                link()
//            }
//
//            validated = validated && compiler.check()
//            validated = validated && compiler checkProgram programName
//        }
//
//        // Get variables locations
//        if (validated)
//            uniformMVP = glGetUniformLocation(programName, "MVP")
//
//        // Set some variables
//        if (validated)
//            usingProgram(programName) { Vec4(1.0f, 0.5f, 0.0f, 1.0f) to "Diffuse" }
//
//        return validated && checkError("initProgram")
//    }
//
//    fun initBuffer(): Boolean {
//
//        initBuffers(bufferName) {
//            withArrayAt(buffer.VERTEX) {data(positionData, GL_STATIC_DRAW)}
//            withElementAt(buffer.ELEMENT) {data(elementData, GL_STATIC_DRAW)}
//        }
//
//        return checkError("initBuffer")
//    }
//
//    fun initVertexArray(): Boolean {
//
//        initVertexArray(vertexArrayName) {
//            array(bufferName[buffer.VERTEX], glf.pos2)
//            element(bufferName[buffer.ELEMENT])
//        }
//
//        return checkError("initVertexArray")
//    }
//
//    override fun render(): Boolean {
//
//        val buffers = GL_BACK
//        glDrawBuffers(buffers)
//
//        // Compute the MVP (Model View Projection matrix)
//        val projection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 100.0f)
//        val model = Mat4()
//        val mvp = projection * view * model
//
//        // Set the display viewport
//        glViewport(windowSize)
//
//        // Clear color buffer with black
//        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
//        glClearDepthf()
//        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
//
//        // Bind program
//        glUseProgram(programName)
//
//        // Set the value of MVP uniform.
//        glUniform(uniformMVP, mvp)
//
//        glBindVertexArray(vertexArrayName)
//
//        glDrawElements(elementCount)
//
//        return true
//    }
//
//    override fun end(): Boolean {
//
//        glDeleteBuffers(bufferName)
//        glDeleteVertexArrays(vertexArrayName)
//        glDeleteProgram(programName)
//
//        destroyBuffers(bufferName, vertexArrayName)
//
//        return true
//    }
//}