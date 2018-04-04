package oglSamples.framework

import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER
import uno.gln.programName
import java.io.File
import uno.gln.get

/**
 * Created by GBarbieri on 07.04.2017.
 */

class Compiler {

    val pendingChecks = mutableListOf<Pair<String, Int>>()

    fun create(vararg shader: String): Int {
        val program = glCreateProgram()
        shader.map { create(it) }.forEach { glAttachShader(program, it) }
        return program
    }

    fun create(filename: String): Int {

        val path = "data/$filename"
        val url = ClassLoader.getSystemResource(path)
        val lines = File(url.toURI()).readLines()
        var source = ""
        lines.forEach {
            if (it.startsWith("#include "))
                source += parseInclude(path.substringBeforeLast('/'), it.substring("#include ".length).trim())
            else
                source += it
            source += '\n'
        }
        val name = glCreateShader(filename.substringAfterLast('.').type)
//        println(source)
        glShaderSource(name, source)

        glCompileShader(name)

        pendingChecks += filename to name

        return name
    }

    fun parseInclude(root: String, shader: String): String {
        if (shader.startsWith('"') && shader.endsWith('"'))
            shader.substring(1, shader.length - 1)
        val url = ClassLoader.getSystemResource("$root/$shader")
        return File(url.toURI()).readText() + "\n"
    }

    infix fun checkProgram(program: Enum<*>) = checkProgram(programName[program])
    infix fun checkProgram(program: IntArray) = checkProgram(program[0])
    infix fun checkProgram(program: Int): Boolean {

        if (program == 0) return false

        val result = glGetProgrami(program, GL_LINK_STATUS)

        if (result == GL_TRUE) return true

        println(glGetProgramInfoLog(program))

        return result == GL_TRUE
    }

    fun check(): Boolean {

        var success = true

        pendingChecks.forEach {

            val shaderName = it.second
            val result = glGetShaderi(shaderName, GL_COMPILE_STATUS)

            if (result == GL_TRUE) return@forEach

            println(glGetShaderInfoLog(shaderName))

            success = success && result == GL_TRUE
        }

        return success
    }

    private val String.type
        get() = when (this) {
            "vert" -> GL_VERTEX_SHADER
            "geom" -> GL_GEOMETRY_SHADER
            "frag" -> GL_FRAGMENT_SHADER
            else -> throw Error()
        }
}