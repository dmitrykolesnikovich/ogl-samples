package ogl_samples.framework

fun main(args: Array<String>) {

    AUTOMATED_TESTS = true

    ogl_samples.tests.es200.es_200_draw_elements().loop()
}