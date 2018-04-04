package oglSamples.tests.gl320

import glm_.glm
import glm_.mat3x3.Mat3
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import oglSamples.framework.Compiler
import oglSamples.framework.TestA
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex
import uno.buffer.bufferBig
import uno.buffer.bufferOf
import uno.buffer.destroyBuf
import uno.buffer.shortBufferOf
import uno.caps.Caps.Profile
import gln.glf.Vertex
import gln.glf.glf
import gln.glf.semantic
import java.nio.ByteBuffer


/**
 * Created by GBarbieri on 10.05.2017.
 */

fun main(args: Array<String>) {
    gl_320_buffer_uniform().loop()
}

private class gl_320_buffer_uniform : TestA("gl-320-buffer-uniform", Profile.CORE, 3, 2) {

    val SHADER_SOURCE = "gl-320/buffer-uniform"

    override var vertexCount = 4
    override var vertexData = bufferOf(
            Vertex.pos3_nor3_col4(Vec3(-1f, -1f, 0), Vec3(0f, 0f, 1f), Vec4(1f, 0f, 0f, 1f)),
            Vertex.pos3_nor3_col4(Vec3(+1f, -1f, +0), Vec3(0f, 0f, 1f), Vec4(0f, 1f, 0f, 1f)),
            Vertex.pos3_nor3_col4(Vec3(+1f, +1f, +0), Vec3(0f, 0f, 1f), Vec4(0f, 0f, 1f, 1f)),
            Vertex.pos3_nor3_col4(Vec3(-1f, +1f, +0), Vec3(0f, 0f, 1f), Vec4(1f, 1f, 1f, 1f)))

    override var elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0)

    class Material(
            val ambient: Vec3,
            // padding1: Float,
            val diffuse: Vec3,
            // padding2: Float,
            val specular: Vec3,
            val shininess: Float) {

        infix fun to(buffer: ByteBuffer): ByteBuffer {
            ambient to buffer
            diffuse.to(buffer, Vec4.size)
            specular.to(buffer, Vec4.size * 2)
            buffer.putFloat(Vec4.size * 2 + Vec3.size, shininess)
            return buffer
        }

        companion object {
            val size = Vec4.size * 3
        }
    }

    val materialBuffer = bufferBig(Material.size)

    class Light(val position: Vec3) {

        infix fun to(buffer: ByteBuffer) = position to buffer

        companion object {
            val size = Vec3.size
        }
    }

    val lightBuffer = bufferBig(Light.size)

    object Transform {

        val size = Mat4.size * 2 + Mat3.size
        lateinit var buffer: ByteBuffer

        var p = Mat4()
            set(value) {
                value to buffer
                field = value
            }

        var mv = Mat4()
            set(value) {
                value.to(buffer, Mat4.size)
                field = value
            }

        var normal = Mat3()
            set(value) {
                value.to(buffer, Mat4.size * 2)
                field = value
            }
    }

    override fun begin(): Boolean {

        val validated = super.begin()

        glEnable(GL_DEPTH_TEST)
        glBindFramebuffer(0)
        glDrawBuffer(GL_BACK)
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) return false

        return validated
    }

    override fun initProgram(): Boolean {

        var validated = true

        if (validated) {

            val compiler = Compiler()
            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {

                attach(vertShaderName, fragShaderName)

                "Position".attrib = semantic.attr.POSITION
                "Normal".attrib = semantic.attr.NORMAL
                "Color".attrib = semantic.attr.COLOR
                "Color".fragData = semantic.frag.COLOR
                link()

                validated = validated && compiler.check()
                validated = validated && compiler checkProgram name
            }
        }

        if (validated) withProgram(programName) {
            "per_draw".blockIndex = semantic.uniform.PER_DRAW
            "per_pass".blockIndex = semantic.uniform.PER_PASS
            "per_scene".blockIndex = semantic.uniform.PER_SCENE
        }

        return validated
    }

    override fun initBuffer(): Boolean {

        initBuffers(vertexData, elementData)

        withUniformBuffer(Buffer.PER_DRAW) { data(Transform.size, GL_DYNAMIC_DRAW) }

        withUniformBuffer(Buffer.PER_PASS) {
            val light = Light(Vec3(0f, 0f, 100f))
            data(light to lightBuffer, GL_STATIC_DRAW)
        }

        withUniformBuffer(Buffer.PER_SCENE) {
            val material = Material(Vec3(0.7f, 0f, 0f), Vec3(0f, 0.5f, 0f), Vec3(0f, 0f, 0.5f), 128f)
            data(material to materialBuffer, GL_STATIC_DRAW)
        }
        return true
    }

    override fun initVertexArray() = initVertexArray(glf.pos3_nor3_col4)

    override fun render(): Boolean {

        mappingUniformBufferRange(Buffer.PER_DRAW, Transform.size, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT) {

            Transform.buffer = buffer

            val projection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 100f)
            val model = glm.rotate(Mat4(), -glm.PIf * 0.5f, Vec3(0f, 0f, 1f))

            Transform.mv = view * model
            Transform.p = projection
            Transform.normal = Transform.mv.inverse().transpose().toMat3()
        }

        glViewport(windowSize)
        glClearColorBuffer(0.2f, 0.2f, 0.2f, 1f)
        glClearDepthBuffer()

        glUseProgram(programName)
        glBindUniformBufferBase(semantic.uniform.PER_SCENE, Buffer.PER_SCENE)
        glBindUniformBufferBase(semantic.uniform.PER_PASS, Buffer.PER_PASS)
        glBindUniformBufferBase(semantic.uniform.PER_DRAW, Buffer.PER_DRAW)
        glBindVertexArray(vertexArrayName)

        glDrawElementsInstancedBaseVertex(elementCount, GL_UNSIGNED_SHORT, 1, 0)

        return true
    }

    override fun end(): Boolean {
        super.end()
        destroyBuf(materialBuffer, lightBuffer)
        return true
    }
}