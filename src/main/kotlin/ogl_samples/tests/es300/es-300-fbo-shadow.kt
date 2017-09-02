package ogl_samples.tests.es300

import glm_.BYTES
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import glm_.vec4.Vec4b
import glm_.vec4.Vec4ub
import ogl_samples.framework.Compiler
import ogl_samples.framework.Test
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.glFramebufferTexture
import org.lwjgl.opengl.GL42.*
import uno.buffer.bufferBig
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.gl.fBuf
import uno.gl.v4Buf
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    es_300_fbo_shadow().loop()
}

private class es_300_fbo_shadow : Test("es-300-fbo-shadow", Caps.Profile.ES, 3, 0, Vec2(0f, -glm.PIf * 0.3f)) {

    val VERT_SHADER_SOURCE_DEPTH = "es-300/fbo-shadow-depth.vert"
    val FRAG_SHADER_SOURCE_DEPTH = "es-300/fbo-shadow-depth.frag"
    val VERT_SHADER_SOURCE_RENDER = "es-300/fbo-shadow-render.vert"
    val FRAG_SHADER_SOURCE_RENDER = "es-300/fbo-shadow-render.frag"

    val vertexCount = 8
    val vertexSize = vertexCount * glf.pos3_col4b.stride
    val vertices = arrayOf(
            Vec3(-1.0f, -1.0f, 0.0f),
            Vec3(+1.0f, -1.0f, 0.0f),
            Vec3(+1.0f, +1.0f, 0.0f),
            Vec3(-1.0f, +1.0f, 0.0f),
            Vec3(-0.1f, -0.1f, 0.2f),
            Vec3(+0.1f, -0.1f, 0.2f),
            Vec3(+0.1f, +0.1f, 0.2f),
            Vec3(-0.1f, +0.1f, 0.2f))
    val colors = arrayOf(
            Vec4b(255, 127, 0, 255),
            Vec4b(255, 127, 0, 255),
            Vec4b(255, 127, 0, 255),
            Vec4b(255, 127, 0, 255),
            Vec4b(0, 127, 255, 255),
            Vec4b(0, 127, 255, 255),
            Vec4b(0, 127, 255, 255),
            Vec4b(0, 127, 255, 255))

