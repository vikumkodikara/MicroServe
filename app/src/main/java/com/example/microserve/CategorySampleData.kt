package com.example.microserve

import android.content.Context

object CategorySampleData {

    private data class SampleEntry(
        val requesterName: String,
        val title: String,
        val description: String
    )

    private val samplesByCategoryId: Map<String, List<SampleEntry>> = mapOf(
        "plumbing" to listOf(
            SampleEntry("Sisira Kumara", "Paint house", "Paint house"),
            SampleEntry("Kulathunga Herath", "Tap & faucet installation", "Tap & faucet installation"),
            SampleEntry("Kavindya Sathsarani", "Bathroom fitting", "Bathroom fitting"),
            SampleEntry("Yohan Silva", "Blocked drain cleaning", "Blocked drain cleaning"),
            SampleEntry("Mihiri Katunayaka", "Pipe leak repair", "Pipe leak repair")
        ),
        "gardening" to listOf(
            SampleEntry("Nimal Perera", "Weed removal", "Weed removal from front garden"),
            SampleEntry("Sanduni Jayawardena", "Lawn mowing", "Weekly lawn mowing service"),
            SampleEntry("Ravi Fernando", "Tree trimming", "Trim overgrown trees in backyard")
        ),
        "cleaning" to listOf(
            SampleEntry("Anjali Silva", "Deep house cleaning", "Full home deep cleaning"),
            SampleEntry("Priya Mendis", "Office cleaning", "Weekly office cleaning"),
            SampleEntry("Dilshan Perera", "Carpet shampooing", "Living room carpet cleaning")
        ),
        "painting" to listOf(
            SampleEntry("Kasun Rajapaksa", "House repainting", "Full exterior house repainting"),
            SampleEntry("Malini Fernando", "Room painting", "Bedroom wall painting"),
            SampleEntry("Tharindu Wick", "Wood polishing", "Wooden door polishing")
        ),
        "electric" to listOf(
            SampleEntry("Sunil Bandara", "Wiring repair", "Fix faulty kitchen wiring"),
            SampleEntry("Chamari Perera", "Light installation", "Install ceiling lights"),
            SampleEntry("Nuwan Silva", "Socket replacement", "Replace damaged power sockets")
        ),
        "handyman" to listOf(
            SampleEntry("Roshan Perera", "Furniture assembly", "Assemble bedroom furniture"),
            SampleEntry("Lakmal Jayasinghe", "Door repair", "Fix squeaky front door"),
            SampleEntry("Ishara Kumari", "Shelf installation", "Install wall shelves")
        ),
        "hvac" to listOf(
            SampleEntry("Asanka Perera", "AC servicing", "Annual AC maintenance"),
            SampleEntry("Dinesh Fernando", "AC installation", "Install split AC unit"),
            SampleEntry("Gayani Silva", "Duct cleaning", "Clean ventilation ducts")
        ),
        "mechanic" to listOf(
            SampleEntry("Mahesh Rajapaksa", "Engine repair", "Car engine diagnostic and repair"),
            SampleEntry("Nadeeka Perera", "Brake service", "Replace brake pads"),
            SampleEntry("Kamal Silva", "Oil change", "Full oil and filter change")
        ),
        "carpentry" to listOf(
            SampleEntry("Suneth Perera", "Custom cabinets", "Kitchen cabinet installation"),
            SampleEntry("Harsha Mendis", "Door frame repair", "Repair damaged door frame"),
            SampleEntry("Piyumi Jay", "Deck building", "Build outdoor wooden deck")
        )
    )

    fun seedIfEmpty(context: Context, category: CategoryCatalog.Category) {
        if (RequestStore.hasPendingRequestsForCategory(context, category.storeKeys)) return

        val samples = samplesByCategoryId[category.id] ?: return
        val storeCategory = category.storeKeys.first()

        samples.forEach { sample ->
            RequestStore.addRequest(
                context = context,
                requesterName = sample.requesterName,
                title = sample.title,
                category = storeCategory,
                contact = "0770000000",
                location = "Colombo",
                description = sample.description
            )
        }
    }

    fun seedAllIfEmpty(context: Context) {
        CategoryCatalog.all.forEach { category ->
            seedIfEmpty(context, category)
        }
    }
}
