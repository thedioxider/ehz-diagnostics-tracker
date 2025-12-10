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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val PROFILE_DATABASE_NAME = "profile-db"

@Database(
    version = 2,
    entities = [
        Profile::class
    ]
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns
                db.execSQL("ALTER TABLE profiles ADD COLUMN surname TEXT NOT NULL DEFAULT ''")

                // Split existing name column into name and second_name
                db.execSQL(
                    """
                    UPDATE profiles 
                    SET surname = CASE 
                        WHEN instr(name, ' ') > 0
                        THEN substr(name, 1, instr(name, ' ') - 1)
                        ELSE ''
                    END,
                    name = CASE 
                        WHEN instr(name, ' ') > 0 
                        THEN substr(name, instr(name, ' ') + 1)
                        ELSE name
                    END
                """
                )
            }
        }
    }
}

@Entity(
    tableName = "profiles"
)
data class Profile(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "surname") val surname: String,
    @ColumnInfo(name = "hashword") val hashword: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0
) {
    companion object {
        fun checkName(name: String): Boolean = Regex("^[\\w-.]+$").matches(name)
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