package com.android.carinvestmanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.carinvestmanagement.data.Person
import com.android.carinvestmanagement.data.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonViewModel(private val repository: PersonRepository = PersonRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<PersonUiState>(PersonUiState.Loading)
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    private val _allPersons = MutableStateFlow<List<Person>>(emptyList())
    
    init {
        fetchPersons()
    }

    fun fetchPersons() {
        viewModelScope.launch {
            _uiState.value = PersonUiState.Loading
            val persons = repository.getAllPersons()
            _allPersons.value = persons
            if (persons.isEmpty()) {
                _uiState.value = PersonUiState.Error("Клиенты не найдены")
            } else {
                _uiState.value = PersonUiState.Success(persons)
            }
        }
    }

    fun searchPersons(query: String) {
        val persons = _allPersons.value
        if (query.isEmpty()) {
            _uiState.value = PersonUiState.Success(persons)
            return
        }
        
        val filtered = persons.filter {
            it.firstName.contains(query, ignoreCase = true) ||
            it.lastName.contains(query, ignoreCase = true) ||
            it.patronymic.contains(query, ignoreCase = true) ||
            it.documentNumber.contains(query, ignoreCase = true)
        }
        _uiState.value = PersonUiState.Success(filtered)
    }

    fun deletePerson(id: String) {
        viewModelScope.launch {
            val success = repository.deletePerson(id)
            if (success) {
                fetchPersons()
            }
        }
    }

    fun createPerson(person: Person) {
        viewModelScope.launch {
            val personId = repository.createPerson(person)
            if (personId != null) {
                fetchPersons()
            }
        }
    }
}

sealed class PersonUiState {
    object Loading : PersonUiState()
    data class Success(val persons: List<Person>) : PersonUiState()
    data class Error(val message: String) : PersonUiState()
}
