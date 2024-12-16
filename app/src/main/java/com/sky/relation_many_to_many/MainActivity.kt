package com.sky.relation_many_to_many

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.sky.relation_many_to_many.ui.theme.Relation_many_to_manyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Relation_many_to_manyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                         many_to_many(this)
                }
            }
        }
    }
}


@Entity(tableName = "owner")
data class Owner(
    @PrimaryKey
    val ownerId: Int,
    val name: String
)


@Entity(tableName = "dog")
data class Dog(
    @PrimaryKey
    val dogId: Int,
    val name: String
)


@Entity(primaryKeys = ["ownerId", "dogId"])
data class OwnerDogCrossRef(
    val ownerId: Int,
    val dogId: Int
)

data class OwnerWithDogs(
    @Embedded val owner: Owner,
    @Relation(
        parentColumn = "ownerId",
        entityColumn = "dogId",
        associateBy = Junction(OwnerDogCrossRef::class)
    )
    val dogs: List<Dog>
)


data class DogWithOwners(
    @Embedded val dog: Dog,
    @Relation(
        parentColumn = "dogId",
        entityColumn = "ownerId",
        associateBy = Junction(OwnerDogCrossRef::class)
    )
    val owners: List<Owner>
)


@Dao
interface OwnerDogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertOwner(owner: Owner)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertDog(dog: Dog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertOwnerDogCrossRef(crossRef: OwnerDogCrossRef)


    @Transaction
    @Query("SELECT * FROM owner WHERE ownerId = :ownerId")
     fun getOwnerWithDogs(ownerId: Int): List<OwnerWithDogs>


    @Transaction
    @Query("SELECT * FROM dog WHERE dogId = :dogId")
     fun getDogWithOwners(dogId: Int): List<DogWithOwners>
}


@Database(
    entities = [Owner::class, Dog::class, OwnerDogCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class OwnerDogDatabase : RoomDatabase() {
    abstract fun ownerDogDao(): OwnerDogDao

    companion object {
        @Volatile
        private var INSTANCE: OwnerDogDatabase? = null

        fun getInstance(context: Context): OwnerDogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OwnerDogDatabase::class.java,
                    "owner_dog_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
}


class UserRepository(private val OwnerDogDao: OwnerDogDao) {

     fun insertOwner(owner: Owner){
        OwnerDogDao.insertOwner(owner)
    }

     fun insertDog(dog: Dog){
        OwnerDogDao.insertDog(dog)
    }

     fun insertOwnerDogCrossRef(crossRef: OwnerDogCrossRef){
        OwnerDogDao.insertOwnerDogCrossRef(crossRef)
    }

     fun getOwnerWithDogs(ownerId: Int): List<OwnerWithDogs>{
        return OwnerDogDao.getOwnerWithDogs(ownerId)
    }

     fun getDogWithOwners(dogId:Int):List<DogWithOwners>{
        return OwnerDogDao.getDogWithOwners(dogId)
    }

}


class vm(app:Application):AndroidViewModel(app) {
    var obj: UserRepository

    init {
        var instance = OwnerDogDatabase.getInstance(app).ownerDogDao()
        obj = UserRepository(instance)
    }

     fun insertOwner(owner: Owner){
        obj.insertOwner(owner)
    }

     fun insertDog(dog: Dog){
        obj.insertDog(dog)
    }

     fun insertOwnerDogCrossRef(crossRef: OwnerDogCrossRef){
        obj.insertOwnerDogCrossRef(crossRef)
    }

     fun getOwnerWithDogs(ownerId: Int): List<OwnerWithDogs>{
        return obj.getOwnerWithDogs(ownerId)
    }

     fun getDogWithOwners(dogId:Int):List<DogWithOwners>{
        return obj.getDogWithOwners(dogId)
    }

}
val owners = listOf(
    Owner(1, "Alice"),
    Owner(2, "Bob")
)
val dogs = listOf(
    Dog(1, "Rex"),
    Dog(2, "Buddy")
)
val ownerDogCrossRefs = listOf(
    OwnerDogCrossRef(1, 1), // Alice owns Rex
    OwnerDogCrossRef(1, 2), // Alice owns Buddy
    OwnerDogCrossRef(2, 1)  // Bob owns Rex
)



@Composable
fun many_to_many(mainActivity: MainActivity){


  var vm = ViewModelProvider(mainActivity)[vm::class.java]


    dogs.forEach {
        dog ->
        vm.insertDog(dog)
    }

    owners.forEach {
        vm.insertOwner(it)
    }

    ownerDogCrossRefs.forEach {
        vm.insertOwnerDogCrossRef(it)
    }

    var Dogslist by remember{
        mutableStateOf(vm.getOwnerWithDogs(1))
    }

    var Ownerslist by remember{
        mutableStateOf(vm.getDogWithOwners(1))
    }

    var ownerId by remember{
        mutableStateOf("")
    }

    var DogId by remember{
        mutableStateOf("")
    }

    LazyColumn(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally){
        item{
           OutlinedTextField(
               value = ownerId,
               onValueChange = {
                   ownerId = it
               },
               label = {
                   Text(text = "enter owner id")
               }
           )
            Button(onClick = {
                Dogslist = vm.getOwnerWithDogs(if(ownerId.isNotBlank() && ownerId.isDigitsOnly()) ownerId.toInt() else 0)
            }) {
                Text(text = "submit")
            }
        }
        itemsIndexed(Dogslist){index, it ->
            Text(text = "Owner name :-" +it.owner.name)
            Text("Dogs name list")
            it.dogs.forEach {
                Text(text = it.name)
            }
        }
        item{
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = DogId,
                onValueChange = {
                    DogId = it
                },
                label = {
                    Text(text = "enter dog id")
                }
            )
            Button(onClick = {
                Ownerslist = vm.getDogWithOwners(if(DogId.isNotBlank() && DogId.isDigitsOnly()) DogId.toInt() else 0)
            }) {
                Text(text = "submit")
            }
        }
        itemsIndexed(Ownerslist){index, it ->
            Text(text = "Dog name :-" +it.dog.name)
            Text("Owners name list")
            it.owners.forEach { owners ->
                Text(text = owners.name)
            }
        }
    }
}
