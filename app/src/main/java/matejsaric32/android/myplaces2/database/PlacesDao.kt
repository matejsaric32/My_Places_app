package matejsaric32.android.myplaces2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import matejsaric32.android.myplaces2.models.PlaceEntity

@Dao
interface PlacesDao {
    @Insert
    fun insertPlace(place: PlaceEntity)

    @Query("SELECT * FROM places")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id limit 1")
    fun getPlaceById(id: Int): Flow<PlaceEntity>

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("DELETE FROM places where id = :id")
    suspend fun deletePlaceById(id: Int)
}