import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
        x += step * cos(direction)
        y += step * sin(direction)
        setspeed(getspeed())
        println("$getname (водитель) движется прямо в точку (${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }
}
