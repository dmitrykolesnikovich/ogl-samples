package oglSamples.tests.gl320


import glm_.L
import oglSamples.framework.TestA
import uno.caps.Caps
import uno.gln.glClearColorBuffer
import uno.gln.glViewport

fun main(args: Array<String>) {
    gl_320_caps().loop()
}

private class gl_320_caps : TestA("es-320-caps", Caps.Profile.CORE, 3, 2) {

    override fun begin(): Boolean {

        var validated = true

        validated = validated && caps.limits.MAX_VERTEX_UNIFORM_BLOCKS >= 12
        validated = validated && caps.limits.MAX_GEOMETRY_UNIFORM_BLOCKS >= 12
        validated = validated && caps.limits.MAX_FRAGMENT_UNIFORM_BLOCKS >= 12

        validated = validated && caps.limits.MAX_VERTEX_UNIFORM_COMPONENTS >= 1024
        validated = validated && caps.limits.MAX_GEOMETRY_UNIFORM_COMPONENTS >= 1024
        validated = validated && caps.limits.MAX_FRAGMENT_UNIFORM_COMPONENTS >= 1024

        validated = validated && caps.limits.MAX_COMBINED_UNIFORM_BLOCKS >= 36
        validated = validated && caps.limits.MAX_UNIFORM_BUFFER_BINDINGS >= 36
        validated = validated && caps.limits.MAX_UNIFORM_BLOCK_SIZE >= 16384

        val combinedVertUniformCount = caps.limits.MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS.L
        val combinedGeomUniformCount = caps.limits.MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS.L
        val combinedFragUniformCount = caps.limits.MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS.L

        val vertUniformCount = caps.limits.MAX_VERTEX_UNIFORM_BLOCKS.L * caps.limits.MAX_UNIFORM_BLOCK_SIZE.L / 4 +
                caps.limits.MAX_VERTEX_UNIFORM_COMPONENTS.L
        val geomUniformCount = caps.limits.MAX_GEOMETRY_UNIFORM_BLOCKS.L * caps.limits.MAX_UNIFORM_BLOCK_SIZE.L / 4 +
                caps.limits.MAX_GEOMETRY_UNIFORM_COMPONENTS.L
        val fragUniformCount = caps.limits.MAX_FRAGMENT_UNIFORM_BLOCKS.L * caps.limits.MAX_UNIFORM_BLOCK_SIZE.L / 4 +
                caps.limits.MAX_FRAGMENT_UNIFORM_COMPONENTS.L

        println("combinedVertUniformCount: $combinedVertUniformCount")
        println("combinedGeomUniformCount: $combinedGeomUniformCount")
        println("combinedFragUniformCount: $combinedFragUniformCount")
        println("vertUniformCount: $vertUniformCount")
        println("geomUniformCount: $geomUniformCount")
        println("fragUniformCount: $fragUniformCount")

        validated = validated && combinedVertUniformCount <= vertUniformCount
        validated = validated && combinedGeomUniformCount <= geomUniformCount
        validated = validated && combinedFragUniformCount <= fragUniformCount

        return validated
    }

    override fun render(): Boolean {

        glViewport(windowSize)
        glClearColorBuffer(1f, 0.5f, 0f, 1f)

        return true
    }
}
