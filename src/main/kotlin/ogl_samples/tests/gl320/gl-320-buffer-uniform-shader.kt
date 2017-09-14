package ogl_samples.tests.gl320

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL30.GL_MAP_INVALIDATE_BUFFER_BIT
import org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT
import uno.buffer.bufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.glf.glf
import uno.glf.semantic
import uno.gln.*

fun main(args: Array<String>) {
    gl_320_buffer_uniform_shared().loop()
}

private class gl_320_buffer_uniform_shared : TestA("es-320-buffer-uniform-shared", Caps.Profile.CORE, 3, 2) {

    val SHADER_SOURCE = "gl-320/buffer-uniform-shared"

    override var vertexCount = 4
    override var vertexData = bufferOf(
            Vec2(-1f, -1f),
            Vec2(+1f, -1f),
            Vec2(+1f, +1f),
            Vec2(-1f, +1f))

    override var elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0)

    var uniformBlockSizeTransform = 0
    var uniformBlockSizeMaterial = 0

    override fun initProgram(): Boolean {

        var validated = true

        if (validated) {

            val shaderName = intArrayBig<Shader>()

            val compiler = Compiler()
            shaderName[Shader.VERT] = compiler.create("$SHADER_SOURCE.vert")
            shaderName[Shader.FRAG] = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {

                attach(shaderName[Shader.VERT], shaderName[Shader.FRAG])

                "Position".attrib = semantic.attr.POSITION
                "Color".fragData = semantic.frag.COLOR
                link()

                validated = validated && compiler.check()
                validated = validated && compiler checkProgram name
            }
        }

        if (validated) withProgram(programName) {
            Uniform.material = "material".blockIndex
            Uniform.transform = "transform".blockIndex
        }

        return validated && checkError("initProgram")
    }

    override fun initVertexArray() = initVertexArray(glf.pos2)

    override fun initBuffer(): Boolean {

        val uniformBufferOffset = caps.limits.UNIFORM_BUFFER_OFFSET_ALIGNMENT

        withUniformBlock(programName, Uniform.transform) {
            uniformBlockSizeTransform = (dataSize / uniformBufferOffset + 1) * uniformBufferOffset
        }
        withUniformBlock(programName, Uniform.material) {
            uniformBlockSizeMaterial = (dataSize / uniformBufferOffset + 1) * uniformBufferOffset
        }
        withUniformBuffer(Buffer.UNIFORM) {
            data(uniformBlockSizeTransform + uniformBlockSizeMaterial, GL_DYNAMIC_DRAW)
        }
        initBuffers(vertexData, elementData)

        return checkError("initBuffer")
    }

    override fun render(): Boolean {

        val access = GL_MAP_WRITE_BIT or GL_MAP_INVALIDATE_BUFFER_BIT
        mappingUniformBufferRange(Buffer.UNIFORM, uniformBlockSizeTransform + Vec4.size, access) {
            // Compute the mvp (model View projection matrix)
            val projection = glm.perspective(glm.PIf * 0.25f, windowSize, 0.1f, 100f)
            val model = Mat4()
            val mvp = projection * view * model

            val diffuse = Vec4(1f, 0.5f, 0f, 1f)

            pointer = mvp
            diffuse.to(buffer, uniformBlockSizeTransform)
        }   // automatic unmapped and unbound to make sure the uniform buffer is uploaded

        glViewport(windowSize)
        glClearColorBuffer()

        glUseProgram(programName)
        glUniformBlockBinding(programName, Uniform.transform, semantic.uniform.TRANSFORM0)
        glUniformBlockBinding(programName, Uniform.material, semantic.uniform.MATERIAL)

        // Attach the buffer to UBO binding point semantic::uniform::TRANSFORM0
        glBindUniformBufferRange(semantic.uniform.TRANSFORM0, Buffer.UNIFORM, uniformBlockSizeTransform)
        // Attach the buffer to UBO binding point semantic::uniform::MATERIAL
        glBindUniformBufferRange(semantic.uniform.MATERIAL, Buffer.UNIFORM, uniformBlockSizeTransform, uniformBlockSizeMaterial)

        // Bind vertex array & draw
        glBindVertexArray(vertexArrayName)
        glDrawElementsInstancedBaseVertex(elementCount, GL_UNSIGNED_SHORT, 1, 0)

        return true
    }
}