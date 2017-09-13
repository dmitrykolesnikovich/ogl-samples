package ogl_samples.tests.gl300

/**
 * Created by elect on 08/04/17.
 */

import gli.Texture2d
import gli.gl
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.ARBFramebufferObject.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import uno.buffer.bufferOf
import uno.caps.Caps.Profile
import uno.glf.Vertex
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    gl_300_fbo_multisample().loop()
}

private class gl_300_fbo_multisample : TestA("gl-300-fbo-multisample", Profile.COMPATIBILITY, 3, 0) {

    val SHADER_SOURCE = "gl-300/image-2d"
    val TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds"
    val FRAMEBUFFER_SIZE = Vec2i(160, 120)

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    override var vertexCount = 6
    override var vertexData = bufferOf(
            Vertex.pos2_tc2(Vec2(-2f, -1.5f), Vec2(0f, 0f)),
            Vertex.pos2_tc2(Vec2(+2f, -1.5f), Vec2(1f, 0f)),
            Vertex.pos2_tc2(Vec2(+2f, +1.5f), Vec2(1f, 1f)),
            Vertex.pos2_tc2(Vec2(+2f, +1.5f), Vec2(1f, 1f)),
            Vertex.pos2_tc2(Vec2(-2f, +1.5f), Vec2(0f, 1f)),
            Vertex.pos2_tc2(Vec2(-2f, -1.5f), Vec2(0f, 0f)))

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

        if (validated) usingProgram(programName) {
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
            at(Texture.COLOR) {
                filter(min = nearest, mag = nearest)
                image(GL_RGBA8, FRAMEBUFFER_SIZE, GL_RGBA, GL_UNSIGNED_BYTE)
            }
        }
        texture.dispose()   // TODO rename to destroy

        return checkError("initTexture")
    }

    override fun initRenderbuffer(): Boolean {

        initRenderbuffers {
            at(Renderbuffer.COLOR) {
                storageMultisample(8, GL_RGBA8, FRAMEBUFFER_SIZE)   // The first parameter is the number of samples.
                if (size != FRAMEBUFFER_SIZE || samples != 8 || format != GL_RGBA8)
                    return false
            }
        }
        return checkError("initRenderbuffer")
    }

    override fun initFramebuffer(): Boolean {

        initFramebuffers {

            at(Framebuffer.RENDER) {
                renderbuffer(GL_COLOR_ATTACHMENT0, Renderbuffer.COLOR)
                if (!complete) return false
            }

            at(Framebuffer.RESOLVE) {
                texture2D(GL_COLOR_ATTACHMENT0, Texture.COLOR)
                if (!complete) return false
            }
        }
        return checkError("initFramebuffer")
    }

    override fun initVertexArray() = initVertexArray(glf.pos2_tc2)

    override fun render(): Boolean {

        // Clear the framebuffer
        glBindFramebuffer()
        glClearColorBuffer(1f, 0.5f, 0f, 1f)

        glUseProgram(programName)

        // Pass 1
        // Render the scene in a multisampled framebuffer
        glEnable(GL_MULTISAMPLE)
        renderFBO(Framebuffer.RENDER)
        glDisable(GL_MULTISAMPLE)

        // Resolved multisampling
        glBindFramebuffer(GL_READ_FRAMEBUFFER, Framebuffer.RENDER)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, Framebuffer.RESOLVE)
        glBlitFramebuffer(FRAMEBUFFER_SIZE)
        glBindFramebuffer()

        // Pass 2
        // Render the colorbuffer from the multisampled framebuffer
        glViewport(windowSize)
        renderFB(Texture.COLOR)

        return true
    }

    fun renderFBO(framebuffer: Enum<*>): Boolean {

        glBindFramebuffer(framebuffer)
        glClearColor(0f, 0.5f, 1f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        val perspective = glm.perspective(glm.PIf * 0.25f, FRAMEBUFFER_SIZE, 0.1f, 100f)
        val model = Mat4()
        val mvp = perspective * view * model
        glUniform(Uniform.mvp, mvp)

        glViewport(FRAMEBUFFER_SIZE)

        withTexture2d(0, Texture.DIFFUSE) {

            glBindVertexArray(vertexArrayName)
            glDrawArrays(vertexCount)
        }
        return checkError("renderFBO")
    }

    fun renderFB(texture2DName: Enum<*>) {

        val perspective = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100f)
        val model = Mat4()
        val mvp = perspective * view * model
        glUniform(Uniform.mvp, mvp)

        withTexture2d(0, Texture.COLOR) {

            glBindVertexArray(vertexArrayName)
            glDrawArrays(vertexCount)
        }

        checkError("renderFB")
    }
}