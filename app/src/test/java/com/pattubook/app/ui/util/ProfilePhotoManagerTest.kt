package com.pattubook.app.ui.util

import com.pattubook.app.data.local.entity.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePhotoManagerTest {

    @Test
    fun personDefaultPhotoPathIsNull() {
        val person = Person(id = 1, name = "Test Person")
        assertNull(person.photoPath)
    }

    @Test
    fun personWithCustomPhotoPath() {
        val path = "profile_photos/person_1_12345.jpg"
        val person = Person(id = 1, name = "Test Person", photoPath = path)
        assertEquals(path, person.photoPath)
    }

    @Test
    fun personCopyPhotoPathRemoval() {
        val path = "profile_photos/person_1_12345.jpg"
        val person = Person(id = 1, name = "Test Person", photoPath = path)
        val updatedPerson = person.copy(photoPath = null)
        assertNull(updatedPerson.photoPath)
    }
}
