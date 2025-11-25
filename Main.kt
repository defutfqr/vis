import kotlin.math.*
import kotlin.random.Random

open class Human(
    private var name: String,
    private var age: Int,
    private var cspeed: Double
) {
    private var x: Double = 0.0
    private var y: Double = 0.0
    private val random = Random

    fun getname(): String = name
    fun getage(): Int = age
    fun getspeed(): Double = cspeed
    fun getX(): Double = x
    fun getY(): Double = y

    fun setname(n: String) { name = n }
    fun setAge(a: Int) { age = a }
    fun setspeed(s: Double) { cspeed = s }

    open fun move() {
        val angle = random.nextDouble() * 2 * PI
        val step = random.nextDouble() * cspeed
        x += step * cos(angle)
        y += step * sin(angle)
        println("$name переместился в точку (${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }

    override fun toString(): String {
        return "Human(fullName='$name', age=$age, speed=${cspeed}, " +
                "position=(${"%.2f".format(x)}, ${"%.2f".format(y)}))"
    }
}

class Driver(
    name: String,
    age: Int,
    cspeed: Double,
    private var direction: Double = 0.0 
) : Human(name, age, cspeed) {

    fun setDirection(angle: Double) {
        direction = angle
    }
    override fun move() {
        val step = Random.nextDouble() * getspeed()
        val newX = getX() + step * cos(direction)
        val newY = getY() + step * sin(direction)
        setspeed(getspeed())
        println("$getname (водитель) движется прямо в точку (${"%.2f".format(newX)}, ${"%.2f".format(newY)})")
    }
}

fun main() {
    val human1 = Human("Иван Иванов", 25, 2.5)
    val human2 = Human("Мария Петрова", 30, 1.8)
    val human3 = Human("Алексей Сидоров", 22, 3.0)
    val human4 = Human("Елена Козлова", 28, 2.0)
    val driver = Driver("Петр Водителев", 35, 4.0, PI/4)
    val entities = listOf(human1, human2, human3, human4, driver)
    val threads = entities.map { entity ->
        Thread {
            repeat(5) {
                entity.move()
                Thread.sleep(500)
            }
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
    println("\nФинальные позиции:")
    entities.forEach { println(it) }
}
