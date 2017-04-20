package ogl_samples.framework

import glm.L
import glm.vec._2.Vec2
import glm.vec._2.Vec2us
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import glm.vec._4.Vec4ub
import glm.Glm.sqrt
import glm.glm
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

    object pos2 : VertexLayout {
        override var attribute = arrayOf(
                VertexAttribute(semantic.attr.POSITION, Vec2.length, GL11.GL_FLOAT, false, Vec2.SIZE, 0))
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

fun generateIcosahedron(subdivision: Int): List<Vec3> {

    //The golden ratio
    val t = (1 + sqrt(5f)) / 2
    val size = 1.0f

    val A = glm.normalize(Vec3(-size, t * size, 0.0f))    // 0
    val B = glm.normalize(Vec3(+size, t * size, 0.0f))    // 1
    val C = glm.normalize(Vec3(-size, -t * size, 0.0f))    // 2
    val D = glm.normalize(Vec3(+size, -t * size, 0.0f))    // 3

    val E = glm.normalize(Vec3(0.0f, -size, t * size))    // 4
    val F = glm.normalize(Vec3(0.0f, size, t * size))    // 5
    val G = glm.normalize(Vec3(0.0f, -size, -t * size))    // 6
    val H = glm.normalize(Vec3(0.0f, size, -t * size))    // 7

    val I = glm.normalize(Vec3(t * size, 0.0f, -size))    // 8
    val J = glm.normalize(Vec3(t * size, 0.0f, size))    // 9
    val K = glm.normalize(Vec3(-t * size, 0.0f, -size))    // 10
    val L = glm.normalize(Vec3(-t * size, 0.0f, size))    // 11

    val vertexData = ArrayList<Vec3>()

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

    return vertexData
}

fun subdiviseIcosahedron(vertexData: ArrayList<Vec3>, a0: Vec3, b0: Vec3, c0: Vec3, subdivise: Int) {

    if (subdivise == 0) {
        vertexData.add(a0)
        vertexData.add(b0)
        vertexData.add(c0)
    } else {
        val a1 = (b0 + c0) * 0.5f
        val b1 = (c0 + a0) * 0.5f
        val c1 = (a0 + b0) * 0.5f

        if (glm.length(a1) > 0.0f)
            a1.normalize_()
        if (glm.length(b1) > 0.0f)
            b1.normalize_()
        if (glm.length(c1) > 0.0f)
            c1.normalize_()

        subdiviseIcosahedron(vertexData, a0, b1, c1, subdivise - 1)
        subdiviseIcosahedron(vertexData, b0, c1, a1, subdivise - 1)
        subdiviseIcosahedron(vertexData, c0, a1, b1, subdivise - 1)
        subdiviseIcosahedron(vertexData, b1, a1, c1, subdivise - 1)
    }
}