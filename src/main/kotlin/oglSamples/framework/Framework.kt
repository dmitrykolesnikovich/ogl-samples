package oglSamples.framework

import gli_.Format
import gli_.Texture2d
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import gln.checkError
import gln.framebuffer.glBindFramebuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWMouseButtonCallbackI
import org.lwjgl.opengl.ARBES2Compatibility.GL_IMPLEMENTATION_COLOR_READ_FORMAT
import org.lwjgl.opengl.ARBES2Compatibility.GL_IMPLEMENTATION_COLOR_READ_TYPE
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import uno.buffer.intBufferBig
import uno.caps.Caps
import uno.caps.Caps.Profile
import uno.glfw.GlfwWindow
import uno.glfw.glfw

/**
 * Created by GBarbieri on 27.03.2017.
 */

val DEBUG = true
val APPLE = Platform.get() == Platform.MACOSX
val EXIT_SUCCESS = false
val EXIT_FAILURE = true
var AUTOMATED_TESTS = false

abstract class Framework(
        val title: String,
        val profile: Profile, val major: Int, val minor: Int,
        val windowSize: Vec2i = Vec2i(640, 480),
        orientation: Vec2 = Vec2(0),
        position: Vec2 = Vec2(0, 4),
        val frameCount: Int = 2,
        val success: Success = Success.MATCH_TEMPLATE,
        val heuristic: Heuristic = Heuristic.ALL) {

    val dataDirectory = "data/"

    init {
        assert(windowSize.x > 0 && windowSize.y > 0)

        glfw.init()

        glfw.windowHint {

            resizable = false
            visible = true
            srgb = false
            decorated = true
            val t = this@Framework
            api = if (t.profile == Profile.ES) "es" else "gl"

            if (version(t.major, t.minor) >= version(3, 2) || t.profile == Profile.ES) {
                context.version = "${t.major}.${t.minor}"
                if (APPLE) {
                    profile = "core"
                    forwardComp = true
                }
                if (t.profile != Profile.ES) {
                    profile = if (t.profile == Profile.CORE) "core" else "compat"
                    forwardComp = t.profile == Profile.CORE
                }
                debug = DEBUG
            }
        }
    }

    val window = run {
        val dpi = if (APPLE) 2 else 1
        GlfwWindow(windowSize / dpi, title)
    }

    init {

        if (window.handle != NULL)
            with(window) {

                pos = Vec2i(64)

                glfwSetMouseButtonCallback(handle, MouseButtonCallback())
                glfwSetKeyCallback(handle, KeyCallback())

                makeContextCurrent()
                GL.createCapabilities()
                show()
            }
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
    val translationOrigin = Vec2(position)
    val translationCurrent = Vec2(position)
    val rotationOrigin = Vec2(orientation)
    val rotationCurrent = Vec2(orientation)
    var mouseButtonFlags = 0
    val keyPressed = BooleanArray(512, { false })
    var error = false

    val caps = Caps(profile)

    init {
        glGetError()    // consume caps vec2 error, FIXME
    }

    abstract fun begin(): Boolean
    abstract fun render(): Boolean
    abstract fun end(): Boolean
    private fun endInternal(): Boolean {
        val result = end()
        window.destroy()
        return result
    }

    fun loop(): Boolean {

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

        val colorType = if (profile == Profile.ES) glGetInteger(GL_IMPLEMENTATION_COLOR_READ_TYPE) else GL_UNSIGNED_BYTE
        val colorFormat = if (profile == Profile.ES) glGetInteger(GL_IMPLEMENTATION_COLOR_READ_FORMAT) else GL_RGBA

        val windowSize = window.framebufferSize

        val textureRead = Texture2d(if (colorFormat == GL_RGBA) Format.RGBA8_UNORM_PACK8 else Format.RGB8_UNORM_PACK8, windowSize, 1)
        val textureRGB = Texture2d(Format.RGB8_UNORM_PACK8, windowSize, 1)

        glBindFramebuffer()
        glReadPixels(0, 0, windowSize.x, windowSize.y, colorFormat, colorType,
                if (textureRead.format == Format.RGBA8_UNORM_PACK8) textureRead.data() else textureRGB.data())

        if (textureRead.format == Format.RGBA8_UNORM_PACK8) {
            for (y in 0 until textureRGB.extent().y)
                for (x in 0 until textureRGB.extent().x) {

                    val texelCoord = Vec2i(x, y)

//                    val color = textureRead.load < glm::u8vec4 > (texelCoord, 0)
//                    textureRGB.store(texelCoord, 0, color)
                }
        }

        return true // TODO
    }

    fun checkFramebuffer(): Boolean {
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
                else -> "GL_FRAMEBUFFER_UNDEFINED" // TODO there is enum
            }})")
            false
        } else true
    }

    val view: Mat4
        get() {
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
        ALL(EQUAL_BIT or ABSOLUTE_DIFFERENCE_MAX_ONE_BIT or ABSOLUTE_DIFFERENCE_MAX_ONE_KERNEL_BIT.i or
                ABSOLUTE_DIFFERENCE_MAX_ONE_LARGE_KERNEL_BIT.i or MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_ONE_BIT.i or
                MIPMAPS_ABSOLUTE_DIFFERENCE_MAX_FOUR_BIT.i);

        infix fun or(b: Heuristic) = i or b.i
    }


    enum class Vendor { DEFAULT, AMD, INTEL, NVIDIA, MAX }

    enum class SyncMode { VSYNC, ASYNC, TEARING }

    enum class Success { RUN_ONLY, GENERATE_ERROR, MATCH_TEMPLATE }

    fun version(major: Int, minor: Int) = major * 100 + minor * 10

    fun checkGLVersion(majorVersionRequire: Int, minorVersionRequire: Int): Boolean {
        val majorVersionContext = glGetInteger(GL_MAJOR_VERSION)
        val minorVersionContext = glGetInteger(GL_MINOR_VERSION)
        val api = if (profile != Profile.ES) "OpenGL" else "OpenGL ES"
        println("$api Version Needed $majorVersionRequire.$minorVersionRequire ( $majorVersionContext.$minorVersionContext Found )")
        return version(majorVersionContext, minorVersionContext) >= version(majorVersionContext, minorVersionContext)
    }

    inner class MouseButtonCallback : GLFWMouseButtonCallbackI {
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

    inner class KeyCallback : GLFWKeyCallbackI {
        override fun invoke(windowHandle: Long, key: Int, scancode: Int, action: Int, mods: Int) {
            if (key < 0) return
            keyPressed[key] = action == Key.PRESS
            if (isKeyPressed(GLFW_KEY_ESCAPE))
                window.shouldClose = true
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