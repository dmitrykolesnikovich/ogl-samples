//package ogl_samples.tests.gl300
//
//import gli.Texture2d
//import gli.gl
//import glm.vec._2.Vec2i
//import ogl_samples.framework.Test
//import ogl_samples.framework.semantic
//import ogl_samples.framework.Compiler
//import org.lwjgl.opengl.ARBFramebufferObject.*
//import org.lwjgl.opengl.GL11.*
//import org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
//import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
//import org.lwjgl.opengl.GL13.GL_TEXTURE0
//import org.lwjgl.opengl.GL13.glActiveTexture
//import org.lwjgl.opengl.GL15.*
//import org.lwjgl.opengl.GL20.*
//import uno.buffer.floatBufferOf
//import uno.buffer.intBufferBig
//import uno.caps.Caps
//import uno.glf.Vertex_v2fv2f
//
///**
// * Created by elect on 08/04/17.
// */
//
//fun main(args: Array<String>) {
//    gl_300_fbo_multisample().setup()
//}
//
//class gl_300_fbo_multisample : Test("gl-300-fbo-multisample", Caps.Profile.COMPATIBILITY, 3, 0) {
//
//    val SHADER_SOURCE = "gl-300/image-2d"
//    val TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds"
//    val FRAMEBUFFER_SIZE = Vec2i(160, 120)
//
//    // With DDS textures, v texture coordinate are reversed, from top to bottom
//    val vertexCount = 6
//    val vertexSize = vertexCount * Vertex_v2fv2f.SIZE
//    val vertexData = floatBufferOf(
//            -2.0f, -1.5f, /**/ 0.0f, 0.0f,
//            +2.0f, -1.5f, /**/ 1.0f, 0.0f,
//            +2.0f, +1.5f, /**/ 1.0f, 1.0f,
//            +2.0f, +1.5f, /**/ 1.0f, 1.0f,
//            -2.0f, +1.5f, /**/ 0.0f, 1.0f,
//            -2.0f, -1.5f, /**/ 0.0f, 0.0f)
//
//    object Texture {
//        val DIFFUSE = 0
//        val COLOR = 1
//        val MAX = 2
//    }
//
//    object Framebuffer {
//        val RENDER = 0
//        val RESOLVE = 1
//        val MAX = 2
//    }
//
//    var programName = 0
//    val vertexArrayName = intBufferBig(1)
//    val colorRenderbufferName = intBufferBig(1)
//    val bufferName = intBufferBig(1)
//    val textureName = intBufferBig(Texture.MAX)
//    val framebufferName = intBufferBig(Framebuffer.MAX)
//    var uniformMVP = -1
//    var uniformDiffuse = -1
//
//
//    fun initProgram(): Boolean {
//
//        var validated = true
//
//        val compiler = Compiler()
//
//        if (validated) {
//
//            val vertShaderName = compiler.create(this::class, SHADER_SOURCE + ".vert")
//            val fragShaderName = compiler.create(this::class, SHADER_SOURCE + ".frag")
//
//            val programName = glCreateProgram()
//            glAttachShader(programName, vertShaderName)
//            glAttachShader(programName, fragShaderName)
//
//            glBindAttribLocation(programName, semantic.attr.POSITION, "Position")
//            glBindAttribLocation(programName, semantic.attr.TEXCOORD, "Texcoord")
//            glLinkProgram(programName)
//
//            validated = validated && compiler.check()
//            validated = validated && compiler.checkProgram(programName)
//        }
//
//        if (validated) {
//            uniformMVP = glGetUniformLocation(programName, "MVP")
//            uniformDiffuse = glGetUniformLocation(programName, "Diffuse")
//        }
//
//        return validated && checkError("initProgram")
//    }
//
//    fun initBuffer(): Boolean {
//
//        glGenBuffers(bufferName)
//        glBindBuffer(GL_ARRAY_BUFFER, bufferName[0])
//        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
//        glBindBuffer(GL_ARRAY_BUFFER, 0)
//
//        return checkError("initBuffer")
//    }
//
//    fun initTexture(): Boolean {
//
//        val texture = Texture2d(gli.loadDDS(javaClass.getResource(TEXTURE_DIFFUSE).toURI()))
//        gl.profile = gl.Profile.GL32
//
//        glGenTextures(textureName)
//        glActiveTexture(GL_TEXTURE0)
//        glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE])
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
//
//        val format = gl.translate(texture.format, texture.swizzles)
//        for (level in 0..texture.levels())
//            glTexImage2D(GL_TEXTURE_2D, level,
//                    format.internal.i,
//                    texture[level].extent().x, texture[level].extent().y,
//                    0,
//                    format.external.i, format.type.i,
//                    texture[level].data())
//
//        texture.dispose()
//
//        return checkError("initTexture")
//    }
//
//    fun initFramebuffer():Boolean    {
//
//        glGenRenderbuffers(colorRenderbufferName)
//        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbufferName[0])
//        glRenderbufferStorageMultisample(GL_RENDERBUFFER, 8, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y)
//        // The second parameter is the number of samples.
//
//        glGenFramebuffers(framebufferName)
//
//        glBindFramebuffer(GL_FRAMEBUFFER, FramebufferRenderName)
//        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, ColorRenderbufferName)
//        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
//            return false
//        glBindFramebuffer(GL_FRAMEBUFFER, 0)
//
//        glGenTextures(1, &ColorTextureName)
//        glBindTexture(GL_TEXTURE_2D, ColorTextureName)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
//
//        glGenFramebuffers(1, &FramebufferResolveName)
//        glBindFramebuffer(GL_FRAMEBUFFER, FramebufferResolveName)
//        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ColorTextureName, 0)
//        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
//            return false
//        glBindFramebuffer(GL_FRAMEBUFFER, 0)
//
//        return this->checkError("initFramebuffer")
//    }
//
////    bool initVertexArray()
////    {
////        glGenVertexArrays(1, &VertexArrayName)
////        glBindVertexArray(VertexArrayName)
////        glBindBuffer(GL_ARRAY_BUFFER, BufferName)
////        glVertexAttribPointer(semantic::attr::POSITION, 2, GL_FLOAT, GL_FALSE, sizeof(glf::vertex_v2fv2f), BUFFER_OFFSET(0))
////        glVertexAttribPointer(semantic::attr::TEXCOORD, 2, GL_FLOAT, GL_FALSE, sizeof(glf::vertex_v2fv2f), BUFFER_OFFSET(sizeof(glm::vec2)))
////        glBindBuffer(GL_ARRAY_BUFFER, 0)
////
////        glEnableVertexAttribArray(semantic::attr::POSITION)
////        glEnableVertexAttribArray(semantic::attr::TEXCOORD)
////        glBindVertexArray(0)
////
////        return this->checkError("initVertexArray")
////    }
//}