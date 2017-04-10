package ogl_samples.framework

import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER
import java.io.File
import kotlin.reflect.KClass

/**
 * Created by GBarbieri on 07.04.2017.
 */

class Compiler {

    val pendingChecks = mutableListOf<Pair<String, Int>>()

    fun create(context: KClass<*>, vararg shader: String):Int {
        val program = glCreateProgram()
        shader.map{ create(context, it) }.forEach { glAttachShader(program, it) }
        return program
    }

    fun create(context: KClass<*>, filename: String): Int {

        val path = "/data/$filename"
        val url = context::class.java.getResource(path)
        val lines = File(url.toURI()).readLines()
        var source = ""
        lines.forEach {
            if (it.startsWith("#include "))
                source += parseInclude(context, path.substringBeforeLast('/'), it.substring("#include ".length).trim())
            else
                source += it
            source += '\n'
        }
        val name = glCreateShader(getType(filename.toString().substringAfterLast('.')))
//        println(source)
        glShaderSource(name, source)

        glCompileShader(name)

        pendingChecks += filename to name

        return name
    }

    fun parseInclude(context: KClass<*>, root: String, shader: String): String {
        if (shader.startsWith('"') && shader.endsWith('"'))
            shader.substring(1, shader.length - 1)
        val url = context::class.java.getResource("$root/$shader")
        return File(url.toURI()).readText() + "\n"
    }

    fun checkProgram(programName: Int): Boolean {

        if (programName == 0) return false

        val result = glGetProgrami(programName, GL_LINK_STATUS)

        if (result == GL_TRUE) return true

        println(glGetProgramInfoLog(programName))

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

    fun getType(ext: String) = when (ext) {
        "vert" -> GL_VERTEX_SHADER
        "geom" -> GL_GEOMETRY_SHADER
        "frag" -> GL_FRAGMENT_SHADER
        else -> throw Error()
    }
}