import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class AddExpenseViewModel : ViewModel() {

    // StateFlow to hold the user's name. UI will observe this.
    // Start with a default value like "Loading..." to give user feedback.
    private val _userName = MutableStateFlow("Loading...")
    val userName: StateFlow<String> = _userName

    init {
        // Fetch the name as soon as the ViewModel is created
        fetchUserName()
    }

    private fun fetchUserName() {
        viewModelScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val phoneNumber = currentUser?.phoneNumber

            if (phoneNumber.isNullOrEmpty()) {
                // Handle case where user has no phone number
                // Fallback to DisplayName from auth provider (if available) or a default
                _userName.value = currentUser?.displayName ?: "Guest"
                return@launch
            }

            try {
                val firestore = FirebaseFirestore.getInstance()
                val document = firestore.collection("users").document(phoneNumber).get().await()

                if (document.exists()) {
                    // Successfully found the name in Firestore
                    val fetchedName = document.getString("name")
                    _userName.value = fetchedName ?: "User" // Use a default if 'name' field is null
                } else {
                    // Document doesn't exist, use a fallback
                    _userName.value = currentUser?.displayName ?: "User"
                }
            } catch (e: Exception) {
                // Handle errors during the fetch
                _userName.value = "User" // Fallback on error
                Log.e("AddExpenseViewModel", "Error fetching user name", e)
            }
        }
    }
}