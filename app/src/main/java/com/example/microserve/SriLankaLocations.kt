package com.example.microserve

object SriLankaLocations {

    data class Province(val name: String, val districts: List<District>)
    data class District(val name: String, val cities: List<String>)

    private val provinces = listOf(
        Province(
            "Western Province",
            listOf(
                District("Colombo", listOf("Colombo", "Dehiwala", "Moratuwa", "Sri Jayawardenepura Kotte", "Nugegoda")),
                District("Gampaha", listOf("Gampaha", "Negombo", "Kelaniya", "Wattala", "Ja-Ela")),
                District("Kalutara", listOf("Kalutara", "Panadura", "Horana", "Beruwala", "Matugama"))
            )
        ),
        Province(
            "Central Province",
            listOf(
                District("Kandy", listOf("Kandy", "Peradeniya", "Gampola", "Katugastota", "Akurana")),
                District("Matale", listOf("Matale", "Dambulla", "Sigiriya", "Galewela")),
                District("Nuwara Eliya", listOf("Nuwara Eliya", "Hatton", "Bandarawela", "Talawakele"))
            )
        ),
        Province(
            "Southern Province",
            listOf(
                District("Galle", listOf("Galle", "Hikkaduwa", "Ambalangoda", "Elpitiya")),
                District("Matara", listOf("Matara", "Weligama", "Akuressa", "Deniyaya")),
                District("Hambantota", listOf("Hambantota", "Tangalle", "Tissamaharama", "Ambalantota"))
            )
        ),
        Province(
            "Northern Province",
            listOf(
                District("Jaffna", listOf("Jaffna", "Nallur", "Chavakachcheri", "Point Pedro")),
                District("Kilinochchi", listOf("Kilinochchi", "Paranthan")),
                District("Mannar", listOf("Mannar", "Pesalai")),
                District("Vavuniya", listOf("Vavuniya", "Cheddikulam")),
                District("Mullaitivu", listOf("Mullaitivu", "Puthukudiyiruppu"))
            )
        ),
        Province(
            "Eastern Province",
            listOf(
                District("Batticaloa", listOf("Batticaloa", "Kalkudah", "Eravur")),
                District("Ampara", listOf("Ampara", "Kalmunai", "Sainthamaruthu")),
                District("Trincomalee", listOf("Trincomalee", "Kinniya", "Mutur"))
            )
        ),
        Province(
            "North Western Province",
            listOf(
                District("Kurunegala", listOf("Kurunegala", "Polgahawela", "Kuliyapitiya", "Narammala")),
                District("Puttalam", listOf("Puttalam", "Chilaw", "Wennappuwa", "Anamaduwa"))
            )
        ),
        Province(
            "North Central Province",
            listOf(
                District("Anuradhapura", listOf("Anuradhapura", "Medawachchiya", "Kekirawa", "Thambuttegama")),
                District("Polonnaruwa", listOf("Polonnaruwa", "Medirigiriya", "Hingurakgoda"))
            )
        ),
        Province(
            "Uva Province",
            listOf(
                District("Badulla", listOf("Badulla", "Bandarawela", "Hali-Ela", "Mahiyanganaya")),
                District("Monaragala", listOf("Monaragala", "Wellawaya", "Bibile", "Kataragama"))
            )
        ),
        Province(
            "Sabaragamuwa Province",
            listOf(
                District("Ratnapura", listOf("Ratnapura", "Embilipitiya", "Balangoda", "Kuruwita")),
                District("Kegalle", listOf("Kegalle", "Mawanella", "Warakapola", "Rambukkana"))
            )
        )
    )

    fun getProvinces(): List<String> = provinces.map { it.name }

    fun getDistricts(provinceName: String): List<String> {
        return provinces.firstOrNull { it.name == provinceName }?.districts?.map { it.name }.orEmpty()
    }

    fun getCities(provinceName: String, districtName: String): List<String> {
        val province = provinces.firstOrNull { it.name == provinceName } ?: return emptyList()
        return province.districts.firstOrNull { it.name == districtName }?.cities.orEmpty()
    }
}
