package ogl_samples.tests.es300

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec4.Vec4ub
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL20.glDrawBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL30.*
import uno.buffer.bufferOf
import uno.buffer.destroyBuf
import uno.buffer.intBufferBig
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.glf.Vertex
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    es_300_fbo_shadow().loop()
}

private class es_300_fbo_shadow : TestA("es-300-fbo-shadow", Caps.Profile.ES, 3, 0, Vec2(0f, -glm.PIf * 0.3f)) {

    val VERT_SHADER_SOURCE_DEPTH = "es-300/fbo-shadow-depth.vert"
    val FRAG_SHADER_SOURCE_DEPTH = "es-300/fbo-shadow-depth.frag"
    val VERT_SHADER_SOURCE_RENDER = "es-300/fbo-shadow-render.vert"
    val FRAG_SHADER_SOURCE_RENDER = "es-300/fbo-shadow-render.frag"

    override var vertexCount = 8
    override var vertexData = bufferOf(
            Vertex.pos3_col4ub(Vec3(-1.0f, -1.0f, 0.0f), Vec4ub(255, 127, 0, 255)),
            Vertex.pos3_col4ub(Vec3(+1.0f, -1.0f, 0.0f), Vec4ub(255, 127, 0, 255)),
            Vertex.pos3_col4ub(Vec3(+1.0f, +1.0f, 0.0f), Vec4ub(255, 127, 0, 255)),
            Vertex.pos3_col4ub(Vec3(-1.0f, +1.0f, 0.0f), Vec4ub(255, 127, 0, 255)),
            Vertex.pos3_col4ub(Vec3(-0.1f, -0.1f, 0.2f), Vec4ub(0, 127, 255, 255)),
            Vertex.pos3_col4ub(Vec3(+0.1f, -0.1f, 0.2f), Vec4ub(0, 127, 255, 255)),
            Vertex.pos3_col4ub(Vec3(+0.1f, +0.1f, 0.2f), Vec4ub(0, 127, 255, 255)),
            Vertex.pos3_col4ub(Vec3(-0.1f, +0.1f, 0.2f), Vec4ub(0, 127, 255, 255)))

    override var elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0,
            4, 5, 6,
            6, 7, 4)

    val shadowSize = Vec2i(64)

    override fun initProgram(): Boolean {

        var validated = true

        val shaderName = intArrayBig<Shader>()

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_RENDER] = compiler.create(VERT_SHADER_SOURCE_RENDER)
            shaderName[Shader.FRAG_RENDER] = compiler.create(FRAG_SHADER_SOURCE_RENDER)
            validated = validated && compiler.check()

            programName[Program.RENDER] = initProgram {
                attach(shaderName[Shader.VERT_RENDER], shaderName[Shader.FRAG_RENDER])
                "Position".attrib = semantic.attr.POSITION
                "Color".attrib = semantic.attr.COLOR
                link()
            }

            validated = validated && compiler.checkProgram(programName[Program.RENDER])
        }

        if (validated)
            with(Uniform.Render) {
                withProgram(programName[Program.RENDER]) {
                    shadow = "Shadow".uniform
                    mvp = "MVP".uniform
                    depthBiasMVP = "DepthBiasMVP".uniform
                }
            }

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_DEPTH] = compiler.create(VERT_SHADER_SOURCE_DEPTH)
            shaderName[Shader.FRAG_DEPTH] = compiler.create(FRAG_SHADER_SOURCE_DEPTH)
            validated = validated && compiler.check()

            programName[Program.DEPTH] = initProgram {
                attach(shaderName[Shader.VERT_DEPTH], shaderName[Shader.FRAG_DEPTH])
                "Position".attrib = semantic.attr.POSITION
                link()
            }

            validated = validated && compiler.checkProgram(programName[Program.DEPTH])
        }

        if (validated)
            withProgram(programName[Program.DEPTH]) { Uniform.depthMVP = "MVP".uniform }

        return validated
    }

    override fun initBuffer() = initBuffers(vertexData, elementData)

    override fun initTexture(): Boolean {

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

    override fun initVertexArray(): Boolean {

        initVertexArrays(vertexArrayName) {

            at(VertexArray.RENDER) {

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

    override fun initFramebuffer(): Boolean {

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

        glBindVertexArray(vertexArrayName[VertexArray.RENDER])
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

            glBindVertexArray(vertexArrayName[VertexArray.RENDER])
            glDrawElements(elementCount, GL_UNSIGNED_SHORT)
        }

        glDisable(GL_DEPTH_TEST)

        checkError("renderFramebuffer")
    }
}