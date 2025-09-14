import kotlin.random.Random

class Human(
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

    fun move() {
        val angle = random.nextDouble() * 2 * Math.PI
        val step = random.nextDouble() * cspeed
        x += step * Math.cos(angle)
        y += step * Math.sin(angle)
        println("$name переместился в точку (${x}, ${y})")
    }

    override fun toString(): String {
        return "Human(fullName='$name', age=$age, speed=${cspeed}, " +
                "position=(${x}, ${y}))"
    }
}
fun main() {
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петров Петр Петрович", 30, 2.0),
        Human("Сидорова Анна Сергеевна", 28, 1.8),
        Human("Кузнецов Алексей Дмитриевич", 35, 2.2),
        Human("Смирнова Екатерина Владимировна", 22, 1.6),
    )

    val t = 5
    val timeStep = 1

    println("Начало симуляции движения людей")
    println("Время симуляции: ${t} секунд")
    println("=" * 50)

    for (second in 1..t) {
        println("\nСекунда $second:")
        println("-" * 30)
        humans.forEach { it.move() }
        Thread.sleep(500)
    }

    println("\n" + "=" * 50)
    println("Симуляция завершена!")
    println("\nФинальные позиции:")
    humans.forEach { println(it) }
}

operator fun String.times(n: Int): String {
    return this.repeat(n)
}
