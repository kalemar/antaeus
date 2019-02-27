package io.pleo.antaeus.core.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

internal class SchedulesTest {
    private val pragueZoneId = ZoneId.of("Europe/Prague")
    private val notFirstDayOfMonth = LocalDate.of(2018, 11, 21).atStartOfDay(pragueZoneId)
    private val firstDayOfMonth = LocalDate.of(2018, 3, 1).atStartOfDay(pragueZoneId)

    @Test
    fun `everyFirstDayOfMonth must return next first day of month when it does not start on first day of month`() {
        //Arrange
        val schedule = Schedules.everyFirstDayOfMonth(startingFrom = notFirstDayOfMonth)

        //Act
        val first = schedule.firstOrNull()!!

        //Assert
        val nextFirstDayOfMonth = LocalDate.of(2018, 12, 1).atStartOfDay(pragueZoneId)
        assertThat(first).isEqualTo(nextFirstDayOfMonth)
    }

    @Test
    fun `everyFirstDayOfMonth must return current first day of month when it starts on the first day of month`() {
        //Arrange
        val schedule = Schedules.everyFirstDayOfMonth(startingFrom = firstDayOfMonth)

        //Act
        val first = schedule.firstOrNull()!!

        //Assert
        assertThat(first).isEqualTo(firstDayOfMonth)
    }

    @Test
    fun `everyFirstDayOfMonth must return first days of three consecutive months`() {
        //Arrange
        val schedule = Schedules.everyFirstDayOfMonth(startingFrom = notFirstDayOfMonth)

        //Act
        val iterator = schedule.iterator()
        val first = iterator.next()
        val second = iterator.next()
        val third = iterator.next()

        //Assert
        val firstNextFirstDay = LocalDate.of(2018, 12, 1).atStartOfDay(pragueZoneId)
        val secondNextFirstDay = LocalDate.of(2019, 1, 1).atStartOfDay(pragueZoneId)
        val thirdNextFirstDay = LocalDate.of(2019, 2, 1).atStartOfDay(pragueZoneId)
        assertThat(first).isEqualTo(firstNextFirstDay)
        assertThat(second).isEqualTo(secondNextFirstDay)
        assertThat(third).isEqualTo(thirdNextFirstDay)
    }
}
