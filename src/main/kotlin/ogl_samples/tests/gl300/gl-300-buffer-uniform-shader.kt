package ogl_samples.tests.gl300

import glm_.vec2.Vec2
import ogl_samples.framework.TestA
import ogl_samples.framework.Compiler
import org.lwjgl.opengl.GL20.glCreateProgram
import uno.buffer.bufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.gln.initProgram

fun main(args: Array<String>) {
    gl_320_buffer_uniform_shader().loop()
}

private class gl_320_buffer_uniform_shader : TestA("es-320-fbo-shadow", Caps.Profile.CORE, 3, 2) {

    val SHADER_SOURCE = "gl-300/buffer-uniform-shared"

    override var vertexCount = 4
    override var vertexData = bufferOf(
            Vec2(-1f, -1f),
            Vec2(+1f, -1f),
            Vec2(+1f, +1f),
            Vec2(-1f, +1f))

    override var elementData = shortBufferOf(
            0, 1, 2,
            2, 3, 0)


    override fun initProgram():Boolean    {

        var validated = true

        if(validated)        {

            val shaderName = intArrayBig<Shader>()

            val compiler = Compiler()
            shaderName[Shader.VERT] = compiler.create("$SHADER_SOURCE.vert")
            shaderName[Shader.FRAG] = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {

                attach(shaderName[Shader.VERT], shaderName[Shader.FRAG])

                glBindAttribLocation(ProgramName, semantic::attr::POSITION, "Position")
                glBindFragDataLocation(ProgramName, semantic::frag::COLOR, "Color")
                glLinkProgram(ProgramName)

                validated = validated && Compiler.check()
                validated = validated && Compiler.check_program(ProgramName)
            }
        }

        if(validated)
        {
            UniformMaterial = glGetUniformBlockIndex(ProgramName, "material")
            UniformTransform = glGetUniformBlockIndex(ProgramName, "transform")
        }

        return validated && this->checkError("initProgram")
    }
}