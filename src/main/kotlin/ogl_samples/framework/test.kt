package ogl_samples.framework

import glm.glm
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._2.Vec2i
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWMouseButtonCallbackI
import org.lwjgl.opengl.ARBES2Compatibility.GL_IMPLEMENTATION_COLOR_READ_FORMAT
import org.lwjgl.opengl.ARBES2Compatibility.GL_IMPLEMENTATION_COLOR_READ_TYPE
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS
import org.lwjgl.system.Platform
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferBig
import uno.buffer.intBufferBig
import uno.caps.Caps.Profile


/**
 * Created by GBarbieri on 27.03.2017.
 */

val DEBUG = true
val APPLE = Platform.get() == Platform.MACOSX
val EXIT_SUCCESS = false
val EXIT_FAILURE = true
val AUTOMATED_TESTS = false

abstract class Test(
        val title: String,
        val profile: Profile, val major: Int, val minor: Int,
        val windowSize: Vec2i = Vec2i(640, 480),
        orientation: Vec2 = Vec2(0),
        position: Vec2 = Vec2(0, 4),
        val frameCount: Int = 2,
        val success: Success = Success.MATCH_TEMPLATE,
        val heuristic: Heuristic = Heuristic.ALL) {

    val data = "data"

    init {
        assert(windowSize.x > 0 && windowSize.y > 0)

        glfwInit()

        with(windowHint) {

            resizable = false
            visible = true
            srgb = false
            decorated = true
            api = if (this@Test.profile == Profile.ES) "es" else "gl"

            if (version(this@Test.major, this@Test.minor) >= version(3, 2) || this@Test.profile == Profile.ES) {
                major = this@Test.major
                minor = this@Test.minor
                if (APPLE) {
                    profile = "core"
                    forwardComp = true
                } else {
                    if (this@Test.profile != Profile.ES) {
                        profile = if (this@Test.profile == Profile.CORE) "core" else "compat"
                        forwardComp = this@Test.profile == Profile.CORE
                    }
                    debug = DEBUG
                }
            }
        }
    }

    val window: GlfwWindow

    init {

        val dpi = if (APPLE) 2 else 1
        window = GlfwWindow(windowSize / dpi, title)

        if (window.handle != 0L)

            with(window) {

                pos = Vec2i(64)

                glfwSetMouseButtonCallback(handle, MouseListener())
                glfwSetKeyCallback(handle, KeyListener())
            }

        glfwMakeContextCurrent(window.handle)

        GL.createCapabilities()
    }

    constructor(title: String, profile: Profile, major: Int, minor: Int,
                frameCount: Int,
                success: Success,
                windowSize: Vec2i)
            : this(title, profile, major, minor, windowSize, Vec2(0), Vec2(0), frameCount, success)

    constructor(title: String, profile: Profile, major: Int, minor: Int,
                frameCount: Int,
                windowSize: Vec2i = Vec2i(640, 480),
                orientation: Vec2 = Vec2(0),
                position: Vec2 = Vec2(0, 4))
            : this(title, profile, major, minor, windowSize, orientation, position, frameCount, Success.RUN_ONLY)

    constructor(title: String, profile: Profile, major: Int, minor: Int,
                orientation: Vec2,
                success: Success = Success.MATCH_TEMPLATE)
            : this(title, profile, major, minor, Vec2i(640, 480), orientation, Vec2(0, 4), 2, success)

    constructor(title: String, profile: Profile, major: Int, minor: Int,
                heuristic: Heuristic)
            : this(title, profile, major, minor, Vec2i(640, 480), Vec2(0), Vec2(0, 4), 2, Success.MATCH_TEMPLATE, heuristic)

    val timeQueryName = intBufferBig(1)
    var timeSum = 0.0
    var timeMin = Double.MAX_VALUE
    var timeMax = 0.0
    var mouseOrigin = Vec2(windowSize shr 1)
    var mouseCurrent = Vec2(windowSize shr 1)
    var translationOrigin = Vec2(position)
    var translationCurrent = Vec2(position)
    var rotationOrigin = Vec2(orientation)
    var rotationCurrent = Vec2(orientation)
    var mouseButtonFlags = 0
    val keyPressed = BooleanArray(512, { false })
    var error = false

//    lateinit var caps: Caps

    abstract fun begin(): Boolean
    abstract fun render(): Boolean
    abstract fun end(): Boolean
    private fun endInternal(): Boolean {
        val result = end()
        window.dispose()
        return result
    }

    fun setup(): Boolean {

        if (window.handle == 0L)
            return EXIT_FAILURE

        var result = EXIT_SUCCESS

        if (version(major, minor) >= version(3, 0))
            result = if (checkGLVersion(major, minor)) EXIT_SUCCESS else EXIT_FAILURE

        if (result == EXIT_SUCCESS)
            result = if (begin()) EXIT_SUCCESS else EXIT_FAILURE

        var frameNum = frameCount

        while (result == EXIT_SUCCESS && !error) {

            result = if (render()) EXIT_SUCCESS else EXIT_FAILURE
            result = result && checkError("render")

            glfwPollEvents()
            if (window.shouldClose || (AUTOMATED_TESTS && frameCount == 0)) {
                if (success == Success.MATCH_TEMPLATE) {
                    if (!checkTemplate(window.handle, title))
                        result = EXIT_FAILURE
                    checkError("checkTemplate")
                }
                break
            }

            swap()

            if (AUTOMATED_TESTS) --frameNum
        }

        if (result == EXIT_SUCCESS)
            result = endInternal() && if (result == EXIT_SUCCESS) EXIT_SUCCESS else EXIT_FAILURE

        return if (success == Success.GENERATE_ERROR)
            if (result != EXIT_SUCCESS || error) EXIT_SUCCESS else EXIT_FAILURE
        else
            if (result == EXIT_SUCCESS || !error) EXIT_SUCCESS else EXIT_FAILURE
    }

    fun swap() = glfwSwapBuffers(window.handle)

    fun checkTemplate(pWindow: Long, title: String): Boolean {

        val coloryType = if (profile == Profile.ES) glGetInteger(GL_IMPLEMENTATION_COLOR_READ_TYPE) else GL_UNSIGNED_BYTE
        val coloryFormat = if (profile == Profile.ES) glGetInteger(GL_IMPLEMENTATION_COLOR_READ_FORMAT) else GL_RGBA

        return true // TODO
    }

    fun checkError(title: String): Boolean {

        val error = glGetError()
        if (error != GL_NO_ERROR) {
            val errorString = when (error) {
                GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                else -> "UNKNOWN"
            }
            println("OpenGL Error($errorString): $title")
            assert(false)
        }
        return error == GL_NO_ERROR
    }

    fun checkFramebuffer(framebufferName: Int): Boolean {
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        return if (status != GL_FRAMEBUFFER_COMPLETE) {
            println("OpenGL Error(${when (status) {
                GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT"
                GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"
                GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER"
                GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER"
                GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED"
                GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE"
                GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS -> "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS"
                else -> "GL_FRAMEBUFFER_UNDEFINED"
            }})")
            false
        } else true
    }

    fun view(): Mat4 {
        val viewTranslate = Mat4().translate(0f, 0f, -translationCurrent.y)
        val viewRotateX = viewTranslate.rotate(rotationCurrent.y, 1f, 0f, 0f)
        return viewRotateX.rotate(rotationCurrent.x, 0f, 1f, 0f)
    }

    enum class Heuristic(val i: Int) {
        EQUAL_BIT(1 shl 0),
        ABSOLUTE_DIFFERENCE_MAX_ONE_BIT(1 shl 1),
        ABSOLUTE_DIFFERENCE_MAX_ONE_KERNEL_BIT(1 shl 2),
        ABSOLUTE_DIFFERENCE_MAX_ONE_LARGE_KERNEL_BIT(1 shl 3),
        MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_ONE_BIT(1 shl 4),
        MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_FOUR_BIT(1 shl 5),
        MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_CHANNEL_BIT(1 shl 6),
        ALL(EQUAL_BIT or ABSOLUTE_DIFFERENCE_MAX_ONE_BIT or ABSOLUTE_DIFFERENCE_MAX_ONE_KERNEL_BIT or
                ABSOLUTE_DIFFERENCE_MAX_ONE_LARGE_KERNEL_BIT or MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_ONE_BIT or
                MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_FOUR_BIT);

        infix fun or(e: Heuristic) = i or e.i
    }

    enum class Vendor {DEFAULT, AMD, INTEL, NVIDIA, MAX }

    enum class SyncMode {VSYNC, ASYNC, TEARING }

    enum class Success {RUN_ONLY, GENERATE_ERROR, MATCH_TEMPLATE }

    fun version(major: Int, minor: Int) = major * 100 + minor * 10

    fun checkGLVersion(majorVersionRequire: Int, minorVersionRequire: Int): Boolean {
        val majorVersionContext = glGetInteger(GL_MAJOR_VERSION)
        val minorVersionContext = glGetInteger(GL_MAJOR_VERSION)
        println("OpenGL Version Needed $majorVersionRequire.$minorVersionRequire ( $majorVersionContext.$minorVersionContext Found )")
        return version(majorVersionContext, minorVersionContext) >= version(majorVersionContext, minorVersionContext)
    }

    object windowHint {
        var resizable = true
            set(value) = glfwWindowHint(GLFW_RESIZABLE, if (value) GLFW_TRUE else GLFW_FALSE)
        var visible = true
            set(value) = glfwWindowHint(GLFW_VISIBLE, if (value) GLFW_TRUE else GLFW_FALSE)
        var srgb = true
            set(value) = glfwWindowHint(GLFW_SRGB_CAPABLE, if (value) GLFW_TRUE else GLFW_FALSE)
        var decorated = true
            set(value) = glfwWindowHint(GLFW_DECORATED, if (value) GLFW_TRUE else GLFW_FALSE)
        var api = ""
            set(value) = glfwWindowHint(GLFW_CLIENT_API, when (value) {
                "gl" -> GLFW_OPENGL_API
                "es" -> GLFW_OPENGL_ES_API
                else -> GLFW_NO_API
            })
        var major = 0
            set(value) = glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, value)
        var minor = 0
            set(value) = glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, value)
        var profile = ""
            set(value) = glfwWindowHint(GLFW_OPENGL_PROFILE,
                    when (value) {
                        "core" -> GLFW_OPENGL_CORE_PROFILE
                        "compat" -> GLFW_OPENGL_COMPAT_PROFILE
                        else -> GLFW_OPENGL_ANY_PROFILE
                    })
        var forwardComp = true
            set(value) = glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, if (value) GLFW_TRUE else GLFW_FALSE)
        var debug = true
            set(value) = glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, if (value) GLFW_TRUE else GLFW_FALSE)
    }

    inner class MouseListener : GLFWMouseButtonCallbackI {
        override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
            when (action) {
                GLFW_PRESS -> {
                    mouseOrigin put mouseCurrent
                    when (button) {
                        GLFW_MOUSE_BUTTON_LEFT -> {
                            mouseButtonFlags = mouseButtonFlags or MouseButton.LEFT
                            translationOrigin put translationCurrent
                        }
                        GLFW_MOUSE_BUTTON_MIDDLE -> mouseButtonFlags = mouseButtonFlags or MouseButton.MIDDLE
                        GLFW_MOUSE_BUTTON_RIGHT -> {
                            mouseButtonFlags = mouseButtonFlags or MouseButton.RIGHT
                            rotationOrigin put rotationCurrent
                        }
                    }
                }
                GLFW_RELEASE -> when (button) {
                    GLFW_MOUSE_BUTTON_LEFT -> {
                        translationOrigin += (mouseCurrent - mouseOrigin) / 10f
                        mouseButtonFlags and MouseButton.LEFT.inv()
                    }
                    GLFW_MOUSE_BUTTON_MIDDLE -> mouseButtonFlags = mouseButtonFlags and MouseButton.MIDDLE.inv()
                    GLFW_MOUSE_BUTTON_RIGHT -> {
                        rotationOrigin += glm.radians(mouseCurrent - mouseOrigin)
                        mouseButtonFlags = mouseButtonFlags and MouseButton.RIGHT.inv()
                    }
                }
            }
        }
    }

    inner class KeyListener : GLFWKeyCallbackI {
        override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
            if (key < 0) return

            keyPressed[key] = action == Key.PRESS

            if (isKeyPressed(GLFW_KEY_ESCAPE))
                this@Test.window.shouldClose = true
        }
    }

    fun isKeyPressed(key: Int) = keyPressed[key]

    object MouseButton {
        val NONE = 0
        val LEFT = 1 shl 0
        val RIGHT = 1 shl 1
        val MIDDLE = 1 shl 2
    }

    object Key {
        val PRESS = GLFW_PRESS
        val RELEASE = GLFW_RELEASE
        val REPEAT = GLFW_REPEAT
    }

}

