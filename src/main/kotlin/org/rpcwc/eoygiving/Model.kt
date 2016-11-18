package org.rpcwc.eoygiving
import java.time.LocalDate

/**
 * Created by raymondrishty on 9/14/16.
 */

data class Envelope(val number: Int, val name: String)

data class Transaction(val date: LocalDate, val envelope: Int? = null, val name: String? = null, val amount: Double)

data class NamedAddress(val name: String, val address: String)

data class Report(val namedAddress: NamedAddress, val transactions: List<Transaction>)