package ogl_samples.tests.es200

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import glm_.vec4.Vec4b
import glm_.vec4.Vec4ub
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL30.*
import uno.buffer.bufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.glf.Vertex
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    es_200_fbo_shadow().loop()
}

private class es_200_fbo_shadow : TestA("es-200-fbo-shadow", Caps.Profile.ES, 2, 0, Vec2(0f, -glm.PIf * 0.3f)) {

    val VERT_SHADER_SOURCE_DEPTH = "es-200/fbo-shadow-depth.vert"
    val FRAG_SHADER_SOURCE_DEPTH = "es-200/fbo-shadow-depth.frag"
    val VERT_SHADER_SOURCE_RENDER = "es-200/fbo-shadow-render.vert"
    val FRAG_SHADER_SOURCE_RENDER = "es-200/fbo-shadow-render.frag"

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

    override fun initProgram(): Boolean {

        var validated = true

        val shaderName = intArrayBig<Shader>()

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
            with(Uniform.Light) {
                withProgram(programName[Program.DEPTH]) {
                    proj = "LightProj".uniform
                    view = "LightView".uniform
                    world = "LightWorld".uniform
                    pointLightPosition = "PointLightPosition".uniform
                    clipNearFar = "ShadowClipNearFar".uniform
                }
            }

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
                    p = "P".uniform
                    v = "V".uniform
                    w = "W".uniform
                    shadow = "Shadow".uniform
                    pointLightPosition = "PointLightPosition".uniform
                    clipNearFar = "ShadowClipNearFar".uniform
                    bias = "Bias".uniform
                }
            }

        return validated
    }

    override fun initBuffer() = initBuffers(vertexData, elementData)

    override fun initTexture(): Boolean {

        initRenderbuffers(renderbufferName) {
            at(Renderbuffer.COLOR) { storage(GL_DEPTH_COMPONENT16, windowSize) }
            at(Renderbuffer.DEPTH) { storage(GL_DEPTH_COMPONENT16, shadowSize) }
        }

        initTextures2d(textureName) {

            at(Texture.COLOR) { image(GL_RGBA, windowSize, GL_RGBA, GL_UNSIGNED_BYTE) }

            at(Texture.DEPTH) {
                wrap(s = clampToEdge, t = clampToEdge)
                filter(min = linear, mag = linear)
                image(GL_RGBA32F, shadowSize, GL_RGBA, GL_FLOAT)
            }
        }
        return true
    }

    override fun initFramebuffer(): Boolean {

        initFramebuffers(framebufferName) {

            at(Framebuffer.DEPTH) {
                checkError("b")
                texture(GL_COLOR_ATTACHMENT0, textureName[Texture.DEPTH])
                checkError("c")
                renderbuffer(GL_DEPTH_ATTACHMENT, renderbufferName[Renderbuffer.DEPTH])
                checkError("d")
                if (!complete) return false
            }
            at(Framebuffer.RENDER) {
                texture(GL_COLOR_ATTACHMENT0, textureName[Texture.COLOR])
                renderbuffer(GL_DEPTH_ATTACHMENT, renderbufferName[Renderbuffer.COLOR])
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

            usingProgram(programName[Program.DEPTH]) {
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

            usingProgram(programName[Program.RENDER]) {
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

        withTexture2d(0, textureName[Texture.DEPTH]) {
            glDrawElements(elementCount, GL_UNSIGNED_SHORT)
        }

        checkError("renderFramebuffer")
    }
}