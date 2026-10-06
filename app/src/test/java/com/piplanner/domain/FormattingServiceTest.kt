package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class FormattingServiceTest {

    private lateinit var formattingService: FormattingService

    @Before
    fun setUp() {
        formattingService = FormattingService()
    }

    @Test
    fun formatInrFromPaisa_formatsOneLakh() {
        // 1,00,000 rupees = 10_000_000 paisa
        assertThat(formattingService.formatInrFromPaisa(10_000_000L)).isEqualTo("₹1,00,000")
    }

    @Test
    fun formatInrFromPaisa_formatsThirteenLakhPlus() {
        // PRD R20 example: ₹13,10,796
        assertThat(formattingService.formatInrFromPaisa(131_079_600L)).isEqualTo("₹13,10,796")
    }

    @Test
    fun formatInrFromPaisa_formatsZero() {
        assertThat(formattingService.formatInrFromPaisa(0L)).isEqualTo("₹0")
    }

    @Test
    fun formatInrFromPaisa_formatsUnderOneThousand() {
        assertThat(formattingService.formatInrFromPaisa(99_900L)).isEqualTo("₹999")
    }

    @Test
    fun formatInrFromPaisa_formatsThousandsWithIndianGrouping() {
        assertThat(formattingService.formatInrFromPaisa(1_234_500L)).isEqualTo("₹12,345")
    }

    @Test
    fun formatInrFromPaisa_dropsPaiseFraction() {
        // 10050 paisa = ₹100.50 → whole rupees ₹100
        assertThat(formattingService.formatInrFromPaisa(10_050L)).isEqualTo("₹100")
    }

    @Test
    fun formatInrFromRupees_formatsCroreScale() {
        assertThat(formattingService.formatInrFromRupees(1_00_00_000L)).isEqualTo("₹1,00,00,000")
    }
}
