package ogl_samples.tests.es300

/**
 * Created by GBarbieri on 21.04.2017.
 */

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.ARBFramebufferObject.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.ARBFramebufferObject.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_INVALIDATE_BUFFER_BIT
import org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_WRITE_BIT
import org.lwjgl.opengl.ARBUniformBufferObject.GL_UNIFORM_BUFFER
import org.lwjgl.opengl.ARBUniformBufferObject.glBindBufferBase
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL21.GL_SRGB
import org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8
import uno.buffer.bufferOf
import uno.caps.Caps.Profile
import uno.glf.generateIcosahedron
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

/**
 * Created by GBarbieri on 30.03.2017.
 */

fun main(args: Array<String>) {
    es_300_fbo_srgb().loop()
}

private class es_300_fbo_srgb : TestA("es-300-fbo-srgb", Profile.ES, 3, 0) {

    val SHADER_SOURCE_RENDER = "es-300/fbo-srgb"
    val SHADER_SOURCE_SPLASH = "es-300/fbo-srgb-blit"

    override fun begin(): Boolean {
        glEnable(GL_CULL_FACE)
        return super.begin()
    }

    override fun initProgram(): Boolean {

        var validated = true

        val compiler = Compiler()

        val shaderName = IntArray(Shader.values().size)

        initPrograms(programName) {

            if (validated) {

                shaderName[Shader.VERT_RENDER] = compiler.create("$SHADER_SOURCE_RENDER.vert")
                shaderName[Shader.FRAG_RENDER] = compiler.create("$SHADER_SOURCE_RENDER.frag")

                with(Program.RENDER) {
                    attach(shaderName[Shader.VERT_RENDER], shaderName[Shader.FRAG_RENDER])
                    link()
                }
            }

            if (validated) {

                shaderName[Shader.VERT_SPLASH] += compiler.create("$SHADER_SOURCE_SPLASH.vert")
                shaderName[Shader.FRAG_SPLASH] = compiler.create("$SHADER_SOURCE_SPLASH.frag")

                with(Program.SPLASH) {
                    attach(shaderName[Shader.VERT_SPLASH], shaderName[Shader.FRAG_SPLASH])
                    link()
                }
            }

            if (validated) {

                validated = validated && compiler.check()
                validated = validated && compiler.checkProgram(programName[Program.RENDER])
                validated = validated && compiler.checkProgram(programName[Program.SPLASH])
            }

            if (validated) {

                using(Program.RENDER) { "transform".blockIndex = semantic.uniform.TRANSFORM0 }
                using(Program.SPLASH) { "Diffuse".unit = semantic.sampler.DIFFUSE }
            }
        }
        return validated && checkError("initProgram")
    }

    override fun initBuffer(): Boolean {

        val vertices = generateIcosahedron(4)
        vertexCount = vertices.size * Vec3.length
        vertexData = bufferOf(vertices)

        initArrayBuffer(vertexData)

        val uniformBlockSize = glm.max(Mat4.size, caps.limits.UNIFORM_BUFFER_OFFSET_ALIGNMENT)
        withUniformBuffer(bufferName[Buffer.TRANSFORM]) { data(uniformBlockSize, GL_STATIC_DRAW) }

        return true
    }

    override fun initTexture(): Boolean {

        val windowSize = windowSize * framebufferScale

        initTextures2d(textureName) {

            at(Texture.COLORBUFFER) {
                levels(base = 0, max = 0)
                filter(min = linear, mag = linear)
                storage(GL_SRGB8_ALPHA8, windowSize)
            }
            at(Texture.RENDERBUFFER) {
                levels(base = 0, max = 0)
                filter(min = nearest, mag = nearest)
                storage(GL_DEPTH_COMPONENT24, windowSize)
            }
        }
        return true
    }

    override fun initVertexArray(): Boolean {

        initVertexArrays(vertexArrayName) {
            at(Program.RENDER) { array(bufferName[Buffer.VERTEX], glf.pos3) }
            at(Program.SPLASH) {}
        }
        return true
    }

    override fun initFramebuffer(): Boolean {

        initFramebuffer(framebufferName) {

            //  GL_FRAMEBUFFER, which is the default target, is equivalent to GL_DRAW_FRAMEBUFFER, we can then omit it
            texture2D(GL_COLOR_ATTACHMENT0, textureName[Texture.COLORBUFFER])
            texture2D(GL_DEPTH_ATTACHMENT, textureName[Texture.RENDERBUFFER])

            if (getColorEncoding() != GL_SRGB)
                return false

            if (!complete)
                return false
        }
/*
		GLint Encoding = -1;
		glGetFramebufferAttachmentParameteriv(GL_DRAW_FRAMEBUFFER, GL_BACK_LEFT, GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, &Encoding);
		GLint Capable = 0;
		glGetIntegerv(GL_FRAMEBUFFER_SRGB_CAPABLE_EXT, &Capable);
*/
        return true
    }

    override fun render(): Boolean {

        mappingUniformBufferRange(bufferName[Buffer.TRANSFORM], Mat4.size, GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT) {

            //glm::mat4 Projection = glm::perspectiveFov(glm::pi<float>() * 0.25f, 640.f, 480.f, 0.1f, 100.0f);
            val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100f)

            pointer = projection * view

        } // Automatically unmapped and unbinded, to make sure the uniform buffer is uploaded

        // Render to a sRGB framebuffer object.
        run {
            glEnable(GL_DEPTH_TEST)
            glDepthFunc(GL_LEQUAL)

            glViewport(windowSize * framebufferScale)
            glBindFramebuffer(framebufferName)

            glClearDepthBuffer()
            glClearColorBuffer(1f, 0.5f, 0f, 1f)

            glUseProgram(programName[Program.RENDER])
            glBindVertexArray(vertexArrayName[Program.RENDER])
            glBindBufferBase(GL_UNIFORM_BUFFER, semantic.uniform.TRANSFORM0, bufferName[Buffer.TRANSFORM])

            glDrawArraysInstanced(vertexCount, 1)
        }

        // Blit the sRGB framebuffer to the default framebuffer back buffer.
        run {
            glDisable(GL_DEPTH_TEST)

            glViewport(windowSize)

            glBindFramebuffer()
            glUseProgram(programName[Program.SPLASH])
            withTexture2d(0, textureName[Texture.COLORBUFFER]) {
                glBindVertexArray(vertexArrayName[Program.SPLASH])
                glDrawArraysInstanced(3, 1)
            }
        }

        return true
    }
}