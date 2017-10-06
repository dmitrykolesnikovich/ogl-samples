package ogl_samples.tests.gl320


import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4b
import ogl_samples.framework.Compiler
import ogl_samples.framework.TestA
import uno.buffer.bufferOf
import uno.buffer.shortBufferOf
import uno.caps.Caps
import uno.glf.semantic
import uno.gln.checkError
import uno.gln.initProgram
import uno.gln.programName
import uno.gln.withProgram

fun main(args: Array<String>) {
    gl_320_draw_base_vertex().loop()
}

private class gl_320_draw_base_vertex : TestA("es-320-draw-base-vertex", Caps.Profile.CORE, 3, 2, Vec2(glm.PIf * 0.2f)) {

    val SHADER_SOURCE = "gl-320/draw-base-vertex"

    override var elementData = shortBufferOf(
            0, 1, 2,
            0, 2, 3)

    override var vertexCount = 8
    //    val positionSize = vertexCount * Vec2.size
    var positionData = bufferOf(
            Vec3(-1.0f, -1.0f, +0.5f),
            Vec3(+1.0f, -1.0f, +0.5f),
            Vec3(+1.0f, +1.0f, +0.5f),
            Vec3(-1.0f, +1.0f, +0.5f),
            Vec3(-0.5f, -1.0f, -0.5f),
            Vec3(+0.5f, -1.0f, -0.5f),
            Vec3(+1.5f, +1.0f, -0.5f),
            Vec3(-1.5f, +1.0f, -0.5f))

    val colorData = bufferOf(
            Vec4b(255, 0, 0, 255),
            Vec4b(255, 255, 0, 255),
            Vec4b(0, 255, 0, 255),
            Vec4b(0, 0, 255, 255),
            Vec4b(255, 128, 0, 255),
            Vec4b(255, 128, 0, 255),
            Vec4b(255, 128, 0, 255),
            Vec4b(255, 128, 0, 255))

    override fun initProgram(): Boolean {

        var validated = true

        if (validated) {

            val compiler = Compiler()
            val vertShaderName = compiler.create("$SHADER_SOURCE.vert")
            val fragShaderName = compiler.create("$SHADER_SOURCE.frag")

            initProgram(programName) {

                attach(vertShaderName, fragShaderName)

                "Position".attrib = semantic.attr.POSITION
                "Color".attrib = semantic.attr.COLOR
                "Color".fragData = semantic.frag.COLOR
                link()

                validated = validated && compiler.check()
                validated = validated && compiler checkProgram name
            }
        }

        if (validated) withProgram(programName) {
            Uniform.transform = "transform".blockIndex
            "transform".blockIndex = semantic.uniform.TRANSFORM0
        }

        return validated && checkError("initProgram")
    }
}