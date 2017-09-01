package ogl_samples.tests.es200

import com.jogamp.opengl.GL
import glm_.BYTES
import glm_.detail.Random.float
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.set
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import glm_.vec4.Vec4b
import ogl_samples.framework.Test
import ogl_samples.framework.Compiler
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.glFramebufferTexture
import org.lwjgl.system.MemoryUtil
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
    es_200_fbo_shadow().loop()
}

private class es_200_fbo_shadow : Test("es-200-draw-elements", Caps.Profile.ES, 2, 0, Vec2(0f, -glm.PIf * 0.3f)) {

    val VERT_SHADER_SOURCE_DEPTH = "es-200/fbo-shadow-depth.vert"
    val FRAG_SHADER_SOURCE_DEPTH = "es-200/fbo-shadow-depth.frag"
    val VERT_SHADER_SOURCE_RENDER = "es-200/fbo-shadow-render.vert"
    val FRAG_SHADER_SOURCE_RENDER = "es-200/fbo-shadow-render.frag"

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

    object Attachment {
        val COLORBUFFER = 0
        val DEPTHBUFFER = 1
        val MAX = 2
    }

    object Framebuffer {
        val RENDER = 0
        val DEPTH = 1
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
    val programName = IntArray(Framebuffer.MAX)
    val bufferName = intBufferBig(Buffer.MAX)
    val textureName = intBufferBig(Attachment.MAX)
    val renderbufferName = intBufferBig(Attachment.MAX)

    object Uniform {

        object Light {
            var proj = -1
            var view = -1
            var world = -1
            var pointLightPosition = -1
            var clipNearFar = -1
        }

        object Render {
            var p = -1
            var v = -1
            var w = -1
            var shadow = -1
            var pointLightPosition = -1
            var clipNearFar = -1
            var bias = -1
        }
    }

    override fun begin(): Boolean {

        var validated = true

        if (validated)
            validated = initProgram()
        if (validated)
            validated = initBuffer()
        if (validated)
            validated = initTexture()
        if (validated)
            validated = initFramebuffer()

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        withArrayBuffer(bufferName[Buffer.VERTEX]) {
            glVertexAttribPointer(semantic.attr.POSITION, Vec3.length, GL_FLOAT, false, glf.pos3_col4b.stride, 0)
            glVertexAttribPointer(semantic.attr.COLOR, Vec4b.length, GL_UNSIGNED_BYTE, true, glf.pos3_col4b.stride, Vec3.size)
        }

        glEnableVertexAttribArray(semantic.attr.POSITION)
        glEnableVertexAttribArray(semantic.attr.COLOR)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])

