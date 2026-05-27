package com.example.microserve

/**
 * Sri Lanka administrative hierarchy for cascading location pickers.
 * Coordinates are approximate city/town centers for map default camera.
 */
object SriLankaLocations {

    data class City(val name: String, val latitude: Double, val longitude: Double)

    data class District(val name: String, val cities: List<City>)

    data class Province(val name: String, val districts: List<District>)

    val provinces: List<Province> = listOf(
        Province(
            "Western",
            listOf(
                District("Colombo", listOf(
                    City("Colombo", 6.9271, 79.8612),
                    City("Dehiwala-Mount Lavinia", 6.8518, 79.8617),
                    City("Moratuwa", 6.7730, 79.8816),
                    City("Sri Jayawardenepura Kotte", 6.8941, 79.9025)
                )),
                District("Gampaha", listOf(
                    City("Gampaha", 7.0917, 80.0081),
                    City("Negombo", 7.2088, 79.8358),
                    City("Kelaniya", 6.9497, 79.9196),
                    City("Wattala", 6.9890, 79.8910)
                )),
                District("Kalutara", listOf(
                    City("Kalutara", 6.5854, 79.9607),
                    City("Panadura", 6.7133, 79.9013),
                    City("Horana", 6.7150, 80.0626),
                    City("Beruwala", 6.4780, 79.9828)
                ))
            )
        ),
        Province(
            "Central",
            listOf(
                District("Kandy", listOf(
                    City("Kandy", 7.2906, 80.6337),
                    City("Peradeniya", 7.2694, 80.5950),
                    City("Gampola", 7.1647, 80.5765),
                    City("Katugastota", 7.2739, 80.6081)
                )),
                District("Matale", listOf(
                    City("Matale", 7.4675, 80.6234),
                    City("Dambulla", 7.8567, 80.6490),
                    City("Sigiriya", 7.9570, 80.7603)
                )),
                District("Nuwara Eliya", listOf(
                    City("Nuwara Eliya", 6.9497, 80.7891),
                    City("Hatton", 6.8918, 80.5955),
                    City("Bandarawela", 6.8290, 80.9982)
                ))
            )
        ),
        Province(
            "Southern",
            listOf(
                District("Galle", listOf(
                    City("Galle", 6.0535, 80.2210),
                    City("Hikkaduwa", 6.1395, 80.1065),
                    City("Ambalangoda", 6.2354, 80.0538)
                )),
                District("Matara", listOf(
                    City("Matara", 5.9549, 80.5550),
                    City("Weligama", 5.9739, 80.4297),
                    City("Dikwella", 5.9667, 80.6833)
                )),
                District("Hambantota", listOf(
                    City("Hambantota", 6.1241, 81.1185),
                    City("Tangalle", 6.0241, 80.7941),
                    City("Tissamaharama", 6.2796, 81.2865)
                ))
            )
        ),
        Province(
            "Northern",
            listOf(
                District("Jaffna", listOf(
                    City("Jaffna", 9.6615, 80.0255),
                    City("Chavakachcheri", 9.6670, 80.1610),
                    City("Point Pedro", 9.8167, 80.2333)
                )),
                District("Kilinochchi", listOf(
                    City("Kilinochchi", 9.3961, 80.4036)
                )),
                District("Mannar", listOf(
                    City("Mannar", 8.9810, 79.9045)
                )),
                District("Vavuniya", listOf(
                    City("Vavuniya", 8.7514, 80.4971)
                )),
                District("Mullaitivu", listOf(
                    City("Mullaitivu", 9.2671, 80.8142)
                ))
            )
        ),
        Province(
            "Eastern",
            listOf(
                District("Batticaloa", listOf(
                    City("Batticaloa", 7.7102, 81.6924),
                    City("Kalkudah", 7.9333, 81.5667)
                )),
                District("Ampara", listOf(
                    City("Ampara", 7.2976, 81.6728),
                    City("Kalmunai", 7.4167, 81.8167),
                    City("Arugam Bay", 6.8406, 81.8361)
                )),
                District("Trincomalee", listOf(
                    City("Trincomalee", 8.5874, 81.2152),
                    City("Nilaveli", 8.6927, 81.1885)
                ))
            )
        ),
        Province(
            "North Western",
            listOf(
                District("Kurunegala", listOf(
                    City("Kurunegala", 7.4818, 80.3650),
                    City("Kuliyapitiya", 7.4708, 80.0405),
                    City("Polgahawela", 7.3330, 80.3000)
                )),
                District("Puttalam", listOf(
                    City("Puttalam", 8.0362, 79.8283),
                    City("Chilaw", 7.5759, 79.7953),
                    City("Anamaduwa", 8.0333, 79.9833)
                ))
            )
        ),
        Province(
            "North Central",
            listOf(
                District("Anuradhapura", listOf(
                    City("Anuradhapura", 8.3114, 80.4037),
                    City("Medawachchiya", 8.5333, 80.3167)
                )),
                District("Polonnaruwa", listOf(
                    City("Polonnaruwa", 7.9403, 81.0188),
                    City("Hingurakgoda", 8.0333, 80.9500)
                ))
            )
        ),
        Province(
            "Uva",
            listOf(
                District("Badulla", listOf(
                    City("Badulla", 6.9934, 81.0550),
                    City("Bandarawela", 6.8290, 80.9982),
                    City("Ella", 6.8667, 81.0500)
                )),
                District("Monaragala", listOf(
                    City("Monaragala", 6.8728, 81.3507),
                    City("Wellawaya", 6.7369, 81.1028)
                ))
            )
        ),
        Province(
            "Sabaragamuwa",
            listOf(
                District("Ratnapura", listOf(
                    City("Ratnapura", 6.6828, 80.4037),
                    City("Balangoda", 6.6500, 80.7000),
                    City("Embilipitiya", 6.3500, 80.8500)
                )),
                District("Kegalle", listOf(
                    City("Kegalle", 7.2513, 80.3464),
                    City("Mawanella", 7.2500, 80.4500),
                    City("Warakapola", 7.2000, 80.2000)
                ))
            )
        )
    )

    fun districtsForProvince(provinceName: String): List<District> =
        provinces.firstOrNull { it.name == provinceName }?.districts.orEmpty()

    fun citiesForDistrict(provinceName: String, districtName: String): List<City> =
        districtsForProvince(provinceName)
            .firstOrNull { it.name == districtName }
            ?.cities
            .orEmpty()

    fun cityCoordinates(provinceName: String, districtName: String, cityName: String): Pair<Double, Double>? {
        val city = citiesForDistrict(provinceName, districtName).firstOrNull { it.name == cityName }
        return city?.let { it.latitude to it.longitude }
    }
}
