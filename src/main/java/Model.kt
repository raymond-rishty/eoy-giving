import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAmount
import java.util.*

/**
 * Created by raymondrishty on 9/14/16.
 */

data class Envelope(val number: Int, val name: String)

data class Transaction(val date: LocalDate, val envelope: Int? = null, val name: String? = null, val amount: Double)
