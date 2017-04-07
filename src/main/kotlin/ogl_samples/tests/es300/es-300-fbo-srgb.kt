//package ogl_samples.tests.es300
//
//import glm.BYTES
//import glm.mat.Mat4
//import glm.vec._2.Vec2
//import ogl_samples.framework.Test
//import uno.buffer.floatBufferOf
//import uno.buffer.intBufferBig
//import uno.buffer.intBufferOf
//import uno.caps.Caps
//
///**
// * Created by GBarbieri on 30.03.2017.
// */
//
//fun main(args: Array<String>) {
//    es_300_fbo_srgb().run()
//}
//
//class es_300_fbo_srgb : Test("es-300-fbo-srgb", Caps.Profile.ES, 3, 0) {
//
//    val SHADER_SOURCE_RENDER = "es-300/flat-srgb"
//    val SHADER_SOURCE_SPLASH = "es-300/fbo-srgb-blit"
//
//    val elementCount = 6
//    val elementSize = elementCount * Int.BYTES
//    val elementData = intBufferOf(
//            0, 1, 2,
//            0, 2, 3)
//
//    val vertexCount = 4
//    val positionSize = vertexCount * Vec2.SIZE
//    val positionData = floatBufferOf(
//            -1.0f, -1.0f,
//            +1.0f, -1.0f,
//            +1.0f, +1.0f,
//            -1.0f, +1.0f)
//
//    object buffer {
//        val VERTEX = 0
//        val ELEMENT = 1
//        val MAX = 2
//    }
//
//    val bufferName = intBufferBig(buffer.MAX)
//    val vertexArrayName = intBufferBig(1)
//    var programName = 0
//    var uniformMVP = 0
//    var uniformDiffuse = 0
//
//    val projection = Mat4()
//
//    val buffers = intBufferBig(1)
//
//    override fun begin(): Boolean{
//
//        initProgram()
//
////        initBuffer(this)
////
////        initVertexArray(this)
//    }
//
//    fun initProgram():Boolean {
//
//        var validated = true
//
//        val compiler = ogl_samples.framework.Compiler()
//
//        val shaderName = mutableListOf<Int>()
//
//        if(validated)
//        {
//            shaderName += compiler.create(this::class, SHADER_SOURCE_RENDER, "--version 300 --profile es")
//            ShaderName[shader::FRAG_RENDER] = Compiler.create(GL_FRAGMENT_SHADER, getDataDirectory() + FRAG_SHADER_SOURCE_RENDER, "--version 300 --profile es");
//
//            ProgramName[program::RENDER] = glCreateProgram();
//            glAttachShader(ProgramName[program::RENDER], ShaderName[shader::VERT_RENDER]);
//            glAttachShader(ProgramName[program::RENDER], ShaderName[shader::FRAG_RENDER]);
//            glLinkProgram(ProgramName[program::RENDER]);
//        }
//
//        if(validated)
//        {
//            ShaderName[shader::VERT_SPLASH] = Compiler.create(GL_VERTEX_SHADER, getDataDirectory() + VERT_SHADER_SOURCE_SPLASH, "--version 300 --profile es");
//            ShaderName[shader::FRAG_SPLASH] = Compiler.create(GL_FRAGMENT_SHADER, getDataDirectory() + FRAG_SHADER_SOURCE_SPLASH, "--version 300 --profile es");
//
//            ProgramName[program::SPLASH] = glCreateProgram();
//            glAttachShader(ProgramName[program::SPLASH], ShaderName[shader::VERT_SPLASH]);
//            glAttachShader(ProgramName[program::SPLASH], ShaderName[shader::FRAG_SPLASH]);
//            glLinkProgram(ProgramName[program::SPLASH]);
//        }
//
//        if(validated)
//        {
//            validated = validated && Compiler.check();
//            validated = validated && Compiler.check_program(ProgramName[program::RENDER]);
//            validated = validated && Compiler.check_program(ProgramName[program::SPLASH]);
//        }
//
//        if(validated)
//        {
//            UniformTransform = glGetUniformBlockIndex(ProgramName[program::RENDER], "transform");
//            UniformDiffuse = glGetUniformLocation(ProgramName[program::SPLASH], "Diffuse");
//
//            glUseProgram(ProgramName[program::RENDER]);
//            glUniformBlockBinding(ProgramName[program::RENDER], UniformTransform, semantic::uniform::TRANSFORM0);
//
//            glUseProgram(ProgramName[program::SPLASH]);
//            glUniform1i(UniformDiffuse, 0);
//        }
//
//        return validated && this->checkError("initProgram");
//    }
//}