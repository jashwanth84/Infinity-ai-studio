package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.ChatMessageResponse
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessageResponse>() }
    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    
    var showFeedbackDialog by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var feedbackComment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    val agents by viewModel.agents.collectAsState()
                    var selectedAgent by remember { mutableStateOf<com.example.data.api.AiAgent?>(null) }
                    
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(selectedAgent?.name ?: "GPT-4o", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("GPT-4o (Default)") },
                                onClick = { 
                                    selectedAgent = null
                                    expanded = false
                                }
                            )
                            if (agents.isNotEmpty()) {
                                Divider()
                                agents.forEach { agent ->
                                    DropdownMenuItem(
                                        text = { Text(agent.name) },
                                        onClick = { 
                                            selectedAgent = agent
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* File Upload */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val userMessage = ChatMessageResponse(id = "user_${System.currentTimeMillis()}", text = messageText, timestamp = System.currentTimeMillis())
                                messages.add(userMessage)
                                val textToSend = messageText
                                messageText = ""
                                isGenerating = true
                                coroutineScope.launch {
                                    val response = viewModel.sendMessage("1", textToSend, "gpt-4")
                                    isGenerating = false
                                    response.onSuccess {
                                        messages.add(it)
                                    }.onFailure {
                                        messages.add(ChatMessageResponse("err", "Error communicating with backend.", System.currentTimeMillis()))
                                    }
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() && !isGenerating
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (isGenerating) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp))
                }
            }
            items(messages.reversed()) { msg ->
                val isUser = msg.id.startsWith("user")
                MessageBubble(
                    message = msg.text, 
                    isUser = isUser,
                    onFeedback = { rating ->
                        showFeedbackDialog = Pair(msg.id, rating)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Start a conversation", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        showFeedbackDialog?.let { (msgId, rating) ->
            AlertDialog(
                onDismissRequest = { 
                    showFeedbackDialog = null 
                    feedbackComment = ""
                },
                title = { Text("Provide Feedback", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Would you like to add any comments to your rating?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = feedbackComment,
                            onValueChange = { feedbackComment = it },
                            placeholder = { Text("Optional comments...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.submitFeedback(msgId, rating, feedbackComment.takeIf { it.isNotBlank() })
                        showFeedbackDialog = null
                        feedbackComment = ""
                    }) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.submitFeedback(msgId, rating, null)
                        showFeedbackDialog = null
                        feedbackComment = ""
                    }) {
                        Text("Skip")
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: String, 
    isUser: Boolean,
    onFeedback: ((Int) -> Unit)? = null // 1 for up, -1 for down
) {
    var feedbackState by remember { mutableStateOf(0) } // 0=none, 1=up, -1=down

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = if (isUser) 0.dp else 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(16.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    text = message,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        if (!isUser) {
            Row(
                modifier = Modifier
                    .padding(start = 40.dp, top = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = { 
                        if (feedbackState != 1) {
                            feedbackState = 1
                            onFeedback?.invoke(1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (feedbackState == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Thumbs Up",
                        modifier = Modifier.size(16.dp),
                        tint = if (feedbackState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { 
                        if (feedbackState != -1) {
                            feedbackState = -1
                            onFeedback?.invoke(-1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (feedbackState == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Thumbs Down",
                        modifier = Modifier.size(16.dp),
                        tint = if (feedbackState == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
