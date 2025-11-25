import kotlin.math.*
import kotlin.random.Random

open class Human(
    private var name: String,
    private var age: Int,
    private var cspeed: Double
) : Movable {
    
    override var x: Double = 0.0
        private set
    override var y: Double = 0.0
        private set
    override val speed: Double
        get() = cspeed
    
    private val random = Random

    fun getname(): String = name
    fun getage(): Int = age
    fun getspeed(): Double = cspeed

    fun setname(n: String) { name = n }
    fun setAge(a: Int) { age = a }
    fun setspeed(s: Double) { cspeed = s }

    override fun move() {
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
