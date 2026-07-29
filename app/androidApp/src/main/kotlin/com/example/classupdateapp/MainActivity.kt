package com.example.classupdateapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

val availableSessions = listOf("2021-2022", "2022-2023", "2024-2025", "2025-2026")
val noticeCategories = listOf("Class Cancel/Change 🚨", "Exam Update 📝", "Assignment 📚", "General Notice 📢")

data class Notice(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val session: String = "",
    val postedBy: String = "",
    val type: String = "Class",
    val category: String = "General Notice 📢",
    val isImportant: Boolean = false,
    val university: String = "",
    val department: String = ""
)

data class User(
    val uid: String = "",
    val name: String = "",
    val role: String = "",
    val session: String = "",
    val regNo: String = "",
    val notifPreference: String = "All",
    val university: String = "",
    val department: String = ""
)

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("splash") }
                    var userRole by remember { mutableStateOf("") }
                    var userSession by remember { mutableStateOf("") }
                    var userNotifPref by remember { mutableStateOf("All") }
                    var userUniversity by remember { mutableStateOf("") }
                    var userDepartment by remember { mutableStateOf("") }
                    var currentUserId by remember { mutableStateOf("") }
                    var selectedChatPartner by remember { mutableStateOf<User?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    // ==================== SPLASH & AUTO LOGIN CHECK ====================
                    LaunchedEffect(Unit) {
                        delay(2000)

                        val auth = FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            currentUserId = currentUser.uid
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(currentUserId).get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        userRole = document.getString("role") ?: "Student"
                                        userSession = document.getString("session") ?: "2021-2022"
                                        userNotifPref = document.getString("notifPreference") ?: "All"
                                        userUniversity = document.getString("university") ?: ""
                                        userDepartment = document.getString("department") ?: ""
                                        currentScreen = if (userRole == "CR") "cr_dashboard" else "student_dashboard"
                                    } else {
                                        currentScreen = "login"
                                    }
                                }
                                .addOnFailureListener {
                                    currentScreen = "login"
                                }
                        } else {
                            currentScreen = "login"
                        }
                    }

                    if (isLoading) {
                        MarvelousLoadingScreen()
                    } else {
                        when (currentScreen) {
                            "splash" -> {
                                SplashScreenInterface()
                            }
                            "login" -> {
                                LoginScreen(
                                    onLoginSuccess = { regNoInput, password ->
                                        if (regNoInput.isNotBlank() && password.isNotBlank()) {
                                            isLoading = true
                                            try {
                                                val db = FirebaseFirestore.getInstance()
                                                val auth = FirebaseAuth.getInstance()

                                                db.collection("users")
                                                    .whereEqualTo("regNo", regNoInput.trim())
                                                    .get()
                                                    .addOnSuccessListener { querySnapshot ->
                                                        if (!querySnapshot.isEmpty) {
                                                            val document = querySnapshot.documents[0]
                                                            val userEmail = document.getString("email") ?: ""

                                                            auth.signInWithEmailAndPassword(userEmail, password)
                                                                .addOnCompleteListener { task ->
                                                                    isLoading = false
                                                                    if (task.isSuccessful) {
                                                                        currentUserId = auth.currentUser?.uid ?: ""
                                                                        userRole = document.getString("role") ?: "Student"
                                                                        userSession = document.getString("session") ?: "2021-2022"
                                                                        userNotifPref = document.getString("notifPreference") ?: "All"
                                                                        userUniversity = document.getString("university") ?: ""
                                                                        userDepartment = document.getString("department") ?: ""
                                                                        currentScreen = if (userRole == "CR") "cr_dashboard" else "student_dashboard"
                                                                    } else {
                                                                        Toast.makeText(context, "Incorrect Password!", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                        } else {
                                                            isLoading = false
                                                            Toast.makeText(context, "Registration Number not found!", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        isLoading = false
                                                        Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                            } catch (e: Exception) {
                                                isLoading = false
                                            }
                                        } else {
                                            Toast.makeText(context, "Please enter Registration No & Password", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNavigateToSignup = { currentScreen = "signup" }
                                )
                            }
                            "signup" -> {
                                SignUpScreen(
                                    onSignUpSuccess = { name, email, phone, password, regNo, uni, dept, selectedSession, role ->
                                        isLoading = true
                                        try {
                                            val auth = FirebaseAuth.getInstance()
                                            val db = FirebaseFirestore.getInstance()
                                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        currentUserId = auth.currentUser?.uid ?: ""
                                                        val userMap = hashMapOf(
                                                            "uid" to currentUserId,
                                                            "name" to name,
                                                            "email" to email.trim(),
                                                            "phone" to phone,
                                                            "regNo" to regNo.trim(),
                                                            "university" to uni.trim(),
                                                            "department" to dept.trim(),
                                                            "session" to selectedSession,
                                                            "role" to role,
                                                            "notifPreference" to "All"
                                                        )

                                                        db.collection("users").document(currentUserId).set(userMap)
                                                            .addOnSuccessListener {
                                                                isLoading = false
                                                                userRole = role
                                                                userSession = selectedSession
                                                                userUniversity = uni.trim()
                                                                userDepartment = dept.trim()
                                                                userNotifPref = "All"
                                                                currentScreen = if (role == "CR") "cr_dashboard" else "student_dashboard"
                                                            }
                                                            .addOnFailureListener { isLoading = false }
                                                    } else {
                                                        isLoading = false
                                                    }
                                                }
                                        } catch (e: Exception) {
                                            isLoading = false
                                        }
                                    },
                                    onNavigateToLogin = { currentScreen = "login" }
                                )
                            }
                            "cr_dashboard" -> {
                                CRDashboardScreen(
                                    currentUserId = currentUserId,
                                    session = userSession,
                                    university = userUniversity,
                                    department = userDepartment,
                                    onPostNotice = { title, desc, type, category, isImportant ->
                                        isLoading = true
                                        val db = FirebaseFirestore.getInstance()
                                        val noticeMap = hashMapOf(
                                            "title" to title,
                                            "description" to desc,
                                            "date" to java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                                            "session" to userSession,
                                            "university" to userUniversity,
                                            "department" to userDepartment,
                                            "postedBy" to "CR ($userSession)",
                                            "type" to type,
                                            "category" to category,
                                            "isImportant" to isImportant
                                        )
                                        db.collection("notices").add(noticeMap)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                Toast.makeText(context, "Notice Posted Successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { isLoading = false }
                                    },
                                    onOpenChat = { studentUser ->
                                        selectedChatPartner = studentUser
                                        currentScreen = "chat"
                                    },
                                    onLogout = {
                                        FirebaseAuth.getInstance().signOut()
                                        currentScreen = "login"
                                    }
                                )
                            }
                            "student_dashboard" -> {
                                StudentDashboardScreen(
                                    context = context,
                                    currentUserId = currentUserId,
                                    userSession = userSession,
                                    userUniversity = userUniversity,
                                    userDepartment = userDepartment,
                                    currentNotifPref = userNotifPref,
                                    onUpdateNotifPref = { newPref ->
                                        userNotifPref = newPref
                                        FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                                            .update("notifPreference", newPref)
                                        Toast.makeText(context, "Notification Preference Updated: $newPref", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenChatWithCR = { crUser ->
                                        selectedChatPartner = crUser
                                        currentScreen = "chat"
                                    },
                                    onLogout = {
                                        FirebaseAuth.getInstance().signOut()
                                        currentScreen = "login"
                                    }
                                )
                            }
                            "chat" -> {
                                selectedChatPartner?.let { partner ->
                                    MessengerChatScreen(
                                        currentUserId = currentUserId,
                                        partnerUser = partner,
                                        onBack = {
                                            currentScreen = if (userRole == "CR") "cr_dashboard" else "student_dashboard"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SPLASH SCREEN INTERFACE ====================
@Composable
fun SplashScreenInterface() {
    MarvelousBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF00D2FF), Color(0xFF0084FF)))),
                contentAlignment = Alignment.Center
            ) {
                Text("🎓", fontSize = 56.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Department Hub",
                fontSize = 34.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Smart Class & Communication Portal",
                fontSize = 14.sp,
                color = Color(0xFF00D2FF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = Color(0xFF00D2FF), modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        }
    }
}

// ==================== NOTIFICATION FUNCTIONS ====================
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "class_updates_channel",
            "Class Updates & Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for receiving instant class and exam alerts"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun sendLocalNotification(context: Context, title: String, message: String) {
    val builder = NotificationCompat.Builder(context, "class_updates_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
}

// ==================== BACKGROUND & LOADING ====================
@Composable
fun MarvelousBackground(content: @Composable BoxScope.() -> Unit) {
    val gradientColors = listOf(Color(0xFF090D16), Color(0xFF131B2E), Color(0xFF1E1035), Color(0xFF0A1128))
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(gradientColors))) { content() }
}

@Composable
fun MarvelousLoadingScreen() {
    MarvelousBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00D2FF), modifier = Modifier.size(60.dp), strokeWidth = 5.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Loading...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==================== LOGIN SCREEN ====================
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit, onNavigateToSignup: () -> Unit) {
    var regNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    MarvelousBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⚡", fontSize = 54.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Department Hub", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Smart Class & Communication Portal", fontSize = 13.sp, color = Color(0xFF8A99AD))
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = regNo, onValueChange = { regNo = it },
                        label = { Text("Registration No.", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password", color = Color.LightGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onLoginSuccess(regNo, password) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF))
                    ) {
                        Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToSignup) {
                Text("Don't have an account? Sign Up", color = Color(0xFF00D2FF), fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ==================== SIGNUP SCREEN ====================
@Composable
fun SignUpScreen(
    onSignUpSuccess: (name: String, email: String, phone: String, password: String, regNo: String, uni: String, dept: String, session: String, role: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }

    var selectedSession by remember { mutableStateOf(availableSessions[0]) }
    var selectedRole by remember { mutableStateOf("Student") }

    MarvelousBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text("Join Portal", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))

                        // New Fields for Global Platform
                        OutlinedTextField(value = university, onValueChange = { university = it }, label = { Text("University (e.g. DU, SUST)", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department (e.g. CSE, EEE)", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (Min 6 chars)", color = Color.LightGray) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = regNo, onValueChange = { regNo = it }, label = { Text("Reg No:", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Session:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            availableSessions.forEach { sess ->
                                FilterChip(
                                    selected = selectedSession == sess,
                                    onClick = { selectedSession = sess },
                                    label = { Text(sess, fontSize = 10.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Register As:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = selectedRole == "Student", onClick = { selectedRole = "Student" })
                            Text("Student", fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = selectedRole == "CR", onClick = { selectedRole = "CR" })
                            Text("Class Rep (CR)", fontSize = 14.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if(university.isNotBlank() && department.isNotBlank()) {
                                    onSignUpSuccess(name, email, phone, password, regNo, university, department, selectedSession, selectedRole)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF))
                        ) {
                            Text("SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Log In", color = Color(0xFF00D2FF)) }
            }
        }
    }
}

// ==================== CR DASHBOARD ====================
@Composable
fun CRDashboardScreen(
    currentUserId: String,
    session: String,
    university: String,
    department: String,
    onPostNotice: (title: String, desc: String, type: String, category: String, isImportant: Boolean) -> Unit,
    onOpenChat: (User) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var noticeType by remember { mutableStateOf("Class") }
    var selectedCategory by remember { mutableStateOf(noticeCategories[0]) }
    var isImportant by remember { mutableStateOf(false) }
    var messagedStudents by remember { mutableStateOf(listOf<User>()) }

    DisposableEffect(currentUserId) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("chats")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val studentIds = mutableSetOf<String>()
                    snapshot.documents.forEach { doc ->
                        val docId = doc.id
                        if (docId.contains(currentUserId)) {
                            val parts = docId.split("_")
                            for (part in parts) {
                                if (part != currentUserId) {
                                    studentIds.add(part)
                                }
                            }
                        }
                    }

                    if (studentIds.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("uid", studentIds.toList())
                            .addSnapshotListener { usersSnapshot, _ ->
                                if (usersSnapshot != null) {
                                    messagedStudents = usersSnapshot.documents.mapNotNull { it.toObject(User::class.java) }
                                }
                            }
                    } else {
                        messagedStudents = emptyList()
                    }
                }
            }
        onDispose { listener.remove() }
    }

    MarvelousBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CR Portal 📢", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("$university • $department", fontSize = 12.sp, color = Color(0xFF00D2FF))
                    Text("Session: $session", fontSize = 11.sp, color = Color.LightGray)
                }
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B))) {
                    Text("Logout", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Post Notice", color = Color.White) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Messages 💬", color = Color.White) })
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                LazyColumn {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Post Notice", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Notice Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(selected = noticeType == "Class", onClick = { noticeType = "Class" }, label = { Text("Class") })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(selected = noticeType == "Department", onClick = { noticeType = "Department" }, label = { Text("Dept 🏫") })
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Sub-Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                LazyColumn(modifier = Modifier.height(100.dp)) {
                                    items(noticeCategories) { cat ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable { selectedCategory = cat }.padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                                            Text(text = cat, color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isImportant, onCheckedChange = { isImportant = it })
                                    Text("Mark as URGENT / Important 🚨", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details & Deadline/Time", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth().height(100.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D2FF), unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (title.isNotEmpty() && description.isNotEmpty()) {
                                            onPostNotice(title, description, noticeType, selectedCategory, isImportant)
                                            title = ""
                                            description = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF))
                                ) {
                                    Text("Post Update", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            } else {
                if (messagedStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No direct messages yet!", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn {
                        items(messagedStudents) { student ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenChat(student) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF00D2FF)), contentAlignment = Alignment.Center) {
                                        Text(text = student.name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        Text(text = "Session: ${student.session} | Reg: ${student.regNo}", fontSize = 12.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== STUDENT DASHBOARD ====================
@Composable
fun StudentDashboardScreen(
    context: Context,
    currentUserId: String,
    userSession: String,
    userUniversity: String,
    userDepartment: String,
    currentNotifPref: String,
    onUpdateNotifPref: (String) -> Unit,
    onOpenChatWithCR: (User) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var noticeList by remember { mutableStateOf(listOf<Notice>()) }
    var crList by remember { mutableStateOf(listOf<User>()) }
    var isFirstLoad by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        // Filtering notices ONLY for this university and department
        val noticeListener = db.collection("notices")
            .whereEqualTo("university", userUniversity)
            .whereEqualTo("department", userDepartment)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val newList = snapshot.documents.mapNotNull { it.toObject(Notice::class.java) }.reversed()
                    if (!isFirstLoad && newList.size > noticeList.size) {
                        val latestNotice = newList.firstOrNull()
                        if (latestNotice != null) {
                            var shouldNotify = false
                            when (currentNotifPref) {
                                "All" -> shouldNotify = true
                                "Exam Only" -> if (latestNotice.category.contains("Exam")) shouldNotify = true
                                "Class Changes Only" -> if (latestNotice.category.contains("Cancel")) shouldNotify = true
                            }

                            if (shouldNotify) {
                                sendLocalNotification(
                                    context = context,
                                    title = if (latestNotice.isImportant) "🚨 URGENT: ${latestNotice.title}" else "📢 ${latestNotice.category}",
                                    message = latestNotice.description
                                )
                            }
                        }
                    }
                    noticeList = newList
                    isFirstLoad = false
                }
            }

        // Filtering CRs ONLY for this university and department
        val crListener = db.collection("users")
            .whereEqualTo("role", "CR")
            .whereEqualTo("university", userUniversity)
            .whereEqualTo("department", userDepartment)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    crList = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                }
            }

        onDispose {
            noticeListener.remove()
            crListener.remove()
        }
    }

    val filteredList = when (selectedTab) {
        0 -> noticeList.filter { it.type != "Department" && it.session == userSession }
        1 -> noticeList.filter { it.type != "Department" }
        2 -> noticeList.filter { it.type == "Department" }
        else -> emptyList()
    }

    MarvelousBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Class Updates 📌", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("$userUniversity • $userDepartment", fontSize = 12.sp, color = Color(0xFF00D2FF))
                    Text("Session: $userSession", fontSize = 11.sp, color = Color.LightGray)
                }
                Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B))) {
                    Text("Logout", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Running", color = Color.White, fontSize = 10.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("All", color = Color.White, fontSize = 10.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Dept 🏫", color = Color.White, fontSize = 10.sp) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Alerts ⚙️", color = Color.White, fontSize = 10.sp) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Chat 💬", color = Color.White, fontSize = 10.sp) })
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 3) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("🔕 Notification Preferences", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Choose what alerts you want to receive on phone:", fontSize = 12.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))

                        listOf("All", "Exam Only", "Class Changes Only").forEach { pref ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onUpdateNotifPref(pref) }.padding(vertical = 8.dp)
                            ) {
                                RadioButton(selected = currentNotifPref == pref, onClick = { onUpdateNotifPref(pref) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (pref) {
                                        "All" -> "🔔 All Notifications & Notices"
                                        "Exam Only" -> "📝 Only Exam & Test Updates"
                                        else -> "🚨 Only Class Cancel / Time Changes"
                                    },
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else if (selectedTab == 4) {
                LazyColumn {
                    items(crList) { cr ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenChatWithCR(cr) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF00D2FF)), contentAlignment = Alignment.Center) {
                                    Text(text = "⭐", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "CR: ${cr.name} (${cr.session})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Text(text = "Class Representative • Session: ${cr.session}", fontSize = 12.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            } else {
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notices found in this category!", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn {
                        items(filteredList) { notice ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notice.isImportant) Color(0xFF2E0000).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.09f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = notice.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Surface(
                                            color = if (notice.isImportant) Color(0xFFFF5252) else Color(0xFF00D2FF),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (notice.isImportant) "🚨 URGENT" else notice.category,
                                                color = Color.Black,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = notice.description, fontSize = 14.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Category: ${notice.category}", fontSize = 11.sp, color = Color(0xFF00D2FF))
                                        Text(text = "Posted: ${notice.date}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== MESSENGER CHAT SCREEN ====================
@Composable
fun MessengerChatScreen(
    currentUserId: String,
    partnerUser: User,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }

    val chatId = if (currentUserId < partnerUser.uid) "${currentUserId}_${partnerUser.uid}" else "${partnerUser.uid}_$currentUserId"

    DisposableEffect(chatId) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    chatMessages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                }
            }
        onDispose { listener.remove() }
    }

    MarvelousBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            Surface(
                color = Color(0xFF131B2E),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⬅️", fontSize = 20.sp, modifier = Modifier.clickable { onBack() })
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF00D2FF)), contentAlignment = Alignment.Center) {
                        Text(text = partnerUser.name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (partnerUser.role == "CR") "${partnerUser.name} (${partnerUser.session})" else partnerUser.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF66)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Active Now • ${partnerUser.role}", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(chatMessages) { msg ->
                    val isMe = msg.senderId == currentUserId
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) Color(0xFF0084FF) else Color(0xFF3E4042)
                            ),
                            shape = RoundedCornerShape(
                                topStart = 18.dp, topEnd = 18.dp,
                                bottomStart = if (isMe) 18.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 18.dp
                            )
                        ) {
                            Text(
                                text = msg.message,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Surface(
                color = Color(0xFF131B2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Aa", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF242A38),
                            unfocusedContainerColor = Color(0xFF242A38),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0084FF))
                            .clickable {
                                if (messageText.isNotBlank()) {
                                    val db = FirebaseFirestore.getInstance()
                                    val msgMap = hashMapOf(
                                        "senderId" to currentUserId,
                                        "receiverId" to partnerUser.uid,
                                        "message" to messageText.trim(),
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    db.collection("chats").document(chatId).collection("messages").add(msgMap)
                                    messageText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➔", fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }
    }
}