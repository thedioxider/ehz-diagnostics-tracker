package space.diomentia.ehzdiagnosticstracker.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

const val PROFILE_DATABASE_NAME = "profile-db"

@Database(
    version = 1,
    entities = [
        Profile::class
    ]
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}

@Entity(
    tableName = "profiles"
)
data class Profile(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "hashword") val hashword: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0
) {
    companion object {
        fun checkName(name: String): Boolean = Regex("^\\w+( \\w+){1,2}$").matches(name)
    }

    init {
        if (!checkName(name)) throw IllegalArgumentException("wrong name format used")
    }
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getProfiles(): Flow<List<Profile>>

    @Insert
    fun createProfile(profile: Profile)

    @Update
    fun updateProfile(profile: Profile)

    @Delete
    fun deleteProfile(profile: Profile)
}