infix fun Int.or(e: Test.Heuristic) = this or e.i


class GlfwWindow(windowSize: Vec2i, title: String) {

    val x = intBufferBig(1)
    val y = intBufferBig(1)
    val handle = glfwCreateWindow(windowSize.x, windowSize.y, title, 0L, 0L)
    var shouldClose = false

    var pos = Vec2i()
        get() {
            glfwGetWindowPos(handle, x, y)
            return field.put(x[0], y[0])
        }
        set(value) = glfwSetWindowPos(handle, value.x, value.y)

    fun dispose() {
        destroyBuffers(x, y)
    }
}

val mat4Buffer = floatBufferBig(16)
val vec4Buffer = floatBufferBig(4)
val clearColor = floatBufferBig(4)
val clearDepth = floatBufferBig(1)

fun glUniform4fv(location: Int, vec4: Vec4) = glUniform4fv(location, vec4 to vec4Buffer)
fun glUniformMatrix4fv(location: Int, mat4: Mat4) = glUniformMatrix4fv(location, false, mat4 to mat4Buffer)

fun glCreatePrograms(programs: IntArray) = programs.forEachIndexed { i, it -> programs[i] = glCreateProgram() }
fun glDeletePrograms(programs: IntArray) = programs.forEachIndexed { i, it -> glDeleteProgram(programs[i]) }

