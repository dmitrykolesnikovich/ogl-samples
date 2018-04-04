package oglSamples.framework

fun main(args: Array<String>) {

    AUTOMATED_TESTS = true

    oglSamples.tests.es200.es_200_draw_elements().loop()
}