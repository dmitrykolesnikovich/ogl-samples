package ogl_samples.framework

import glm.L
import glm.vec._2.Vec2
import glm.vec._2.Vec2us
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import glm.vec._4.Vec4ub
import org.lwjgl.opengl.GL11

/**
 * Created by elect on 18/04/17.
 */


object glf {

    class v2fv2f {

        companion object {
            val SIZE = Vec2.SIZE * 2
        }
    }

    object pos4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec4.length, GL11.GL_FLOAT, false, Vec4.SIZE, 0))
    }

    object pos3_col4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec3.length, GL11.GL_FLOAT, false, Vec3.SIZE + Vec4.SIZE, 0),
                VertexAttribute(semantic.attr.COLOR, Vec4.length, GL11.GL_FLOAT, false, Vec3.SIZE + Vec4.SIZE, Vec4.SIZE.L))
    }

    object pos4_col4 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec4.length, GL11.GL_FLOAT, false, Vec4.SIZE * 2, 0),
                VertexAttribute(semantic.attr.COLOR, Vec4.length, GL11.GL_FLOAT, false, Vec4.SIZE * 2, Vec4.SIZE.L))
    }


    object pos2_tc2 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec2.length, GL11.GL_FLOAT, false, Vec2.SIZE * 2, 0),
                VertexAttribute(semantic.attr.TEXCOORD, Vec2.length, GL11.GL_FLOAT, false, Vec2.SIZE * 2, Vec2.SIZE.L))
    }

    object pos2us_tc2us : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec2us.length, GL11.GL_UNSIGNED_SHORT, false, Vec2us.SIZE * 2, 0),
                VertexAttribute(semantic.attr.TEXCOORD, Vec2us.length, GL11.GL_UNSIGNED_SHORT, false, Vec2us.SIZE * 2, Vec2us.SIZE.L))
    }

    object pos3_tc2 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec3.length, GL11.GL_FLOAT, false, Vec3.SIZE + Vec2.SIZE, 0),
                VertexAttribute(semantic.attr.TEXCOORD, Vec2.length, GL11.GL_FLOAT, false, Vec3.SIZE + Vec2.SIZE, Vec3.SIZE.L))
    }

    object pos3_col4ub : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec3.length, GL11.GL_FLOAT, false, Vec3.SIZE + Vec4ub.SIZE, 0),
                VertexAttribute(semantic.attr.COLOR, Vec4ub.length, GL11.GL_UNSIGNED_BYTE, false, Vec3.SIZE + Vec4ub.SIZE, Vec3.SIZE.L))
    }

    object pos2_tc3 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec2.length, GL11.GL_FLOAT, false, Vec2.SIZE + Vec3.SIZE, 0),
                VertexAttribute(semantic.attr.TEXCOORD, Vec3.length, GL11.GL_FLOAT, false, Vec2.SIZE + Vec3.SIZE, Vec2.SIZE.L))
    }

    object pos3_tc3 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec3.length, GL11.GL_FLOAT, false, Vec3.SIZE * 2, 0),
                VertexAttribute(semantic.attr.TEXCOORD, Vec3.length, GL11.GL_FLOAT, false, Vec3.SIZE * 2, Vec3.SIZE.L))
    }
}


interface VertexLayout {
    var attribute: Array<VertexAttribute>
    operator fun get(index: Int) = attribute[index]
}

class VertexAttribute(
        var index: Int,
        var size: Int,
        var type: Int,
        var normalized: Boolean,
        var interleavedStride: Int,
        var pointer: Long)