fun glClearBufferfv(buffer: Int, drawBuffer: Int, value: Float) = glClearBufferfv(buffer, drawBuffer, clearDepth.put(0, value))
fun glClearBufferfv(buffer: Int, drawBuffer: Int, value: Vec4) = glClearBufferfv(buffer, drawBuffer, value to clearColor)
fun glViewport(size: Vec2i) = glViewport(0, 0, size.x, size.y)
fun glViewport(x:Int, y:Int, size: Vec2i) = glViewport(x, y, size.x, size.y)

fun generateIcosahedron(vertexData: MutableList<Vec3>, subdivision: Int) {

    //The golden ratio
    val t = (1 + glm.sqrt(5f)) / 2
    val size = 1.0f

    val A = glm.normalize(Vec3(-size, +t * size, 0.0f))    // 0
    val B = glm.normalize(Vec3(+size, +t * size, 0.0f))    // 1
    val C = glm.normalize(Vec3(-size, -t * size, 0.0f))    // 2
    val D = glm.normalize(Vec3(+size, -t * size, 0.0f))    // 3

    val E = glm.normalize(Vec3(0.0f, -size, +t * size))    // 4
    val F = glm.normalize(Vec3(0.0f, size, +t * size))    // 5
    val G = glm.normalize(Vec3(0.0f, -size, -t * size))    // 6
    val H = glm.normalize(Vec3(0.0f, size, -t * size))    // 7

    val I = glm.normalize(Vec3(+t * size, 0.0f, -size))    // 8
    val J = glm.normalize(Vec3(+t * size, 0.0f, size))    // 9
    val K = glm.normalize(Vec3(-t * size, 0.0f, -size))    // 10
    val L = glm.normalize(Vec3(-t * size, 0.0f, size))    // 11

    subdiviseIcosahedron(vertexData, A, L, F, subdivision)
    subdiviseIcosahedron(vertexData, A, F, B, subdivision)
    subdiviseIcosahedron(vertexData, A, B, H, subdivision)
    subdiviseIcosahedron(vertexData, A, H, K, subdivision)
    subdiviseIcosahedron(vertexData, A, K, L, subdivision)

    subdiviseIcosahedron(vertexData, B, F, J, subdivision)
    subdiviseIcosahedron(vertexData, F, L, E, subdivision)
    subdiviseIcosahedron(vertexData, L, K, C, subdivision)
    subdiviseIcosahedron(vertexData, K, H, G, subdivision)
    subdiviseIcosahedron(vertexData, H, B, I, subdivision)

    subdiviseIcosahedron(vertexData, D, J, E, subdivision)
    subdiviseIcosahedron(vertexData, D, E, C, subdivision)
    subdiviseIcosahedron(vertexData, D, C, G, subdivision)
    subdiviseIcosahedron(vertexData, D, G, I, subdivision)
    subdiviseIcosahedron(vertexData, D, I, J, subdivision)

    subdiviseIcosahedron(vertexData, E, J, F, subdivision)
    subdiviseIcosahedron(vertexData, C, E, L, subdivision)
    subdiviseIcosahedron(vertexData, G, C, K, subdivision)
    subdiviseIcosahedron(vertexData, I, G, H, subdivision)
    subdiviseIcosahedron(vertexData, J, I, B, subdivision)
}

fun subdiviseIcosahedron(vertexData: MutableList<Vec3>, a0: Vec3, b0: Vec3, c0: Vec3, subdivise: Int) {
    if (subdivise == 0) {
        vertexData.add(a0)
        vertexData.add(b0)
        vertexData.add(c0)
    } else {
        val a1 = (b0 + c0) * 0.5f
        val b1 = (c0 + a0) * 0.5f
        val c1 = (a0 + b0) * 0.5f

        if (a1.length() > 0f)
            a1.normalize_()
        if (b1.length() > 0f)
            b1.normalize_()
        if (c1.length() > 0f)
            c1.normalize_()

        subdiviseIcosahedron(vertexData, a0, b1, c1, subdivise - 1)
        subdiviseIcosahedron(vertexData, b0, c1, a1, subdivise - 1)
        subdiviseIcosahedron(vertexData, c0, a1, b1, subdivise - 1)
        subdiviseIcosahedron(vertexData, b1, a1, c1, subdivise - 1)
    }
}