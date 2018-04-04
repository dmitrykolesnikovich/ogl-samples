package oglSamples.tests.gl300

/**
 * Created by GBarbieri on 25.04.2017.
 */


import gli.Texture2d
import gli.gl
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import oglSamples.framework.Compiler
import oglSamples.framework.TestA
import org.lwjgl.opengl.GL11.*
import uno.buffer.bufferOf
import uno.caps.Caps.Profile
import gln.glf.Vertex
import gln.glf.glf
import gln.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    gl_300_test_alpha().loop()
}

private class gl_300_test_alpha : TestA("gl-300-test-alpha", Profile.COMPATIBILITY, 3, 0) {

    val SHADER_SOURCE = "gl-300/image-2d"
    val TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds"

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    override var vertexCount = 6
    override var vertexData = bufferOf(
            Vertex.pos2_tc2(Vec2(-1f, -1f), Vec2(0f, 1f)),
            Vertex.pos2_tc2(Vec2(+1f, -1f), Vec2(1f, 1f)),
            Vertex.pos2_tc2(Vec2(+1f, +1f), Vec2(1f, 0f)),
            Vertex.pos2_tc2(Vec2(+1f, +1f), Vec2(1f, 0f)),
            Vertex.pos2_tc2(Vec2(-1f, +1f), Vec2(0f, 0f)),
            Vertex.pos2_tc2(Vec2(-1f, -1f), Vec2(0f, 1f)))

    override fun begin(): Boolean {

        val maxVaryingOutputComp = caps.limits.MAX_VARYING_COMPONENTS
        val maxVaryingOutputVec = caps.limits.MAX_VARYING_VECTORS

        return super.begin()
    }

    override fun initTest(): Boolean {

        glEnable(GL_ALPHA_TEST)
        glAlphaFunc(GL_GREATER, 0.2f)

        //To test alpha blending:
        //glEnable(GL_BLEND);
        //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        return checkError("initTest")
    }

    override fun initProgram(): Boolean {

        var validated = true

        val compiler = Compiler()

        if (validated) {

            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {

                attach(vertShaderName, fragShaderName)

                "Position".attrib = semantic.attr.POSITION
                "Texcoord".attrib = semantic.attr.TEX_COORD
                link()

                validated = validated && compiler.check()
                validated = validated && compiler checkProgram name
            }
        }

        if (validated) withProgram(programName) {
            Uniform.mvp = "MVP".uniform
            Uniform.diffuse = "Diffuse".unit
        }

        return validated && checkError("initProgram")
    }

    override fun initBuffer() = initArrayBuffer(vertexData)

    override fun initTexture(): Boolean {

        val texture = Texture2d(gli.loadDDS("$dataDirectory/$TEXTURE_DIFFUSE"))
        gli.gl.profile = gl.Profile.GL32

        initTextures2d() {
            at(Texture.DIFFUSE) {
                levels(base = 0, max = texture.levels() - 1)
                filter(min = linear_mmLinear, mag = linear)

                val format = gl.translate(texture.format, texture.swizzles)
                for (level in 0 until texture.levels())
                    glTexImage2D(level, format, texture)
            }
        }
        texture.dispose()
        return checkError("initTexture")
    }

    override fun initVertexArray() = initVertexArray(glf.pos2_tc2)

    override fun render(): Boolean {

        val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100f)
        val model = Mat4()
        val mvp = projection * view * model

        glViewport(windowSize)
        glClearColor(1f, 0.5f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        glUseProgram(programName)
        glUniform(Uniform.diffuse, 0)
        glUniform(Uniform.mvp, mvp)

        withTexture2d(0, Texture.DIFFUSE) {

            glBindVertexArray(vertexArrayName)

            glDrawArrays(vertexCount)
        }

        return true
    }
}