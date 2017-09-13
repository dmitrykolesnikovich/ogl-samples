package ogl_samples.tests.es300

import glm_.BYTES
import glm_.glm
import glm_.i
import glm_.mat4x4.Mat4
import glm_.s
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4i
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_UNSIGNED_SHORT_4_4_4_4
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import uno.buffer.bufferBig
import uno.buffer.bufferOf
import uno.caps.Caps.Profile
import uno.glf.Vertex
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

/**
 * Created by GBarbieri on 30.03.2017.
 */

fun main(args: Array<String>) {
    es_300_texture_format_packed().loop()
}

private class es_300_texture_format_packed : TestA("es-300-texture-format-packed", Profile.ES, 3, 0) {

    val SHADER_SOURCE = "es-300/texture-format-packed"

    override var vertexCount = 8
    override var vertexData = bufferOf(
            Vertex.pos2_tc2(Vec2(-1f, -1f), Vec2(0f, 0f)),
            Vertex.pos2_tc2(Vec2(-1f, 3f), Vec2(0f, 1f)),
            Vertex.pos2_tc2(Vec2(3f, -1f), Vec2(1f, 0f)))

    val viewport = arrayOf(
            Vec4i(0, 0, 320, 240),
            Vec4i(320, 0, 320, 240),
            Vec4i(0, 240, 320, 240),
            Vec4i(320, 240, 320, 240))

    override fun initProgram(): Boolean {

        var validated = true

        val compiler = Compiler()
        val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
        val fragShaderName = compiler.create("$SHADER_SOURCE.frag")
        validated = validated && compiler.check()

        initProgram(programName) {

            attach(vertShaderName, fragShaderName)

            "Position".attrib = semantic.attr.POSITION
            "Texcoord".attrib = semantic.attr.TEX_COORD
            "Color".fragData = semantic.frag.COLOR
            link()
            validated = validated && compiler checkProgram name

            Uniform.mvp = "MVP".uniform
            validated = validated && Uniform.mvp != -1
            Uniform.diffuse = "Diffuse".uniform
            validated = validated && Uniform.diffuse != -1
        }
        return validated && checkError("initProgram")
    }

    override fun initBuffer() = initArrayBuffer(vertexData)

    override fun initTexture(): Boolean {

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        initTextures2d(textureName) {

            val dataRGBA4 = bufferBig(Short.BYTES).putShort(0, 0xF80C.s)
            val dataRGBA8 = bufferBig(Int.BYTES).putInt(0, 0xCC0088FF.i)
            val dataBGRA4 = bufferBig(Short.BYTES).putShort(0, 0x08FC)
            val dataBGRA8 = bufferBig(Int.BYTES).putInt(0, 0xCCFF8800.i)

            at(Texture.RGBA4) {
                levels(base = 0, max = 0)
                filter(min = nearest, mag = nearest)
                image(GL_RGBA4, 1, 1, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, dataRGBA4)
            }
            at(Texture.RGBA4_REV) {
                levels(base = 0, max = 0)
                filter(min = nearest, mag = nearest)
                image(GL_RGBA8, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, dataRGBA8)
            }
            at(Texture.BGRA4) {
                levels(base = 0, max = 0)
                filter(min = nearest, mag = nearest)
                swizzle(r = blue, g = green, b = red, a = alpha)
                image(GL_RGBA4, 1, 1, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, dataBGRA4)
            }
            at(Texture.BGRA4_REV) {
                levels(base = 0, max = 0)
                filter(min = nearest, mag = nearest)
                swizzle(r = blue, g = green, b = red, a = alpha)
                image(GL_RGBA8, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, dataBGRA8)
            }
        }

        return checkError("initTexture")
    }

    override fun initVertexArray() = initVertexArray(glf.pos2_tc2)

    override fun render(): Boolean {

        val projection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 100f)
        val model = glm.scale(Mat4(), Vec3(3f))
        val mvp = projection * view * model

        usingProgram(programName) {

            glUniform(Uniform.diffuse, semantic.sampler.DIFFUSE)
            glUniform(Uniform.mvp, mvp)
            glBindVertexArray(vertexArrayName)
            glActiveTexture(GL_TEXTURE0)

            glViewport(viewport[0])
            glBindTexture(GL_TEXTURE_2D, Texture.RGBA4)
            glDrawArraysInstanced(vertexCount, 1)

            glViewport(viewport[1])
            glBindTexture(GL_TEXTURE_2D, Texture.RGBA4_REV)
            glDrawArraysInstanced(vertexCount, 1)

            glViewport(viewport[2])
            glBindTexture(GL_TEXTURE_2D, Texture.BGRA4)
            glDrawArraysInstanced(vertexCount, 1)

            glViewport(viewport[3])
            glBindTexture(GL_TEXTURE_2D, Texture.BGRA4_REV)
            glDrawArraysInstanced(vertexCount, 1)
        }

        return true
    }
}