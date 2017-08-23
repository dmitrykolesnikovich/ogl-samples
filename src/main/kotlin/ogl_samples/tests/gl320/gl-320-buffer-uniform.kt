//package ogl_samples.tests.gl320
//
//import glm_.BYTES
//import glm_.L
//import glm_.glm
//import glm_.f
//import glm_.mat3x3.Mat3
//import glm_.mat4x4.Mat4
//import glm_.vec1.Vec1
//import glm_.vec3.Vec3
//import glm_.vec4.Vec4
//import ogl_samples.framework.Test
//import ogl_samples.framework.Compiler
//import org.lwjgl.opengl.GL11.*
//import uno.buffer.byteBufferOf
//import uno.buffer.floatBufferOf
//import uno.buffer.intBufferBig
//import uno.buffer.shortBufferOf
//import uno.caps.Caps.Profile
//import uno.glf.glf
//import unsigned.Ushort
//import java.nio.ByteBuffer
//import org.lwjgl.opengl.GL15.*
//import org.lwjgl.opengl.GL20.*
//import org.lwjgl.opengl.GL30.*
//import org.lwjgl.opengl.GL31.*
//import org.lwjgl.system.MemoryUtil.NULL
//import uno.glf.semantic
//import uno.gln.*
//import kotlin.run as krun
//
//
///**
// * Created by GBarbieri on 10.05.2017.
// */
//
//fun main(args: Array<String>) {
//    gl_320_fbo_readPixels().run()
//}
//
//private class gl_320_fbo_readPixels : Test("gl-320-fbo-readPixels", Profile.CORE, 3, 2) {
//
//    val SHADER_SOURCE = "buffer-uniform"
//
//    val vertexCount = 4
//    val vertexSize = vertexCount * glf.pos3_nor3_col4.size
//    val vertexData = floatBufferOf(
//            // position        // normal        // color
//            -1.0f, -1.0f, 0.0, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
//            +1.0f, -1.0f, 0.0, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f,
//            +1.0f, +1.0f, 0.0, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
//            -1.0f, +1.0f, 0.0, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
//
//    val elementCount = 6
//    val elementSize = elementCount * Ushort.BYTES
//    val elementData = shortBufferOf(
//            0, 1, 2,
//            2, 3, 0)
//
//    object Buffer {
//        val vertex = 0
//        val element = 1
//        val perScene = 2
//        val perPass = 3
//        val perDraw = 4
//        val MAX = 5
//    }
//
//    object Uniform {
//        val perScene = 0
//        val perPass = 1
//        val perDraw = 2
//        val light = 3
//    }
//
//    class Material(val ambient: Vec3, val diffuse: Vec3, val specular: Vec3, val shininess: Float) {
//
//        infix fun to(buffer: ByteBuffer) = buffer.apply {
//            ambient to this
//            diffuse.to(this, Vec4.size)
//            specular.to(this, Vec4.size * 2)
//            putFloat(Vec4.size * 2 + Vec3.size, shininess)
//        }
//
//        companion object {
//            val size = Vec4.size * 2 + Vec3.size
//        }
//    }
//
//    val materialBuffer = byteBufferOf(Material.size)
//
//    class Light(val position: Vec3) {
//
//        infix fun to(buffer: ByteBuffer) = position to buffer
//
//        companion object {
//            val size = Vec3.size
//        }
//    }
//
//    val lightBuffer = byteBufferOf(Light.size)
//
//    class Transform(val buffer: ByteBuffer) {
//
//        var mv = Mat4()
//            set(value) {
//                value to buffer
//                field = value
//            }
//
//        var p = Mat4()
//            set(value) {
//                value.to(buffer, Mat4.size)
//                field = value
//            }
//
//        var normal = Mat3()
//            set(value) {
//                value.to(buffer, Mat4.size * 2)
//                field = value
//            }
//
//        companion object {
//            val size = Mat4.size * 2 + Mat3.size
//        }
//    }
//
//    val vertexArrayName = intBufferBig(1)
//    val bufferName = intBufferBig(Buffer.MAX)
//    var programName = 0
//    var uniformPerDraw = -1
//    var uniformPerPass = -1
//    var uniformPerScene = -1
//
//    override fun begin(): Boolean {
//
//        var validated = true
//
//        if (validated)
//            validated = initProgram()
//        if (validated)
//            validated = initBuffer()
//        if (validated)
//            validated = initVertexArray()
//
//        glEnable(GL_DEPTH_TEST)
//        glBindFramebuffer(GL_FRAMEBUFFER, 0)
//        glDrawBuffer(GL_BACK)
//        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
//            return false
//
//        return validated
//    }
//
//    fun initProgram(): Boolean {
//
//        var validated = true
//
//        if (validated) {
//
//            val compiler = Compiler()
//            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
//            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")
//
//            programName = glCreateProgram()
//            glAttachShader(programName, vertShaderName)
//            glAttachShader(programName, fragShaderName)
//
//            glBindAttribLocation(programName, semantic.attr.POSITION, "Position")
//            glBindAttribLocation(programName, semantic.attr.NORMAL, "Normal")
//            glBindAttribLocation(programName, semantic.attr.COLOR, "Color")
//            glBindFragDataLocation(programName, semantic.frag.COLOR, "Color")
//            glLinkProgram(programName)
//
//            validated = validated && compiler.check()
//            validated = validated && compiler.checkProgram(programName)
//        }
//
//        if (validated) {
//            uniformPerDraw = glGetUniformBlockIndex(programName, "per_draw")
//            uniformPerPass = glGetUniformBlockIndex(programName, "per_pass")
//            uniformPerScene = glGetUniformBlockIndex(programName, "per_scene")
//
//            glUniformBlockBinding(programName, uniformPerDraw, Uniform.perDraw)
//            glUniformBlockBinding(programName, uniformPerPass, Uniform.perPass)
//            glUniformBlockBinding(programName, uniformPerScene, Uniform.perScene)
//        }
//
//        return validated
//    }
//
//    fun initBuffer(): Boolean {
//
//        glGenBuffers(bufferName)
//
//        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.element])
//        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementData, GL_STATIC_DRAW)
//        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
//
//        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.vertex])
//        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
//        glBindBuffer(GL_ARRAY_BUFFER, 0)
//
//        krun {
//
//            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.perDraw])
//            glBufferData(GL_UNIFORM_BUFFER, Transform.size.L, GL_DYNAMIC_DRAW)
//            glBindBuffer(GL_UNIFORM_BUFFER, 0)
//        }
//
//        krun {
//
//            val light = Light(Vec3(0.0f, 0.0f, 100.f))
//
//            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.perPass])
//            glBufferData(GL_UNIFORM_BUFFER, light to lightBuffer, GL_STATIC_DRAW)
//            glBindBuffer(GL_UNIFORM_BUFFER, 0)
//        }
//
//        krun {
//
//            val material = Material(Vec3(0.7f, 0.0f, 0.0f), Vec3(0.0f, 0.5f, 0.0f), Vec3(0.0f, 0.0f, 0.5f), 128.0f)
//
//            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.perScene])
//            glBufferData(GL_UNIFORM_BUFFER, material to materialBuffer, GL_STATIC_DRAW)
//            glBindBuffer(GL_UNIFORM_BUFFER, 0)
//        }
//
//        return true
//    }
//
//    fun initVertexArray(): Boolean {
//
//        glGenVertexArrays(vertexArrayName)
//        glBindVertexArray(vertexArrayName[0])
//        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.vertex])
//        glVertexAttribPointer(glf.pos3_nor3_col4)
//        glVertexAttribPointer(glf.pos3_nor3_col4[1])
//        glVertexAttribPointer(glf.pos3_nor3_col4[2])
//        glBindBuffer(GL_ARRAY_BUFFER, 0)
//
//        glEnableVertexAttribArray(glf.pos3_nor3_col4)
//        glEnableVertexAttribArray(glf.pos3_nor3_col4[1])
//        glEnableVertexAttribArray(glf.pos3_nor3_col4[2])
//
//        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.element])
//        glBindVertexArray()
//
//        return true
//    }
//
//    override fun render(): Boolean {
//
//        krun {
//            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.perDraw])
//            val transform = Transform(glMapBufferRange(GL_UNIFORM_BUFFER, Transform.size, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT))
//
//            val projection = glm.perspective(glm.PIf * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f)
//            val view = view()
//            val model = glm.rotate(Mat4(1.0f), -glm.PIf * 0.5f, Vec3(0.0f, 0.0f, 1.0f))
//
//            transform.mv = view * model
//            transform.p = projection
//            transform.normal = transform.mv.inverse().transpose_().toMat3()
//
//            glUnmapBuffer(GL_UNIFORM_BUFFER)
//            glBindBuffer(GL_UNIFORM_BUFFER, 0)
//        }
//
//        glViewport(windowSize)
//        glClearBuffer(GL_COLOR, 0, Vec4(0.2f, 0.2f, 0.2f, 1.0f))
//        glClearBuffer(GL_DEPTH, 0, Vec1(1.0f))
//
//        glUseProgram(ProgramName)
//        glBindBufferBase(GL_UNIFORM_BUFFER, uniform::PER_SCENE, BufferName[buffer::PER_SCENE])
//        glBindBufferBase(GL_UNIFORM_BUFFER, uniform::PER_PASS, BufferName[buffer::PER_PASS])
//        glBindBufferBase(GL_UNIFORM_BUFFER, uniform::PER_DRAW, BufferName[buffer::PER_DRAW])
//        glBindVertexArray(VertexArrayName)
//
//        glDrawElementsInstancedBaseVertex(GL_TRIANGLES, ElementCount, GL_UNSIGNED_SHORT, nullptr, 1, 0)
//
//        return true
//    }
//}