        return validated && checkError("begin")
    }

    fun initProgram(): Boolean {

        var validated = true

        val shaderName = IntArray(Shader.MAX)

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_DEPTH] = compiler.create(VERT_SHADER_SOURCE_DEPTH)
            shaderName[Shader.FRAG_DEPTH] = compiler.create(FRAG_SHADER_SOURCE_DEPTH)
            validated = validated && compiler.check()

            programName[Framebuffer.DEPTH] = glCreateProgram {
                attach(shaderName[Shader.VERT_DEPTH], shaderName[Shader.FRAG_DEPTH])
                "Position".location = semantic.attr.POSITION
                link()
            }

            validated = validated && compiler.checkProgram(programName[Framebuffer.DEPTH])
        }

        if (validated)
            with(Uniform.Light) {
                withProgram(programName[Framebuffer.DEPTH]) {
                    proj = "LightProj".location
                    view = "LightView".location
                    world = "LightWorld".location
                    pointLightPosition = "PointLightPosition".location
                    clipNearFar = "ShadowClipNearFar".location
                }
            }

        if (validated) {
            val compiler = Compiler()
            shaderName[Shader.VERT_RENDER] = compiler.create(VERT_SHADER_SOURCE_RENDER)
            shaderName[Shader.FRAG_RENDER] = compiler.create(FRAG_SHADER_SOURCE_RENDER)
            validated = validated && compiler.check()

            programName[Framebuffer.RENDER] = glCreateProgram {
                attach(shaderName[Shader.VERT_RENDER], shaderName[Shader.FRAG_RENDER])
                "Position".location = semantic.attr.POSITION
                "Color".location = semantic.attr.COLOR
                link()
            }

            validated = validated && compiler.checkProgram(programName[Framebuffer.RENDER])
        }

        if (validated)
            with(Uniform.Render) {
                withProgram(programName[Framebuffer.RENDER]) {
                    p = "P".location
                    v = "V".location
                    w = "W".location
                    shadow = "Shadow".location
                    pointLightPosition = "PointLightPosition".location
                    clipNearFar = "ShadowClipNearFar".location
                    bias = "Bias".location
                }
            }

        return validated
    }

    fun initBuffer(): Boolean {

        initBuffers(bufferName) {
            withElementAt(Buffer.ELEMENT) {
                data(elementData, GL_STATIC_DRAW)
            }
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

        initRenderbuffers(renderbufferName) {
            at(Attachment.COLORBUFFER) { storage(GL_DEPTH_COMPONENT16, windowSize) }
            at(Attachment.DEPTHBUFFER) { storage(GL_DEPTH_COMPONENT16, shadowSize) }
        }

        initTextures2d(textureName) {

            at(Attachment.COLORBUFFER) { image(GL_RGBA, windowSize, GL_RGBA, GL_UNSIGNED_BYTE) }

            at(Attachment.DEPTHBUFFER) {
                wrapS = clampToEdge
                wrapT = clampToEdge
                minFilter = linear
                magFilter = linear
                image(GL_RGBA32F, shadowSize, GL_RGBA, GL_FLOAT)
            }
        }
        return true
    }

    fun initFramebuffer(): Boolean {

        initFramebuffers(framebufferName) {

            at(Framebuffer.DEPTH) {
                checkError("b")
                texture(GL_COLOR_ATTACHMENT0, textureName[Framebuffer.DEPTH])
                checkError("c")
                renderbuffer(GL_DEPTH_ATTACHMENT, renderbufferName[Framebuffer.DEPTH])
                checkError("d")
                if (!complete) return false
            }
            at(Framebuffer.RENDER) {
                texture(GL_COLOR_ATTACHMENT0, textureName[Framebuffer.RENDER])
                renderbuffer(GL_DEPTH_ATTACHMENT, renderbufferName[Framebuffer.RENDER])
                if (!complete) return false
            }
        }

        withFramebuffer { if (!complete) return false }

        return true
    }

    override fun render(): Boolean {

        run {
            val lightP = glm.perspective(glm.PIf * 0.25f, 1f, 0.1f, 10f)
            val lightV = glm.lookAt(Vec3(0.5, 1, 2), Vec3(), Vec3(0, 0, 1))
            val lightW = Mat4()

            usingProgram(programName[Framebuffer.DEPTH]) {
                lightP to Uniform.Light.proj
                lightV to Uniform.Light.view
                lightW to Uniform.Light.world
                glUniform(Uniform.Light.pointLightPosition, 0f, 0f, 10f)
                glUniform(Uniform.Light.clipNearFar, 0.01f, 10f)

                renderShadow()
            }
        }

        run {
            val renderP = glm.perspective(glm.PIf * 0.25f, 4f / 3f, 0.1f, 10f)
            val renderV = view
            val renderW = Mat4()

            usingProgram(programName[Framebuffer.RENDER]) {

                renderP to Uniform.Render.p
                renderV to Uniform.Render.v
                renderW to Uniform.Render.w
                0 to Uniform.Render.shadow
                glUniform(Uniform.Render.pointLightPosition, 0f, 0f, 10f)
                glUniform(Uniform.Render.clipNearFar, 0.01f, 10f)
                glUniform(Uniform.Render.bias, 0.002f)

                renderFramebuffer()
            }
        }

        return checkError("render")
    }

    fun renderShadow() {

        glViewport(shadowSize)

        glBindFramebuffer(framebufferName[Framebuffer.DEPTH])

        glClearDepthBuffer(1f)

        glDrawElements(elementCount, GL_UNSIGNED_SHORT)

        checkError("renderShadow")
    }

    fun renderFramebuffer() {

        glViewport(windowSize)

        glBindFramebuffer()
        glClearDepthBuffer(1f)
        glClearColorBuffer(Vec4(0f, 0f, 0f, 1f))

        withTexture2d(0, textureName[Framebuffer.DEPTH]) {
            glDrawElements(elementCount, GL_UNSIGNED_SHORT)
        }

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