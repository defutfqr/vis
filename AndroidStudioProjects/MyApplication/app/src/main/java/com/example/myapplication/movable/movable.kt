import kotlin.math.cos
import kotlin.math.sin

interface Movable {
    val x: Double
    val y: Double
    val speed: Double
    
    fun move()
    fun getPosition(): Pair<Double, Double> = Pair(x, y)
}

