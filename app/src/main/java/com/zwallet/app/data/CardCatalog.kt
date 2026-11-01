package com.zwallet.app.data

/**
 * Katalog bank dan varian kartu.
 *
 * Gambar kartu dibaca dari  app/src/main/assets/cards/<bank.id>.(png|webp|jpg)
 * Logo bank dibaca dari     app/src/main/assets/logos/<bank.id>.(png|webp|jpg)
 * Jika file belum ada, UI menampilkan placeholder (bukan gambar kartu tiruan).
 */
data class Bank(val id: String, val name: String, val initials: String)

data class CardVariant(val id: String, val bankId: String, val name: String, val description: String)

object CardCatalog {
    val banks = listOf(
        Bank("bca", "BCA", "BCA"),
        Bank("mandiri", "Bank Mandiri", "MDR"),
        Bank("bri", "BRI", "BRI"),
        Bank("bni", "BNI", "BNI"),
        Bank("cimb", "CIMB Niaga", "CIMB"),
        Bank("danamon", "Danamon", "DNM"),
        Bank("lainnya", "Bank lainnya", "•••")
    )

    // Satu gambar kartu per bank. ID varian = ID bank = nama file di assets/cards.
    val variants = listOf(
        CardVariant("bca", "bca", "BCA Xpresi", "Kartu debit virtual BCA."),
        CardVariant("mandiri", "mandiri", "Kartu Debit Mandiri", "Kartu debit virtual Bank Mandiri."),
        CardVariant("bri", "bri", "Kartu Debit BRI", "Kartu debit virtual BRI."),
        CardVariant("bni", "bni", "Kartu Debit BNI", "Kartu debit virtual BNI."),
        CardVariant("cimb", "cimb", "Kartu Debit CIMB Niaga", "Kartu debit virtual CIMB Niaga."),
        CardVariant("danamon", "danamon", "Kartu Debit Danamon", "Kartu debit virtual Danamon."),
        CardVariant("lainnya", "lainnya", "Kartu Debit Umum", "Untuk bank yang belum ada di daftar.")
    )

    fun bank(id: String): Bank? = banks.firstOrNull { it.id == id }
    fun variant(id: String): CardVariant? = variants.firstOrNull { it.id == id }
    fun variantsOf(bankId: String): List<CardVariant> = variants.filter { it.bankId == bankId }
}