    val elementCount = 12
    val elementSize = elementCount * Short.BYTES
    val elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0,
            4, 5, 6,
            6, 7, 4)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val MAX = 2
    }

    object Texture {
        val COLORBUFFER = 0
        val DEPTHBUFFER = 1
        val SHADOWMAP = 2
        val MAX = 3
    }

    object Program {
        val DEPTH = 0
        val RENDER = 1
        val MAX = 2
    }

    object Framebuffer {
        val FRAMEBUFFER = 0
        val SHADOW = 1
        val MAX = 2
    }

    object Shader {
        val VERT_RENDER = 0
        val FRAG_RENDER = 1
        val VERT_DEPTH = 2
        val FRAG_DEPTH = 3
        val MAX = 4
    }

    val shadowSize = Vec2i(64)

    val framebufferName = intBufferBig(Framebuffer.MAX)
    val programName = IntArray(Program.MAX)
    val vertexArrayName = intBufferBig(Program.MAX)
    val bufferName = intBufferBig(Buffer.MAX)
    val textureName = intBufferBig(Texture.MAX)

    object Uniform {

        var depthMVP = -1

        object Render {
            var mvp = -1
            var depthBiasMVP = -1
            var shadow = -1
        }
    }

    override fun begin(): Boolean {

        var validated = true

        if (validated)
            validated = initProgram()
        if (validated)
            validated = initBuffer()
        if (validated)
            validated = initVertexArray()
        if (validated)
            validated = initTexture()
        if (validated)
            validated = initFramebuffer()

        return validated && checkError("begin")
    }

    fun initProgram(): Boolean {

        var validated = true

        val shaderName = IntArray(Shader.MAX)

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_RENDER] = compiler.create(VERT_SHADER_SOURCE_RENDER)
            shaderName[Shader.FRAG_RENDER] = compiler.create(FRAG_SHADER_SOURCE_RENDER)
            validated = validated && compiler.check()

            programName[Program.RENDER] = glCreateProgram {
                attach(shaderName[Shader.VERT_RENDER], shaderName[Shader.FRAG_RENDER])
                "Position".location = semantic.attr.POSITION
                "Color".location = semantic.attr.COLOR
                link()
            }

            validated = validated && compiler.checkProgram(programName[Program.RENDER])
        }

        if (validated)
            with(Uniform.Render) {
                withProgram(programName[Program.RENDER]) {
                    shadow = "Shadow".location
                    mvp = "MVP".location
                    depthBiasMVP = "DepthBiasMVP".location
                }
            }

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_DEPTH] = compiler.create(VERT_SHADER_SOURCE_DEPTH)
            shaderName[Shader.FRAG_DEPTH] = compiler.create(FRAG_SHADER_SOURCE_DEPTH)
            validated = validated && compiler.check()

            programName[Program.DEPTH] = glCreateProgram {
                attach(shaderName[Shader.VERT_DEPTH], shaderName[Shader.FRAG_DEPTH])
                "Position".location = semantic.attr.POSITION
                link()
            }

            validated = validated && compiler.checkProgram(programName[Program.DEPTH])
        }

        if (validated)
            withProgram(programName[Program.DEPTH]) { Uniform.depthMVP = "MVP".location }

        return validated
    }

    fun initBuffer(): Boolean {

        initBuffers(bufferName) {

            withElementAt(Buffer.ELEMENT) { data(elementData, GL_STATIC_DRAW) }

            withArrayAt(Buffer.VERTEX) {

                val vertexData = bufferBig(vertexSize)
                for (i in 0 until vertexCount) {
                    vertices[i].to(vertexData, i * glf.pos3_col4b.stride)
                    colors[i].to(vertexData, i * glf.pos3_col4b.stride + Vec3.size)
                }
                data(vertexData, GL_STATIC_DRAW)

                vertexData.destroy()
            }
        }
        return true
    }

    fun initTexture(): Boolean {

        initTextures2d(textureName) {

            at(Texture.COLORBUFFER) {
                levels(base = 0, max = 0)
                storage(GL_RGBA8, windowSize)
            }
            at(Texture.DEPTHBUFFER) {
                levels(base = 0, max = 0)
                storage(GL_DEPTH_COMPONENT24, windowSize)
            }
            at(Texture.SHADOWMAP) {
                levels(base = 0, max = 0)
                wrap(s = clampToEdge, t = clampToEdge)
                filter(min = linear, mag = linear)
                compare(func = lessEqual, mode = rToTexture)
                storage(GL_DEPTH_COMPONENT24, shadowSize)
            }
        }
        return true
    }

    fun initVertexArray(): Boolean {

        initVertexArrays(vertexArrayName) {

            at(Program.RENDER) {

                withArrayBuffer(bufferName[Buffer.VERTEX]) {
                    glVertexAttribPointer(semantic.attr.POSITION, Vec3.length, GL_FLOAT, false, glf.pos3_col4ub.stride, 0)
                    glVertexAttribPointer(semantic.attr.COLOR, Vec4ub.length, GL_UNSIGNED_BYTE, true, glf.pos3_col4ub.stride, Vec3.size)
                }

                glEnableVertexAttribArray(semantic.attr.POSITION)
                glEnableVertexAttribArray(semantic.attr.COLOR)

                element(bufferName[Buffer.ELEMENT])
            }
        }

        return true
    }

    fun initFramebuffer(): Boolean {

        initFramebuffers(framebufferName) {

            at(Framebuffer.FRAMEBUFFER) {
                texture(GL_COLOR_ATTACHMENT0, textureName[Texture.COLORBUFFER])
                texture(GL_DEPTH_ATTACHMENT, textureName[Texture.DEPTHBUFFER])
                glDrawBuffers(GL_COLOR_ATTACHMENT0) // glDrawBuffer is deprecated!
                if (!complete) return false
            }
            at(Framebuffer.SHADOW) {
                texture(GL_DEPTH_ATTACHMENT, textureName[Texture.SHADOWMAP])
                if (!complete) return false
            }

            withFramebuffer { if (!complete) return false }
        }
        return true
    }

    override fun render(): Boolean {

        val model = Mat4()

        val depthProjection = glm.ortho(-1f, 1f, -1f, 1f, -4f, 8f)
        val depthView = glm.lookAt(Vec3(0.5, 1, 2), Vec3(), Vec3(0, 0, 1))
        val depthMVP = depthProjection * depthView * model

        val biasMatrix = Mat4(
                0.5, 0.0, 0.0, 0.0,
                0.0, 0.5, 0.0, 0.0,
                0.0, 0.0, 0.5, 0.0,
                0.5, 0.5, 0.5, 1.0)

        val depthMVPBias = biasMatrix * depthMVP

        val renderProjection = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 10f)
        val renderMVP = renderProjection * view * model

        usingProgram(programName[Program.DEPTH]) {

            glUniform(Uniform.depthMVP, depthMVP)
            renderShadow()
        }

        usingProgram(programName[Program.RENDER]) {

            glUniform(Uniform.Render.shadow, 0)
            glUniform(Uniform.Render.mvp, renderMVP)
            glUniform(Uniform.Render.depthBiasMVP, depthMVPBias)

            renderFramebuffer()
        }

        return checkError("render")
    }

    fun renderShadow(): Boolean {

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        glViewport(shadowSize)

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferName[Framebuffer.SHADOW])
        glClearDepthBuffer(1f)

        glBindVertexArray(vertexArrayName[Program.RENDER])
        glDrawElements(elementCount, GL_UNSIGNED_SHORT)

        glDisable(GL_DEPTH_TEST)

        return checkError("renderShadow")
    }

    fun renderFramebuffer() {

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        glViewport(windowSize)

        glBindFramebuffer()
        glClearDepthBuffer()
        glClearColorBuffer()

        withTexture2d(0, textureName[Texture.SHADOWMAP]) {

            glBindVertexArray(vertexArrayName[Program.RENDER])
            glDrawElements(elementCount, GL_UNSIGNED_SHORT)
        }

        glDisable(GL_DEPTH_TEST)

        checkError("renderFramebuffer")
    }


    override fun end(): Boolean {

        glDeletePrograms(programName)
        glDeleteFramebuffers(framebufferName)
        glDeleteBuffers(bufferName)
        glDeleteTextures(textureName)

        return checkError("end")
    }

}