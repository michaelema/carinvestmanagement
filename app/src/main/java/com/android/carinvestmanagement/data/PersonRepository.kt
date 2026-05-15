package com.android.carinvestmanagement.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class PersonRepository {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun getAllPersons(): List<Person> {
        return try {
            val result = functions
                .getHttpsCallable("list_persons_oncall")
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? List<Map<String, Any>> ?: emptyList()

            data.map { map ->
                mapToPerson(map)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPerson(person: Person): String? {
        return try {
            val params = mutableMapOf<String, Any?>(
                "firstName" to person.firstName,
                "lastName" to person.lastName,
                "phoneNumber" to person.phoneNumber
            )
            addToMapNotEmpty(params, "patronymic", person.patronymic)
            addToMapNotEmpty(params, "dateOfBirth", person.dateOfBirth)
            addToMapNotEmpty(params, "address", person.address)
            addToMapNotEmpty(params, "documentNumber", person.documentNumber)
            addToMapNotEmpty(params, "dateOfIssue", person.dateOfIssue)
            addToMapNotEmpty(params, "issuingAuthority", person.issuingAuthority)
            addToMapNotEmpty(params, "documentScan", person.documentScan)
            addToMapNotEmpty(params, "ownerDescription", person.ownerDescription)
            addToMapNotEmpty(params, "messenger", person.messenger)

            val result = functions
                .getHttpsCallable("create_person_oncall")
                .call(params)
                .await()

            @Suppress("UNCHECKED_CAST")
            val responseData = result.data as? Map<String, Any>
            val rawId =
                responseData?.get("id") as? String ?: responseData?.get("person_id") as? String
            rawId?.substringAfterLast('/')
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPerson(id: String): Person? {
        return try {
            val result = functions
                .getHttpsCallable("get_person_oncall")
                .call(mapOf("person_id" to id))
                .await()

            @Suppress("UNCHECKED_CAST")
            val responseRootElement = result.data as? Map<String, Any> ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            val data = responseRootElement["person"] as? Map<String, Any> ?: return null
            mapToPerson(data)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updatePerson(person: Person): Boolean {
        return try {
            val params = mutableMapOf<String, Any?>(
                "person_id" to person.id,
                "firstName" to person.firstName,
                "lastName" to person.lastName,
                "phoneNumber" to person.phoneNumber
            )
            addToMapNotEmpty(params, "patronymic", person.patronymic)
            addToMapNotEmpty(params, "dateOfBirth", person.dateOfBirth)
            addToMapNotEmpty(params, "address", person.address)
            addToMapNotEmpty(params, "documentNumber", person.documentNumber)
            addToMapNotEmpty(params, "dateOfIssue", person.dateOfIssue)
            addToMapNotEmpty(params, "issuingAuthority", person.issuingAuthority)
            addToMapNotEmpty(params, "documentScan", person.documentScan)
            addToMapNotEmpty(params, "ownerDescription", person.ownerDescription)
            addToMapNotEmpty(params, "messenger", person.messenger)

            functions
                .getHttpsCallable("update_person_oncall")
                .call(params)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePerson(id: String): Boolean {
        return try {
            functions
                .getHttpsCallable("delete_person_oncall")
                .call(mapOf("person_id" to id))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }


    private fun mapToPerson(map: Map<String, Any>): Person {
        val rawId = map["id"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val messengerMap = map["messenger"] as? Map<String, String>
        return Person(
            id = rawId.substringAfterLast('/'),
            firstName = map["firstName"] as? String ?: "",
            lastName = map["lastName"] as? String ?: "",
            patronymic = map["patronymic"] as? String ?: "",
            dateOfBirth = map["dateOfBirth"] as? String,
            phoneNumber = map["phoneNumber"] as? String ?: "",
            address = map["address"] as? String ?: "",
            documentNumber = map["documentNumber"] as? String ?: "",
            dateOfIssue = map["dateOfIssue"] as? String ?: "",
            issuingAuthority = map["issuingAuthority"] as? String ?: "",
            documentScan = map["documentScan"] as? String ?: "",
            ownerDescription = map["ownerDescription"] as? String ?: "",
            messenger = messengerMap ?: emptyMap()
        )
    }

    private fun addToMapNotEmpty(map: MutableMap<String, Any?>, key: String, value: Any?) {
        if ((value != null) and (value.toString().isNotEmpty())) map[key] = value
    }
}