package uz.ttpu.hiltlab.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

/**
 * Q1/Q2/Q7: manual DI without Hilt.
 * HardCar creates its own dependency, SoftCar receives it from outside.
 */
class ManualDiTest {

    open class Engine { open fun start() = "vroom" }

    class HardCar {
        val engine = Engine()
    }

    class SoftCar(private val engine: Engine) {
        fun start() = engine.start()
    }

    class ServiceLocator {
        val engine = Engine()
    }

    @Test
    fun hardCarBuildsItsOwnEngine() {
        assertEquals("vroom", HardCar().engine.start())
    }

    @Test
    fun softCarAcceptsAnyEngine() {
        val fake = object : Engine() {
            override fun start() = "fake"
        }
        assertEquals("fake", SoftCar(fake).start())
    }

    @Test
    fun serviceLocatorHidesTheDependency() {
        val first = ServiceLocator().engine
        val second = ServiceLocator().engine
        assertNotSame(first, second)
    }
}
